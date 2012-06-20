package org.javacc.examples.spl;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.IOException;

public class SPLTest {
  @Test
  public void factorial() throws IOException, ParseException {
    parse("fact.spl");
  }

  @Test
  public void odd() throws IOException, ParseException {
    parse("odd.spl");
  }

  @Test
  public void sqrt() throws IOException, ParseException {
    parse("sqrt.spl");
  }

  private void parse(String name) throws IOException, ParseException {
    SPLParser parser = new SPLParser(
        new SPLScanner(
            new CharStream.ForCharSequence(
                open(name))));
    parser.CompilationUnit();
    parser.jjTree.rootNode().interpret();
  }

  private static String open(String name) throws IOException {
    return Resources.toString(
        Resources.getResource(name), Charsets.UTF_8);
  }
}
