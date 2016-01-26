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

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import com.duprasville.guava.probably.CuckooFilter;
import com.duprasville.guava.probably.CuckooStrategyMurmurBealDupras32;

import junit.framework.TestCase;

import java.util.Random;

import static com.duprasville.guava.probably.CuckooStrategies.MURMUR128_BEALDUPRAS_32;
import static com.duprasville.guava.probably.CuckooStrategies.RESERVED;
import static com.duprasville.guava.probably.CuckooStrategies.values;
import static com.duprasville.guava.probably.CuckooTable.readBits;
import static com.duprasville.guava.probably.CuckooTable.writeBits;
import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for CuckooFilterStrategies.
 *
 * @author Brian Dupras
 */
public class CuckooStrategiesTest extends TestCase {
  public void testBloom() throws Exception {
    int numInsertions = 1000000;
    double fpp = 0.03D;
    Random random = new Random(1L);

    BloomFilter<Long> filter = BloomFilter.create(Funnels.longFunnel(), numInsertions, fpp);
    for (int l = 0; l < numInsertions; l++) {
      filter.put(random.nextLong());
    }

    random = new Random(1L);
    for (int l = 0; l < numInsertions; l++) {
      assertTrue(filter.mightContain(random.nextLong()));
    }
  }

  public void testCuckoo() throws Exception {
    int numInsertions = 1000000;
    double fpp = 0.03D;
    Random random = new Random(1L);

    CuckooFilter<Long> filter = CuckooFilter.create(Funnels.longFunnel(), numInsertions, fpp);

    for (int l = 0; l < numInsertions; l++) {
      final long nextLong = random.nextLong();
      assertTrue("Filter should put " + nextLong, filter.add(nextLong));
      assertTrue("Filter should mightContain " + nextLong + " just after adding",
          filter.contains(nextLong));
      assertEquals("Filter size should be the same as the number of insertions", l + 1,
          filter.size());
    }

    random = new Random(1L);
    for (int l = 0; l < numInsertions; l++) {
      final long nextLong = random.nextLong();
      assertTrue("Filter should mightContain " + nextLong + " after adding a while ago",
          filter.contains(nextLong));
    }

    random = new Random(1L);
    for (int l = 0; l < numInsertions; l++) {
      final long nextLong = random.nextLong();
      assertTrue("Filter should delete " + nextLong + " after adding a while ago",
          filter.remove(nextLong));
      assertEquals("Filter size should be the same as the number of insertions less the " +
          "number of deletions", numInsertions - l - 1, filter.size());
    }

    final long nextLong = random.nextLong();
    assertFalse("Filter should NOT delete " + nextLong + " since it should be empty!!",
        filter.remove(nextLong));
  }

  public void testFingerprintBoundaries() throws Exception {
    assertThat(CuckooStrategyMurmurBealDupras32.fingerprint(0x80000000, 1)).isEqualTo(0x01);
    assertThat(CuckooStrategyMurmurBealDupras32.fingerprint(0xC0000000, 2)).isEqualTo(0x03);
    assertThat(CuckooStrategyMurmurBealDupras32.fingerprint(0xE0000000, 3)).isEqualTo(0x04);
    assertThat(CuckooStrategyMurmurBealDupras32.fingerprint(0xE0000000, 8)).isEqualTo(0xE0);
    assertThat(CuckooStrategyMurmurBealDupras32.fingerprint(0xE0000000, 16)).isEqualTo(0xE000);
    assertThat(CuckooStrategyMurmurBealDupras32.fingerprint(0x80000000, Integer.SIZE)).isEqualTo(0x80000000);
    for (int f = 1; f < Integer.SIZE; f++) {
      assertThat(CuckooStrategyMurmurBealDupras32.fingerprint(0x00, f)).isNotEqualTo(0x00);
    }
  }

  public void testIndexIsModuloM() throws Exception {
    final int min = Integer.MIN_VALUE;
    final int max = Integer.MAX_VALUE;
    final int incr = 100000;
    final long m = 0x1DEAL;

    for (int hash = min; hash != next(hash, incr, max); hash = next(hash, incr, max)) {
      final long index = new CuckooStrategyMurmurBealDupras32(-1).index(hash, m);
      assertThat(index).isLessThan(m);
      assertThat(index).isGreaterThan(-1L);
    }
  }

  public void testAltIndexIsReversible() throws Exception {
    final long max = Long.MAX_VALUE - 1L; // must be even!
    final long incr = 1000000L;
    final Random random = new Random(1L);
    final byte[] fingerprint = new byte[1];

    for (long index = 0; index != next(index, incr, max); index = next(index, incr, max)) {
      random.nextBytes(fingerprint);
      int f = (random.nextInt(126) + 1) * (random.nextBoolean() ? 1 : -1);
      final long altIndex = new CuckooStrategyMurmurBealDupras32(-1).altIndex(index, f, max);
      final long altAltIndex = new CuckooStrategyMurmurBealDupras32(-1).altIndex(altIndex, f, max);
      assertEquals("index should equal altIndex(altIndex(index)):" + f, index, altAltIndex);
    }
  }

