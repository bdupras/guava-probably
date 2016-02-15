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

import com.google.common.hash.Hashing;

/**
 * Collections of strategies of generating the f-bit fingerprint, index i1 and index i2 required for
 * an element to be mapped to a CuckooTable of m buckets with hash function h. These strategies are
 * part of the serialized form of the Cuckoo filters that use them, thus they must be preserved as
 * is (no updates allowed, only introduction of new versions). <p/> Important: the order of the
 * constants cannot change, and they cannot be deleted - we depend on their ordinal for CuckooFilter
 * serialization.
 *
 * @author Brian Dupras
 */
public enum CuckooStrategies {
  /**
   * Adaptation of <i>"Cuckoo Filter: Practically Better Than Bloom", Bin Fan, et al</i>, that is
   * comparable to a Bloom Filter's memory efficiency, supports entry deletion, and can accept up to
   * 12.8 billion entries at 3% FPP.
   *
   * <p>This strategy uses 32 bits of {@link Hashing#murmur3_128} to find an entry's primary index.
   * The next non-zero f-bit segment of the hash is used as the entry's fingerprint. An entry's
   * alternate index is defined as {@code [hash(fingerprint) * parsign(index)] modulo bucket_count},
   * where {@code hash(fingerprint)} is always odd, and {@code parsign(index)} is defined as {@code
   * +1} when {@code index} is even and {@code -1} when {@code index} is odd. The filter's bucket
   * count is rounded up to an even number. By specifying an even number of buckets and an odd
   * fingerprint hash, the parity of the alternate index is guaranteed to be opposite the parity of
   * the primary index. The use of the index's parity to apply a sign to {@code hash(fingerprint)}
   * causes the operation to be reversible, i.e. {@code index(e) == altIndex(altIndex(e))}.</p>
   *
   * <p>A notable difference of this strategy from "Cuckoo Filter" is the method of selecting an
   * entry's alternate index. In the paper, the alternate index is defined as {@code index xor
   * hash(fingerprint)}. The use of {@code xor} requires that the index space be defined as
   * [0..2^f]. The side-effect of this is that the Cuckoo Filter's bucket count must be a power of
   * 2, meaning the memory utilization of the filter must be "rounded up" to the next power of two.
   * This side-effect of the paper's algorithm is avoided by the algorithm as described above.</p>
   */
  MURMUR128_BEALDUPRAS_32() {
    @Override
    public CuckooStrategy strategy() {
      return new CuckooStrategyMurmurBealDupras32(this.ordinal());
    }
  };

  public abstract CuckooStrategy strategy();
}
