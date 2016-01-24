/*
 * Copyright (C) 2015 Brian Dupras
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.duprasville.guava.probably;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.hash.Funnel;
import com.google.common.primitives.SignedBytes;

import com.duprasville.guava.probably.cuckoo.CuckooFilterStrategies;
import com.duprasville.guava.probably.cuckoo.CuckooTable;
import com.duprasville.guava.probably.cuckoo.Strategy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.math.DoubleMath.log2;
import static com.google.common.math.LongMath.divide;
import static java.lang.Math.ceil;
import static java.math.RoundingMode.CEILING;
import static java.math.RoundingMode.HALF_DOWN;

/**
 * A Cuckoo filter for instances of {@code T}. A Cuckoo filter offers an approximate containment
 * test with one-sided error: if it claims that an element is contained in it, this might be in
 * error, but if it claims that an element is <i>not</i> contained in it, then this is definitely
 * true. <p/> <p>The false positive probability ({@code FPP}) of a cuckoo filter is defined as the
 * probability that {@link #contains(Object)} will erroneously return {@code true} for an object
 * that has not actually been added to the {@link CuckooFilter}. <p/> <p>Cuckoo filters are
 * serializable. They also support a more compact serial representation via the {@link
 * #writeTo(OutputStream)} and {@link #readFrom(InputStream, Funnel)} methods. Both serialized forms
 * will continue to be supported by future versions of this library. However, serial forms generated
 * by newer versions of the code may not be readable by older versions of the code (e.g., a
 * serialized cuckoo filter generated today may <i>not</i> be readable by a binary that was compiled
 * 6 months ago). <p/> ref: <i>Cuckoo Filter: Practically Better Than Bloom</i> Bin Fan, David G.
 * Andersen, Michael Kaminsky†, Michael D. Mitzenmacher‡ Carnegie Mellon University, †Intel Labs,
 * ‡Harvard University https://www.cs.cmu.edu/~dga/papers/cuckoo-conext2014.pdf
 *
 * @param <T> the type of objects that the {@link CuckooFilter} accepts
 * @author Brian Dupras
 * @author Alex Beal
 */
@Beta
public final class CuckooFilter<T> implements ProbabilisticFilter<T>, Serializable {
  /**
   * Minimum false positive probability supported, 8.67E-19.
   */
  public static double MIN_FPP = 2.0D * 8 / Math.pow(2, Long.SIZE); // 8 is max entries per bucket

  /**
   * Maximum false positive probability supported, 0.99.
   */
  public static double MAX_FPP = 0.99D;

  /**
   * Combines this cuckoo filter with another cuckoo filter by performing multiset sum of the
   * underlying data. The mutations happen to <b>this</b> instance. Callers must ensure the cuckoo
   * filters are appropriately sized to avoid saturating them. The behavior of this operation is
   * undefined if the specified filter is modified while the operation is in progress.
   *
   * @param f The cuckoo filter to combine this cuckoo filter with. It is not mutated.
   * @return {@code true} if the filters are successfully summed.
   * @throws IllegalArgumentException if {@link #isCompatible(ProbabilisticFilter)}{@code == false}
   */
  public boolean addAll(ProbabilisticFilter<T> f) {
    checkNotNull(f);
    checkArgument(this != f, "Cannot combine a " + this.getClass().getSimpleName() +
        " with itself.");
    checkCompatibility(f, "combine");
    return this.strategy.addAll(this.table, ((CuckooFilter) f).table);
  }

  private void checkCompatibility(ProbabilisticFilter<T> f, String verb) {
    checkArgument(f instanceof CuckooFilter, "Cannot" + verb + " a " +
        this.getClass().getSimpleName() + " with a " + f.getClass().getSimpleName());
    checkArgument(this.isCompatible(f), "Cannot" + verb + " incompatible filters. " +
        this.getClass().getSimpleName() + " instances must have equivalent funnels; the same " +
        "strategy; and the same number of buckets, entries per bucket, and bits per entry.");
  }

  /**
   * Adds all of the elements in the specified collection to the filter. Some elements of {@code c}
   * may have been added to the filter even when {@code false} is returned. In this case, the caller
   * may {@link #remove(Object)} the additions by comparing the filter {@link #size()} before and
   * after the invocation, knowing that additions from {@code c} occurred in {@code c}'s iteration
   * order. The behavior of this operation is undefined if the specified collection is modified
   * while the operation is in progress.
   *
   * @return {@code true} if all elements of the collection were successfully added
   */
  public boolean addAll(Collection<? extends T> c) {
    for (T e : c) {
      if (!add(e)) {
        return false;
      }
    }
    return true;
  }

