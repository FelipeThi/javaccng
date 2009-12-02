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
  private int beginOffset, endOffset;

  public CharSequenceCharStream(final CharSequence chars) {
    if (chars == null) {
      throw new IllegalArgumentException("chars is null");
    }
    this.chars = chars;
  }

  public void beginToken() throws IOException {
    beginOffset = endOffset;
  }

  public int readChar() throws IOException {
    if (next >= chars.length()) {
      return -1;
    }
    char c = chars.charAt(next);
    next++;
    endOffset++;
    return c;
  }

  public void backup(final int amount) {
    next -= amount;
    endOffset -= amount;
  }

  public String getImage() {
    return chars.subSequence(beginOffset, endOffset).toString();
  }

  public char[] getSuffix(final int length) {
    char[] c = new char[length];
    for (int n = 0; n < length; n++) {
      c[n] = chars.charAt(endOffset - length + n);
    }
    return c;
  }

  public int getBeginOffset() {
    return beginOffset;
  }

  public int getEndOffset() {
    return endOffset;
  }

  public int getBeginLine() {
    throw new UnsupportedOperationException();
  }

  public int getBeginColumn() {
    throw new UnsupportedOperationException();
  }

  public int getEndLine() {
    throw new UnsupportedOperationException();
  }

  public int getEndColumn() {
    throw new UnsupportedOperationException();
  }

  public void close() throws IOException {
    next = 0;
    beginOffset = endOffset = 0;
  }
}
