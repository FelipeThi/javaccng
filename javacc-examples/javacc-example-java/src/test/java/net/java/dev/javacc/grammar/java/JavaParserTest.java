package net.java.dev.javacc.grammar.java;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.junit.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class JavaParserTest {
  static class Source {
    final File file;
    final String content;

    Source(File file, String content) {
      this.file = file;
      this.content = content;
    }
  }

  @Test
  public void parseJavaFiles() throws IOException, ParseException {
    ArrayList<Source> sources = new ArrayList<Source>();

    list(sources, new FileFilter() {
      @Override public boolean accept(File f) {
        return f.getName().endsWith(".java")
            && !"package-info.java".equals(f.getName());
      }
    }, new File("."));

    long started = System.currentTimeMillis();
    long chars = 0;

    for (Source source : sources) {
      CharStream stream = new CharStream.Escaping(
          new CharStream.ForCharSequence(source.content));
      JavaScanner scanner = new JavaScanner(stream);
      JavaParser parser = new JavaParser(scanner);
      try {
        parser.CompilationUnit();
      }
      catch (Exception ex) {
        throw new IOException("Error parsing file " + source.file, ex);
      }
      chars += source.content.length();
    }

    long now = System.currentTimeMillis();

    System.out.println(String.format("Parsed %d files (%d chars) in %s seconds",
        sources.size(), chars, (now - started) / 1000.0));
  }

  @Test
  public void lexicalErrorReporting() throws IOException {
    String source = "/* comment";
    CharStream stream = new CharStream.Escaping(
        new CharStream.ForCharSequence(source));
    Scanner scanner = new JavaScanner(stream);
    ScannerException exception = null;
    try {
      scanner.getNextToken();
    }
    catch (ScannerException ex) {
      exception = ex;
    }
    assertNotNull(exception);
    assertEquals("Lexical error at line 1, column 1. Encountered: <EOF> after: \"/* comment\"",
        exception.getMessage());
  }

  @Test
  public void parsingErrorReporting()
      throws IOException, ParseException {
    String source = "final class MyClass {\nvoid ();\n}";
    CharStream stream = new CharStream.Escaping(
        new CharStream.ForCharSequence(source));
    JavaScanner scanner = new JavaScanner(stream);
    ParseException exception = null;
    JavaParser parser = new JavaParser(scanner);
    try {
      parser.CompilationUnit();
    }
    catch (ParseException ex) {
      exception = ex;
    }
    assertNotNull(exception);
    assertEquals("Encountered: \"(\" at line 2, column 6.\nWas expecting:\n   <IDENTIFIER>...",
        exception.getMessage());
  }

  void list(ArrayList<Source> sources, FileFilter filter, File parentFile)
      throws IOException, ParseException {
    File[] files = parentFile.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isDirectory()) {
          list(sources, filter, file);
        }
        else {
          if (filter.accept(file)) {
            sources.add(new Source(file, Files.toString(file, Charsets.UTF_8)));
          }
        }
      }
    }
  }
}
