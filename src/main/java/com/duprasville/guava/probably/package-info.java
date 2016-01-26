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

/**
 * Probabilistic data structures for Guava.
 *
 * <p>This package is a part of the open-source <a target="_top" href="https://github.com/bdupras/guava-probably">Guava-Probably
 * library</a>.
 *
 * <h2>Contents</h2>
 *
 * <h3>Probabilistic Filters</h3>
 *
 * <ul>
 *
 * <li>{@link com.duprasville.guava.probably.ProbabilisticFilter} - interface defining basic methods
 * of probabilistic filters: {@link com.duprasville.guava.probably.ProbabilisticFilter#add(Object)},
 * {@link com.duprasville.guava.probably.ProbabilisticFilter#contains(Object)}, and {@link
 * com.duprasville.guava.probably.ProbabilisticFilter#currentFpp()}.</li>
 *
 * <li>{@link com.duprasville.guava.probably.CuckooFilter} - Cuckoo filter implementation that
 * supports deletion.</li>
 *
 * <li>{@link com.duprasville.guava.probably.BloomFilter} - Bloom filter implementation backed by
 * Guava's BloomFilter.</li>
 *
 * </ul>
 */
package com.duprasville.guava.probably;
