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

import javax.annotation.CheckReturnValue;

/**
 * A probabilistic filter offers an approximate containment test with one-sided error: if it claims
 * that an object is contained in it, this might be in error, but if it claims that an object is
 * <i>not</i> contained in it, then this is definitely true. <p/> <p>The false positive probability
 * ({@code FPP}) of a probabilistic filter is defined as the probability that {@linkplain
 * #mightContain(Object)} will erroneously return {@code true} for an object that has not actually
 * been put in the {@code ProbabilisticFilter}. <p/>
 *
 * @param <T> the type of instances that the {@code ProbabilisticFilter} accepts
 * @author Brian Dupras
 */
public interface ProbabilisticFilter<T> {
  /**
   * Queries the filter for the given Object.
   *
   * @return {@code true} if the object <i>might</i> have been put in the filter, {@code false} if
   * this is <i>definitely</i> not the case.
   */
  @CheckReturnValue
  boolean mightContain(T object);

  /**
   * Puts an Object into the filter.
   *
   * @return {@code true} if the object was successfully added to the filter, {@code false} if this
   * is <i>definitely</i> not the case. A return value of {@code true} ensures that {@code
   * mightContain(t)} will also return {@code true}.
   */
  @CheckReturnValue
  boolean put(T object);

  /**
   * Queries this {@code ProbabilisticFilter} for its current false positive probability.
   *
   * @return the probability that {@linkplain #mightContain(Object)} will erroneously return {@code
   * true} for an object that has not actually been put in the filter.
   */
  @CheckReturnValue
  double expectedFpp();
}
