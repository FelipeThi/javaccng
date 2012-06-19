package org.javacc.runtime;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

public class EscapingCharStreamTest extends CharStreamBaseTestCase {
  @Override CharStream.Escaping newCharStream(CharSequence content) {
    return new CharStream.Escaping(new CharStream.ForCharSequence(content));
  }

  @Test
  public void readsUnicodeEscapes() throws IOException {
    InputStream in = getClass().getResourceAsStream("escapes.txt");
    BufferedReader r = new BufferedReader(new InputStreamReader(in));

    // case 1
    {
      assertEquals(1, "\uu005a".length());
      String s = r.readLine();
      assertEquals(7, s.length());
      CharStream.Escaping stream = newCharStream(s);
      assertEquals(0, stream.position());
      assertEquals('Z', (char) stream.read());
      assertEquals(-1, stream.read());
    }
    // case 2
    {
      assertEquals(7, "\\uu005a".length());
      String s = r.readLine();
      assertEquals(8, s.length());
      CharStream.Escaping stream = newCharStream(s);
      assertEquals(0, stream.position());
      assertEquals('\\', stream.read());
      assertEquals('\\', stream.read());
      assertEquals('u', stream.read());
      assertEquals('u', stream.read());
      assertEquals('0', stream.read());
      assertEquals('0', stream.read());
      assertEquals('5', stream.read());
      assertEquals('a', stream.read());
      assertEquals(-1, stream.read());
    }
    // case 3
    {
      assertEquals(2, "\\\uu005a".length());
      String s = r.readLine();
      assertEquals(9, s.length());
      CharStream.Escaping stream = newCharStream(s);
      assertEquals(0, stream.position());
      assertEquals('\\', stream.read());
      assertEquals(1, stream.position());
      assertEquals('\\', stream.read());
      assertEquals(2, stream.position());
      assertEquals('Z', stream.read());
      assertEquals(-1, stream.read());
    }
    // case 4
    {
      assertEquals(8, "\\\\uu005a".length());
      String s = r.readLine();
      assertEquals(10, s.length());
      CharStream.Escaping stream = newCharStream(s);
      assertEquals(0, stream.position());
      assertEquals('\\', stream.read());
      assertEquals(1, stream.position());
      assertEquals('\\', stream.read());
      assertEquals(2, stream.position());
      assertEquals('\\', stream.read());
      assertEquals(3, stream.position());
      assertEquals('\\', stream.read());
      assertEquals(4, stream.position());
      assertEquals('u', stream.read());
      assertEquals(5, stream.position());
      assertEquals('u', stream.read());
      assertEquals(6, stream.position());
      assertEquals('0', stream.read());
      assertEquals(7, stream.position());
      assertEquals('0', stream.read());
      assertEquals(8, stream.position());
      assertEquals('5', stream.read());
      assertEquals(9, stream.position());
      assertEquals('a', stream.read());
      assertEquals(-1, stream.read());
    }
  }

  @Test
  public void incompleteEscapeSequence() throws IOException {
    assertEquals("\\", read("\\"));
    assertEquals("\\u", read("\\u"));
    assertEquals("\\uuu", read("\\uuu"));
    assertEquals("\\uuu0", read("\\uuu0"));
    assertEquals("\\uuu00", read("\\uuu00"));
    assertEquals("\\uuu000", read("\\uuu000"));
    assertEquals("\\uuu000X", read("\\uuu000X"));
  }

  private String read(String input) throws IOException {
    return read(newCharStream(input));
  }

  private String read(CharStream stream) throws IOException {
    StringBuilder s = new StringBuilder();
    int c;
    while ((c = stream.read()) != -1) {
      s.append((char) c);
    }
    return s.toString();
  }
}
