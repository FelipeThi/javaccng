package net.java.dev.javacc.grammar.java;

import java.io.IOException;

/** A char stream that reads characters from the provided CharSequence. */
public final class CharSequenceCharStream implements CharStream {
  private final CharSequence chars;
  private int next;
  private int beginOffset, endOffset;
  private int line, column;

  public CharSequenceCharStream(final CharSequence chars) {
    if (chars == null) {
      throw new IllegalArgumentException("chars is null");
    }
    this.chars = chars;
  }

  public char readChar() throws IOException {
    if (next >= chars.length()) {
      throw new IOException("end of stream");
    }
    char c = chars.charAt(next);
    next++;
    endOffset++;
    return c;
  }

  public char beginToken() throws IOException {
    beginOffset = endOffset;
    return readChar();
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
    return 0;
  }

  public int getBeginColumn() {
    return 0;
  }

  public int getEndColumn() {
    return 0;
  }

  public int getEndLine() {
    return 0;
  }

  public void close() throws IOException {
    next = 0;
    beginOffset = endOffset = 0;
  }
}
