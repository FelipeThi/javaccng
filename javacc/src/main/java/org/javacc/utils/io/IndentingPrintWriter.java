package org.javacc.utils.io;

import java.io.IOException;
import java.io.Writer;
import java.util.Formatter;
import java.util.Locale;

/**
 * Writes indentation spaces after eol character.
 * for simpler source code printing.
 * Also takes care of converting eol character <code>\n</code>
 * to platform-dependent characters sequence, such as <code>\r\n</code>
 * on Windows.
 */
public class IndentingPrintWriter extends Writer {
  private final String lineSeparator;
  private final Writer out;
  private String indentString = "  ";
  private Formatter formatter;
  private int level;
  private boolean indent;

  public IndentingPrintWriter(final Writer writer) {
    out = writer;
    lineSeparator = System.getProperty("line.separator", "\n");
  }

  public IndentingPrintWriter(final Writer writer, String eol) {
    out = writer;
    lineSeparator = eol;
  }

  public String getIndentString() {
    return indentString;
  }

  public void setIndentString(final String indentString) {
    this.indentString = indentString;
  }

  /** Increase indentation level. */
  public IndentingPrintWriter indent() {
    level++;
    return this;
  }

  /** Decrease indentation level. */
  public IndentingPrintWriter unindent() {
    if (level == 0) {
      throw new IllegalStateException("Canned unindent any further");
    }
    level--;
    return this;
  }

  /**
   * Translates new line character \n to platform dependent new line sequence,
   * takes care of indentation.
   *
   * @param c A char to write.
   */
  @Override
  public void write(final int c) {
    try {
      if (c == '\n') {
        indent = true;
        out.write(lineSeparator);
      }
      else {
        if (indent) {
          for (int n = 0; n < level; n++) {
            out.write(indentString);
          }
        }
        indent = false;
        out.write(c);
      }
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void write(final char[] buf) {
    write(buf, 0, buf.length);
  }

  @Override
  public void write(final char[] buf, final int off, final int len) {
    for (int n = off; n < off + len; n++) {
      write(buf[n]);
    }
  }

  @Override
  public void write(final String str) {
    write(str, 0, str.length());
  }

  @Override
  public void write(final String str, final int off, final int len) {
    for (int n = off; n < off + len; n++) {
      write(str.charAt(n));
    }
  }

  @Override
  public void flush() {
    try {
      out.flush();
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void close() {
    try {
      out.close();
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private void newLine() {
    write("\n");
    flush();
  }

  /* Methods that do not terminate lines */

  public IndentingPrintWriter print(boolean b) {
    write(b ? "true" : "false");
    return this;
  }

  public IndentingPrintWriter print(char c) {
    write(c);
    return this;
  }

  public IndentingPrintWriter print(int i) {
    write(String.valueOf(i));
    return this;
  }

  public IndentingPrintWriter print(long l) {
    write(String.valueOf(l));
    return this;
  }

  public IndentingPrintWriter print(float f) {
    write(String.valueOf(f));
    return this;
  }

  public IndentingPrintWriter print(double d) {
    write(String.valueOf(d));
    return this;
  }

  public IndentingPrintWriter print(char s[]) {
    write(s);
    return this;
  }

  public IndentingPrintWriter print(String s) {
    if (s == null) {
      s = "null";
    }
    write(s);
    return this;
  }

  public IndentingPrintWriter print(Object obj) {
    write(String.valueOf(obj));
    return this;
  }

  /* Methods that do terminate lines */

  public IndentingPrintWriter println() {
    newLine();
    return this;
  }

  public IndentingPrintWriter println(boolean x) {
    print(x);
    println();
    return this;
  }

  public IndentingPrintWriter println(char x) {
    print(x);
    println();
    return this;
  }

  public IndentingPrintWriter println(int x) {
    print(x);
    println();
    return this;
  }

  public IndentingPrintWriter println(long x) {
    print(x);
    println();
    return this;
  }

  public IndentingPrintWriter println(float x) {
    print(x);
    println();
    return this;
  }

  public IndentingPrintWriter println(double x) {
    print(x);
    println();
    return this;
  }

  public IndentingPrintWriter println(char x[]) {
    print(x);
    println();
    return this;
  }

  public IndentingPrintWriter println(String x) {
    print(x);
    println();
    return this;
  }

  public IndentingPrintWriter println(Object x) {
    String s = String.valueOf(x);
    print(s);
    println();
    return this;
  }

  public IndentingPrintWriter printf(String format, Object... args) {
    return format(format, args);
  }

  public IndentingPrintWriter printf(Locale l, String format, Object... args) {
    return format(l, format, args);
  }

  public IndentingPrintWriter format(String format, Object... args) {
    if (formatter == null || formatter.locale() != Locale.getDefault()) {
      formatter = new Formatter(this);
    }
    formatter.format(Locale.getDefault(), format, args);
    return this;
  }

  public IndentingPrintWriter format(Locale l, String format, Object... args) {
    if (formatter == null || formatter.locale() != l) {
      formatter = new Formatter(this, l);
    }
    formatter.format(l, format, args);
    return this;
  }

  @Override
  public IndentingPrintWriter append(CharSequence seq) {
    if (seq == null) {
      write("null");
    }
    else {
      write(seq.toString());
    }
    return this;
  }

  @Override
  public IndentingPrintWriter append(CharSequence seq, int start, int end) {
    if (seq == null) {
      write("null");
    }
    else {
      write(seq.subSequence(start, end).toString());
    }
    return this;
  }

  @Override
  public IndentingPrintWriter append(char c) {
    write(c);
    return this;
  }
}
