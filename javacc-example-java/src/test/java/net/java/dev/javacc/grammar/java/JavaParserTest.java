package net.java.dev.javacc.grammar.java;

import org.junit.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/** Parses all java files in the current project. */
public class JavaParserTest {
  static final FileFilter filter = new FileFilter() {
    public boolean accept(final File f) {
      return f.isDirectory() || f.getName().endsWith(".java");
    }
  };

  int count;

  @Test
  public void parseJavaFiles() throws IOException, ParseException {
    long started = System.currentTimeMillis();
    list(new File("."));
    long now = System.currentTimeMillis();
    System.out.println("Parsed " + count + " java files in " + ((now - started) / 1000.0) + " seconds");
  }

  private void parseJavaFile(final File file) throws IOException, ParseException {
    final InputStream inputStream = new FileInputStream(file);
    try {
      final InputStreamReader streamReader = new InputStreamReader(inputStream, "UTF-8");
      final JavaCharStream charStream = new JavaCharStream(streamReader);
      final JavaParserTokenManager tokenManager = new JavaParserTokenManager(charStream);
      final JavaParser parser = new JavaParser(tokenManager);
      parser.CompilationUnit();
    }
    finally {
      inputStream.close();
    }
    count++;
  }

  private void list(File parentFile) throws IOException, ParseException {
    final File[] files = parentFile.listFiles(filter);
    if (files != null) {
      for (File file : files) {
        if (file.isDirectory()) {
          list(file);
        }
        else {
          parseJavaFile(file);
        }
      }
    }
  }
}
