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
  @Test
  public void scan() throws IOException {
    String s = " { //comment\n hello( \"string literal\" ) ; } ";
    JavaCCScanner scanner = new JavaCCScanner(
        new CharStream.ForCharSequence(s));
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
  }

  @Test
  public void escape() throws IOException {
    String s = " '\\uuu005a' \"\\uuu005a\" ";
    JavaCCScanner scanner = new JavaCCScanner(
        new CharStream.Escaping(
            new CharStream.ForCharSequence(s)));
    assertToken(scanner.getAnyNextToken(), 1, 11, 0, 1, "'Z'");
    assertToken(scanner.getAnyNextToken(), 12, 22, 0, 12, "\"Z\"");
  }

  @Test
  public void file() throws IOException {
    Pattern pattern = Pattern.compile("\\\"\\\\u+[0-9a-zA-Z]{4}\\\"");

    String source = Files.toString(new File("src/main/javacc/JavaCC.jj"),
        Charsets.UTF_8);

    JavaCCScanner scanner = new JavaCCScanner(
        new CharStream.Escaping(
            new CharStream.ForCharSequence(source)));
    while (true) {
      Token token = scanner.getAnyNextToken();
      if (token.getKind() == JavaCCConstants.EOF) {
        break;
      }
      String image = source.substring(token.getBegin(), token.getEnd());
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
    JavaCCScanner scanner = new JavaCCScanner(
        new CharStream.ForCharSequence(s));
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

  private static void assertToken(Token t, int begin, int end,
                                  int line, int column, String image) {
    assertEquals("begin position does not match,", begin, t.getBegin());
    assertEquals("end position does not match,", end, t.getEnd());
    assertEquals("line number does not match,", line, t.getLine());
    assertEquals("column number does not match,", column, t.getColumn());
    assertEquals("image does not match,", image, t.getImage());
  }
}
