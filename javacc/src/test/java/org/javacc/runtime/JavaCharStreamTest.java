package org.javacc.runtime;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;

public class JavaCharStreamTest extends CharStreamBaseTestCase {
  @Override
  CharStream makeCharStream(final CharSequence content) {
    return new JavaCharStream(new StringReader(content.toString()));
  }

  @Test
  public void readsUnicodeEscapes() throws IOException {
    final InputStream inputStream = getClass().getResourceAsStream("escapes.txt");
    final BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));

    // case 1
    {
      assertEquals(1, "\uu005a".length());
      final String s = r.readLine();
      assertEquals(7, s.length());
      final JavaCharStream cs = new JavaCharStream(new StringReader(s));
      assertEquals('Z', cs.readChar());
      assertEquals("Z", cs.getImage());
      assertEquals(0, cs.getBeginOffset());
      assertEquals(7, cs.getEndOffset());
      cs.backup(1);
      assertEquals('Z', cs.readChar());
      assertEquals("Z", cs.getImage());
      assertEquals(0, cs.getBeginOffset());
      assertEquals(7, cs.getEndOffset());
      assertEquals(-1, cs.readChar());
    }
    // case 2
    {
      assertEquals(7, "\\uu005a".length());
      final String s = r.readLine();
      assertEquals(8, s.length());
      final JavaCharStream cs = new JavaCharStream(new StringReader(s));
      assertEquals('\\', cs.readChar());
      assertEquals('\\', cs.readChar());
      assertEquals('u', cs.readChar());
      assertEquals('u', cs.readChar());
      assertEquals('0', cs.readChar());
      assertEquals('0', cs.readChar());
      assertEquals('5', cs.readChar());
      assertEquals('a', cs.readChar());
      assertEquals("\\\\uu005a", cs.getImage());
      assertEquals(0, cs.getBeginOffset());
      assertEquals(8, cs.getEndOffset());
      assertEquals(-1, cs.readChar());
    }
    // case 3
    {
      assertEquals(2, "\\\uu005a".length());
      final String s = r.readLine();
      assertEquals(9, s.length());
      final JavaCharStream cs = new JavaCharStream(new StringReader(s));
      assertEquals('\\', cs.readChar());
      assertEquals('\\', cs.readChar());
      assertEquals('Z', cs.readChar());
      assertEquals("\\\\Z", cs.getImage());
      assertEquals(0, cs.getBeginOffset());
      assertEquals(9, cs.getEndOffset());
      assertEquals(-1, cs.readChar());
    }
    // case 4
    {
      assertEquals(8, "\\\\uu005a".length());
      final String s = r.readLine();
      assertEquals(10, s.length());
      final JavaCharStream cs = new JavaCharStream(new StringReader(s));
      assertEquals('\\', cs.readChar());
      assertEquals('\\', cs.readChar());
      assertEquals('\\', cs.readChar());
      assertEquals('\\', cs.readChar());
      assertEquals('u', cs.readChar());
      assertEquals('u', cs.readChar());
      assertEquals('0', cs.readChar());
      assertEquals('0', cs.readChar());
      assertEquals('5', cs.readChar());
      assertEquals('a', cs.readChar());
      assertEquals("\\\\\\\\uu005a", cs.getImage());
      assertEquals(0, cs.getBeginOffset());
      assertEquals(10, cs.getEndOffset());
      assertEquals(-1, cs.readChar());
    }
  }

  @Test
  public void skipsIncompleteEscapes() throws IOException {
    // prematurely ended escape sequence
    final JavaCharStream cs = new JavaCharStream(new StringReader("\\uuu123"));
    assertEquals('\\', cs.readChar());
    assertEquals('u', cs.readChar());
    assertEquals('u', cs.readChar());
    assertEquals('u', cs.readChar());
    assertEquals('1', cs.readChar());
    assertEquals('2', cs.readChar());
    assertEquals('3', cs.readChar());
    assertEquals(-1, cs.readChar());
  }

  @Test
  public void validatesEscapeSequence() throws IOException {
    // escape sequence contains illegal hex digits
    final JavaCharStream cs = new JavaCharStream(new StringReader("\\uuu000X"));
    String s = null;
    try {
      cs.readChar();
    }
    catch (IOException ex) {
      s = ex.getMessage();
    }
    assertEquals("Illegal hex digit in escape sequence 'X' at 1,8", s);
  }

  @Test
  public void beginTokenRetainsBackedUpCharacters() throws IOException {
    final JavaCharStream cs = new JavaCharStream(new StringReader("123"));
    assertEquals('1', cs.readChar());
    assertEquals('2', cs.readChar());
    assertEquals('3', cs.readChar());
    assertEquals("123", cs.getImage());
    cs.backup(2);
    assertEquals("1", cs.getImage());
    cs.beginToken();
    assertEquals('2', cs.readChar());
    assertEquals('3', cs.readChar());
    assertEquals("23", cs.getImage());
    assertEquals(1, cs.getBeginOffset());
    assertEquals(3, cs.getEndOffset());
  }
}
