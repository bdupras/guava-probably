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

import java.io.Serializable;
import java.util.Collection;

import javax.annotation.CheckReturnValue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A Bloom filter for instances of {@code T}, backed by Google Guava's <a target="guavadoc"
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
 * that {@link #contains(Object)} will erroneously return {@code true} for an object that has
 * not actually been put in the {@link BloomFilter}. </blockquote>
 *
 * @param <T> the type of instances that the {@link BloomFilter} accepts.
 * @author Brian Dupras
 * @author Guava Authors (underlying BloomFilter implementation)
 * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html">com.google.common.hash.BloomFilter</a>
 */
public final class BloomFilter<T> extends ForwardingBloomFilter<T>
    implements ProbabilisticFilter<T>, Serializable {
  private final com.google.common.hash.BloomFilter<T> delegate;
  private final long capacity;
  private final double fpp;
  private long size;

  @Override
  protected com.google.common.hash.BloomFilter<T> delegate() {
    return delegate;
  }

  private BloomFilter(com.google.common.hash.BloomFilter<T> delegate, long capacity, double fpp, long size) {
    super();
    this.delegate = delegate;
    this.capacity = capacity;
    this.fpp = fpp;
    this.size = size;
  }

  /**
   * Creates a {@link BloomFilter} with the expected number of insertions and
   * expected false positive probability.
   *
   * <p>Note that overflowing a {@link BloomFilter} with significantly more elements than specified,
   * will result in its saturation, and a sharp deterioration of its false positive probability.
   *
   * <p>The constructed {@link BloomFilter} will be serializable if the provided {@link
   * Funnel} is.
   *
   * <p>It is recommended that the funnel be implemented as a Java enum. This has the benefit of
   * ensuring proper serialization and deserialization, which is important since {@link #equals(Object)}
   * also relies on object identity of funnels.
   *
   * @param funnel   the funnel of T's that the constructed {@link BloomFilter} will use
   * @param capacity the number of expected insertions to the constructed {@link BloomFilter};
   *                 must be positive
   * @param fpp      the desired false positive probability (must be positive and less than 1.0)
   * @return a {@link BloomFilter}
   * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html#create(com.google.common.hash.Funnel,
   * int, double)">com.google.common.hash.BloomFilter#create(com.google.common.hash.Funnel, int,
   * double)</a>
   */
  @CheckReturnValue
  public static <T> BloomFilter<T> create(Funnel<T> funnel, int capacity, double fpp) {
    return new BloomFilter<T>(
        com.google.common.hash.BloomFilter.create(funnel, capacity, fpp),
        capacity, fpp, 0L);
  }

  /**
   * Creates a {@link BloomFilter BloomFilter<T>} with the expected number of insertions and a
   * default expected false positive probability of 3%.
   *
   * <p>Note that overflowing a {@link BloomFilter} with significantly more objects than specified,
   * will result in its saturation, and a sharp deterioration of its false positive probability.
   *
   * <p>The constructed {@link BloomFilter} will be serializable if the provided {@code
   * Funnel<T>} is.
   *
   * <p>It is recommended that the funnel be implemented as a Java enum. This has the benefit of
   * ensuring proper serialization and deserialization, which is important since {@link #equals}
   * also relies on object identity of funnels.
   *
   * @param funnel   the funnel of T's that the constructed {@link BloomFilter} will use
   * @param capacity the number of expected insertions to the constructed {@link BloomFilter};
   *                 must be positive
   * @return a {@link BloomFilter}
   * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html#create(com.google.common.hash.Funnel,
   * int)">com.google.common.hash.BloomFilter#create(com.google.common.hash.Funnel, int)</a> </a>
   */
  @CheckReturnValue
  public static <T> BloomFilter<T> create(Funnel<T> funnel, int capacity) {
    return new BloomFilter<T>(
        com.google.common.hash.BloomFilter.create(funnel, capacity, 0.03D),
        capacity, 0.03D, 0L);
  }

  /**
   * Adds an object into this {@link BloomFilter}. Ensures that subsequent invocations of {@link
   * #contains(Object)} with the same object will always return {@code true}.
   *
   * The underlying Guava {@code com.google.common.hash.BloomFilter}, which returns {@code true}
   * when the internal state of the filter is mutated as a result of the call to {@code put(t)}, and
   * {@code false} otherwise. In either case, subsequent calls to {@code
   * com.google.common.hash.BloomFilter#mightContain(t)} will <i>always</i> return {@code true}.
   *
   * The return contract of {@link ProbabilisticFilter#add(Object)} differs, indicating success or failure
   * of adding an item to the filter. After {@link #add(Object)} returns {@code false}, subsequent calls
   * {@link #contains(Object)} <i>may</i> return {@code false}.
   *
   * @return always {@code true} as {@code com.google.common.hash.BloomFilter} cannot fail to add an
   * object.
   * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html#put(T)">com.google.common.hash.BloomFilter#put(T)</a>
   */
  public boolean add(T e) {
    super.put(e);
    size++;
    return true;
  }

  public boolean isCompatible(ProbabilisticFilter<T> f) {
    return (f instanceof BloomFilter) &&
        this.delegate().isCompatible(((BloomFilter) f).delegate);
  }

  public boolean addAll(ProbabilisticFilter<T> f) {
    checkNotNull(f);
    checkArgument(this != f, "Cannot combine a " + this.getClass().getSimpleName() +
        " with itself.");
    checkArgument(f instanceof ForwardingBloomFilter, "Cannot combine a " +
        this.getClass().getSimpleName() + " with a " + f.getClass().getSimpleName());
    checkArgument(this.isCompatible(f), "Cannot combine incompatible filters. " +
        this.getClass().getSimpleName() + " instances must have equivalent funnels; the same " +
        "strategy; and the same number of buckets, entries per bucket, and bits per entry.");

    super.putAll((ForwardingBloomFilter) f);
    size += f.size();
    return true;
  }

  public boolean addAll(Collection<? extends T> c) {
    for (T e : c) {
      add(e);
    }
    return true;
  }

  public boolean isEmpty() {
    return 0 == this.size();
  }

  public long size() {
    return size;
  }

  public long capacity() {
    return capacity;
  }

  public double fpp() {
    return fpp;
  }

  /**
   * Creates a new {@link BloomFilter} that's a copy of this instance. The returned instance {@code
   * equals(f) == true} but shares no mutable state.
   */
  public static <T> BloomFilter<T> copyOf(BloomFilter<T> f) {
    return new BloomFilter<T>(f.delegate().copy(), f.capacity(), f.fpp(), f.size());
  }

}