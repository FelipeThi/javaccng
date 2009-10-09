package net.java.dev.javacc.grammar.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

class Test {
  public static void main(String[] args) throws Exception {
    if (args.length == 0 || args.length > 2) {
      System.out.println("usage: Test FILE [ENCODING]");
      return;
    }
    final Reader reader;
    if (args.length == 2) {
      reader = new InputStreamReader(new FileInputStream(new File(args[0])), args[1]);
    }
    else {
      reader = new InputStreamReader(new FileInputStream(new File(args[0])));
    }
    final JavaCharStream charStream = new JavaCharStream(reader);
    final JavaParserTokenManager tokenManager = new JavaParserTokenManager(charStream);
    final JavaParser parser = new JavaParser(tokenManager);
    parser.CompilationUnit();
  }
}
