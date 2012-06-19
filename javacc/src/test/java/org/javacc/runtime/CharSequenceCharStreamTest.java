package org.javacc.runtime;

public class CharSequenceCharStreamTest extends CharStreamBaseTestCase {
  @Override CharStream newCharStream(CharSequence content) {
    return new CharStream.ForCharSequence(content);
  }
}
