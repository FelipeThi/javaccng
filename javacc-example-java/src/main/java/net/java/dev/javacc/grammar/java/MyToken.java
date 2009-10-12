package net.java.dev.javacc.grammar.java;

public class MyToken extends Token {
  public MyToken(final int kind) {
    super(kind);
  }

  public MyToken(final int kind, final String image) {
    super(kind, image);
  }

  int realKind = JavaParserConstants.GT;

  /** Returns a new Token object. */
  public static Token newToken(int ofKind, String tokenImage) {
    return new MyToken(ofKind, tokenImage);
  }
}
