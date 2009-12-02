package org.javacc.uitl.io;

import org.javacc.utils.io.IndentingPrintWriter;
import static org.junit.Assert.*;
import org.junit.Test;

import java.io.StringWriter;

public class IndentingPrintWriterTest {
  @Test
  public void writeIndented() {
    final StringWriter s = new StringWriter();
    final IndentingPrintWriter w = new IndentingPrintWriter(s, "\r\n");
    w.setIndentString(">>");
    w.write("uno\n\n");
    w.indent().write("due\n\n");
    w.indent().write("tre\n\n");
    w.println("eins")
        .unindent().println("zwei")
        .unindent().println("drei");
    assertEquals("uno\r\n\r\n>>due\r\n\r\n>>>>tre\r\n\r\n>>>>eins\r\n>>zwei\r\ndrei\r\n", s.toString());
  }
}
