package net.java.dev.javacc.grammar.java;

public class MyToken extends Token {
  int realKind = JavaParserConstants.GT;

  public MyToken(final int kind) {
    super(kind);
  }

  public MyToken(int kind, String image) {
    super(kind, image);
  }

  public static Token newToken(int ofKind, String image) {
    switch (ofKind) {
      default:
        return new MyToken(ofKind, image);
    }
  }

  public static Token newToken(int ofKind) {
    return newToken(ofKind, null);
  }
}
