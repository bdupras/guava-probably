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

import com.google.common.hash.Funnels;

import junit.framework.TestCase;

import java.util.Random;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for implementations of ProbabilisticFilter.
 *
 * @author Brian Dupras
 */
public class ProbabilisticFilterImplsTest extends TestCase {
  Random random = new Random(1L);

  public void testBloom() throws Exception {
    for (double fpp = 0.0000001; fpp < 0.1; fpp *= 10) {
      for (int capacity = 100; capacity <= 10000; capacity *= 10) {
        basicTests(
            BloomFilter.create(Funnels.stringFunnel(UTF_8), capacity, fpp),
            capacity, fpp);
      }
    }
  }

  public void testCuckoo() throws Exception {
    for (double fpp = 0.0000001; fpp < 0.1; fpp *= 10) {
      for (int capacity = 100; capacity <= 10000; capacity *= 10) {
        basicTests(CuckooFilter.create(
            Funnels.stringFunnel(UTF_8), capacity, fpp,
            CuckooStrategies.MURMUR128_BEALDUPRAS_32.strategy()), capacity, fpp);
      }
    }
  }

  private void basicTests(
      final ProbabilisticFilter<CharSequence> filter, int capacity, double fpp) {
    checkArgument(capacity > 0, "capacity (%s) must be > 0", capacity);
    checkArgument(fpp > 0, "fpp (%s) must be > 0.0", fpp);

    assertEquals("expectedFpp should be 0 when filter is empty", 0.0D, filter.currentFpp());

    assertFalse("mightContain should return false when filter is empty",
        filter.contains("Nope"));

    assertTrue("put should return true when inserting the first item", filter.add("Yep!"));

    int falseInsertions = 0;

    for (int i = 0; i < capacity - 1; i++) { //minus 1 since we've already inserted one above
      double expectedFppBefore = filter.currentFpp();

      if (filter.add(Integer.toString(i))) {
        assertTrue("expectedFpp should not decrease after put returns true",
            filter.currentFpp() >= expectedFppBefore);
      } else {
        falseInsertions++;
        assertEquals("expectedFpp should not change after put returns false",
            expectedFppBefore, filter.currentFpp());
      }

      assertTrue("mightContain should return true when queried with an inserted item",
          filter.contains(Integer.toString(i)));
    }

    // fill up the filter until put has returned `true` numInsertion times in total
    //noinspection StatementWithEmptyBody
    while (filter.add(Integer.toString(random.nextInt())) && (--falseInsertions > 0)) ;

    assertWithMessage(
        "expectedFpp should be, approximately, at most the requested fpp after inserting the " +
            "requested number of items")
        .that(filter.currentFpp())
        .isAtMost(fpp * 1.2);

    assertWithMessage(
        "expectedFpp should be, approximately, at least the half the requested fpp after " +
            "inserting the requested number of items: " + capacity + ", " + fpp)
        .that(filter.currentFpp())
        .isAtLeast(fpp * 0.5);
  }

}
