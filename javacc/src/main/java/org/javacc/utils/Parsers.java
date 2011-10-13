package org.javacc.utils;

import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.Token;

public final class Parsers {
  public static String escape(String str) {
    String result = "";
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      if (ch == '\b') {
        result += "\\b";
      }
      else if (ch == '\t') {
        result += "\\t";
      }
      else if (ch == '\n') {
        result += "\\n";
      }
      else if (ch == '\f') {
        result += "\\f";
      }
      else if (ch == '\r') {
        result += "\\r";
      }
      else if (ch == '\"') {
        result += "\\\"";
      }
      else if (ch == '\'') {
        result += "\\\'";
      }
      else if (ch == '\\') {
        result += "\\\\";
      }
      else if (ch < 0x20 || ch > 0x7e) {
        String s = "0000" + Integer.toString(ch, 16);
        result += "\\u" + s.substring(s.length() - 4, s.length());
      }
      else {
        result += ch;
      }
    }
    return result;
  }

  public static String unicodeEscape(String str) {
    String result = "";
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      if (ch < 0x20 || ch > 0x7e || ch == '\\') {
        String s = "0000" + Integer.toString(ch, 16);
        result += "\\u" + s.substring(s.length() - 4, s.length());
      }
      else {
        result += ch;
      }
    }
    return result;
  }

  public static String unescape(Token t, String str) {
    String result = "";
    int index = 1;
    while (index < str.length() - 1) {
      if (str.charAt(index) != '\\') {
        result += str.charAt(index);
        index++;
        continue;
      }
      index++;
      char ch = str.charAt(index);
      if (ch == 'b') {
        result += '\b';
        index++;
        continue;
      }
      if (ch == 't') {
        result += '\t';
        index++;
        continue;
      }
      if (ch == 'n') {
        result += '\n';
        index++;
        continue;
      }
      if (ch == 'f') {
        result += '\f';
        index++;
        continue;
      }
      if (ch == 'r') {
        result += '\r';
        index++;
        continue;
      }
      if (ch == '"') {
        result += '\"';
        index++;
        continue;
      }
      if (ch == '\'') {
        result += '\'';
        index++;
        continue;
      }
      if (ch == '\\') {
        result += '\\';
        index++;
        continue;
      }
      if (ch >= '0' && ch <= '7') {
        int ordinal = ((int) ch) - ((int) '0');
        index++;
        char ch1 = str.charAt(index);
        if (ch1 >= '0' && ch1 <= '7') {
          ordinal = ordinal * 8 + ((int) ch1) - ((int) '0');
          index++;
          ch1 = str.charAt(index);
          if (ch <= '3' && ch1 >= '0' && ch1 <= '7') {
            ordinal = ordinal * 8 + ((int) ch1) - ((int) '0');
            index++;
          }
        }
        result += (char) ordinal;
        continue;
      }
      if (ch == 'u') {
        index++;
        ch = str.charAt(index);
        if (isHexDigit(ch)) {
          int ordinal = hexDigit(ch);
          index++;
          ch = str.charAt(index);
          if (isHexDigit(ch)) {
            ordinal = ordinal * 16 + hexDigit(ch);
            index++;
            ch = str.charAt(index);
            if (isHexDigit(ch)) {
              ordinal = ordinal * 16 + hexDigit(ch);
              index++;
              ch = str.charAt(index);
              if (isHexDigit(ch)) {
                ordinal = ordinal * 16 + hexDigit(ch);
                index++;
                continue;
              }
            }
          }
        }
        JavaCCErrors.parseError(t, "Encountered non-hex character '" + ch +
            "' at position " + index + " of string " +
            "- Unicode escape must have 4 hex digits after it.");
        return result;
      }
      JavaCCErrors.parseError(t, "Illegal escape sequence '\\" + ch +
          "' at position " + index + " of string.");
      return result;
    }
    return result;
  }

  public static boolean isHexDigit(char ch) {
    if (ch >= '0' && ch <= '9') { return true; }
    if (ch >= 'A' && ch <= 'F') { return true; }
    if (ch >= 'a' && ch <= 'f') { return true; }
    return false;
  }

  public static int hexDigit(char ch) {
    if (ch >= '0' && ch <= '9') { return ((int) ch) - ((int) '0'); }
    if (ch >= 'A' && ch <= 'F') { return ((int) ch) - ((int) 'A') + 10; }
    return ((int) ch) - ((int) 'a') + 10;
  }
}
