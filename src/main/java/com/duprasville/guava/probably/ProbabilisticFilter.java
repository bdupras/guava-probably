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
 * that an element is contained in it, this <i>might</i> be in error, but if it claims that an
 * element is <i>not</i> contained in it, then this is <i>definitely</i> true. <p/> <p>The false
 * positive probability ({@code FPP}) of a probabilistic filter is defined as the probability that
 * {@link #contains(Object)} will erroneously return {@code true} for an element that is not
 * actually contained in the filter. <p/>
 *
 * @param <E> the type of elements that this filter accepts
 * @author Brian Dupras
 * @see CuckooFilter
 * @see BloomFilter
 */
public interface ProbabilisticFilter<E> {
  /**
   * Adds the specified element to this filter (optional operation). A return value of {@code true}
   * ensures that {@link #contains(Object)} given {@code e} will also return {@code true}.
   *
   * @param e element to be added to this filter
   * @return {@code true} if {@code e} was successfully added to the filter, {@code false} if this
   * is <i>definitely</i> not the case
   * @throws UnsupportedOperationException if the {@link #add(Object)} operation is not supported by
   *                                       this filter
   * @throws ClassCastException            if the class of the specified element prevents it from
   *                                       being added to this filter
   * @throws NullPointerException          if the specified element is {@code null} and this filter
   *                                       does not permit {@code null} elements
   * @throws IllegalArgumentException      if some property of the specified element prevents it
   *                                       from being added to this filter
   * @see #contains(Object)
   * @see #addAll(Collection)
   * @see #addAll(ProbabilisticFilter)
   */
  @CheckReturnValue
  boolean add(E e);

  /**
   * Combines {@code this} filter with another compatible filter (optional operation). The mutations
   * happen to {@code this} instance. Callers must ensure {@code this} filter is appropriately sized
   * to avoid saturating it or running out of space.
   *
   * @param f filter to be combined into {@code this} filter - {@code f} is not mutated
   * @return {@code true} if the operation was successful, {@code false} otherwise
   * @throws UnsupportedOperationException if the {@link #addAll(ProbabilisticFilter)} operation is
   *                                       not supported by this filter
   * @throws NullPointerException          if the specified filter is {@code null}
   * @throws IllegalArgumentException      if {@link #isCompatible(ProbabilisticFilter)} {@code ==
   *                                       false}
   * @throws IllegalStateException         if this filter cannot be combined with the specified
   *                                       filter at this time due to insertion restrictions
   * @see #add(Object)
   * @see #addAll(Collection)
   * @see #contains(Object)
   */
  @CheckReturnValue
  boolean addAll(ProbabilisticFilter<E> f);

  /**
   * Adds all of the elements in the specified collection to this filter (optional operation). The
   * behavior of this operation is undefined if the specified collection is modified while the
   * operation is in progress.
   *
   * @param c collection containing elements to be added to this filter
   * @return {@code true} if all elements of the collection were successfully added, {@code false}
   * otherwise
   * @throws UnsupportedOperationException if the {@link #addAll(Collection)} operation is not
   *                                       supported by this filter
   * @throws ClassCastException            if the class of an element of the specified collection
   *                                       prevents it from being added to this filter
   * @throws NullPointerException          if the specified collection contains a {@code null}
   *                                       element and this filter does not permit {@code null}
   *                                       elements, or if the specified collection is {@code null}
   * @throws IllegalArgumentException      if some property of an element of the specified
   *                                       collection prevents it from being added to this filter
   * @throws IllegalStateException         if not all the elements can be added at this time due to
   *                                       insertion restrictions
   * @see #add(Object)
   * @see #addAll(ProbabilisticFilter)
   * @see #contains(Object)
   */
  @CheckReturnValue
  boolean addAll(Collection<? extends E> c);

  /**
   * Removes all of the elements from this filter (optional operation). The filter will be empty
   * after this call returns.
   *
   * @throws UnsupportedOperationException if the {@link #clear()} method is not supported by this
   *                                       filter
   * @see #sizeLong()
   * @see #isEmpty()
   */
  void clear();

  /**
   * Removes the specified element from this filter (optional operation). The element must be
   * contained in the filter prior to invocation. Removing an element that isn't contained in the
   * filter may put the filter in an inconsistent state causing it to return false negative
   * responses from {@link #contains(Object)}.
   *
   * If {@code false} is returned, this is <i>definitely</i> an indication that the specified
   * element wasn't contained in the filter prior to invocation. If the implementation treats this
   * condition as an error, then this filter can no longer be relied upon to return correct {@code
   * false} responses from {@link #contains(Object)}, unless {@link #isEmpty()} is also {@code
   * true}.
   *
   * @param e element to be removed from this filter
   * @return {@code true} if this filter probably contained the specified element, {@code false}
   * otherwise
   * @throws ClassCastException            if the type of the specified element is incompatible with
   *                                       this filter (optional)
   * @throws NullPointerException          if the specified element is {@code null} and this filter
   *                                       does not permit {@code null} elements
   * @throws UnsupportedOperationException if the {@link #remove(Object)} operation is not supported
   *                                       by this filter
   * @see #contains(Object)
   * @see #removeAll(Collection)
   * @see #removeAll(ProbabilisticFilter)
   */
  @CheckReturnValue
  boolean remove(E e);

  /**
   * Removes from this filter all of its elements that are contained in the specified collection
   * (optional operation). All element contained in the specified collection must be contained in
   * the filter prior to invocation. Removing elements that aren't contained in the filter may put
   * the filter in an inconsistent state causing it to return false negative responses from {@link
   * #contains(Object)}.
   *
   * If {@code false} is returned, this is <i>definitely</i> an indication that the specified
   * collection contained elements that were not contained in this filter prior to invocation. If
   * the implementation treats this condition as an error, then this filter can no longer be relied
   * upon to return correct {@code false} responses from {@link #contains(Object)}, unless {@link
   * #isEmpty()} is also {@code true}.
   *
   * @param c collection containing elements to be removed from this filter
   * @return {@code true} if all of the elements of the specified collection were successfully
   * removed from the filter, {@code false} if any of the elements was not successfully removed
   * @throws ClassCastException            if the types of one or more elements in the specified
   *                                       collection are incompatible with this filter (optional)
   * @throws NullPointerException          if the specified collection contains one or more null
   *                                       elements and this filter does not permit {@code null}
   *                                       elements (optional), or if the specified collection is
   *                                       {@code null}
   * @throws UnsupportedOperationException if the {@link #removeAll(Collection)} operation is not
   *                                       supported by this filter
   * @see #contains(Object)
   * @see #remove(Object)
   * @see #removeAll(ProbabilisticFilter)
   */
  @CheckReturnValue
  boolean removeAll(Collection<? extends E> c);

  /**
   * Subtracts the specified filter from {@code this} filter. The mutations happen to {@code this}
   * instance. Callers must ensure that the specified filter represents elements that are currently
   * contained in {@code this} filter.
   *
   * If {@code false} is returned, this is <i>definitely</i> an indication that the specified filter
   * contained elements that were not contained in this filter prior to invocation. If the
   * implementation treats this condition as an error, then this filter can no longer be relied upon
   * to return correct {@code false} responses from {@link #contains(Object)}, unless {@link
   * #isEmpty()} is also {@code true}.
   *
   * @param f filter containing elements to remove from {@code this} filter. {@code f} is not
   *          mutated
   * @return {@code true} if the operation was successful, {@code false} otherwise
   * @throws UnsupportedOperationException if the {@link #removeAll(ProbabilisticFilter)} operation
   *                                       is not supported by this filter
   * @throws NullPointerException          if the specified filter is {@code null}
   * @throws IllegalArgumentException      if {@link #isCompatible(ProbabilisticFilter)} {@code ==
   *                                       false} given {@code f}
   * @see #contains(Object)
   * @see #remove(Object)
   * @see #removeAll(Collection)
   */
  @CheckReturnValue
  boolean removeAll(ProbabilisticFilter<E> f);

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
   */
  boolean contains(E e);

  /**
   * Returns {@code true} if this filter <i>might</i> contain all of the elements of the specified
   * collection (optional operation). More formally, returns {@code true} if {@link
   * #contains(Object)} {@code == true} for all of the elements of the specified collection.
   *
   * @param c collection containing elements to be checked for containment in this filter
   * @return {@code true} if this filter <i>might</i> contain all elements of the specified
   * collection
   * @throws ClassCastException   if the types of one or more elements in the specified collection
   *                              are incompatible with this filter (optional)
   * @throws NullPointerException if the specified collection contains one or more {@code null}
   *                              elements and this filter does not permit {@code null} elements
   *                              (optional), or if the specified collection is {@code null}
   * @see #contains(Object)
   * @see #containsAll(ProbabilisticFilter)
   */
  boolean containsAll(Collection<? extends E> c);

  /**
   * Returns {@code true} if this filter <i>might</i> contain all elements contained in the
   * specified filter (optional operation).
   *
   * @param f filter containing elements to be checked for probable containment in this filter
   * @return {@code true} if this filter <i>might</i> contain all elements contained in the
   * specified filter, {@code false} if this is <i>definitely</i> not the case.
   * @throws UnsupportedOperationException if the {@link #containsAll(ProbabilisticFilter)}
   *                                       operation is not supported by this filter
   * @throws NullPointerException          if the specified filter is {@code null}
   * @throws IllegalArgumentException      if {@link #isCompatible(ProbabilisticFilter)} {@code ==
   *                                       false} given {@code f}
   * @see #contains(Object)
   * @see #containsAll(Collection)
   */
  boolean containsAll(ProbabilisticFilter<E> f);

  /**
   * Returns {@code true} if this filter contains no elements.
   *
   * @return {@code true} if this filter contains no elements
   * @see #sizeLong()
   */
  boolean isEmpty();

  /**
   * Returns the number of elements contained in this filter (its cardinality). If this filter
   * contains more than {@code Long.MAX_VALUE} elements, returns {@code Long.MAX_VALUE}.
   *
   * @return the number of elements contained in this filter (its cardinality)
   * @see #capacity()
   * @see #isEmpty()
   * @see #size()
   */
  long sizeLong();

  /**
   * Returns the number of elements contained in this filter (its cardinality). If this filter
   * contains more than {@code Integer.MAX_VALUE} elements, returns {@code Integer.MAX_VALUE}. Use
   * {@link #sizeLong()} to obtain filter sizes lager than {@code Integer.MAX_VALUE};
   *
   * <p>This method is provided for consistency with the Collections API.</p>
   *
   * @return the number of elements contained in this filter (its cardinality)
   * @see #capacity()
   * @see #isEmpty()
   * @see #sizeLong()
   */
  long size();

  /**
   * Returns {@code true} if the specified filter is compatible with {@code this} filter. {@code f}
   * is considered compatible if {@code this} filter can use it in combinatoric operations (e.g.
   * {@link #addAll(ProbabilisticFilter)}, {@link #containsAll(ProbabilisticFilter)}, {@link
   * #removeAll(ProbabilisticFilter)}).
   *
   * @param f filter to check for compatibility with {@code this} filter
   * @return {@code true} if the specified filter is compatible with {@code this} filter
   * @throws NullPointerException if the specified filter is {@code null}
   * @see #addAll(ProbabilisticFilter)
   * @see #containsAll(ProbabilisticFilter)
   * @see #removeAll(ProbabilisticFilter)
   */
  boolean isCompatible(ProbabilisticFilter<E> f);

  /**
   * Returns the number of elements this filter can represent at its requested {@code FPP}. This may
   * not be a hard limit of the filter implementation. It is permissible for a filter to contain
   * more elements than its requested capacity, though its {@code FPP} may suffer.
   *
   * @return the number of elements this filter can represent at its requested {@code FPP}.
   * @see #fpp()
   * @see #currentFpp()
   * @see #sizeLong()
   */
  long capacity();

  /**
   * Returns the current false positive probability ({@code FPP}) of this filter.
   *
   * @return the probability that {@link #contains(Object)} will erroneously return {@code true}
   * given an element that has not actually been added to the filter.
   * @see #fpp()
   */
  double currentFpp();

  /**
   * Returns the intended {@code FPP} limit of this filter. This may not be a hard limit of the
   * filter implementation. It is permissible for a filter's {@code FPP} to degrade (e.g. via
   * saturation) beyond its intended limit.
   *
   * @return the intended {@code FPP} limit of this filter.
   * @see #currentFpp()
   */
  double fpp();
}
