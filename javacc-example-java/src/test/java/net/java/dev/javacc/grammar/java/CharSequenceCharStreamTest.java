package net.java.dev.javacc.grammar.java;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;

public class CharSequenceCharStreamTest {
  @Test
  public void providesValidOffset() throws ParseException, IOException {
    final String source = "12345";
    final CharStream charStream = new CharSequenceCharStream(source);
    assertEquals(0, charStream.getBeginOffset());
    assertEquals(0, charStream.getEndOffset());
    charStream.beginToken();
    assertEquals(0, charStream.getBeginOffset());
    assertEquals(1, charStream.getEndOffset());
    charStream.readChar();
    assertEquals(0, charStream.getBeginOffset());
    assertEquals(2, charStream.getEndOffset());
    charStream.readChar();
    assertEquals(0, charStream.getBeginOffset());
    assertEquals(3, charStream.getEndOffset());
    charStream.backup(1);
    assertEquals(0, charStream.getBeginOffset());
    assertEquals(2, charStream.getEndOffset());
    charStream.beginToken();
    assertEquals(2, charStream.getBeginOffset());
    assertEquals(3, charStream.getEndOffset());
    charStream.readChar();
    assertEquals(2, charStream.getBeginOffset());
    assertEquals(4, charStream.getEndOffset());
  }

  @Test
  public void reportsEofByThrowingException() throws IOException {
    final CharStream charStream = new CharSequenceCharStream("123");
    charStream.readChar();
    charStream.readChar();
    charStream.readChar();
    try {
      charStream.readChar();
      fail();
    }
    catch (IOException ex) {
      //
    }
  }
}
