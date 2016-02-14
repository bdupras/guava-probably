package com.duprasville.guava.probably;

import com.google.common.hash.Funnels;

import org.junit.Test;

import java.util.Arrays;

import static com.google.common.base.Charsets.UTF_8;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * CuckooFilter tests of ProbabilisticFilter interface implementations.
 *
 * @author Brian Dupras
 */
public class CuckooFilterProbabilisticFilterTest extends AbstractProbabilisticFilterTest {
  ProbabilisticFilter<CharSequence> filter(int capacity, double fpp) {
    return CuckooFilter.create(Funnels.stringFunnel(UTF_8), capacity, fpp);
  }

  @Test
  public void addEShouldReturnFalseWhenFilterIsFull() {
    for (int i = 0; i < tinyFilter.capacity() * 2; i++) {
      tinyFilter.add("foo" + i);
    }
    assertFalse(tinyFilter.add("bust"));
  }

  @Test
  public void addAllCollectionOfEContainingTooManyItemsShouldReturnFalse() {
    assertFalse(tinyFilter.addAll(Arrays.asList("foo", "bar", "baz", "boz", "foz", "biz", "fiz",
        "fuz", "buz")));
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
  public void size() {
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
    assertEquals(1000007, filter.capacity());
    assertEquals(1000003, filter(1000003, 0.9D).capacity());

    assertEquals(7, tinyFilter.capacity());
    assertEquals(3, filter(3, 0.9D).capacity());
  }

  @Test
  public void fpp() {
    assertEquals(FILTER_FPP, filter.fpp(), FILTER_FPP * 0.1);
    assertEquals(TINY_FILTER_FPP, tinyFilter.fpp(), TINY_FILTER_FPP * 0.1);
  }

}