package org.javacc.examples.spl;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

public class SPLTest {
  @Test
  public void factorial() throws IOException, ParseException {
    parse("/fact.spl");
  }

  @Test
  public void odd() throws IOException, ParseException {
    parse("/odd.spl");
  }

  @Test
  public void sqrt() throws IOException, ParseException {
    parse("/sqrt.spl");
  }

  private void parse(String name) throws IOException, ParseException {
    SPLParser parser = new SPLParser(
        new SPLParserScanner(
            new SimpleCharStream(
                new InputStreamReader(
                    open(name)))));
    parser.CompilationUnit();
    parser.jjtree.rootNode().interpret();
  }

  private static InputStream open(String name) {
    InputStream is = SPLTest.class.getResourceAsStream(name);
    assertNotNull(is);
    return is;
  }
}
