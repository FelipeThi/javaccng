package org.javacc.runtime;

import java.nio.CharBuffer;

public class ReadableCharStreamTest extends CharStreamBaseTestCase {
  @Override CharStream newCharStream(CharSequence content) {
    return new CharStream.ForReadable(CharBuffer.wrap(content));
  }
}
