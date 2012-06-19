package org.javacc.runtime;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public abstract class CharStreamBaseTestCase {
  abstract CharStream newCharStream(CharSequence content);

  @Test
  public void readEmpty() throws IOException {
    CharStream s = newCharStream("");
    assertRead(s, 0, 0, 0, -1);
    assertRead(s, 0, 0, 0, -1);
  }

  @Test
  public void readNonEmpty() throws IOException {
    CharStream s = newCharStream("123");
    assertRead(s, 0, 0, 0, '1');
    assertRead(s, 1, 0, 1, '2');
    assertRead(s, 2, 0, 2, '3');
    assertRead(s, 3, 0, 3, -1);
    assertRead(s, 3, 0, 3, -1);
  }

  @Test
  public void lineColumn() throws IOException {
    CharStream s = newCharStream("1\r\n2\r\n3\n\n4\n");
    assertRead(s, 0, 0, 0, '1');
    assertRead(s, 1, 0, 1, '\r');
    assertRead(s, 2, 0, 1, '\n');
    assertRead(s, 3, 1, 0, '2');
    assertRead(s, 4, 1, 1, '\r');
    assertRead(s, 5, 1, 1, '\n');
    assertRead(s, 6, 2, 0, '3');
    assertRead(s, 7, 2, 1, '\n');
    assertRead(s, 8, 3, 0, '\n');
    assertRead(s, 9, 4, 0, '4');
    assertRead(s, 10, 4, 1, '\n');
    assertRead(s, 11, 5, 0, -1);
    assertRead(s, 11, 5, 0, -1);
  }

  private static void assertRead(CharStream s, int position, int line, int column, int c)
      throws IOException {
    assertEquals(position, s.position());
    assertEquals(line, line(s));
    assertEquals(column, column(s));
    assertEquals(c, s.read());
  }

  private static int line(CharStream s) {
    return ((CharStream.LineColumnInfo) s).line();
  }

  private static int column(CharStream s) {
    return ((CharStream.LineColumnInfo) s).column();
  }
}
