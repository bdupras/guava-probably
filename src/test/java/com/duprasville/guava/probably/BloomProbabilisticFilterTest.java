package com.duprasville.guava.probably;

import com.google.common.hash.Funnels;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static com.google.common.base.Charsets.UTF_8;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * BloomFilter tests of ProbabilisticFilter interface implementations.
 *
 * @author Brian Dupras
 */
public class BloomProbabilisticFilterTest extends AbstractProbabilisticFilterTest {
  ProbabilisticFilter<CharSequence> filter(int capacity, double fpp) {
    return BloomFilter.create(Funnels.stringFunnel(UTF_8), capacity, fpp);
  }

  @Test
  public void addEShouldReturnTrueWhenFilterIsFull() {
    for (int i = 0; i < tinyFilter.capacity() * 2; i++) {
      tinyFilter.add("foo" + i);
    }
    assertTrue("Bloom filters cannot reject additions", tinyFilter.add("bust"));
  }

  @Test
  public void addAllCollectionOfEContainingTooManyItemsShouldReturnTrue() {
    assertTrue(tinyFilter.addAll(Arrays.asList("foo", "bar", "baz", "boz", "foz", "biz", "fiz",
        "fuz", "buz")));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void removeEShouldThrowUnsupportedOperationException() {
    filter.remove("nope");
  }

  @Test(expected = UnsupportedOperationException.class)
  public void removeAllCollectionOfEShouldThrowUnsupportedOperationException() {
    filter.removeAll(Arrays.asList("nope", "neither"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void removeAllProbabilisticFilterOfEShouldThrowUnsupportedOperationException() {
    filter.removeAll(filter);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void containsAllProbabilisticFilterShouldThrowUnsupportedOperationException() {
    filter.containsAll(filter);
  }

  @Test
  public void currentFppShouldGrowOrStayTheSameWithEverySuccessfulInsertion() {
    assertEquals(0.0D, filter.currentFpp());
    double lastFpp = filter.currentFpp();
    for (int i = 0; i < filter.capacity(); i++) {
      if (filter.add(String.valueOf(i))) {
        double currentFpp = filter.currentFpp();
        assertTrue(lastFpp <= currentFpp);
        lastFpp = currentFpp;
      }
    }
  }

  @Override
  public void capacity() {
    assertEquals(1000000, filter.capacity());
    assertEquals(1000003, filter(1000003, 0.9D).capacity());

    assertEquals(1, tinyFilter.capacity());
    assertEquals(3, filter(3, TINY_FILTER_FPP).capacity());
  }

  @Override
  public void fpp() {
    assertEquals(FILTER_FPP, filter.fpp(), FILTER_FPP);
    assertEquals(TINY_FILTER_FPP, tinyFilter.fpp(), TINY_FILTER_FPP);
  }
}