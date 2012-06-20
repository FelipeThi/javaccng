package org.javacc.examples.idl;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;

public class IDLParserTest {
  @Test
  public void parse() throws Exception {
    String source = Resources.toString(
        Resources.getResource("Hello.idl"), Charsets.UTF_8);
    IDLParser parser = new IDLParser(
        new IDLScanner(
            new CharStream.ForCharSequence(source)));
    parser.specification();
  }
}
