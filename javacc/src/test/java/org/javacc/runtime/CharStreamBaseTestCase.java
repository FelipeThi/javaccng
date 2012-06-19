package org.javacc.runtime;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;

import static org.junit.Assert.*;

public abstract class CharStreamBaseTestCase {
  abstract CharStream makeCharStream(CharSequence content);

  @Test
  public void canReadEmptyStream() throws IOException {
    final CharStream s = makeCharStream("");
    assertEquals(-1, s.readChar());
  }

  @Test
  public void providesValidOffset() throws ParseException, IOException {
    final CharStream cs = makeCharStream("12345");
    cs.beginToken();
    assertEquals(0, cs.getBegin());
    assertEquals(0, cs.getEnd());
    assertEquals('1', cs.readChar());
    assertEquals(0, cs.getBegin());
    assertEquals(1, cs.getEnd());
    assertEquals('2', cs.readChar());
    assertEquals(0, cs.getBegin());
    assertEquals(2, cs.getEnd());
    cs.backup(1);
    assertEquals(0, cs.getBegin());
    assertEquals(1, cs.getEnd());
    cs.beginToken();
    assertEquals('2', cs.readChar());
    assertEquals(1, cs.getBegin());
    assertEquals(2, cs.getEnd());
    assertEquals('3', cs.readChar());
    assertEquals(1, cs.getBegin());
    assertEquals(3, cs.getEnd());
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
  public void reportsEofEx() throws IOException {
    final CharStream s = new JavaCharStream(new StringReader("x"));
    assertCharLineColumn(s, 'x', 1, 1);
    assertCharLineColumn(s, -1, 1, 1);
  }

  @Test
  public void providesLineAndColumnNumbers() throws IOException {
    final CharStream cs = makeCharStream("a1\r\nb2\nc3\rd");
    cs.beginToken();
    assertCharLineColumn(cs, 'a', 1, 1);
    assertCharLineColumn(cs, '1', 1, 1);
    assertCharLineColumn(cs, '\r', 1, 1);
    assertCharLineColumn(cs, '\n', 1, 1);
    assertCharLineColumn(cs, 'b', 1, 1);
    assertCharLineColumn(cs, '2', 1, 1);
    assertCharLineColumn(cs, '\n', 1, 1);
    assertCharLineColumn(cs, 'c', 1, 1);
    assertCharLineColumn(cs, '3', 1, 1);
    assertCharLineColumn(cs, '\r', 1, 1);
    assertCharLineColumn(cs, 'd', 1, 1);
    assertCharLineColumn(cs, -1, 1, 1);
    cs.backup(3);
    assertCharLineColumn(cs, '3', 1, 1);
    assertCharLineColumn(cs, '\r', 1, 1);
    assertCharLineColumn(cs, 'd', 1, 1);
    assertCharLineColumn(cs, -1, 1, 1);
    cs.backup(3);
    cs.beginToken();
    assertCharLineColumn(cs, '3', 3, 2);
    assertCharLineColumn(cs, '\r', 3, 2);
    assertCharLineColumn(cs, 'd', 3, 2);
    assertCharLineColumn(cs, -1, 3, 2);
  }

  static void assertCharLineColumn(
      CharStream cs, int c, int line, int column) throws IOException {
    assertEquals(c, cs.readChar());
    assertEquals(line, cs.getLine());
    assertEquals(column, cs.getColumn());
  }
}
