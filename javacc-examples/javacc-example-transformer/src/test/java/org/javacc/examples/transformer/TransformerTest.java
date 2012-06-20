package org.javacc.examples.transformer;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class TransformerTest {
  @Test
  public void test() throws IOException, ParseException {
    String source = Resources.toString(
        Resources.getResource("divide.toy"), Charsets.UTF_8);

    ToyParser parser = new ToyParser(
        new ToyScanner(
            new CharStream.Escaping(
                new CharStream.ForCharSequence(source))));

    ASTCompilationUnit cu = parser.CompilationUnit();

    PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out));
    try {
      cu.process(out);
    }
    finally {
      out.close();
    }
  }
}
