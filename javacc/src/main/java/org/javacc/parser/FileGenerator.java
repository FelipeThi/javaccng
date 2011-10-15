package org.javacc.parser;

import java.io.IOException;

public interface FileGenerator {
  void start() throws MetaParseException, IOException;
}
