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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.math.LongMath;
import com.google.common.primitives.Ints;

import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.pow;

class CuckooTable {
  static final int EMPTY_ENTRY = 0x00;

  public long[] data() {
    return data;
  }

  public long numBuckets() {
    return numBuckets;
  }

  public int numEntriesPerBucket() {
    return numEntriesPerBucket;
  }

  public int numBitsPerEntry() {
    return numBitsPerEntry;
  }

  final long[] data;
  final long numBuckets;
  final int numEntriesPerBucket;
  final int numBitsPerEntry;
  private long size;
  private long checksum;

  public CuckooTable(long numBuckets, int numEntriesPerBucket, int numBitsPerEntry) {
    this(new long[calculateDataLength(numBuckets, numEntriesPerBucket, numBitsPerEntry)]
        , numBuckets
        , numEntriesPerBucket
        , numBitsPerEntry
        , 0L
    );
  }

  CuckooTable(final long[] data, long numBuckets, int numEntriesPerBucket,
              int numBitsPerEntry, long checksum) {
    this(data, 0L, checksum, numBuckets, numEntriesPerBucket, numBitsPerEntry);
  }

  public CuckooTable(final long[] data, long size, long checksum, long numBuckets,
                     int numEntriesPerBucket, int numBitsPerEntry) {
    this.data = data;
    this.size = size;
    this.numBuckets = numBuckets;
    this.numEntriesPerBucket = numEntriesPerBucket;
    this.numBitsPerEntry = numBitsPerEntry;
    this.checksum = checksum;
  }

  public CuckooTable copy() {
    return new CuckooTable(
        data.clone(), size, checksum, numBuckets, numEntriesPerBucket, numBitsPerEntry);
  }

  public static int calculateDataLength(long numBuckets, int numEntriesPerBucket, int numBitsPerEntry) {
    checkArgument(numBuckets > 0, "numBuckets (%s) must be > 0", numBuckets);
    checkArgument(numEntriesPerBucket > 0, "numEntriesPerBucket (%s) must be > 0",
        numEntriesPerBucket);
    checkArgument(numBitsPerEntry > 0, "numBitsPerEntry (%s) must be > 0", numBitsPerEntry);

    return Ints.checkedCast(LongMath.divide(
        LongMath.checkedMultiply(numBuckets,
            LongMath.checkedMultiply(numEntriesPerBucket, numBitsPerEntry)),
        Long.SIZE, RoundingMode.CEILING));
  }

  public int findEntry(int value, long bucket) {
    for (int i = 0; i < numEntriesPerBucket; i++) {
      if (value == readEntry(bucket, i)) {
        return i;
      }
    }
    return -1;
  }

  public int countEntry(int value, long bucket) {
    int ret = 0;
    for (int i = 0; i < numEntriesPerBucket; i++) {
      if (value == readEntry(bucket, i)) {
        ret++;
      }
    }
    return ret;
  }

  public boolean hasEntry(int value, long bucket) {
    return findEntry(value, bucket) >= 0;
  }

  public int readEntry(long bucket, int entry) {
    return readBits(
        data, bitOffset(bucket, entry, numEntriesPerBucket, numBitsPerEntry), numBitsPerEntry);
  }

  public boolean swapAnyEntry(int valueIn, int valueOut, long bucket) {
    final int entry = findEntry(valueOut, bucket);
    if (entry >= 0) {
      final int kicked = swapEntry(valueIn, bucket, entry);
      assert valueOut == kicked : "expected valueOut [" + valueOut + "] != actual kicked [" +
          kicked + "]";
      return true;
    }
    return false;
  }

  int swapEntry(int value, long bucket, int entry) {
    final int kicked = writeBits(value, data,
        bitOffset(bucket, entry, numEntriesPerBucket, numBitsPerEntry), numBitsPerEntry);
    checksum += value - kicked;

    if ((EMPTY_ENTRY == value) && (EMPTY_ENTRY != kicked)) {
      size--;
    } else if ((EMPTY_ENTRY != value) && (EMPTY_ENTRY == kicked)) {
      size++;
    }
    assert size >= 0 : "Hmm - that's strange. CuckooTable size [" + size + "] shouldn't be < 0l";

    return kicked;
  }

