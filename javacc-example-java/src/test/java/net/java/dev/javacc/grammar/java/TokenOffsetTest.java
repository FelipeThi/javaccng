package net.java.dev.javacc.grammar.java;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

public class TokenOffsetTest {
  @Test
  public void providesValidOffset() throws ParseException, IOException {
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
  }

  @Test
  public void realWorldTest() throws ParseException, IOException {
    final String s = "./src/main/java/net/java/dev/javacc/grammar/java/JavaGenerics.java";
    final String source = readFile(s);
    final CharStream charStream = new JavaCharStream(new StringReader(source));
    final TokenManager tokenManager = new JavaParserTokenManager(charStream);
    Token token;
    while ((token = tokenManager.getNextToken()).kind != 0) {
      String image = source.substring(token.beginOffset, token.endOffset);
      assertImage(token, image);
    }
  }

  private void assertImage(final Token token, final String image) {
    assertEquals("Wrong token " + JavaParserConstants.tokenImage[token.kind] + " offset at " + token.beginLine + ", " + token.beginColumn,
                 token.image, image);
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
}
