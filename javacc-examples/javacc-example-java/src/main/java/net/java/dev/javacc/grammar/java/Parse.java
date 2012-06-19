package net.java.dev.javacc.grammar.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

class Parse {
  public static void main(String[] args) throws Exception {
    if (args.length == 0 || args.length > 2) {
      System.out.println("usage: Parse FILE [ENCODING]");
      return;
    }
    Reader reader;
    if (args.length == 2) {
      reader = new InputStreamReader(new FileInputStream(new File(args[0])), args[1]);
    }
    else {
      reader = new InputStreamReader(new FileInputStream(new File(args[0])));
    }
    CharStream stream = new CharStream.ForReader(reader);
    JavaScanner scanner = new JavaScanner(stream);
    JavaParser parser = new JavaParser(scanner);
    parser.CompilationUnit();
  }
}
