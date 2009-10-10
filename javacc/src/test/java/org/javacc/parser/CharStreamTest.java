package org.javacc.parser;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

public class CharStreamTest {
  @Test
  public void providesValidOffsetForEscapes() throws ParseException, IOException {
    final String source = "<\\uuu005a>";
    assertEquals(10, source.length());
    final JavaCharStream charStream = new JavaCharStream(new StringReader(source));
    assertEquals(0, charStream.getBeginOffset());
    assertEquals(0, charStream.getEndOffset());
    assertEquals('<', charStream.beginToken());
    assertEquals('Z', charStream.readChar());
    assertEquals('>', charStream.readChar());
    assertEquals(0, charStream.getBeginOffset());
    assertEquals(10, charStream.getEndOffset());
    assertEquals(1, charStream.getBeginLine());
    assertEquals(1, charStream.getBeginColumn());
    assertEquals(1, charStream.getEndLine());
    assertEquals(10, charStream.getEndColumn());
    charStream.backup(3);
    assertEquals('<', charStream.readChar());
    assertEquals('Z', charStream.readChar());
    assertEquals('>', charStream.readChar());
    assertEquals(0, charStream.getBeginOffset());
    assertEquals(10, charStream.getEndOffset());
    assertEquals(-1, charStream.readChar());
  }

  @Test
  public void providesValidOffsetForSlashes() throws ParseException, IOException {
    final String source = "<\\\\X>";
    assertEquals(5, source.length());
    final JavaCharStream charStream = new JavaCharStream(new StringReader(source));
    assertEquals(0, charStream.getBeginOffset());
    assertEquals(0, charStream.getEndOffset());
    assertEquals('<', charStream.beginToken());
    assertEquals('\\', charStream.readChar());
    assertEquals('\\', charStream.readChar());
    assertEquals('X', charStream.readChar());
    assertEquals('>', charStream.readChar());
    assertEquals(0, charStream.getBeginOffset());
    assertEquals(5, charStream.getEndOffset());
    assertEquals(1, charStream.getBeginLine());
    assertEquals(1, charStream.getBeginColumn());
    assertEquals(1, charStream.getEndLine());
    assertEquals(5, charStream.getEndColumn());
    charStream.backup(3);
    assertEquals('\\', charStream.readChar());
    assertEquals('X', charStream.readChar());
    assertEquals('>', charStream.readChar());
    assertEquals(0, charStream.getBeginOffset());
    assertEquals(5, charStream.getEndOffset());
    assertEquals(-1, charStream.readChar());
  }

  /**
   * Make sure that the parsed content between token.beginOffset and
   * token.endOffset is equal to the token's image by parsing
   * JavaCC grammar file.
   */
  @Test
  public void providesValidOffsetsAndImagesFromJavaCCGrammar() throws ParseException, IOException {
    final String s = "./src/main/javacc/JavaCC.jj";
    final String source = readFile(s);
    final CharStream charStream = new JavaCharStream(new StringReader(source));
    final TokenManager tokenManager = new JavaCCParserTokenManager(charStream);
    Token token;
    while ((token = tokenManager.getNextToken()).kind != 0) {
      String image = source.substring(token.beginOffset, token.endOffset);
      assertImage(token, image);
    }
  }

  private static void assertImage(Token token, String image) {
    // We only compare identifiers because other kinds
    // my have transformed their image in the parser
    if (token.kind == JavaCCParserConstants.IDENTIFIER) {
      assertEquals("Wrong token " + JavaCCParserConstants.tokenImage[token.kind] +
          " offset at " + token.beginLine + ", " + token.beginColumn, token.image, image);
    }
  }

  @Test(expected = IOException.class)
  public void propagatesException() throws IOException, ParseException {
    Options.init(); // To avoid NullPointerException while parsing
    final CharStream charStream = new JavaCharStream(new ThrowingReader());
    final TokenManager tokenManager = new JavaCCParserTokenManager(charStream);
    JavaCCParser parser = new JavaCCParser(tokenManager);
    parser.javacc_input(); // Should rethrow IOException from ThrowingReader
  }

  /**
   * Read file content.
   *
   * @param fileName File name.
   * @return File content.
   */
  static String readFile(String fileName) throws IOException {
    final StringBuilder s = new StringBuilder();
    final InputStream inputStream = new FileInputStream(fileName);
    try {
      final Reader reader = new InputStreamReader(inputStream, "UTF-8");
      int c;
      while ((c = reader.read()) != -1) {
        s.append((char) c);
      }
    }
    finally {
      inputStream.close();
    }
    return s.toString();
  }

  class ThrowingReader extends Reader {
    @Override
    public int read(final char[] cbuf, final int off, final int len) throws IOException {
      throw new IOException("test exception");
    }

    @Override
    public void close() throws IOException {}
  }
}
