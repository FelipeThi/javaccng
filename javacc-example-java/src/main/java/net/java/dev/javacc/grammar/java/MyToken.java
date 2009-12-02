package net.java.dev.javacc.grammar.java;

public class MyToken extends Token {
  public MyToken(final int kind) {
    super(kind);
  }

  int realKind = JavaParserConstants.GT;

  /** Returns a new Token object. */
  public static Token newToken(int ofKind) {
    return new MyToken(ofKind);
  }
}
