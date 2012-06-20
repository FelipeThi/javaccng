package org.javacc.parser;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.javacc.utils.io.IndentingPrintWriter;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class JavaCCScannerTest {
  static class JavaCCScannerEx extends JavaCCScanner {
    public JavaCCScannerEx(CharStream charStream) {
      super(charStream);
    }

    @Override protected Token newToken(int kind, int begin, int end,
                                       int line, int column, String image) {
      return super.newToken(kind, begin, end, line, column, image);
    }

    @Override protected void reportError(int state, int pos, int line, int column,
                                         int character) {
      super.reportError(state, pos, line, column, character);
    }
  }

  @Test
  public void scan() throws IOException {
    String s = " { //comment\n hello( \"string literal\" ) ; } ";
    JavaCCScanner scanner = scanner(s);
    assertToken(scanner.getAnyNextToken(), 1, 2, 0, 1, "{");
    assertToken(scanner.getAnyNextToken(), 3, 13, 0, 3, "//comment\n");
    assertToken(scanner.getAnyNextToken(), 14, 19, 1, 1, "hello");
    assertToken(scanner.getAnyNextToken(), 19, 20, 1, 6, "(");
    assertToken(scanner.getAnyNextToken(), 21, 37, 1, 8, "\"string literal\"");
    assertToken(scanner.getAnyNextToken(), 38, 39, 1, 25, ")");
    assertToken(scanner.getAnyNextToken(), 40, 41, 1, 27, ";");
    assertToken(scanner.getAnyNextToken(), 42, 43, 1, 29, "}");
    assertToken(scanner.getAnyNextToken(), 44, 44, 1, 31, "");
    assertToken(scanner.getAnyNextToken(), 44, 44, 1, 31, "");

    assertToken(scanner("").getAnyNextToken(), 0, 0, 0, 0, "");
    assertToken(scanner("null").getAnyNextToken(), 0, 4, 0, 0, "null");
    assertToken(scanner("//\n").getAnyNextToken(), 0, 3, 0, 0, "//\n");
    assertToken(scanner("/**/").getAnyNextToken(), 0, 4, 0, 0, "/**/");
    assertToken(scanner("/***/").getAnyNextToken(), 0, 5, 0, 0, "/***/");
    assertToken(scanner("/**hello*/").getAnyNextToken(), 0, 10, 0, 0, "/**hello*/");
    assertToken(scanner("\'x\'").getAnyNextToken(), 0, 3, 0, 0, "\'x\'");
    assertToken(scanner("\"x\"").getAnyNextToken(), 0, 3, 0, 0, "\"x\"");
  }

  @Test
  public void escape() throws IOException {
    String s = " '\\uuu005a' \"\\uuu005a\" ";
    JavaCCScanner scanner = scanner(s);
    assertToken(scanner.getAnyNextToken(), 1, 11, 0, 1, "'Z'");
    assertToken(scanner.getAnyNextToken(), 12, 22, 0, 12, "\"Z\"");
  }

  @Test
  public void errors() throws IOException {
    assertException(scanner("`"), "Lexical error at 0, line 1, column 1, character '`' (96)");
    assertException(scanner(" ` "), "Lexical error at 1, line 1, column 2, character '`' (96)");
    assertException(scanner("  `  "), "Lexical error at 2, line 1, column 3, character '`' (96)");
    assertException(scanner("'x"), "Lexical error at 2, line 1, column 3, character <EOF>");
    assertException(scanner(" 'x"), "Lexical error at 3, line 1, column 4, character <EOF>");
    assertException(scanner(" 'x "), "Lexical error at 3, line 1, column 4, character ' ' (32)");
    assertException(scanner("\"x"), "Lexical error at 2, line 1, column 3, character <EOF>");
    assertException(scanner(" \"x"), "Lexical error at 3, line 1, column 4, character <EOF>");
    assertException(scanner(" \"x "), "Lexical error at 4, line 1, column 5, character <EOF>");
  }

  @Test
  public void file() throws IOException {
    Pattern pattern = Pattern.compile("\\\"\\\\u+[0-9a-zA-Z]{4}\\\"");
    String s = Files.toString(new File("src/main/javacc/JavaCC.jj"), Charsets.UTF_8);
    JavaCCScanner scanner = scanner(s);
    while (true) {
      Token token = scanner.getAnyNextToken();
      if (token.getKind() == JavaCCConstants.EOF) {
        break;
      }
      String image = s.substring(token.getBegin(), token.getEnd());
      if (pattern.matcher(image).matches()) {
        // unicode escape
      }
      else {
        assertEquals(String.format("line: %d, column: %d",
            token.getLine(), token.getColumn()), image, token.getImage());
      }
    }
  }

  @Test
  public void print() throws IOException {
    String s = " { //comment\n hello( \"string literal\" ) ; } ";
    JavaCCScanner scanner = scanner(s);
    StringWriter w = new StringWriter();
    IndentingPrintWriter p = new IndentingPrintWriter(w);
    TokenPrinter tp = new TokenPrinter();
    while (true) {
      Token t = scanner.getNextToken();
      if (t.getKind() == JavaCCConstants.EOF) {
        break;
      }
      tp.printToken(t, p);
    }
    assertEquals(" { //comment\n hello( \"string literal\" ) ; }",
        w.toString());
  }

  private JavaCCScanner scanner(String s) {
    return new JavaCCScannerEx(
        new CharStream.Escaping(
            new CharStream.ForCharSequence(s)));
  }

  private static void assertToken(Token t, int begin, int end,
                                  int line, int column, String image) {
    assertEquals("begin position does not match,", begin, t.getBegin());
    assertEquals("end position does not match,", end, t.getEnd());
    assertEquals("line number does not match,", line, t.getLine());
    assertEquals("column number does not match,", column, t.getColumn());
    assertEquals("image does not match,", image, t.getImage());
  }

  private static void assertException(Scanner scanner, String message) {
    ScannerException ex = null;
    try {
      scanner.getNextToken();
    }
    catch (ScannerException x) {
      ex = x;
    }
    catch (IOException x) {
      fail(x.getMessage());
    }
    assertNotNull(ex);
    assertEquals(message, ex.getMessage());
  }
}
