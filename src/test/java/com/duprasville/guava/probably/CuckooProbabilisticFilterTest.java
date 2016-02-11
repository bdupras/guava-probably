package com.duprasville.guava.probably;

import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;
import com.google.common.hash.PrimitiveSink;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Random;

import static com.google.common.base.Charsets.UTF_8;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class CuckooProbabilisticFilterTest {
  ProbabilisticFilter<String> filter;
  ProbabilisticFilter<String> filter2;
  ProbabilisticFilter<String> tinyFilter;

  private CuckooFilter<String> filter() {
    return CuckooFilter.create(Funnels.stringFunnel(UTF_8), 1000000, 0.3D);
  }

  private CuckooFilter<String> objectFilter() {
    return CuckooFilter.create(new Funnel<Object>() {
      public void funnel(Object from, PrimitiveSink into) {
        into.putInt(from.hashCode());
      }
    }, 1000000, 0.3D);
  }

  private CuckooFilter<String> tinyFilter() {
    return CuckooFilter.create(Funnels.stringFunnel(UTF_8), 1, 0.9D);
  }

  private CuckooFilter<String> bigFilter() {
    return CuckooFilter.create(Funnels.stringFunnel(UTF_8), 10L + Integer.MAX_VALUE, 0.3D);
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

  @Test
  public void addEShouldReturnFalseWhenFilterIsFull() {
    assert tinyFilter.add("foo");
    assert tinyFilter.add("bar");
    assert tinyFilter.add("baz");
    assert tinyFilter.add("boz");
    assertFalse(tinyFilter.add("bust"));
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
  public void addAllCollectionOfEContainingTooManyItemsShouldReturnFalse() {
    assertFalse(tinyFilter.addAll(Arrays.asList("foo", "bar", "baz", "boz", "foz")));
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
    filter.addAll((ProbabilisticFilter<String>) null);
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
  public void removeEThatIsContainedShouldReturnTrue() {
    assert !filter.contains("foo");
    assert filter.add("foo");
    assert filter.contains("foo");
    assertTrue(filter.remove("foo"));
    assertFalse(filter.contains("foo"));
  }

  @Test
  public void removeEThatIsNotContainedShouldReturnFalse() {
    assert !filter.contains("foo");
    assertFalse(filter.remove("foo"));
  }

  @Test(expected = NullPointerException.class)
  public void removeNullShouldThrowNullPointerException() {
    filter.remove(null);
    fail();
  }

  @Test
  public void removeAllCollectionOfEThatIsFullyContainedShouldReturnTrue() {
    assert filter.addAll(Arrays.asList("foo", "bar"));
    assert filter.containsAll(Arrays.asList("foo", "bar"));
    assertTrue(filter.removeAll(Arrays.asList("foo", "bar")));
  }

  @Test
  public void removeAllCollectionOfEThatIsNotFullyContainedShouldReturnFalse() {
    assert filter.addAll(Arrays.asList("foo", "bar"));
    assert filter.containsAll(Arrays.asList("foo", "bar"));
    assert 2 == filter.size();
    assertFalse(filter.removeAll(Arrays.asList("foo", "boom")));
    assertEquals(1, filter.size());
  }

  @Test(expected = NullPointerException.class)
  public void removeAllCollectionOfEContainingNullShouldThrowNullPointerException() {
    assert filter.addAll(Arrays.asList("foo", "bar"));
    assert filter.containsAll(Arrays.asList("foo", "bar"));
    assert 2 == filter.size();
    filter.removeAll(Arrays.asList("foo", null));
    fail();
  }

  @Test
  public void removeAllProbabilisticFilterOfEThatIsFullyContainedShouldReturnTrue() {
    assert filter.addAll(Arrays.asList("foo", "bar", "baz", "boz"));
    assert filter.containsAll(Arrays.asList("foo", "bar", "baz", "boz"));
    assert 4 == filter.size();
    assert filter2.addAll(Arrays.asList("baz", "boz"));
    assert filter2.containsAll(Arrays.asList("baz", "boz"));
    assert 2 == filter2.size();
    assertTrue(filter.removeAll(filter2));
    assertEquals(2, filter.size());
  }

  @Test
  public void removeAllProbabilisticFilterOfEThatIsNotFullyContainedShouldReturnFalse() {
    assert filter.addAll(Arrays.asList("foo", "bar", "baz", "boz"));
    assert filter.containsAll(Arrays.asList("foo", "bar"));
    assert 4 == filter.size();
    assertFalse(filter.removeAll(Arrays.asList("foo", "bar", "boom")));
    assertEquals(2, filter.size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void removeAllProbabilisticFilterOfEThatIsNotCompatibleShouldThrowIllegalArgumentException() {
    filter.removeAll(tinyFilter);
    fail();
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
    filter.removeAll(Arrays.asList("foo", null));
    fail();
  }

  @Test
  public void containsAllProbabilisticFilterOfEThatIsFullyContainedShouldReturnTrue() {
    assert filter.addAll(Arrays.asList("foo", "bar", "baz"));
    assert filter2.addAll(Arrays.asList("foo", "bar"));
    assertTrue(filter.containsAll(filter2));
    assert filter2.add("baz");
    assertTrue(filter.containsAll(filter2));
  }

  @Test
  public void containsAllProbabilisticFilterOfEThatIsNotFullyContainedShouldReturnFalse() {
    assert filter.addAll(Arrays.asList("foo", "bar", "baz"));
    assert filter2.addAll(Arrays.asList("foo", "bar", "boom"));
    assertFalse(filter.containsAll(filter2));
    assert filter2.add("baz");
    assertFalse(filter.containsAll(filter2));
  }

  @Test
  public void isEmpty() {
    assertTrue(filter.isEmpty());
    assert filter.add("foo");
    assertFalse(filter.isEmpty());
    assert filter.remove("foo");
    assertTrue(filter.isEmpty());
    assert filter.addAll(Arrays.asList("foo", "bar", "baz"));
    assertFalse(filter.isEmpty());
    assert filter.removeAll(Arrays.asList("foo", "bar", "baz"));
    assertTrue(filter.isEmpty());
    assert filter2.addAll(Arrays.asList("foo", "bar", "baz"));
    assert filter.addAll(filter2);
    assertFalse(filter.isEmpty());
    assert filter.removeAll(filter2);
    assertTrue(filter.isEmpty());
  }

  @Test
  public void sizeLong() {
    assertEquals(0L, filter.sizeLong());
    assert filter.add("foo");
    assertEquals(1L, filter.sizeLong());
    assert filter.remove("foo");
    assertEquals(0L, filter.sizeLong());
    assert filter.addAll(Arrays.asList("foo", "bar", "baz"));
    assertEquals(3L, filter.sizeLong());
    assert filter.removeAll(Arrays.asList("foo", "bar", "baz"));
    assertEquals(0L, filter.sizeLong());
    assert filter2.addAll(Arrays.asList("foo", "bar", "baz"));
    assert filter.addAll(filter2);
    assertEquals(3L, filter.sizeLong());
    assert filter.removeAll(filter2);
    assertEquals(0L, filter.sizeLong());
  }

  @Test
  @Ignore
  public void sizeLongReturnsLongMaxValueWhenFilterSizeExceedsLogMaxValue() {
    fail("Test Not Implemented. Current filter impl can't be allocated at a sufficient size.");
  }

  @Test
  public void testSize() {
    assertEquals(0, filter.size());
    assert filter.add("foo");
    assertEquals(1, filter.size());
    assert filter.remove("foo");
    assertEquals(0, filter.size());
    assert filter.addAll(Arrays.asList("foo", "bar", "baz"));
    assertEquals(3, filter.size());
    assert filter.removeAll(Arrays.asList("foo", "bar", "baz"));
    assertEquals(0, filter.size());
    assert filter2.addAll(Arrays.asList("foo", "bar", "baz"));
    assert filter.addAll(filter2);
    assertEquals(3, filter.size());
    assert filter.removeAll(filter2);
    assertEquals(0, filter.size());
  }

  @Test @Ignore("Long execution times make me sad.")
  public void sizeReturnsIntegerMaxValueWhenFilterSizeExceedsIntegerMaxValue() {
    final CuckooFilter<String> bigFilter = bigFilter();
    bigFilter.fill(new Random(1L));
    assertTrue(bigFilter.sizeLong() > Integer.MAX_VALUE);
    assertEquals(Integer.MAX_VALUE, bigFilter.size());
  }

  @Test
  public void isCompatible() {
    assertTrue(filter().isCompatible(filter()));
    assertFalse(filter().isCompatible(tinyFilter()));
    assertFalse(filter().isCompatible(objectFilter()));
  }

  @Test
  public void currentFppShouldGrowWithEverySuccessfulInsertion() {
    assertEquals(0.0D, filter.currentFpp());
    double lastFpp = filter.currentFpp();
    for (int i = 0; i < filter.capacity(); i++) {
      if (filter.add(String.valueOf(i))) {
        double currentFpp = filter.currentFpp();
        assertTrue(lastFpp < currentFpp);
        lastFpp = currentFpp;
      }
    }
  }

  @Test
  public void capacity() {
    assertEquals(1000003, filter.capacity());
    assertEquals(1000003, CuckooFilter.create(Funnels.stringFunnel(UTF_8), 1000003, 0.9D).capacity());

    assertEquals(3, tinyFilter.capacity());
    assertEquals(3, CuckooFilter.create(Funnels.stringFunnel(UTF_8), 3, 0.9D).capacity());
  }

  @Test
  public void fpp() {
    assertEquals(0.3D, filter.fpp());
    assertEquals(0.9D, tinyFilter.fpp());
  }

}