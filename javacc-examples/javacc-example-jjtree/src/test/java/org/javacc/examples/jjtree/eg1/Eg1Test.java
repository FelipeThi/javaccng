package org.javacc.examples.jjtree.eg1;

import org.junit.Test;

import java.io.IOException;

public class Eg1Test {
  @Test
  public void test() throws IOException, ParseException {
    Eg1 parser = new Eg1(
        new Eg1Scanner(
            new CharStream.ForCharSequence(
                "(a + b) * a + d;")));
    SimpleNode n = parser.Start();
    n.dump(">");
  }
}
