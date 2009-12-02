package org.javacc.runtime;

import static org.junit.Assert.*;
import org.junit.Test;

public class CharArrayCharSequenceTest {
  @Test
  public void emptySequence() {
    CharArrayCharSequence s = new CharArrayCharSequence(new char[]{});
    assertEquals(0, s.length());
    try {
      s.charAt(0);
      fail();
    }
    catch (StringIndexOutOfBoundsException x) {
      // pass
    }
    assertEquals("", s.toString());
  }

  @Test
  public void zeroLengthSequence() {
    CharArrayCharSequence s = new CharArrayCharSequence("hello".toCharArray(), 0, 0);
    assertEquals(0, s.length());
    try {
      s.charAt(0);
      fail();
    }
    catch (StringIndexOutOfBoundsException x) {
      // pass
    }
    assertEquals("", s.toString());
  }

  @Test
  public void providesCharacters() {
    CharArrayCharSequence s = new CharArrayCharSequence("hello".toCharArray());
    assertEquals(5, s.length());
    assertEquals('h', s.charAt(0));
    assertEquals('o', s.charAt(4));
    assertEquals("hello", s.toString());
  }

  @Test
  public void providesSubSequence() {
    CharArrayCharSequence s = new CharArrayCharSequence("hello world".toCharArray());
    final CharSequence subSequence = s.subSequence(6, 11);
    assertEquals(5, subSequence.length());
    assertEquals('w', subSequence.charAt(0));
    assertEquals('d', subSequence.charAt(4));
    assertEquals("hello world", s.toString());
    assertEquals("world", subSequence.toString());
  }

  @Test
  public void validatesBounds() {
    CharArrayCharSequence s = new CharArrayCharSequence("hello world".toCharArray());
    try {
      s.charAt(-1);
      fail();
    }
    catch (StringIndexOutOfBoundsException e) {}
    try {
      s.charAt(11);
      fail();
    }
    catch (StringIndexOutOfBoundsException e) {}
    try {
      s.subSequence(-1, 1);
      fail();
    }
    catch (StringIndexOutOfBoundsException e) {}
    try {
      s.subSequence(1, 12);
      fail();
    }
    catch (StringIndexOutOfBoundsException e) {}
  }

  @Test
  public void equality() {
    CharSequence a = new CharArrayCharSequence("alpha".toCharArray());
    CharSequence b = new CharArrayCharSequence("alpha beta".toCharArray()).subSequence(0, 5);
    CharSequence c = new CharArrayCharSequence("beta".toCharArray());
    assertTrue(a.equals(a));
    assertTrue(a.equals(b));
    assertTrue(b.equals(a));
    assertTrue(b.equals(b));
    assertEquals(a.hashCode(), b.hashCode());
    assertFalse(a.equals(c));
    assertFalse(c.equals(a));
    assertFalse(a.hashCode() == c.hashCode());
  }
}
