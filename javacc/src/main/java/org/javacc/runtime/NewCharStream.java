package org.javacc.runtime;

import java.io.IOException;
import java.nio.CharBuffer;

public interface NewCharStream {
  /**
   * Provides information about line and column for the characters
   * from the stream.
   */
  interface LineColumnInfo {
    /** @return Line number of the character just read within the stream, 0-based. */
    int line();

    /** @return Column number of the character just read within the stream, 0-based. */
    int column();
  }

  abstract class AbstractStream implements NewCharStream, LineColumnInfo {
    private int line, column;

    protected AbstractStream() {
      this(0, 0);
    }

    protected AbstractStream(int line, int column) {
      this.line = line;
      this.column = column;
    }

    @Override public final int line() {
      return line;
    }

    @Override public final int column() {
      return column;
    }

    protected void translate(int c) throws IOException {
      if (c == '\r') {
        if (la() == '\n') {
          // skip
        }
        else {
          line++;
          column = 0;
        }
      }
      else if (c == '\n') {
        line++;
        column = 0;
      }
      else {
        column++;
      }
    }

    protected abstract int la() throws IOException;
  }

  class CharSequenceStandardStream extends AbstractStream {
    private final CharSequence chars;
    private final int begin, end;
    private int position;

    public CharSequenceStandardStream(CharSequence chars) {
      this(chars, 0, chars.length());
    }

    public CharSequenceStandardStream(CharSequence chars, int begin, int end) {
      if (begin < 0) { throw new IllegalArgumentException(); }
      if (end > chars.length()) { throw new IllegalArgumentException(); }
      if (begin > end) { throw new IllegalArgumentException(); }
      this.chars = chars;
      this.begin = begin;
      this.end = end;
      this.position = begin;
    }

    public void reset() {
      position = begin;
    }

    @Override public int read() throws IOException {
      if (position < end) {
        char c = chars.charAt(position++);
        translate(c);
        return c;
      }
      return -1;
    }

    @Override public int position() {
      return position;
    }

    @Override protected int la() {
      if (position < end) {
        return chars.charAt(position);
      }
      return -1;
    }
  }

  class StandardStream extends AbstractStream {
    private final Readable reader;
    private final CharBuffer buffer;
    private int position;
    private int la;

    public StandardStream(Readable reader) {
      this.reader = reader;
      buffer = CharBuffer.allocate(128);
      la = Integer.MIN_VALUE;
    }

    @Override public int read() throws IOException {
      int c = readImpl();
      if (c != -1) {
        translate(c);
        return c;
      }
      return c;
    }

    @Override public int position() {
      return position;
    }

    private int readImpl() throws IOException {
      if (la != Integer.MIN_VALUE) {
        return la;
      }
      if (buffer.position() == buffer.limit()) {
        buffer.clear();
        int read = reader.read(buffer);
        if (read == -1) {
          return -1;
        }
      }
      char c = buffer.get();
      position++;
      return c;
    }

    @Override protected int la() {
      return la;
    }
  }

  class JavaCharStream extends AbstractStream {
    private final Readable reader;
    private final CharBuffer buffer;

    public JavaCharStream(Readable reader) {
      this.reader = reader;
      buffer = CharBuffer.allocate(128);
    }

    @Override public int read() throws IOException {
      return buffer.get();
    }

    @Override public int position() {
      return 0;
    }

    @Override protected int la() {
      return 0;
    }
  }

  class CCharStream extends AbstractStream {
    @Override public int read() throws IOException {
      return 0;
    }

    @Override public int position() {
      return 0;
    }

    @Override protected int la() {
      return 0;
    }
  }

  /** @return The next character read, or -1 if eof. */
  int read() throws IOException;

  /** @return Offset to the character just read within the stream, 0-based. */
  int position();
}
