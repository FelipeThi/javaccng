package net.java.dev.javacc.grammar.java;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/** Parses all java files in the current project. */
public class JavaParserTest {
  static class Source {
    final String fileName;
    final String content;

    Source(final String fileName, final String content) {
      this.fileName = fileName;
      this.content = content;
    }
  }

  final List<Source> sources = new ArrayList<Source>(1000);

  static final FileFilter filter = new FileFilter() {
    public boolean accept(final File f) {
      return f.isDirectory()
          || f.getName().endsWith(".java")
          && !"package-info.java".equals(f.getName());
    }
  };

  @Test
  public void parseJavaFiles() throws IOException, ParseException {
    list(new File("."));

    long started = System.currentTimeMillis();
    long chars = 0;

    for (Source source : sources) {
      final Reader reader = new StringReader(source.content);
      final CharStream charStream = new JavaCharStream(reader);
      final TokenManager tokenManager = new JavaParserTokenManager(charStream);
      final JavaParser parser = new JavaParser(tokenManager);
      try {
        parser.CompilationUnit();
      }
      catch (IOException ex) {
        System.out.println("I/O error while parsing file " + source.fileName);
        throw ex;
      }
      catch (ParseException ex) {
        System.out.println("Error parsing file " + source.fileName);
        throw ex;
      }
      chars += source.content.length();
    }

    long now = System.currentTimeMillis();

    System.out.println("Parsed " + sources.size() + " java files"
        + " (" + chars + " chars) in " + ((now - started) / 1000.0) + " seconds");
  }

  @Test
  public void lexicalErrorReporting() throws IOException {
    final Reader reader = new StringReader("/* comment");
    final CharStream charStream = new JavaCharStream(reader);
    final TokenManager tokenManager = new JavaParserTokenManager(charStream);
    TokenManagerException exception = null;
    try {
      tokenManager.getNextToken();
    }
    catch (TokenManagerException ex) {
      exception = ex;
    }
    assertNotNull(exception);
    assertEquals("Lexical error at line 1, column 10. Encountered: <EOF> after: \"/* comment\"", exception.getMessage());
  }

  @Test
  public void parsingErrorReporting() throws IOException {
    final Reader reader = new StringReader("final class MyClass {\nvoid ();\n}");
    final CharStream charStream = new JavaCharStream(reader);
    final TokenManager tokenManager = new JavaParserTokenManager(charStream);
    ParseException exception = null;
    try {
      final JavaParser parser = new JavaParser(tokenManager);
      parser.CompilationUnit();
    }
    catch (ParseException ex) {
      exception = ex;
    }
    assertNotNull(exception);
    assertEquals("Encountered: \"(\" at line 2, column 6.\nWas expecting:\n   <IDENTIFIER>...", exception.getMessage());
  }

  void load(final File file) throws IOException, ParseException {
    sources.add(new Source(file.getCanonicalPath(), readFile(file)));
  }

  void list(File parentFile) throws IOException, ParseException {
    final File[] files = parentFile.listFiles(filter);
    if (files != null) {
      for (File file : files) {
        if (file.isDirectory()) {
          list(file);
        }
        else {
          load(file);
        }
      }
    }
  }

  final StringBuilder buffer = new StringBuilder(0x4000);

  String readFile(File file) throws IOException {
    final String s;
    try {
      final InputStream inputStream = new FileInputStream(file);
      try {
        final Reader reader = new BufferedReader(
            new InputStreamReader(inputStream, "UTF-8"));
        int c;
        while ((c = reader.read()) != -1) {
          buffer.append((char) c);
        }
      }
      finally {
        inputStream.close();
      }
      s = buffer.toString();
    }
    finally {
      buffer.setLength(0);
    }
    return s;
  }
}
