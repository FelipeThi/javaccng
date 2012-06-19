package org.javacc.examples.jjtree.eg4;

import org.junit.Test;

import java.io.IOException;

public class Eg4Test {
  @Test
  public void test() throws IOException, ParseException {
    Eg4 parser = new Eg4(
        new Eg4Scanner(
            new CharStream.ForCharSequence(
                "(a + b) * a + d;")));
    ASTStart n = parser.Start();
    Eg4Visitor v = new Eg4DumpVisitor();
    n.jjtAccept(v, null);
  }
}