  public void clear() {
    table.clear();
  }

  private final CuckooTable table;
  private final Funnel<? super T> funnel;
  private final Strategy strategy;
  private final long capacity;
  private final double fpp;

  /**
   * Creates a CuckooFilter.
   */
  private CuckooFilter(
      CuckooTable table, Funnel<? super T> funnel, Strategy strategy, long capacity, double fpp) {
    this.capacity = capacity;
    this.fpp = fpp;
    this.table = checkNotNull(table);
    this.funnel = checkNotNull(funnel);
    this.strategy = checkNotNull(strategy);
  }

  /**
   * Creates a new {@link CuckooFilter} that's a copy of this instance. The new instance is equal to
   * this instance but shares no mutable state.
   */
  @CheckReturnValue
  public CuckooFilter<T> copy() {
    return new CuckooFilter<T>(table.copy(), funnel, strategy, capacity, fpp);
  }

  /**
   * Returns {@code true} if the object <i>might</i> have been added to this Cuckoo filter, {@code
   * false} if this is <i>definitely</i> not the case.
   */
  @CheckReturnValue
  public boolean contains(T e) {
    return strategy.contains(e, funnel, table);
  }

  /**
   * Returns {@code true} if all elements of the given collection <i>might</i> have been added to
   * this Cuckoo filter, {@code false} if this is <i>definitely</i> not the case.
   */
  public boolean containsAll(Collection<? extends T> c) {
    for (T o : c) {
      if (!contains(o)) return false;
    }
    return true;
  }

  public boolean containsAll(ProbabilisticFilter<T> f) {
    checkNotNull(f);
    if (this == f) {
      return true;
    }
    checkCompatibility(f, "compare");
    return this.strategy.containsAll(this.table, ((CuckooFilter) f).table);
  }

  /**
   * Adds an object into this {@link CuckooFilter}. Ensures that subsequent invocations of {@link
   * #contains(Object)} with the same object will always return {@code true}.
   *
   * @return true if {@code e} has been successfully added to the filter. false if {@code e} was not
   * added to the filter, as would be the case when the filter gets saturated. This may occur even
   * if actualInsertions < capacity. e.g. If {@code e} has already been added 2*b times to the
   * filter, a subsequent attempt will fail.
   */
  @CheckReturnValue
  public boolean add(T e) {
    return strategy.add(e, funnel, table);
  }

  /**
   * Removes {@code e} from this {@link CuckooFilter}. {@code e} must have been previously added to
   * the filter. Removing an {@code e} that hasn't been added to the filter may put the filter in an
   * inconsistent state causing it to return false negative responses from {@link
   * #contains(Object)}. <p/> If {@code false} is returned, this is <i>definitely</i> an indication
   * that either this invocation or a previous invocation has been made without a matching
   * invocation of {@link #add(Object)}. This condition is always an error and this {@link
   * CuckooFilter} can no longer be relied upon to return correct {@code false} responses from
   * {@link #contains(Object)}.
   *
   * @return true if {@code e} was successfully removed from the filter.
   */
  @CheckReturnValue
  public boolean remove(T e) {
    return strategy.remove(e, funnel, table);
  }

  /**
   * Removes all elements of {@code c} from this {@link CuckooFilter}. Each element of {@code c}
   * must represented in the filter before invocation. Removing an element that hasn't been added to
   * the filter may put the filter in an inconsistent state causing it to return false negative
   * responses from {@link #contains(Object)}. <p/> If {@code false} is returned, this is
   * <i>definitely</i> an indication that {@code c} contained at least one element that was not
   * represented in the filter. This condition is always an error and this {@link CuckooFilter} can
   * no longer be relied upon to return correct {@code false} responses from {@link
   * #contains(Object)}.
   *
   * @return true if {@code e} was successfully removed from the filter.
   */
  @CheckReturnValue
  public boolean removeAll(Collection<? extends T> c) {
    for (T e : c) {
      if (!remove(e)) {
        return false;
      }
    }
    return true;
  }

  public boolean removeAll(ProbabilisticFilter<T> f) {
    checkNotNull(f);
    if (this == f) {
      clear();
      return true;
    }
    checkCompatibility(f, "remove");
    return this.strategy.removeAll(this.table, ((CuckooFilter) f).table);
  }

  /**
   * Returns the number of inserted items currently represented in the filter.
   */
  public long size() {
    return table.size();
  }

