package org.javacc.runtime;

import java.io.IOException;

public class CharSequenceCharStreamTest extends CharStreamBaseTestCase {
  @Override
  CharStream makeCharStream(final CharSequence content) {
    return new CharSequenceCharStream(content);
  }

  @Override
  public void providesLineAndColumnNumbers() throws IOException {
    // do nothing as this implementation
    // does not provide line and column numbers
  }
}
