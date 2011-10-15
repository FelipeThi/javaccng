package org.javacc.examples.transformer;

import org.junit.Test;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class TransformerTest {
  @Test
  public void test() throws Exception {
    ToyParser parser = new ToyParser(
        new ToyScanner(
            new JavaCharStream(
                new InputStreamReader(
                    getClass().getResourceAsStream("/divide.toy")))));

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
