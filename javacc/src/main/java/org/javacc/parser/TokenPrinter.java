package org.javacc.parser;

import org.javacc.utils.Parsers;
import org.javacc.utils.io.IndentingPrintWriter;

import java.io.IOException;
import java.util.List;

public class TokenPrinter {
  public int line;
  public int column;

  public TokenPrinter() {}

  public TokenPrinter(Token token) {
    setup(token);
  }

  public void setup(Token token) {
    Token special = token;
    while (special.specialToken != null) {
      special = special.specialToken;
    }
    line = special.getLine();
    column = special.getColumn();
  }

  public void printToken(Token token, IndentingPrintWriter out)
      throws IOException {
    Token special = token.specialToken;
    if (special != null) {
      while (special.specialToken != null) {
        special = special.specialToken;
      }
      while (special != null) {
        printTokenOnly(special, out);
        special = special.next;
      }
    }
    printTokenOnly(token, out);
  }

  public String printToken(Token token) {
    StringBuilder result = new StringBuilder();
    Token special = token.specialToken;
    if (special != null) {
      while (special.specialToken != null) {
        special = special.specialToken;
      }
      while (special != null) {
        result.append(printTokenOnly(special));
        special = special.next;
      }
    }
    result.append(printTokenOnly(token));
    return result.toString();
  }

  public void printTokenOnly(Token token, IndentingPrintWriter out)
      throws IOException {
    for (; line < token.getLine(); line++) {
      out.println();
      column = 0;
    }
    for (; column < token.getColumn(); column++) {
      out.print(" ");
    }
    if (token.getKind() == JavaCCConstants.STRING_LITERAL
        || token.getKind() == JavaCCConstants.CHARACTER_LITERAL) {
      String image = Parsers.unicodeEscape(token.getImage());
      adjust(token, image);
      out.write(image);
    }
    else {
      String image = token.getImage();
      adjust(token, image);
      out.write(image);
    }
  }

  public String printTokenOnly(Token token) {
    String result = "";
    for (; line < token.getLine(); line++) {
      result += "\n";
      column = 0;
    }
    for (; column < token.getColumn(); column++) {
      result += " ";
    }
    if (token.getKind() == JavaCCConstants.STRING_LITERAL
        || token.getKind() == JavaCCConstants.CHARACTER_LITERAL) {
      String image = Parsers.unicodeEscape(token.getImage());
      adjust(token, image);
      result += image;
    }
    else {
      String image = token.getImage();
      adjust(token, image);
      result += image;
    }

    return result;
  }

  private void adjust(Token token, String image) {
    line = token.getLine();
    column = token.getColumn();
    for (int n = 0; n < image.length(); n++) {
      char c = image.charAt(n);
      if (c == '\n') {
        line++;
        column = 0;
      }
      else {
        column++;
      }
    }
  }

  public void printTokenList(List<Token> list, IndentingPrintWriter out)
      throws IOException {
    Token t = null;
    for (Token token : list) {
      printToken(t = token, out);
    }
    if (t != null) {
      printTrailingComments(t);
    }
  }

  public void printLeadingComments(Token token, IndentingPrintWriter out)
      throws IOException {
    if (token.specialToken == null) {
      return;
    }
    Token special = token.specialToken;
    while (special.specialToken != null) {
      special = special.specialToken;
    }
    while (special != null) {
      printTokenOnly(special, out);
      special = special.next;
    }
    if (column != 0 && line != token.getLine()) {
      out.println();
      line++;
      column = 0;
    }
  }

  public String printLeadingComments(Token token) {
    String result = "";
    if (token.specialToken == null) {
      return result;
    }
    Token special = token.specialToken;
    while (special.specialToken != null) {
      special = special.specialToken;
    }
    while (special != null) {
      result += printTokenOnly(special);
      special = special.next;
    }
    if (column != 0 && line != token.getLine()) {
      result += "\n";
      line++;
      column = 0;
    }
    return result;
  }

  public String printTrailingComments(Token token) {
    if (token.next == null) {
      return "";
    }
    return printLeadingComments(token.next);
  }

  void packageDeclaration(List<Token> tokens, IndentingPrintWriter out)
      throws IOException {
    if (tokens.size() != 0
        && tokens.get(0).getKind() == JavaCCConstants.PACKAGE) {
      for (int i = 1; i < tokens.size(); i++) {
        if (tokens.get(i).getKind() == JavaCCConstants.SEMICOLON) {
          line = tokens.get(0).getLine();
          column = tokens.get(0).getColumn();
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