  static long bitOffset(long bucket, int entry, int numEntriesPerBucket, int numBitsPerEntry) {
    return (bucket * numEntriesPerBucket + entry) * numBitsPerEntry;
  }

  static int dataIndex(long bit) {
    return (int) (bit >>> 6);
  }

  @VisibleForTesting
  static int readBits(final long[] data, long bit, int len) {
    final int startLower = (int) (bit % Long.SIZE);
    final int lenLower = Math.min(Long.SIZE - startLower, len);
    final int lenUpper = Math.max(len - lenLower, 0);

    final int indexUpper = dataIndex(bit + len);

    final long lower = (data[dataIndex(bit)] & mask(startLower, lenLower)) >>> startLower;
    final long upper = indexUpper < data.length ?
        (data[indexUpper] & mask(0, lenUpper)) << lenLower : 0x00L;

    return (int) (lower | upper);
  }

  @VisibleForTesting
  static int writeBits(int bits, final long[] data, long bit, int len) {
    final int ret = readBits(data, bit, len);

    final long bitsl = ((long) bits) & 0x00000000FFFFFFFFL; // upcast without carrying the sign

    final int startLower = (int) (bit % Long.SIZE);
    final int lenLower = Math.min(Long.SIZE - startLower, len);
    final int lenUpper = Math.max(len - lenLower, 0);

    final long maskLowerKeep = ~(mask(0, lenLower) << startLower);
    final long maskUpperKeep = mask(lenUpper, Long.SIZE - lenUpper);

    final long bitsLower = (bitsl << startLower) & ~maskLowerKeep;
    final long bitsUpper = (bitsl >>> (len - lenUpper)) & ~maskUpperKeep;

    final int indexLower = dataIndex(bit);
    final int indexUpper = dataIndex(bit + len - 1);

    final long dataLower = (data[indexLower] & maskLowerKeep) | bitsLower;
    data[indexLower] = dataLower;

    if (indexLower != indexUpper) {
      final long dataUpper = (data[indexUpper] & maskUpperKeep) | bitsUpper;
      data[indexUpper] = dataUpper;
    }

    return ret;
  }

  static long mask(int start, int len) {
    return (len <= 0) ? 0L : (0x8000000000000000L >> (len - 1)) >>> (Long.SIZE - (start + len));
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof CuckooTable) {
      CuckooTable that = (CuckooTable) o;
      return this.numBuckets == that.numBuckets
          && this.numEntriesPerBucket == that.numEntriesPerBucket
          && this.numBitsPerEntry == that.numBitsPerEntry
          && this.size == that.size
          && this.checksum == that.checksum
          ;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(numBuckets, numEntriesPerBucket, numBitsPerEntry, size,
        checksum);
  }

  public boolean isCompatible(CuckooTable that) {
    return this.numBuckets == that.numBuckets
        && this.numEntriesPerBucket == that.numEntriesPerBucket
        && this.numBitsPerEntry == that.numBitsPerEntry;
  }

  public long size() {
    return size < 0 ? /* indicates overflow */ Long.MAX_VALUE : size;
  }

  public long checksum() {
    return checksum;
  }

  public long bitSize() {
    return (long) data.length * Long.SIZE;
  }

  public long capacity() {
    return numBuckets * numEntriesPerBucket;
  }

  public double load() {
    return (double) size() / (double) capacity();
  }

  public double currentFpp() {
    return fppAtGivenLoad(load());
  }

  public double fppAtGivenLoad(double load) {
    return 1.0D - pow(
        ( pow(2, numBitsPerEntry) - 2 )
            /
            ( pow(2, numBitsPerEntry) - 1 )
        ,
        2 * numEntriesPerBucket * load
    );
  }

  public double averageBitsPerEntry() {
    return (double) bitSize() / (double) size;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
        "size=" + size +
        ", checksum=" + checksum +
        ", byteSize=" + bitSize() / Byte.SIZE +
        ", load=" + load() +
        ", capacity=" + capacity() +
        ", averageBitsPerEntry=" + averageBitsPerEntry() +
        ", numBuckets=" + numBuckets +
        ", numEntriesPerBucket=" + numEntriesPerBucket +
        ", numBitsPerEntry=" + numBitsPerEntry +
        '}';
  }

  public void clear() {
    Arrays.fill(data, 0L);
    size = 0L;
  }
}
