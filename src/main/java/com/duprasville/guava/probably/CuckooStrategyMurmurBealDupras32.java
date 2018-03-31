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
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.math.LongMath.mod;

/**
 * Cuckoo Filter strategy employing Murmur3 32-bit hashes and parity-based altIndex calculation.
 *
 * @author Brian Dupras
 * @author Alex Beal
 */
class CuckooStrategyMurmurBealDupras32 extends AbstractCuckooStrategy implements CuckooStrategy {
  private static final int MAX_RELOCATION_ATTEMPTS = 500;
  private static final HashFunction hashFunction = Hashing.murmur3_128();

  CuckooStrategyMurmurBealDupras32(int ordinal) {
    super(ordinal);
  }

  public <T> boolean add(T object, Funnel<? super T> funnel, CuckooTable table) {
    final long hash64 = hash(object, funnel).asLong();
    final int hash1 = hash1(hash64);
    final int hash2 = hash2(hash64);
    final int fingerprint = fingerprint(hash2, table.numBitsPerEntry);

    final long index = index(hash1, table.numBuckets);
    return putEntry(fingerprint, table, index) ||
        putEntry(fingerprint, table, altIndex(index, fingerprint, table.numBuckets));
  }

  protected long maxRelocationAttempts() {
    return MAX_RELOCATION_ATTEMPTS;
  }

  private final Random kicker = new Random(1L);

  protected int pickEntryToKick(int numEntriesPerBucket) {
    return kicker.nextInt(numEntriesPerBucket);
  }

  public <T> boolean remove(T object, Funnel<? super T> funnel, CuckooTable table) {
    final long hash64 = hash(object, funnel).asLong();
    final int hash1 = hash1(hash64);
    final int hash2 = hash2(hash64);
    final int fingerprint = fingerprint(hash2, table.numBitsPerEntry);
    final long index1 = index(hash1, table.numBuckets);
    final long index2 = altIndex(index1, fingerprint, table.numBuckets);
    return table.swapAnyEntry(CuckooTable.EMPTY_ENTRY, fingerprint, index1)
        || table.swapAnyEntry(CuckooTable.EMPTY_ENTRY, fingerprint, index2);
  }

  public <T> boolean contains(T object, Funnel<? super T> funnel, CuckooTable table) {
    final long hash64 = hash(object, funnel).asLong();
    final int hash1 = hash1(hash64);
    final int hash2 = hash2(hash64);
    final int fingerprint = fingerprint(hash2, table.numBitsPerEntry);
    final long index1 = index(hash1, table.numBuckets);
    final long index2 = altIndex(index1, fingerprint, table.numBuckets);
    return table.hasEntry(fingerprint, index1) || table.hasEntry(fingerprint, index2);
  }

  <T> HashCode hash(final T object, final Funnel<? super T> funnel) {
    return hashFunction.hashObject(object, funnel);
  }

  int hash1(long hash64) {
    return (int) hash64;
  }

  int hash2(long hash64) {
    return (int) (hash64 >>> 32);
  }

  /**
   * Returns an f-bit portion of the given hash. Iterating by f-bit segments from the least
   * significant side of the hash to the most significant, looks for a non-zero segment. If a
   * non-zero segment isn't found, 1 is returned to distinguish the fingerprint from a
   * non-entry.
   *
   * @param hash 32-bit hash value
   * @param f number of bits to consider from the hash
   * @return first non-zero f-bit value from hash as an int, or 1 if no non-zero value is found
   */
  public static int fingerprint(int hash, int f) {
    checkArgument(f > 0, "f must be greater than zero");
    checkArgument(f <= Integer.SIZE, "f must be less than " + Integer.SIZE);
    int mask = (0x80000000 >> (f - 1)) >>> (Integer.SIZE - f);

    for (int bit = 0; (bit + f) <= Integer.SIZE; bit += f) {
      int ret = (hash >> bit) & mask;
      if (0 != ret) {
        return ret;
      }
    }
    return 0x1;
  }

  /**
   * Calculates a primary index for an entry in the cuckoo table given the entry's 32-bit
   * hash and the table's size in buckets, m.
   *
   * tl;dr simply a wrap-around modulo bound by 0..m-1
   *
   * @param hash 32-bit hash value
   * @param m size of cuckoo table in buckets
   * @return index, bound by 0..m-1 inclusive
   */
  @Override
  public long index(int hash, long m) {
    return mod(hash, m);
  }

  /**
   * Calculates an alternate index for an entry in the cuckoo table.
   *
   * tl;dr
   * Calculates an offset as an odd hash of the fingerprint and adds to, or subtracts from,
   * the starting index, wrapping around the table (mod) as necessary.
   *
   * Detail:
   * Hash the fingerprint
   *   make it odd (*)
   *     flip the sign if starting index is odd
   *       sum with starting index (**)
   *         and modulo to 0..m-1
   *
   * (*) Constraining the CuckooTable to an even size in buckets, and applying odd offsets
   *     guarantees opposite parities for index & altIndex. The parity of the starting index
   *     determines whether the offset is subtracted from or added to the starting index.
   *     This strategy guarantees altIndex() is reversible, i.e.
   *
   *       index == altIndex(altIndex(index, fingerprint, m), fingerprint, m)
   *
   * (**) Summing the starting index and offset can possibly lead to numeric overflow. See
   *      {@link #protectedSum(long, long, long)} protectedSum} for details on how this is
   *      avoided.
   *
   * @param index starting index
   * @param fingerprint fingerprint
   * @param m size of table in buckets; must be even for this strategy
   * @return an alternate index for fingerprint bounded by 0..m-1
   */
  @Override
  public long altIndex(long index, int fingerprint, long m) {
    checkArgument(0L <= index, "index must be a positive!");
    checkArgument((0L <= m) && (0L == (m & 0x1L)), "m must be a positive even number!");
    return mod(protectedSum(index, parsign(index) * odd(hash(fingerprint)), m), m);
  }

  /**
   * Maps parity of i to a sign.
   *
   * @return 1 if i is even parity, -1 if i is odd parity
   */
  static long parsign(long i) {
    return ((i & 0x01L) * -2L) + 1L;
  }

  static int hash(int i) {
    return hashFunction.hashInt(i).asInt();
  }

  static long odd(long i) {
    return i | 0x01L;
  }

  /**
   * Returns the sum of index and offset, reduced by a mod-consistent amount if necessary to
   * protect from numeric overflow. This method is intended to support a subsequent mod operation
   * on the return value.
   *
   * @param index Assumed to be >= 0L.
   * @param offset Any value.
   * @param mod Value used to reduce the result,
   * @return sum of index and offset, reduced by a mod-consistent amount if necessary to protect
   *         from numeric overflow.
   */
  static long protectedSum(long index, long offset, long mod) {
    return canSum(index, offset) ? index + offset : protectedSum(index - mod, offset, mod);
  }

  static boolean canSum(long a, long b) {
    return (a ^ b) < 0 | (a ^ (a + b)) >= 0;
  }

}
