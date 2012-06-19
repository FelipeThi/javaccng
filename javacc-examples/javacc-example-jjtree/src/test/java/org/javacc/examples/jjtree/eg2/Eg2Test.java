package org.javacc.examples.jjtree.eg2;

import org.junit.Test;

import java.io.IOException;

public class Eg2Test {
  @Test
  public void test() throws IOException, ParseException {
    Eg2 parser = new Eg2(
        new Eg2Scanner(
            new CharStream.ForCharSequence(
                "(a + b) * a + d;")));
    ASTStart n = parser.Start();
    n.dump(">");
  }
}
