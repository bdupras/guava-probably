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
public class ProbabilisticFilterTest extends TestCase {
  Random random = new Random(1L);

  public void testBloom() throws Exception {
    for (double fpp = 0.0000001; fpp < 0.1; fpp *= 10) {
      for (int expectedInsertions = 100; expectedInsertions <= 10000; expectedInsertions *= 10) {
        basicTests(
            BloomFilter.create(Funnels.stringFunnel(UTF_8), expectedInsertions, fpp),
            expectedInsertions, fpp);
      }
    }
  }

  public void testCuckoo() throws Exception {
    for (double fpp = 0.0000001; fpp < 0.1; fpp *= 10) {
      for (int expectedInsertions = 100; expectedInsertions <= 10000; expectedInsertions *= 10) {
        basicTests(CuckooFilter.create(
            Funnels.stringFunnel(UTF_8), expectedInsertions, fpp,
            CuckooFilterStrategies.MURMUR128_BEALDUPRAS_32), expectedInsertions, fpp);
      }
    }
  }

  private void basicTests(
      final ProbabilisticFilter<CharSequence> filter, int numInsertions, double fpp) {
    checkArgument(numInsertions > 0, "numInsertions (%s) must be > 0", numInsertions);
    checkArgument(fpp > 0, "fpp (%s) must be > 0.0", fpp);

    assertEquals("expectedFpp should be 0 when filter is empty", 0.0D, filter.expectedFpp());

    assertFalse("mightContain should return false when filter is empty",
        filter.mightContain("Nope"));

    assertTrue("put should return true when inserting the first item", filter.put("Yep!"));

    int falseInsertions = 0;

    for (int i = 0; i < numInsertions - 1; i++) { //minus 1 since we've already inserted one above
      double expectedFppBefore = filter.expectedFpp();

      if (filter.put(Integer.toString(i))) {
        assertTrue("expectedFpp should not decrease after put returns true",
            filter.expectedFpp() >= expectedFppBefore);
      } else {
        falseInsertions++;
        assertEquals("expectedFpp should not change after put returns false",
            expectedFppBefore, filter.expectedFpp());
      }

      assertTrue("mightContain should return true when queried with an inserted item",
          filter.mightContain(Integer.toString(i)));
    }

    // fill up the filter until put has returned `true` numInsertion times in total
    //noinspection StatementWithEmptyBody
    while (filter.put(Integer.toString(random.nextInt())) && (--falseInsertions > 0)) ;

    assertWithMessage(
        "expectedFpp should be, approximately, at most the requested fpp after inserting the " +
            "requested number of items")
        .that(filter.expectedFpp())
        .isAtMost(fpp * 1.2);

    assertWithMessage(
        "expectedFpp should be, approximately, at least the half the requested fpp after " +
            "inserting the requested number of items: " + numInsertions + ", " + fpp)
        .that(filter.expectedFpp())
        .isAtLeast(fpp * 0.5);
  }

}
