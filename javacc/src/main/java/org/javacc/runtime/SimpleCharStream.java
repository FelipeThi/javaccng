package org.javacc.runtime;

/**
 * An implementation of interface CharStream, where the stream is assumed to
 * contain only ASCII characters (without unicode processing).
 */
public final class SimpleCharStream implements CharStream {
  /** Reader that provides characters. */
  private final java.io.Reader reader;
  /** Buffer contains character of the token currently being read. */
  private char[] buffer;
  /** Number of token characters read so far. */
  private int length;
  /** Offset to the next character in the buffer, if backed up. */
  private int offset;
  /** Index of the first character of the current token, inclusive. */
  private int begin;
  /** Index of the last character of the current token, exclusive. */
  private int end;
  /** Line number of the first character of the current token. */
  private int line = 1;
  /** Column number of the first character of the current token. */
  private int column = 1;
  /** Contains line numbers for read characters. */
  private int[] bufLine;
  /** Contains column numbers for read characters. */
  private int[] bufColumn;
  /** For line and column counting -- remembers if the last sequence is of CR LF chars. */
  private int state;
  /** Tab size. */
  private int tabSize = 8;

  /**
   * Create new character stream from the specified reader.
   *
   * @param reader A reader from which to read characters.
   */
  public SimpleCharStream(java.io.Reader reader) {
    this.reader = reader;
    buffer = new char[4096];
    bufLine = new int[4096];
    bufColumn = new int[4096];
  }

  /**
   * Create new character stream from the specified reader.
   *
   * @param reader A reader from which to read characters.
   * @param offset An offset to start count from.
   */
  public SimpleCharStream(java.io.Reader reader, int offset) {
    this(reader);
    begin = offset;
  }

  /**
   * Create new character stream from the specified reader.
   *
   * @param reader      A reader from which to read characters.
   * @param startLine   A line number to start count from.
   * @param startColumn A column number to start count from.
   */
  public SimpleCharStream(java.io.Reader reader, int startLine, int startColumn) {
    this(reader);
    line = startLine;
    column = startColumn;
  }

  /**
   * Create new character stream from the specified reader.
   *
   * @param reader      A reader from which to read characters.
   * @param offset      An offset to start count from.
   * @param startLine   A line number to start count from.
   * @param startColumn A column number to start count from.
   */
  public SimpleCharStream(java.io.Reader reader, int offset, int startLine, int startColumn) {
    this(reader);
    begin = offset;
    line = startLine;
    column = startColumn;
  }

  public void setTabSize(int i) {
    if (i < 0) {
      throw new IllegalArgumentException();
    }
    tabSize = i;
  }

  public int getTabSize(int i) {
    return tabSize;
  }

  public void beginToken() throws java.io.IOException {
    begin = end;
    if (offset == length) {
      // No backed up characters.
      length = 0;
      offset = 0;
    }
    else {
      // Preserve backed up characters.
      System.arraycopy(buffer, offset, buffer, 0, length - offset);
      System.arraycopy(bufLine, offset, bufLine, 0, length - offset);
      System.arraycopy(bufColumn, offset, bufColumn, 0, length - offset);
      length = length - offset;
      offset = 0;
    }
  }

  public int readChar() throws java.io.IOException {
    if (offset < length) {
      // Read backed up characters.
      end++;
      return buffer[offset++];
    }
    // Read new character by reader and put it into buffer.
    return read();
  }

  /**
   * Reads next character from the reader and appends it to the buffer of token chars.
   *
   * @return Character read from the reader.
   * @throws java.io.IOException If reader throws exception.
   */
  private int read() throws java.io.IOException {
    final int c = reader.read();
    if (c != -1) {
      appendChar(c);
    }
    return c;
  }

  /**
   * Append character to the token buffer.
   *
   * @param c Character to append.
   */
  private void appendChar(final int c) {
    end++;
    if (offset == buffer.length) {
      final int newLength = buffer.length * 3 / 2 + 1;
      buffer = java.util.Arrays.copyOf(buffer, newLength);
      bufLine = java.util.Arrays.copyOf(bufLine, newLength);
      bufColumn = java.util.Arrays.copyOf(bufColumn, newLength);
    }
    updateLineColumn(c);
    buffer[offset++] = (char) c;
    length = offset;
  }

  /**
   * Truncate token buffer at the specified offset.
   *
   * @param c     Character to put at the specified index.
   * @param index New buffer length.
   */
  private void insertChar(final int c, int index) {
    buffer[index] = (char) c;
    length = offset = index + 1;
  }

  public void backup(int amount) {
    end -= amount;
    offset -= amount;
  }

  public String getImage() {
    return new String(buffer, 0, offset);
  }

  public char[] getSuffix(int l) {
    char[] ret = new char[l];
    System.arraycopy(buffer, length - l, ret, 0, l);
    return ret;
  }

  private void updateLineColumn(int c) {
    if (state == 1) {
      if (c == '\n') {
        state = 2;
        count(c);
        return;
      }
      else {
        wrap();
      }
    }
    else if (state == 3) {
      wrap();
    }
    else if (state == 2) {
      wrap();
    }

    count(c);

    if (c == '\r') {
      state = 1;
    }
    else if (c == '\n') {
      state = 3;
    }
    else {
      state = 0;
    }
  }

  private void count(final int c) {
    bufLine[offset] = line;
    bufColumn[offset] = column;
    if (c == '\t') {
      column += tabSize - column % tabSize;
    }
    else {
      column++;
    }
  }

  private void wrap() {
    line += column = 1;
    bufLine[offset] = line;
    bufColumn[offset] = column;
  }

  public int getBegin() {
    return begin;
  }

  public int getEnd() {
    return end;
  }

  public int getBeginLine() {
    return bufLine[0];
  }

  public int getBeginColumn() {
    return bufColumn[0];
  }

  public int getEndLine() {
    if (offset > 0) {
      return bufLine[offset - 1];
    }
    return 0;
  }

  public int getEndColumn() {
    if (offset > 0) {
      return bufColumn[offset - 1];
    }
    return 0;
  }

  public void close() {
    buffer = null;
    bufLine = null;
    bufColumn = null;
  }
}