  /**
   * This test will fail whenever someone updates/reorders the BloomFilterStrategies constants. Only
   * appending a new constant is allowed.
   */
  public void testCuckooFilterStrategies() {
    assertThat(values()).hasLength(2);
    assertEquals(RESERVED, values()[0]);
    assertEquals(MURMUR128_BEALDUPRAS_32, values()[1]);
  }

  public void testWriteBits() throws Exception {
    long[] data;

    data = new long[]{0xfafafafafafafafal, 0xfafafafafafafafal};
    assertEquals(0xfafa, writeBits(0xABCD, data, 0, 16));
    assertEquals(0xfafafafafafaABCDl, data[0]);
    assertEquals(0xfafafafafafafafal, data[1]);

    data = new long[]{0xfafafafafafafafal, 0xfafafafafafafafal};
    assertEquals(0xfafa, writeBits(0xABCD, data, 32, 16));
    assertEquals(0xfafaABCDfafafafal, data[0]);
    assertEquals(0xfafafafafafafafal, data[1]);

    data = new long[]{0xfafafafafafafafal, 0xfafafafafafafafal};
    assertEquals(0xfafa, writeBits(0xABCD, data, 48, 16));
    assertEquals(0xABCDfafafafafafal, data[0]);
    assertEquals(0xfafafafafafafafal, data[1]);

    data = new long[]{0xfafafafafafafafal, 0xfafafafafafafafal};
    assertEquals(0x7D7D, writeBits(0xABCD, data, 49, 16));
    assertEquals(0x579Afafafafafafal, data[0]);
    assertEquals(0xfafafafafafafafBl, data[1]);

    data = new long[]{0xfafafafafafafafal, 0xfafafafafafafafal};
    assertEquals(0xfafa, writeBits(0xABCD, data, 56, 16));
    assertEquals(0xCDfafafafafafafal, data[0]);
    assertEquals(0xfafafafafafafaABl, data[1]);

    data = new long[]{0xfafafafafafafafal, 0xfafafafafafafafal};
    assertEquals(0xfafa, writeBits(0xABCD, data, 64, 16));
    assertEquals(0xfafafafafafafafal, data[0]);
    assertEquals(0xfafafafafafaABCDl, data[1]);

    data = new long[]{0xfafafafafafafafal, 0xfafafafafafafafal};
    assertEquals(0xfafa, writeBits(0xABCD, data, 112, 16));
    assertEquals(0xfafafafafafafafal, data[0]);
    assertEquals(0xABCDfafafafafafal, data[1]);
  }

  public void testReadBits() throws Exception {
    assertEquals(0xABCD, readBits(new long[]{0x000000000000ABCDL, 0x00000000000000FFL}, 0, 16));
    assertEquals(0xABCD, readBits(new long[]{0x0000ABCD00000000L, 0x00000000000000FFL}, 32, 16));
    assertEquals(0xABCD, readBits(new long[]{0xABCD000000000000L, 0x00000000000000FFL}, 48, 16));
    assertEquals(0xABCD, readBits(new long[]{0xABCD000000000000L << 1, 0x00000000000FFL}, 49, 16));
    assertEquals(0xABCD, readBits(new long[]{0xCD00000000000000L, 0x0000000000000FABL}, 56, 16));
    assertEquals(0xABCD, readBits(new long[]{0xFF00000000000000L, 0x000000000000ABCDL}, 64, 16));

    assertEquals(0x01CD, readBits(new long[]{0x000000000000ABCDL, 0x00000000000000FFL}, 0, 9));
    assertEquals(0x01CD, readBits(new long[]{0x0000ABCD00000000L, 0x00000000000000FFL}, 32, 9));
    assertEquals(0x01CD, readBits(new long[]{0xABCD000000000000L, 0x00000000000000FFL}, 48, 9));
    assertEquals(0x01CD, readBits(new long[]{0xABCD000000000000L << 1, 0x00000000000FFL}, 49, 9));
    assertEquals(0x01CD, readBits(new long[]{0xCD00000000000000L, 0x0000000000000FABL}, 56, 9));
    assertEquals(0x01CD, readBits(new long[]{0xFF00000000000000L, 0x000000000000ABCDL}, 64, 9));
  }


  // Test utilities

  private int next(int start, int incr, int max) {
    int ret = start + max / incr;
    return ((ret < start) || (ret > max)) ? max : ret;
  }

  private long next(long start, long incr, long max) {
    long ret = start + max / incr;
    return ((ret < start) || (ret > max)) ? max : ret;
  }
}
