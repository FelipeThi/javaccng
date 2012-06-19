package org.javacc.runtime;

import java.io.StringReader;

public class ReaderCharStreamTest extends CharStreamBaseTestCase {
  @Override CharStream newCharStream(CharSequence content) {
    return new CharStream.ForReader(new StringReader(String.valueOf(content)));
  }
}
