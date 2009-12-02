package org.javacc.runtime;

import java.io.StringReader;

public class SimpleCharStreamTest extends CharStreamBaseTestCase {
  @Override
  CharStream makeCharStream(final CharSequence content) {
    return new SimpleCharStream(new StringReader(content.toString()));
  }
}
