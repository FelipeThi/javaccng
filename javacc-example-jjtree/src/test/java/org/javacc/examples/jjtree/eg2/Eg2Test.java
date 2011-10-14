package org.javacc.examples.jjtree.eg2;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

public class Eg2Test {
  @Test
  public void test() throws IOException, ParseException {
    Eg2 parser = new Eg2(
        new Eg2TokenManager(
            new SimpleCharStream(
                new StringReader("(a + b) * a + d;"))));
    ASTStart n = parser.Start();
    n.dump(">");
  }
}
