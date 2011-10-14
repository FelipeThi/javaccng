package org.javacc.examples.jjtree.eg3;

import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

public class Eg3Test {
  @Test
  public void test() throws IOException, ParseException {
    Eg3 parser = new Eg3(
        new Eg3Scanner(
            new SimpleCharStream(
                new StringReader("(a + b) * a + d;"))));
    ASTStart n = parser.Start();
    n.dump(">");
  }
}
