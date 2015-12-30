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

import com.google.common.collect.ForwardingObject;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;

import javax.annotation.Nullable;

abstract class ForwardingBloomFilter<T> extends ForwardingObject {
  @Override
  protected abstract BloomFilter<T> delegate();

  /**
   * protected default constructor for use by sub classes
   */
  protected ForwardingBloomFilter() {
  }

  /**
   * Returns {@code true} if the object <i>might</i> have been put in the underlying {@code
   * com.google.common.hash.BloomFilter}, {@code false} if this is <i>definitely</i> not the case.
   *
   * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html#mightContain(T)">com.google.common.hash.BloomFilter#mightContain(T)</a>
   */
  public boolean mightContain(T object) {
    return delegate().mightContain(object);
  }

  /**
   * Puts an object into the underlying {@code com.google.common.hash.BloomFilter}. Ensures that
   * subsequent invocations of {@link #mightContain(T)} with the same object will always return
   * {@code true}.
   *
   * @return true if the bloom filter's bits changed as a result of this operation. If the bits
   * changed, this is <i>definitely</i> the first time {@code object} has been added to the filter.
   * If the bits haven't changed, this <i>might</i> be the first time {@code object} has been added
   * to the filter. Note that {@code put(t)} always returns the <i>opposite</i> result to what
   * {@code mightContain(t)} would have returned at the time it is called."
   * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html#put(T)">com.google.common.hash.BloomFilter#put(T)</a>
   */
  public boolean put(T object) {
    return delegate().put(object);
  }

  /**
   * Returns the probability that {@linkplain #mightContain(Object)} will erroneously return {@code true}
   * for an object that has not actually been put in the underlying {@code
   * com.google.common.hash.BloomFilter}.
   *
   * <p>Ideally, this number should be close to the {@code fpp} parameter passed in {@linkplain
   * com.google.common.hash.BloomFilter#create(Funnel, int, double)}, or smaller. If it is
   * significantly higher, it is usually the case that too many objects (more than expected) have
   * been put in the bloom filter, degenerating it.
   *
   * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html#put(T)">com.google.common.hash.BloomFilter#put(T)</a>
   */
  public double expectedFpp() {
    return delegate().expectedFpp();
  }

  /**
   * Determines whether a given bloom filter is compatible with this bloom filter. For two bloom
   * filters to be compatible, they must:
   *
   * <ul> <li>not be the same instance</li> <li>have the same number of hash functions</li> <li>have
   * the same bit size</li> <li>have the same strategy</li> <li>have equal funnels</li> </ul>
   *
   * @param that The bloom filter to check for compatibility.
   * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html#isCompatible(com.google.common.hash.BloomFilter)">com.google.common.hash.BloomFilter#isCompatible(com.google.common.hash.BloomFilter)</a>
   */
  public boolean isCompatible(ForwardingBloomFilter<T> that) {
    return delegate().isCompatible(that.delegate());
  }

  /**
   * Combines this bloom filter with another bloom filter by performing a bitwise OR of the
   * underlying data. The mutations happen to <b>this</b> instance. Callers must ensure the bloom
   * filters are appropriately sized to avoid saturating them.
   *
   * @param that The bloom filter to combine this bloom filter with. It is not mutated.
   * @throws IllegalArgumentException if {@code isCompatible(that) == false}
   * @see <a target="guavadoc" href="http://google.github.io/guava/releases/snapshot/api/docs/com/google/common/hash/BloomFilter.html#putAll(com.google.common.hash.BloomFilter)">com.google.common.hash.BloomFilter#putAll(com.google.common.hash.BloomFilter)</a>
   */
  public void putAll(ForwardingBloomFilter<T> that) {
    delegate().putAll(that.delegate());
  }

  @Override
  public boolean equals(@Nullable Object object) {
    if (object instanceof BloomFilter) {
      return delegate().equals(((ForwardingBloomFilter) object).delegate());
    } else {
      return delegate().equals(object);
    }
  }

  @Override
  public int hashCode() {
    return delegate().hashCode();
  }

}