  public long capacity() {
    return capacity;
  }

  public double fpp() {
    return fpp;
  }

  public boolean isEmpty() {
    return 0 == size();
  }

  /**
   * Returns the number of bits in the underlying cuckoo table.
   */
  @VisibleForTesting
  long bitSize() {
    return table.bitSize();
  }


  /**
   * Returns the probability that {@link #contains(Object)} will erroneously return {@code true} for
   * an object that has not actually been added to the {@link CuckooFilter}. <p/> <p>This number
   * should be close to the {@code fpp} parameter passed in when the filter was created, or smaller.
   * Unlike a bloom filter, a cuckoo filter cannot become saturated to the point of significantly
   * degrading its {@code FPP}.
   */
  public double currentFpp() {
    return table.currentFpp();
  }

  /**
   * Returns {@code true} if {@code f} is compatible with {@code this} filter. {@code f} is
   * considered compatible if {@code this} filter can use it in combinatoric operations (e.g. {@link
   * #addAll(ProbabilisticFilter)}).
   *
   * @param f The filter to check for compatibility.
   * @return {@code true} if {@code f} is compatible with {@code this} filter.
   */
  public boolean isCompatible(ProbabilisticFilter<T> f) {
    checkNotNull(f);

    return (this != f)
        && (f instanceof CuckooFilter)
        && (this.table.isCompatible(((CuckooFilter) f).table))
        && (this.strategy.equals(((CuckooFilter) f).strategy))
        && (this.funnel.equals(((CuckooFilter) f).funnel));
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object == this) {
      return true;
    }
    if (object instanceof CuckooFilter) {
      CuckooFilter<?> that = (CuckooFilter<?>) object;
      return this.funnel.equals(that.funnel)
          && this.strategy.equals(that.strategy)
          && this.table.equals(that.table)
          && this.strategy.equivalent(this.table, that.table)
          ;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(funnel, strategy, table);
  }

  /**
   * Creates a {@link CuckooFilter CuckooFilter<T>} with the expected number of insertions and
   * expected false positive probability. <p/> <p>Note that overflowing a {@link CuckooFilter} with
   * significantly more objects than specified, will result in its saturation causing {@link
   * #add(Object)} to reject new additions. <p/> <p>The constructed {@link CuckooFilter} will be
   * serializable if the provided {@code Funnel<T>} is. <p/> <p>It is recommended that the funnel be
   * implemented as a Java enum. This has the benefit of ensuring proper serialization and
   * deserialization, which is important since {@link #equals} also relies on object identity of
   * funnels.
   *
   * @param funnel   the funnel of T's that the constructed {@link CuckooFilter} will use
   * @param capacity the number of expected insertions to the constructed {@link CuckooFilter}; must
   *                 be positive
   * @param fpp      the desired false positive probability (must be positive and less than 1.0)
   * @return a {@link CuckooFilter}
   */
  @CheckReturnValue
  public static <T> CuckooFilter<T> create(
      Funnel<? super T> funnel, int capacity, double fpp) {
    return create(funnel, (long) capacity, fpp);
  }

  /**
   * Creates a {@link CuckooFilter CuckooFilter<T>} with the expected number of insertions and
   * expected false positive probability. <p/> <p>Note that overflowing a {@link CuckooFilter} with
   * significantly more objects than specified, will result in its saturation causing {@link
   * #add(Object)} to reject new additions. <p/> <p>The constructed {@link CuckooFilter} will be
   * serializable if the provided {@code Funnel<T>} is. <p/> <p>It is recommended that the funnel be
   * implemented as a Java enum. This has the benefit of ensuring proper serialization and
   * deserialization, which is important since {@link #equals} also relies on object identity of
   * funnels.
   *
   * @param funnel   the funnel of T's that the constructed {@link CuckooFilter} will use
   * @param capacity the number of expected insertions to the constructed {@link CuckooFilter}; must
   *                 be positive
   * @param fpp      the desired false positive probability (must be positive and less than 1.0).
   * @return a {@link CuckooFilter}
   */
  @CheckReturnValue
  public static <T> CuckooFilter<T> create(
      Funnel<? super T> funnel, long capacity, double fpp) {
    return create(funnel, capacity, fpp,
        CuckooFilterStrategies.MURMUR128_BEALDUPRAS_32.strategy());
  }

