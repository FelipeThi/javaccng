package org.javacc.runtime;

public interface CharStream extends java.io.Closeable {
  /**
   * Provides information about line and column for the characters
   * from the stream.
   */
  interface LineColumnInfo {
    /** @return Line number of the next character to read within the stream, 0-based. */
    int line();

    /** @return Column number of the next character to read within the stream, 0-based. */
    int column();
  }

  /** @return The next character from the input. */
  int read() throws java.io.IOException;

  /** @return Index of the next character to read within the stream, 0-based. */
  int position();

  /**
   * A {@link CharStream} implementation that reads characters from the
   * provided {@link CharSequence}.
   *
   * <p>This implementation does not process java-like unicode escapes.</p>
   */
  final class ForCharSequence
      implements CharStream, LineColumnInfo {
    private final CharSequence chars;
    private int begin, end;
    private int pos;
    private int line, column;

    /**
     * Create new char stream instance.
     *
     * @param chars The char sequence to read characters from.
     */
    public ForCharSequence(CharSequence chars) {
      this(chars, 0, chars.length());
    }

    /**
     * Create new char stream instance.
     *
     * @param chars The char sequence to read characters from.
     * @param begin Index of the first character in the sequence, inclusive.
     * @param end   Index of the last character in the sequence, exclusive.
     */
    public ForCharSequence(CharSequence chars, int begin, int end) {
      if (chars == null) {
        throw new IllegalArgumentException();
      }
      if (begin < 0) {
        throw new StringIndexOutOfBoundsException();
      }
      if (end > chars.length()) {
        throw new StringIndexOutOfBoundsException();
      }
      if (begin > end) {
        throw new StringIndexOutOfBoundsException();
      }
      this.chars = chars;
      this.begin = begin;
      this.end = end;
      this.pos = begin;
    }

    @Override public int read() {
      if (pos >= end) {
        return -1;
      }
      char c = chars.charAt(pos);
      pos++;
      translate(c);
      return c;
    }

    @Override public int position() {
      return pos - begin;
    }

    @Override public int line() {
      return line;
    }

    @Override public int column() {
      return column;
    }

    private void translate(int c) {
      if (c == '\n') {
        line++;
        column = 0;
      }
      else {
        if (c != '\r') {
          column++;
        }
      }
    }

    @Override public void close() {}
  }

  /**
   * A {@link CharStream} implementation that can read from
   * arbitrary {@link Readable}.
   */
  final class ForReadable
      implements CharStream, LineColumnInfo {
    private final Readable readable;
    private final java.nio.CharBuffer buffer;
    private int pos;
    private int line, column;

    /**
     * Create new char stream instance.
     *
     * @param readable The readable to read characters from.
     */
    public ForReadable(Readable readable) {
      this(readable, 1024);
    }

    /**
     * Create new char stream instance.
     *
     * @param readable The readable to read characters from.
     * @param capacity Internal buffer capacity.
     */
    public ForReadable(Readable readable, int capacity) {
      if (readable == null) {
        throw new IllegalArgumentException();
      }
      this.readable = readable;
      buffer = java.nio.CharBuffer.allocate(capacity);
      buffer.position(buffer.limit());
    }

    @Override public int read() throws java.io.IOException {
      if (buffer.position() == buffer.limit()) {
        buffer.position(0);
        readable.read(buffer);
        buffer.flip();
      }
      if (buffer.position() == buffer.limit()) {
        return -1;
      }
      char c = buffer.get();
      pos++;
      translate(c);
      return c;
    }

    @Override public int position() {
      return pos;
    }

    @Override public int line() {
      return line;
    }

    @Override public int column() {
      return column;
    }

    private void translate(int c) {
      if (c == '\n') {
        line++;
        column = 0;
      }
      else {
        if (c != '\r') {
          column++;
        }
      }
    }

    @Override public void close() throws java.io.IOException {
      if (readable instanceof java.io.Closeable) {
        ((java.io.Closeable) readable).close();
      }
    }
  }

  /**
   * A {@link CharStream} implementation that can read from
   * arbitrary {@link java.io.Reader}.
   */
  final class ForReader
      implements CharStream, LineColumnInfo {
    private final java.io.Reader reader;
    private int pos;
    private int line, column;

    /**
     * Create new char stream instance.
     *
     * @param reader The reader to read characters from.
     */
    public ForReader(java.io.Reader reader) {
      if (reader == null) {
        throw new IllegalArgumentException();
      }
      this.reader = reader;
    }

    @Override public int read() throws java.io.IOException {
      int c = reader.read();
      if (c != -1) {
        pos++;
        translate(c);
      }
      return c;
    }

    @Override public int position() {
      return pos;
    }

    @Override public int line() {
      return line;
    }

    @Override public int column() {
      return column;
    }

    private void translate(int c) {
      if (c == '\n') {
        line++;
        column = 0;
      }
      else {
        if (c != '\r') {
          column++;
        }
      }
    }

    @Override public void close() throws java.io.IOException {
      reader.close();
    }
  }

