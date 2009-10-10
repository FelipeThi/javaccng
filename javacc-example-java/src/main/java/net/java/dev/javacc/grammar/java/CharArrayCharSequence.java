package net.java.dev.javacc.grammar.java;

/**
 * Character sequence that wraps array of characters.
 * <p>
 * This implementation does not escape unicode sequences.
 * <p>
 * This implementation does track line and column numbers.
 * <p>
 * Do not modify content of the underlying array otherwise this char sequence
 * will provide erroneous characters on subsequent calls since it does not clone
 * the wrapped array for performance reason.
 */
public final class CharArrayCharSequence implements CharSequence, java.io.Serializable {
  private final char[] chars;
  private final int offset, length;
  private int hashCode;

  /**
   * Create character sequence from the wrapped array of characters.
   *
   * @param chars Character array to wrap.
   */
  public CharArrayCharSequence(final char[] chars) {
    this(chars, 0, chars.length);
  }

  /**
   * Create character sequence from the wrapped array of characters.
   *
   * @param chars  Character array to wrap.
   * @param offset Index of the first character in the array.
   * @param length Number of characters in the array.
   */
  public CharArrayCharSequence(final char[] chars, final int offset, final int length) {
    if (chars == null) {
      throw new IllegalArgumentException("chars is null");
    }
    if (offset < 0 || offset > chars.length) {
      throw new StringIndexOutOfBoundsException(offset);
    }
    if (offset + length > chars.length) {
      throw new StringIndexOutOfBoundsException(length);
    }
    this.chars = chars;
    this.offset = offset;
    this.length = length;
  }

  public int length() {
    return length;
  }

  public char charAt(final int index) {
    if (index < 0 || index >= length) {
      throw new StringIndexOutOfBoundsException(index);
    }
    return chars[offset + index];
  }

  public CharSequence subSequence(final int start, final int end) {
    if (start < 0 || start > length) {
      throw new StringIndexOutOfBoundsException(start);
    }
    if (end < start || end > length) {
      throw new StringIndexOutOfBoundsException(end);
    }
    return start == 0 && end == length ? this : new CharArrayCharSequence(chars, offset + start, end - start);
  }

  /**
   * Get string representation of this character sequence.
   *
   * @return String build from wrapped characters.
   */
  @Override
  public String toString() {
    return new String(chars, offset, length);
  }

  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (object instanceof CharArrayCharSequence) {
      final CharArrayCharSequence that = (CharArrayCharSequence) object;
      if (length == that.length) {
        for (int n = 0; n < length; n++) {
          if (chars[n + offset] != that.chars[n + that.offset]) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (hashCode == 0) {
      int h = 0;
      for (int n = offset; n < length; n++) {
        h = 31 * h + chars[n];
      }
      hashCode = h;
    }
    return hashCode;
  }
}