  @VisibleForTesting
  static <T> CuckooFilter<T> create(Funnel<? super T> funnel, long capacity, double fpp,
                                    Strategy strategy) {
    checkNotNull(funnel);
    checkArgument(capacity > 0, "Expected insertions (%s) must be > 0",
        capacity);
    checkArgument(fpp > 0.0D, "False positive probability (%s) must be > 0.0", fpp);
    checkArgument(fpp < 1.0D, "False positive probability (%s) must be < 1.0", fpp);
    checkNotNull(strategy);

    int numEntriesPerBucket = optimalEntriesPerBucket(fpp);
    long numBuckets = optimalNumberOfBuckets(capacity, numEntriesPerBucket);
    int numBitsPerEntry = optimalBitsPerEntry(fpp, numEntriesPerBucket);

    try {
      return new CuckooFilter<T>(new CuckooTable(numBuckets,
          numEntriesPerBucket, numBitsPerEntry), funnel, strategy, capacity, fpp);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Could not create CuckooFilter of " + numBuckets +
          " buckets, " + numEntriesPerBucket + " entries per bucket, " + numBitsPerEntry +
          " bits per entry", e);
    }
  }

  /**
   * Creates a {@link CuckooFilter CuckooFilter<T>} with the expected number of insertions and a
   * default expected false positive probability of 3.2%. <p/> <p>Note that overflowing a {@code
   * CuckooFilter} with significantly more objects than specified, will result in its saturation
   * causing {@link #add(Object)} to reject new additions. <p/> <p>The constructed {@link
   * CuckooFilter} will be serializable if the provided {@code Funnel<T>} is. <p/> <p>It is
   * recommended that the funnel be implemented as a Java enum. This has the benefit of ensuring
   * proper serialization and deserialization, which is important since {@link #equals} also relies
   * on object identity of funnels.
   *
   * @param funnel   the funnel of T's that the constructed {@link CuckooFilter} will use
   * @param capacity the number of expected insertions to the constructed {@link CuckooFilter}; must
   *                 be positive
   * @return a {@link CuckooFilter}
   */
  @CheckReturnValue
  public static <T> CuckooFilter<T> create(Funnel<? super T> funnel, int capacity) {
    return create(funnel, (long) capacity);
  }

  /**
   * Creates a {@link CuckooFilter CuckooFilter<T>} with the expected number of insertions and a
   * default expected false positive probability of 3.2%. <p/> <p>Note that overflowing a {@code
   * CuckooFilter} with significantly more objects than specified, will result in its saturation
   * causing {@link #add(Object)} to reject new additions. <p/> <p>The constructed {@link
   * CuckooFilter} will be serializable if the provided {@code Funnel<T>} is. <p/> <p>It is
   * recommended that the funnel be implemented as a Java enum. This has the benefit of ensuring
   * proper serialization and deserialization, which is important since {@link #equals} also relies
   * on object identity of funnels.
   *
   * @param funnel   the funnel of T's that the constructed {@link CuckooFilter} will use
   * @param capacity the number of expected insertions to the constructed {@link CuckooFilter}; must
   *                 be positive
   * @return a {@link CuckooFilter}
   */
  @CheckReturnValue
  public static <T> CuckooFilter<T> create(Funnel<? super T> funnel, long capacity) {
    return create(funnel, capacity, 0.032D);
  }

  /*
   * Space optimization cheat sheet, per CuckooFilter § 5.1 :
   *
   * Given:
   *   n: expected insertions
   *   e: expected false positive probability (e.g. 0.03D for 3% fpp)
   *
   * Choose:
   *   b: bucket size in entries (2, 4, 8)
   *   a: load factor (proportional to b)
   *
   * Calculate:
   *   f: fingerprint size in bits
   *   m: table size in buckets
   *
   *
   * 1) Choose b =     8   | 4 |   2
   *      when e : 0.00001 < e ≤ 0.002
   *      ref: CuckooFilter § 5.1 ¶ 5, "Optimal bucket size"
   *
   * 2) Choose a =  50% | 84% | 95.5% | 98%
   *      when b =   1  |  2  |  4    |  8
   *      ref: CuckooFilter § 5.1 ¶ 2, "(1) Larger buckets improve table occupancy"
   *
   * 2) Optimal f = ceil( log2(2b/e) )
   *    ref: CuckooFilter § 5.1 Eq. (6), "f ≥ log2(2b/e) = [log2(1/e) + log2(2b)]"
   *
   * 3) Required m = evenCeil( ceiling( ceiling( n/a ) / b ) )
   *       Minimum entries (B) = n/a rounded up
   *       Minimum buckets (m) = B/b rounded up to an even number
   */

  /**
   * Returns the optimal number of entries per bucket, or bucket size, ({@code b}) given the
   * expected false positive probability ({@code e}).
   *
   * CuckooFilter § 5.1 ¶ 5, "Optimal bucket size"
   *
   * @param e the desired false positive probability (must be positive and less than 1.0)
   * @return optimal number of entries per bucket
   */
  @VisibleForTesting
  static int optimalEntriesPerBucket(double e) {
    checkArgument(e > 0.0D, "e must be > 0.0");
    if (e <= 0.00001) {
      return 8;
    } else if (e <= 0.002) {
      return 4;
    } else {
      return 2;
    }
  }

  /**
   * Returns the optimal load factor ({@code a}) given the number of entries per bucket ({@code
   * b}).
   *
   * CuckooFilter § 5.1 ¶ 2, "(1) Larger buckets improve table occupancy"
   *
   * @param b number of entries per bucket
   * @return load factor, positive and less than 1.0
   */
  @VisibleForTesting
  static double optimalLoadFactor(int b) {
    checkArgument(b == 2 || b == 4 || b == 8, "b must be 2, 4, or 8");
    if (b == 2) {
      return 0.84D;
    } else if (b == 4) {
      return 0.955D;
    } else {
      return 0.98D;
    }
  }

  /**
   * Returns the optimal number of bits per entry ({@code f}) given the false positive probability
   * ({@code e}) and the number of entries per bucket ({@code b}).
   *
   * CuckooFilter § 5.1 Eq. (6), "f ≥ log2(2b/e) = [log2(1/e) + log2(2b)]"
   *
   * @param e the desired false positive probability (must be positive and less than 1.0)
   * @param b number of entries per bucket
   * @return number of bits per entry
   */
  @VisibleForTesting
  static int optimalBitsPerEntry(double e, int b) {
    checkArgument(e >= MIN_FPP, "Cannot create CuckooFilter with FPP[" + e +
        "] < CuckooFilter.MIN_FPP[" + CuckooFilter.MIN_FPP + "]");
    return log2(2 * b / e, HALF_DOWN);
  }

  /**
   * Returns the minimal required number of buckets given the expected insertions {@code n}, and the
   * number of entries per bucket ({@code b}).
   *
   * @param n the number of expected insertions
   * @param b number of entries per bucket
   * @return number of buckets
   */
  @VisibleForTesting
  static long optimalNumberOfBuckets(long n, int b) {
    checkArgument(n > 0, "n must be > 0");
    return evenCeil(divide((long) ceil(n / optimalLoadFactor(b)), b, CEILING));
  }

  static long evenCeil(long n) {
    return (n + 1) / 2 * 2;
  }

  private Object writeReplace() {
    return new SerialForm<T>(this);
  }

  private static class SerialForm<T> implements Serializable {
    final long[] data;
    final long size;
    final long checksum;
    final long numBuckets;
    final int numEntriesPerBucket;
    final int numBitsPerEntry;
    final Funnel<? super T> funnel;
    final Strategy strategy;
    final long capacity;
    final double fpp;

    SerialForm(CuckooFilter<T> filter) {
      this.data = filter.table.data();
      this.numBuckets = filter.table.numBuckets();
      this.numEntriesPerBucket = filter.table.numEntriesPerBucket();
      this.numBitsPerEntry = filter.table.numBitsPerEntry();
      this.size = filter.table.size();
      this.checksum = filter.table.checksum();
      this.funnel = filter.funnel;
      this.strategy = filter.strategy;
      this.capacity = filter.capacity;
      this.fpp = filter.fpp;
    }

    Object readResolve() {
      return new CuckooFilter<T>(
          new CuckooTable(data, size, checksum, numBuckets, numEntriesPerBucket, numBitsPerEntry),
          funnel, strategy, capacity, fpp);
    }

    private static final long serialVersionUID = 1;
  }

  /**
   * Writes this {@link CuckooFilter} to an output stream, with a custom format (not Java
   * serialization). This has been measured to save at least 400 bytes compared to regular
   * serialization. <p/> <p>Use {@link #readFrom(InputStream, Funnel)} to reconstruct the written
   * CuckooFilter.
   */
  public void writeTo(OutputStream out) throws IOException {
    /*
     * Serial form:
     * 1 signed byte for the strategy
     * 1 big endian long, the number of capacity
     * 1 IEEE 754 floating-point double, the expected FPP
     * 1 big endian long, the number of entries in our filter
     * 1 big endian long, the checksum of entries in our filter
     * 1 big endian long for the number of buckets
     * 1 big endian int for the number of entries per bucket
     * 1 big endian int for the fingerprint size in bits
     * 1 big endian int, the number of longs in our table's data
     * N big endian longs of our table's data
     */
    DataOutputStream dout = new DataOutputStream(out);
    dout.writeByte(SignedBytes.checkedCast(strategy.ordinal()));
    dout.writeLong(capacity);
    dout.writeDouble(fpp);
    dout.writeLong(table.size());
    dout.writeLong(table.checksum());
    dout.writeLong(table.numBuckets());
    dout.writeInt(table.numEntriesPerBucket());
    dout.writeInt(table.numBitsPerEntry());
    dout.writeInt(table.data().length);

    for (long value : table.data()) {
      dout.writeLong(value);
    }
  }

  /**
   * Reads a byte stream, which was written by {@link #writeTo(OutputStream)}, into a {@link
   * CuckooFilter}. <p/> The {@code Funnel} to be used is not encoded in the stream, so it must be
   * provided here. <b>Warning:</b> the funnel provided <b>must</b> behave identically to the one
   * used to populate the original Cuckoo filter!
   *
   * @throws IOException if the InputStream throws an {@code IOException}, or if its data does not
   *                     appear to be a CuckooFilter serialized using the {@link
   *                     #writeTo(OutputStream)} method.
   */
  @CheckReturnValue
  public static <T> CuckooFilter<T> readFrom(InputStream in, Funnel<T> funnel) throws IOException {
    checkNotNull(in, "InputStream");
    checkNotNull(funnel, "Funnel");
    int strategyOrdinal = -1;
    long capacity = -1L;
    double fpp = -1.0D;
    long size = -1L;
    long checksum = -1L;
    long numBuckets = -1L;
    int numEntriesPerBucket = -1;
    int numBitsPerEntry = -1;
    int dataLength = -1;
    try {
      DataInputStream din = new DataInputStream(in);
      // currently this assumes there is no negative ordinal; will have to be updated if we
      // add non-stateless strategies (for which we've reserved negative ordinals; see
      // Strategy.ordinal()).
      strategyOrdinal = din.readByte();
      capacity = din.readLong();
      fpp = din.readDouble();
      size = din.readLong();
      checksum = din.readLong();
      numBuckets = din.readLong();
      numEntriesPerBucket = din.readInt();
      numBitsPerEntry = din.readInt();
      dataLength = din.readInt();

      Strategy strategy = CuckooFilterStrategies.values()[strategyOrdinal].strategy();
      long[] data = new long[dataLength];
      for (int i = 0; i < data.length; i++) {
        data[i] = din.readLong();
      }
      return new CuckooFilter<T>(
          new CuckooTable(data, size, checksum, numBuckets, numEntriesPerBucket, numBitsPerEntry),
          funnel, strategy, capacity, fpp);
    } catch (RuntimeException e) {
      IOException ioException = new IOException(
          "Unable to deserialize CuckooFilter from InputStream."
              + " strategyOrdinal: " + strategyOrdinal
              + " capacity: " + capacity
              + " fpp: " + fpp
              + " size: " + size
              + " checksum: " + checksum
              + " numBuckets: " + numBuckets
              + " numEntriesPerBucket: " + numEntriesPerBucket
              + " numBitsPerEntry: " + numBitsPerEntry
              + " dataLength: " + dataLength);
      ioException.initCause(e);
      throw ioException;
    }
  }

  /**
   * Returns the number of longs required by a CuckooTable for storage given the dimensions chosen
   * by the CuckooFilter to support {@code capacity) @ {@code fpp}.
   *
   * CuckooTable current impl uses a single long[] for data storage, so the calculated value must be
   * <= Integer.MAX_VALUE at this time.
   */
  @VisibleForTesting
  static int calculateDataLength(long capacity, double fpp) {
    return CuckooTable.calculateDataLength(
        optimalNumberOfBuckets(capacity, optimalEntriesPerBucket(fpp)),
        optimalEntriesPerBucket(fpp),
        optimalBitsPerEntry(fpp, optimalEntriesPerBucket(fpp)));
  }

  @Override
  public String toString() {
    return "CuckooFilter{" +
        "table=" + table +
        ", funnel=" + funnel +
        ", strategy=" + strategy +
        ", capacity=" + capacity +
        ", fpp=" + fpp +
        ", currentFpp=" + currentFpp() +
        ", size=" + size() +
        '}';
  }
}
