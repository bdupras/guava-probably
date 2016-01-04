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

import java.util.Collection;

import javax.annotation.CheckReturnValue;

/**
 * A probabilistic filter offers an approximate containment test with one-sided error: if it claims
 * that an object is contained in it, this might be in error, but if it claims that an object is
 * <i>not</i> contained in it, then this is definitely true. <p/> <p>The false positive probability
 * ({@code FPP}) of a probabilistic filter is defined as the probability that {@linkplain
 * #contains(Object)} will erroneously return {@code true} for an object that has not actually been
 * added to the {@code ProbabilisticFilter}. <p/>
 *
 * @param <E> the type of instances that the {@code ProbabilisticFilter} accepts
 * @author Brian Dupras
 */
public interface ProbabilisticFilter<E> {
  /**
   * Ensures that this filter contains the specified element.
   *
   * @return {@code true} if {@code e} was successfully added to the filter, {@code false} if this
   * is <i>definitely</i> not the case. A return value of {@code true} ensures that {@code
   * contains(e)} will also return {@code true}.
   */
  @CheckReturnValue
  boolean add(E e);

  /**
   * Combines {@code this} filter with another compatible filter. The mutations happen to {@code
   * this} instance. Callers must ensure {@code this} filter is appropriately sized to avoid
   * saturating it or running out of space.
   *
   * @param f The filter to combine {@code this} filter with. {@code f} is not mutated.
   * @return {@code true} if the operation was successful, {@code false} otherwise.
   * @throws IllegalArgumentException if {@code isCompatible(f) == false}
   */
  @CheckReturnValue
  boolean addAll(ProbabilisticFilter<E> f);

  /**
   * Adds all of the elements in the specified collection to this filter. The behavior of this
   * operation is undefined if the specified collection is modified while the operation is in
   * progress.
   *
   * @return {@code true} if all elements of the collection were successfully added, {@code false}
   * otherwise.
   * @throws ClassCastException       if the class of an element of the specified collection
   *                                  prevents it from being added to this collection
   * @throws NullPointerException     if the specified collection contains a null element and this
   *                                  filter does not permit null elements, or if the specified
   *                                  collection is null
   * @throws IllegalArgumentException if some property of an element of the specified collection
   *                                  prevents it from being added to this filter
   * @throws IllegalStateException    if not all the elements can be added at this time due to
   *                                  insertion restrictions
   */
  @CheckReturnValue
  boolean addAll(Collection<? extends E> c);

  /**
   * Returns {@code true} if this filter <i>might</i> contain the specified element.
   *
   * @param o element whose presence in this filter is to be tested
   * @return {@code true} if this filter <i>might</i> contain the specified element, {@code false}
   * if this is <i>definitely</i> not the case.
   */
  boolean contains(E o);

  /**
   * Returns true if this filter contains no elements.
   *
   * @return {@code true} if this collection contains no elements
   */
  boolean isEmpty();

  /**
   * Returns the number of elements in this filter.
   *
   * @return the number of elements in this filter.
   */
  long size();

  /**
   * Returns the current false positive probability of this filter.
   *
   * @return the probability that {@link #contains(E)} will erroneously return {@code true} for an
   * element that has not actually been added to the filter.
   */
  double currentFpp();

  /**
   * Returns {@code true} if {@code f} is compatible with {@code this} filter. {@code f} is
   * considered compatible if {@code this} filter can use it in combinatoric operations (e.g. {@link
   * #addAll(ProbabilisticFilter)}).
   *
   * @param f The filter to check for compatibility.
   * @return {@code true} if {@code f} is compatible with {@code this} filter.
   */
  boolean isCompatible(ProbabilisticFilter<E> f);

  /**
   * Returns the number of elements this filter can represent at its desired @{code FPP}. This may
   * not be a hard limit of the filter implementation. It is permissible for a filter to accept more
   * elements than its expected capacity, though its {@code FPP} may suffer.
   *
   * @return the number of elements this filter can represent at its desired @{code FPP}.
   * @see #fpp()
   * @see #currentFpp()
   */
  long capacity();

  /**
   * Returns the intended {@FPP} limit of this filter. This may not be a hard limit of the filter
   * implementation. It is permissible for a filter's {@code FPP} to degrade (e.g. via saturation)
   * beyond its intended limit.
   *
   * @return the intended {@FPP} limit of this filter.
   */
  double fpp();
}