  /**
   * An implementation of interface CharStream, where the stream is assumed to
   * contain only ASCII characters (with java-like unicode escape processing).
   */
  final class Escaping
      implements CharStream, LineColumnInfo {
    private final CharStream stream;
    private char[] buffer;
    private int[] position, line, column;
    private int offset, length;

    /**
     * Create new escaping char stream from the specified reader.
     *
     * @param stream A reader from which to read characters.
     */
    public Escaping(CharStream stream) {
      if (stream == null) {
        throw new IllegalArgumentException();
      }
      this.stream = stream;
      buffer = new char[8];
      position = new int[buffer.length];
      line = new int[buffer.length];
      column = new int[buffer.length];
    }

    @Override public int read() throws java.io.IOException {
      if (offset < length) {
        return buffer[offset++];
      }
      clearBuffer();
      int c = readToBuffer();
      if (c == '\\') {
        c = readAndEscape();
      }
      else {
        clearBuffer();
      }
      return c;
    }

    @Override public int position() {
      if (offset < length) {
        return position[offset];
      }
      return stream.position();
    }

    @Override public int line() {
      if (offset < length) {
        return line[offset];
      }
      return streamLine();
    }

    @Override public int column() {
      if (offset < length) {
        return column[offset];
      }
      return streamColumn();
    }

    private int streamColumn() {
      if (stream instanceof LineColumnInfo) {
        return ((LineColumnInfo) stream).column();
      }
      return -1;
    }

    private int streamLine() {
      if (stream instanceof LineColumnInfo) {
        return ((LineColumnInfo) stream).line();
      }
      return -1;
    }

    private int readToBuffer() throws java.io.IOException {
      int xp = stream.position();
      int xl = streamLine();
      int xc = streamColumn();
      int c = stream.read();
      if (c != -1) {
        if (length == buffer.length) {
          int newLength = buffer.length * 2;
          buffer = java.util.Arrays.copyOf(buffer, newLength);
          position = java.util.Arrays.copyOf(position, newLength);
          line = java.util.Arrays.copyOf(line, newLength);
          column = java.util.Arrays.copyOf(column, newLength);
        }
        position[length] = xp;
        line[length] = xl;
        column[length] = xc;
        buffer[length] = (char) c;
        length++;
      }
      return c;
    }

    private void clearBuffer() {
      offset = length = 0;
    }

    /**
     * Read possibly escape sequence that starts with the currently read slash char.
     * <p>
     * If slash starts a unicode escape sequence,
     * then that sequence is transformed into single character.
     * <p>
     * <a href="http://java.sun.com/docs/books/jls/third_edition/html/lexical.html#3.3">Java
     * Language Specification</a> defines unicode escapes as follows:
     * <pre>
     * UnicodeInputCharacter:
     *     UnicodeEscape
     *     RawInputCharacter
     *
     * UnicodeEscape:
     *     \ UnicodeMarker HexDigit HexDigit HexDigit HexDigit
     *
     * UnicodeMarker:
     *     u
     *     UnicodeMarker u
     *
     * RawInputCharacter:
     *     any Unicode character
     *
     * HexDigit: one of
     *     0 1 2 3 4 5 6 7 8 9 a b c d e f A B C D E F
     * </pre>
     *
     * @return Either slash or escaped unicode character.
     * @throws java.io.IOException If reader throws exception.
     */
    private int readAndEscape() throws java.io.IOException {
      int mark = offset;

      int c = readToBuffer();
      if (c == 'u') {
        while (c == 'u') {
          c = readToBuffer();
        }
        if (isDigit(c)) {
          int c1 = readToBuffer();
          if (isDigit(c1)) {
            int c2 = readToBuffer();
            if (isDigit(c2)) {
              int c3 = readToBuffer();
              if (isDigit(c3)) {
                clearBuffer();
                return parseDigit(c3)
                    | (parseDigit(c2) << 4)
                    | (parseDigit(c1) << 8)
                    | (parseDigit(c) << 12);
              }
            }
          }
        }

        // Escape sequence ended prematurely with EOF. We may
        // throw exception here to indicate illegal unicode escape.
        // But instead we will provide it as is, unescaped.
        offset = mark + 1;
        return '\\';
      }

      // A slash that does not start escape sequence.
      offset = mark + 1;
      return '\\';
    }

    @Override public void close() throws java.io.IOException {
      stream.close();
    }

    private static boolean isDigit(int c) {
      return c >= '0' && c <= '9'
          || c >= 'a' && c <= 'f'
          || c >= 'A' && c <= 'F';
    }

    private static int parseDigit(int c) {
      if (c >= '0' && c <= '9') { return c - '0'; }
      if (c >= 'a' && c <= 'z') { return c - 'a' + 10; }
      if (c >= 'A' && c <= 'Z') { return c - 'A' + 10; }
      throw new IllegalStateException();
    }
  }
}
