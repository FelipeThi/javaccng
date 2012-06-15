package net.java.dev.javacc.grammar.java;

public class MyToken extends Token {
  int realKind = JavaConstants.GT;

  public MyToken(int kind, int begin, int end, String image) {
    super(kind, begin, end, image);
  }

  public static Token newToken(int kind, int begin, int end) {
    return newToken(kind, begin, end, null);
  }

  public static Token newToken(int kind, int begin, int end, String image) {
    switch (kind) {
      default:
        return new MyToken(kind, begin, end, image);
    }
  }
}
