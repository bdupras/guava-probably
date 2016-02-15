package com.duprasville.guava.probably;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.truth.Truth.assertWithMessage;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Common tests of ProbabilisticFilter interface implementations.
 *
 * @author Brian Dupras
 */
public abstract class AbstractProbabilisticFilterTest {
  abstract ProbabilisticFilter<CharSequence> filter(int capacity, double fpp);

  Random random = new Random(1L);
  static final int FILTER_CAPACITY = 1000000;
  static double FILTER_FPP = 0.002D;
  ProbabilisticFilter<CharSequence> filter;
  ProbabilisticFilter<CharSequence> filter2;

  static final int TINY_FILTER_CAPACITY = 1;
  static final double TINY_FILTER_FPP = 0.002D;
  ProbabilisticFilter<CharSequence> tinyFilter;

  private ProbabilisticFilter<CharSequence> filter() {
    return filter(FILTER_CAPACITY, FILTER_FPP);
  }

  private ProbabilisticFilter<CharSequence> tinyFilter() {
    return filter(TINY_FILTER_CAPACITY, TINY_FILTER_FPP);
  }

  @Before
  public void setUp() {
    filter = filter();
    filter2 = filter();
    tinyFilter = tinyFilter();
  }

  @Test
  public void addEShouldReturnTrueWhenFilterIsNotFull() {
    assertTrue(filter.add("foo"));
    assertTrue(filter.contains("foo"));
  }

  @Test(expected = NullPointerException.class)
  public void addNullShouldThrowNullPointerException() {
    filter.add(null);
    fail();
  }

  @Test
  public void addAllCollectionOfEShouldReturnTrue() {
    assertTrue(filter.addAll(Arrays.asList("foo", "bar")));
    assertTrue(filter.containsAll(Arrays.asList("bar", "foo")));
  }

  @Test(expected = NullPointerException.class)
  public void addAllNullCollectionShouldThrowNullPointerException() {
    filter.addAll((Collection<String>) null);
    fail();
  }

  @Test(expected = NullPointerException.class)
  public void addAllCollectionOfEContainingNullShouldThrowNullPointerException() {
    filter.addAll(Arrays.asList("foo", "bar", null));
    fail();
  }

  @Test
  public void addAllProbabilisticFilterOfEShouldReturnTrue() {
    assert filter.addAll(Arrays.asList("foo", "bar", "baz", "boz", "foz"));
    assert filter2.addAll(Arrays.asList("foo2", "bar2", "baz2", "boz2", "foz2"));
    assertTrue(filter.addAll(filter2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void addAllProbabilisticFilterOfEThatIsNotCompatibleShouldThrowIllegalArgumentException() {
    filter.addAll(tinyFilter);
    fail();
  }

  @Test(expected = NullPointerException.class)
  public void addAllNullProbabilisticFilterShouldThrowNullPointerException() {
    filter.addAll((ProbabilisticFilter<CharSequence>) null);
    fail();
  }

  @Test
  public void clearShouldRemovePreviouslyContainedElements() {
    assert 0 == filter.size();
    assert filter.add("foo");
    assert 1 == filter.size();
    assert filter.contains("foo");
    filter.clear();
    assertEquals(0, filter.size());
    assertFalse(filter.contains("foo"));
  }

  @Test
  public void containsEThatIsContainedShouldReturnTrue() {
    assert filter.add("foo");
    assertTrue(filter.contains("foo"));
  }

  @Test
  public void containsEThatIsNotContainedShouldReturnFalse() {
    assertFalse(filter.contains("bar"));
  }

  @Test(expected = NullPointerException.class)
  public void containsNullShouldThrowNullPointerException() {
    filter.contains(null);
    fail();
  }

  @Test
  public void containsAllCollectionOfEThatIsFullyContainedShouldReturnTrue() {
    assert filter.addAll(Arrays.asList("foo", "bar", "baz"));
    assertTrue(filter.containsAll(Arrays.asList("foo", "bar")));
    assertTrue(filter.containsAll(Arrays.asList("foo", "bar", "baz")));
  }

  @Test
  public void containsAllCollectionOfEThatIsNotFullyContainedShouldReturnFalse() {
    assert filter.addAll(Arrays.asList("foo", "bar", "baz"));
    assertFalse(filter.containsAll(Arrays.asList("foo", "bar", "boom")));
    assertFalse(filter.containsAll(Arrays.asList("foo", "bar", "baz", "boom")));
  }

  @Test(expected = NullPointerException.class)
  public void containsAllCollectionOfEContainingNullShouldThrowNullPointerException() {
    assert filter.addAll(Arrays.asList("foo", "bar", "baz"));
    filter.containsAll(Arrays.asList("foo", null));
    fail();
  }

  @Test
  @Ignore
  public void sizeLongReturnsLongMaxValueWhenFilterSizeExceedsLogMaxValue() {
    fail("Test Not Implemented. Current filter impls can't be allocated at a sufficient size.");
  }

  @Test @Ignore("Test not yet implemented.")
  public void sizeReturnsIntegerMaxValueWhenFilterSizeExceedsIntegerMaxValue() {
    fail("Test not yet implemented.");
  }

  @Test
  public void isCompatible() {
    assertTrue(filter().isCompatible(filter()));
    assertFalse(filter().isCompatible(tinyFilter()));
  }

  @Test
  abstract public void capacity();

  @Test
  abstract public void fpp();

  @Test
  public void basicGenerativeTests() throws Exception {
    for (double fpp = 0.0000001; fpp < 0.1; fpp *= 10) {
      for (int capacity = 100; capacity <= 100000; capacity *= 10) {
        basicTests(filter(capacity, fpp), capacity, fpp);
      }
    }
  }

  private void basicTests(
      final ProbabilisticFilter<CharSequence> filter, int capacity, double fpp) {
    checkArgument(capacity > 0, "capacity (%s) must be > 0", capacity);
    checkArgument(fpp > 0, "fpp (%s) must be > 0.0", fpp);

    assertEquals("currentFpp should be 0 when filter is empty", 0.0D, filter.currentFpp());

    assertFalse("contains should return false when filter is empty", filter.contains("Nope"));

    assertTrue("add should return true when inserting the first item", filter.add("Yep!"));

    int falseInsertions = 0;

    for (int i = 0; i < capacity - 1; i++) { //minus 1 since we've already inserted one above
      double currentFppBefore = filter.currentFpp();

      if (filter.add(Integer.toString(i))) {
        assertTrue("currentFpp should not decrease after put returns true",
            filter.currentFpp() >= currentFppBefore);

        assertTrue("contains should return true when queried with an inserted item",
            filter.contains(Integer.toString(i)));
      } else {
        falseInsertions++;
        assertEquals("currentFpp should not change after put returns false",
            currentFppBefore, filter.currentFpp());
      }
    }

    // fill up the filter until put has returned `true` numInsertion times in total
    //noinspection StatementWithEmptyBody
    while (filter.add(Integer.toString(random.nextInt())) && (--falseInsertions > 0)) ;

    assertWithMessage(
        "currentFpp should be, approximately, at most the requested fpp after inserting the " +
            "requested number of items")
        .that(filter.currentFpp())
        .isAtMost(fpp * 1.3);

    assertWithMessage(
        "currentFpp should be, approximately, at least the half the requested fpp after " +
            "inserting the requested number of items: " + capacity + ", " + fpp)
        .that(filter.currentFpp())
        .isAtLeast(fpp * 0.65);
  }
}