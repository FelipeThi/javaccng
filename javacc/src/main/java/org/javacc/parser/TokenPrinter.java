package org.javacc.parser;

import org.javacc.utils.Parsers;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class TokenPrinter {
  public static int cLine;
  public static int cCol;

  public static void printTokenSetup(Token t) {
    Token st = t;
    while (st.specialToken != null) {
      st = st.specialToken;
    }
    cLine = st.getBeginLine();
    cCol = st.getBeginColumn();
  }

  public static void printToken(Token t, Writer out)
      throws IOException {
    Token st = t.specialToken;
    if (st != null) {
      while (st.specialToken != null) {
        st = st.specialToken;
      }
      while (st != null) {
        printTokenOnly(st, out);
        st = st.next;
      }
    }
    printTokenOnly(t, out);
  }

  public static String printToken(Token t) {
    String result = "";
    Token st = t.specialToken;
    if (st != null) {
      while (st.specialToken != null) {
        st = st.specialToken;
      }
      while (st != null) {
        result += printTokenOnly(st);
        st = st.next;
      }
    }
    result += printTokenOnly(t);
    return result;
  }

  public static void printTokenOnly(Token t, Writer out)
      throws IOException {
    for (; cLine < t.getBeginLine(); cLine++) {
      out.write("\n");
      cCol = 1;
    }
    for (; cCol < t.getBeginColumn(); cCol++) {
      out.write(" ");
    }
    if (t.getKind() == JavaCCParserConstants.STRING_LITERAL
        || t.getKind() == JavaCCParserConstants.CHARACTER_LITERAL) {
      out.write(Parsers.unicodeEscape(t.getImage()));
    }
    else {
      out.write(t.getImage());
    }
    cLine = t.getEndLine();
    cCol = t.getEndColumn() + 1;
    char last = t.getImage().charAt(t.getImage().length() - 1);
    if (last == '\n' || last == '\r') {
      cLine++;
      cCol = 1;
    }
  }

  public static String printTokenOnly(Token t) {
    String result = "";
    for (; cLine < t.getBeginLine(); cLine++) {
      result += "\n";
      cCol = 1;
    }
    for (; cCol < t.getBeginColumn(); cCol++) {
      result += " ";
    }
    if (t.getKind() == JavaCCParserConstants.STRING_LITERAL
        || t.getKind() == JavaCCParserConstants.CHARACTER_LITERAL) {
      result += Parsers.unicodeEscape(t.getImage());
    }
    else {
      result += t.getImage();
    }
    cLine = t.getEndLine();
    cCol = t.getEndColumn() + 1;
    char last = t.getImage().charAt(t.getImage().length() - 1);
    if (last == '\n' || last == '\r') {
      cLine++;
      cCol = 1;
    }
    return result;
  }

  public static void printTokenList(List<Token> list, Writer out)
      throws IOException {
    Token t = null;
    for (Token token : list) {
      printToken(t = token, out);
    }
    if (t != null) {
      printTrailingComments(t);
    }
  }

  public static void printLeadingComments(Token t, Writer out)
      throws IOException {
    if (t.specialToken == null) {
      return;
    }
    Token st = t.specialToken;
    while (st.specialToken != null) {
      st = st.specialToken;
    }
    while (st != null) {
      printTokenOnly(st, out);
      st = st.next;
    }
    if (cCol != 1 && cLine != t.getBeginLine()) {
      out.write("\n");
      cLine++;
      cCol = 1;
    }
  }

  public static String printLeadingComments(Token t) {
    String result = "";
    if (t.specialToken == null) {
      return result;
    }
    Token st = t.specialToken;
    while (st.specialToken != null) {
      st = st.specialToken;
    }
    while (st != null) {
      result += printTokenOnly(st);
      st = st.next;
    }
    if (cCol != 1 && cLine != t.getBeginLine()) {
      result += "\n";
      cLine++;
      cCol = 1;
    }
    return result;
  }

  public static String printTrailingComments(Token t) {
    if (t.next == null) {
      return "";
    }
    return printLeadingComments(t.next);
  }

  static void packageDeclaration(List<Token> tokens, Writer out)
      throws IOException {
    if (tokens.size() != 0
        && tokens.get(0).getKind() == JavaCCParserConstants.PACKAGE) {
      for (int i = 1; i < tokens.size(); i++) {
        if (tokens.get(i).getKind() == JavaCCParserConstants.SEMICOLON) {
          cLine = tokens.get(0).getBeginLine();
          cCol = tokens.get(0).getBeginColumn();
          for (int j = 0; j <= i; j++) {
            printToken(tokens.get(j), out);
          }
          out.write("\n");
          out.write("\n");
          break;
        }
      }
    }
  }
}
