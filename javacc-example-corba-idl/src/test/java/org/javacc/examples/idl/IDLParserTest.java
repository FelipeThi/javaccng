package org.javacc.examples.idl;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.*;

public class IDLParserTest {
  @Test
  public void test() throws IOException, ParseException {
    InputStream is = getClass().getResourceAsStream("/Hello.idl");
    assertNotNull("Resource not found", is);
    IDLParser parser = new IDLParser(
        new IDLParserTokenManager(
            new SimpleCharStream(
                new InputStreamReader(is))));
    parser.specification();
  }
}
