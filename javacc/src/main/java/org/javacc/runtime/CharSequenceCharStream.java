package org.javacc.runtime;

import java.io.IOException;

/**
 * A char stream that reads characters from the provided CharSequence.
 * <p>
 * This implementation does not process java-like unicode escapes.
 * <p>
 * This implementation does track line and column numbers (yet).
 */
public final class CharSequenceCharStream implements CharStream {
  private final CharSequence chars;
  private int next;
  private int begin, end;

  public CharSequenceCharStream(final CharSequence chars) {
    if (chars == null) {
      throw new IllegalArgumentException("chars is null");
    }
    this.chars = chars;
  }

  public void beginToken() throws IOException {
    begin = end;
  }

  public int readChar() throws IOException {
    if (next >= chars.length()) {
      return -1;
    }
    char c = chars.charAt(next);
    next++;
    end++;
    return c;
  }

  public void backup(final int amount) {
    next -= amount;
    end -= amount;
  }

  public String getImage() {
    return chars.subSequence(begin, end).toString();
  }

  public char[] getSuffix(final int length) {
    char[] c = new char[length];
    for (int n = 0; n < length; n++) {
      c[n] = chars.charAt(end - length + n);
    }
    return c;
  }

  public int getBegin() {
    return begin;
  }

  public int getEnd() {
    return end;
  }

  public int getLine() {
    throw new UnsupportedOperationException();
  }

  public int getColumn() {
    throw new UnsupportedOperationException();
  }

  public void close() throws IOException {
    next = 0;
    begin = end = 0;
  }
}
