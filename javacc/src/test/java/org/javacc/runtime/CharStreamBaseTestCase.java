package org.javacc.runtime;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;

public abstract class CharStreamBaseTestCase {
  abstract CharStream makeCharStream(CharSequence content);

  @Test
  public void providesValidOffset() throws ParseException, IOException {
    final CharStream cs = makeCharStream("12345");
    cs.beginToken();
    assertEquals(0, cs.getBeginOffset());
    assertEquals(0, cs.getEndOffset());
    assertEquals('1', cs.readChar());
    assertEquals(0, cs.getBeginOffset());
    assertEquals(1, cs.getEndOffset());
    assertEquals('2', cs.readChar());
    assertEquals(0, cs.getBeginOffset());
    assertEquals(2, cs.getEndOffset());
    cs.backup(1);
    assertEquals(0, cs.getBeginOffset());
    assertEquals(1, cs.getEndOffset());
    cs.beginToken();
    assertEquals('2', cs.readChar());
    assertEquals(1, cs.getBeginOffset());
    assertEquals(2, cs.getEndOffset());
    assertEquals('3', cs.readChar());
    assertEquals(1, cs.getBeginOffset());
    assertEquals(3, cs.getEndOffset());
  }

  @Test
  public void reportsEof() throws IOException {
    final CharStream cs = makeCharStream("123");
    cs.beginToken();
    assertEquals('1', cs.readChar());
    assertEquals('2', cs.readChar());
    assertEquals('3', cs.readChar());
    assertEquals(-1, cs.readChar());
    cs.backup(1);
    assertEquals('3', cs.readChar());
    assertEquals(-1, cs.readChar());
    cs.backup(1);
    assertEquals('3', cs.readChar());
    assertEquals(-1, cs.readChar());
  }

  @Test
  public void providesLineAndColumnNumbers() throws IOException {
    final CharStream cs = makeCharStream("a1\r\nb2\nc3\rd");
    cs.beginToken();
    assertCharLineColumn(cs, 'a', 1, 1, 1, 1);
    assertCharLineColumn(cs, '1', 1, 1, 1, 2);
    assertCharLineColumn(cs, '\r', 1, 1, 1, 3);
    assertCharLineColumn(cs, '\n', 1, 1, 1, 4);
    assertCharLineColumn(cs, 'b', 1, 1, 2, 1);
    assertCharLineColumn(cs, '2', 1, 1, 2, 2);
    assertCharLineColumn(cs, '\n', 1, 1, 2, 3);
    assertCharLineColumn(cs, 'c', 1, 1, 3, 1);
    assertCharLineColumn(cs, '3', 1, 1, 3, 2);
    assertCharLineColumn(cs, '\r', 1, 1, 3, 3);
    assertCharLineColumn(cs, 'd', 1, 1, 4, 1);
    assertCharLineColumn(cs, -1, 1, 1, 4, 1);
    cs.backup(3);
    assertCharLineColumn(cs, '3', 1, 1, 3, 2);
    assertCharLineColumn(cs, '\r', 1, 1, 3, 3);
    assertCharLineColumn(cs, 'd', 1, 1, 4, 1);
    assertCharLineColumn(cs, -1, 1, 1, 4, 1);
    cs.backup(3);
    cs.beginToken();
    assertCharLineColumn(cs, '3', 3, 2, 3, 2);
    assertCharLineColumn(cs, '\r', 3, 2, 3, 3);
    assertCharLineColumn(cs, 'd', 3, 2, 4, 1);
    assertCharLineColumn(cs, -1, 3, 2, 4, 1);
  }

  private static void assertCharLineColumn(final CharStream cs,
                                           final int c,
                                           final int beginLne, final int beginColumn,
                                           final int endLine, final int endColumn) throws IOException {
    assertEquals(c, cs.readChar());
    assertEquals(beginLne, cs.getBeginLine());
    assertEquals(beginColumn, cs.getBeginColumn());
    assertEquals(endLine, cs.getEndLine());
    assertEquals(endColumn, cs.getEndColumn());
  }
}
