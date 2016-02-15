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

import com.google.common.hash.Funnel;
import com.google.common.math.LongMath;

import java.io.Serializable;
import java.util.Collection;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A Bloom filter for instances of {@code E} that implements the {@link ProbabilisticFilter}
 * interface.
 *
 * <p>This implementation is backed by Google Guava's <a target="guavadoc"
 * href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html">
 * {@code BloomFilter}</a>.
 *
 * From Guava: <blockquote> A Bloom filter offers an approximate containment test with one-sided
 * error: if it claims that an element is contained in it, this might be in error, but if it claims
 * that an element is <i>not</i> contained in it, then this is definitely true.
 *
 * <p>If you are unfamiliar with Bloom filters, this nice <a href="http://llimllib.github.com/bloomfilter-tutorial/">tutorial</a>
 * may help you understand how they work.
 *
 * <p>The false positive probability ({@code FPP}) of a bloom filter is defined as the probability
 * that {@link #contains(Object)} will erroneously return {@code true} for an object that has not
 * actually been put in the {@link BloomFilter}. </blockquote>
 *
 * @param <E> the type of instances that the {@link BloomFilter} accepts.
 * @author Brian Dupras
 * @author Guava Authors (underlying BloomFilter implementation)
 * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html">com.google.common.hash.BloomFilter</a>
 * @see ProbabilisticFilter
 */
public final class BloomFilter<E> implements ProbabilisticFilter<E>, Serializable {
  private com.google.common.hash.BloomFilter<E> delegate;
  private final Funnel<E> funnel;
  private final long capacity;
  private final double fpp;
  private long size;

  private BloomFilter(com.google.common.hash.BloomFilter<E> delegate, Funnel<E> funnel, long capacity, double fpp, long size) {
    super();
    checkNotNull(delegate);
    checkNotNull(funnel);
    checkArgument(capacity >= 0, "capacity must be positive");
    checkArgument(fpp >= 0.0 && fpp < 1.0, "fpp must be positive 0.0 <= fpp < 1.0");
    checkArgument(size >= 0, "size must be positive");
    this.delegate = delegate;
    this.funnel = funnel;
    this.capacity = capacity;
    this.fpp = fpp;
    this.size = size;
  }

  /**
   * Creates a {@link BloomFilter} with the expected number of insertions and expected false
   * positive probability.
   *
   * <p>Note that overflowing a {@link BloomFilter} with significantly more elements than specified,
   * will result in its saturation, and a sharp deterioration of its false positive probability.
   *
   * <p>The constructed {@link BloomFilter} will be serializable if the provided {@link Funnel} is.
   *
   * <p>It is recommended that the funnel be implemented as a Java enum. This has the benefit of
   * ensuring proper serialization and deserialization, which is important since {@link
   * #equals(Object)} also relies on object identity of funnels.
   *
   * @param funnel   the funnel of T's that the constructed {@link BloomFilter} will use
   * @param capacity the number of expected insertions to the constructed {@link BloomFilter}; must
   *                 be positive
   * @param fpp      the desired false positive probability (must be positive and less than 1.0)
   * @return a {@link BloomFilter}
   * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html#create(com.google.common.hash.Funnel,
   * int, double)">com.google.common.hash.BloomFilter#create(com.google.common.hash.Funnel, int,
   * double)</a>
   */
  @CheckReturnValue
  public static <T> BloomFilter<T> create(Funnel<T> funnel, long capacity, double fpp) {
    return new BloomFilter<T>(
        com.google.common.hash.BloomFilter.create(funnel, capacity, fpp),
        funnel, capacity, fpp, 0L);
  }

  /**
   * Creates a {@link BloomFilter BloomFilter<T>} with the expected number of insertions and a
   * default expected false positive probability of 3%.
   *
   * <p>Note that overflowing a {@link BloomFilter} with significantly more objects than specified,
   * will result in its saturation, and a sharp deterioration of its false positive probability.
   *
   * <p>The constructed {@link BloomFilter} will be serializable if the provided {@code Funnel<T>}
   * is.
   *
   * <p>It is recommended that the funnel be implemented as a Java enum. This has the benefit of
   * ensuring proper serialization and deserialization, which is important since {@link #equals}
   * also relies on object identity of funnels.
   *
   * @param funnel   the funnel of T's that the constructed {@link BloomFilter} will use
   * @param capacity the number of expected insertions to the constructed {@link BloomFilter}; must
   *                 be positive
   * @return a {@link BloomFilter}
   * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html#create(com.google.common.hash.Funnel,
   * int)">com.google.common.hash.BloomFilter#create(com.google.common.hash.Funnel, int)</a> </a>
   */
  @CheckReturnValue
  public static <T> BloomFilter<T> create(Funnel<T> funnel, long capacity) {
    return new BloomFilter<T>(
        com.google.common.hash.BloomFilter.create(funnel, capacity, 0.03D),
        funnel, capacity, 0.03D, 0L);
  }

  /**
   * Adds the specified element to this filter. A return value of {@code true} ensures that {@link
   * #contains(Object)} given {@code e} will also return {@code true}.
   *
   * @param e element to be added to this filter
   * @return always {@code true} as {@code com.google.common.hash.BloomFilter} cannot fail to add an
   * object
   * @throws NullPointerException if the specified element is null
   * @see #contains(Object)
   * @see #addAll(Collection)
   * @see #addAll(ProbabilisticFilter)
   * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html#put(T)">com.google.common.hash.BloomFilter#put(T)</a>
   */
  public boolean add(E e) {
    checkNotNull(e);
    delegate.put(e);
    size = LongMath.checkedAdd(size, 1L);
    return true;
  }

  /**
   * Combines {@code this} filter with another compatible filter. The mutations happen to {@code
   * this} instance. Callers must ensure {@code this} filter is appropriately sized to avoid
   * saturating it or running out of space.
   *
   * @param f filter to be combined into {@code this} filter - {@code f} is not mutated
   * @return {@code true} if the operation was successful, {@code false} otherwise
   * @throws NullPointerException     if the specified filter is null
   * @throws IllegalArgumentException if {@link #isCompatible(ProbabilisticFilter)} {@code ==
   *                                  false}
   * @see #add(Object)
   * @see #addAll(Collection)
   * @see #contains(Object)
   */
  public boolean addAll(ProbabilisticFilter<E> f) {
    checkNotNull(f);
    checkArgument(this != f, "Cannot combine a " + this.getClass().getSimpleName() +
        " with itself.");
    checkArgument(f instanceof BloomFilter, "Cannot combine a " +
        this.getClass().getSimpleName() + " with a " + f.getClass().getSimpleName());
    checkArgument(this.isCompatible(f), "Cannot combine incompatible filters. " +
        this.getClass().getSimpleName() + " instances must have equivalent funnels; the same " +
        "strategy; and the same number of buckets, entries per bucket, and bits per entry.");

    delegate.putAll(((BloomFilter<E>) f).delegate);
    size = LongMath.checkedAdd(size, f.sizeLong());
    return true;
  }

  /**
   * Adds all of the elements in the specified collection to this filter. The behavior of this
   * operation is undefined if the specified collection is modified while the operation is in
   * progress.
   *
   * @param c collection containing elements to be added to this filter
   * @return {@code true} if all elements of the collection were successfully added, {@code false}
   * otherwise
   * @throws NullPointerException if the specified collection contains a null element, or if the
   *                              specified collection is null
   * @see #add(Object)
   * @see #addAll(ProbabilisticFilter)
   * @see #contains(Object)
   */
  public boolean addAll(Collection<? extends E> c) {
    checkNotNull(c);
    for (E e : c) {
      checkNotNull(c);
      add(e);
    }
    return true;
  }

  /**
   * Returns {@code true} if this filter <i>might</i> contain the specified element, {@code false}
   * if this is <i>definitely</i> not the case.
   *
   * @param e element whose containment in this filter is to be tested
   * @return {@code true} if this filter <i>might</i> contain the specified element, {@code false}
   * if this is <i>definitely</i> not the case.
   * @throws ClassCastException   if the type of the specified element is incompatible with this
   *                              filter (optional)
   * @throws NullPointerException if the specified element is {@code null} and this filter does not
   *                              permit {@code null} elements
   * @see #containsAll(Collection)
   * @see #containsAll(ProbabilisticFilter)
   * @see #add(Object)
   * @see #remove(Object)
   * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html#mightContain(T)">com.google.common.hash.BloomFilter#mightContain(T)</a>
   */
  public boolean contains(E e) {
    return delegate.mightContain(e);
  }

  /**
   * Returns the current false positive probability ({@code FPP}) of this filter.
   *
   * @return the probability that {@link #contains(Object)} will erroneously return {@code true}
   * given an element that has not actually been added to the filter.
   * @see #fpp()
   * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html#put(T)">com.google.common.hash.BloomFilter#put(T)</a>
   */
  public double currentFpp() {
    return delegate.expectedFpp();
  }

  /**
   * Returns {@code true} if the specified filter is compatible with {@code this} filter. {@code f}
   * is considered compatible if {@code this} filter can use it in combinatoric operations (e.g.
   * {@link #addAll(ProbabilisticFilter)}, {@link #containsAll(ProbabilisticFilter)}).
   *
   * For two bloom filters to be compatible, they must:
   *
   * <ul> <li>not be the same instance</li> <li>have the same number of hash functions</li> <li>have
   * the same bit size</li> <li>have the same strategy</li> <li>have equal funnels</li> </ul>
   *
   * @param f filter to check for compatibility with {@code this} filter
   * @return {@code true} if the specified filter is compatible with {@code this} filter
   * @throws NullPointerException if the specified filter is {@code null}
   * @see #addAll(ProbabilisticFilter)
   * @see #containsAll(ProbabilisticFilter)
   * @see #removeAll(ProbabilisticFilter)
   * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html#isCompatible(com.google.common.hash.BloomFilter)">com.google.common.hash.BloomFilter#isCompatible(com.google.common.hash.BloomFilter)</a>
   */
  public boolean isCompatible(ProbabilisticFilter<E> f) {
    checkNotNull(f);
    return (f instanceof BloomFilter) &&
        this.delegate.isCompatible(((BloomFilter<E>) f).delegate);
  }

  /**
   * Returns {@code true} if this filter <i>might</i> contain all of the elements of the specified
   * collection (optional operation). More formally, returns {@code true} if {@link
   * #contains(Object)} {@code == true} for all of the elements of the specified collection.
   *
   * @param c collection containing elements to be checked for containment in this filter
   * @return {@code true} if this filter <i>might</i> contain all elements of the specified
   * collection
   * @throws NullPointerException if the specified collection contains one or more {@code null}
   *                              elements, or if the specified collection is {@code null}
   * @see #contains(Object)
   * @see #containsAll(ProbabilisticFilter)
   */
  public boolean containsAll(Collection<? extends E> c) {
    checkNotNull(c);
    for (E e : c) {
      checkNotNull(e);
      if (!contains(e)) return false;
    }
    return true;
  }

  /**
   * Not supported.
   *
   * @throws UnsupportedOperationException
   */
  public boolean containsAll(ProbabilisticFilter<E> f) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns {@code true} if this filter contains no elements.
   *
   * @return {@code true} if this filter contains no elements
   * @see #sizeLong()
   */
  public boolean isEmpty() {
    return 0 == this.sizeLong();
  }

  /**
   * Returns the number of elements contained in this filter (its cardinality). If this filter
   * contains more than {@code Long.MAX_VALUE} elements, returns {@code Long.MAX_VALUE}.
   *
   * @return the number of elements contained in this filter (its cardinality)
   * @see #capacity()
   * @see #isEmpty()
   */
  public long sizeLong() {
    return size >= 0 ? size : Long.MAX_VALUE /* overflow */;
  }

  /**
   * Returns the number of elements contained in this filter (its cardinality). If this filter
   * contains more than {@code Integer.MAX_VALUE} elements, returns {@code Integer.MAX_VALUE}.
   *
   * @return the number of elements contained in this filter (its cardinality)
   * @see #capacity()
   * @see #isEmpty()
   * @see #sizeLong()
   */
  public long size() {
    return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : size;
  }

  /**
   * Returns the number of elements this filter can represent at its requested {@code FPP}. This is
   * not be a hard limit of the filter implementation. It is permissible for a filter to contain
   * more elements than its requested capacity, though its {@code FPP} will suffer.
   *
   * @return the number of elements this filter can represent at its requested {@code FPP}.
   * @see #fpp()
   * @see #currentFpp()
   * @see #sizeLong()
   */
  public long capacity() {
    return capacity;
  }

  /**
   * Returns the intended {@code FPP} limit of this filter. This is not a hard limit of the filter
   * implementation. It is permissible for a filter's {@code FPP} to degrade (e.g. via saturation)
   * beyond its intended limit.
   *
   * @return the intended {@code FPP} limit of this filter.
   * @see #currentFpp()
   */
  public double fpp() {
    return fpp;
  }

  /**
   * Creates a new {@link BloomFilter} that's a copy of this instance. The returned instance {@code
   * equals(f) == true} but shares no mutable state.
   */
  public static <T> BloomFilter<T> copyOf(BloomFilter<T> f) {
    return new BloomFilter<T>(f.delegate.copy(), f.funnel, f.capacity(), f.fpp(), f.sizeLong());
  }

  /**
   * Removes all of the elements from this filter. The filter will be empty after this call
   * returns.
   *
   * @see #sizeLong()
   * @see #isEmpty()
   */
  public void clear() {
    this.delegate = com.google.common.hash.BloomFilter.create(funnel, (int) capacity, fpp);
    this.size = 0L;
  }

  /**
   * Not supported. Standard bloom filters do not support element removal.
   *
   * @throws UnsupportedOperationException
   */
  public boolean remove(E e) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. Standard bloom filters do not support element removal.
   *
   * @throws UnsupportedOperationException
   */
  public boolean removeAll(Collection<? extends E> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * Not supported. Standard bloom filters do not support element removal.
   *
   * @throws UnsupportedOperationException
   */
  public boolean removeAll(ProbabilisticFilter<E> f) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof com.google.common.hash.BloomFilter) {
      //noinspection ConstantConditions
      return delegate.equals(((BloomFilter) object).delegate);
    } else {
      return delegate.equals(object);
    }
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }
}