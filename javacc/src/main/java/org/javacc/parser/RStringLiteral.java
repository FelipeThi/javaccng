/* Copyright (c) 2006, Sun Microsystems, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.javacc.parser;

import java.util.Hashtable;

/** Describes string literals. */
public final class RStringLiteral extends RegularExpression {
  /** The string image of the literal. */
  public String image;

  public RStringLiteral(Token t, String image) {
    setLine(t.getBeginLine());
    setColumn(t.getBeginColumn());
    this.image = image;
  }

  @Override
  public Nfa generateNfa(ScannerGen scannerGen, boolean ignoreCase) {
    if (image.length() == 1) {
      return new RCharacterList(image.charAt(0))
          .generateNfa(scannerGen, ignoreCase);
    }

    NfaState state = new NfaState(scannerGen);

    if (image.length() == 0) {
      return new Nfa(state, state);
    }

    NfaState startState = state;
    NfaState finalState = null;

    for (int i = 0; i < image.length(); i++) {
      finalState = new NfaState(scannerGen);
      state.charMoves = new char[1];
      state.addChar(image.charAt(i));

      if (Options.getIgnoreCase() || ignoreCase) {
        state.addChar(Character.toLowerCase(image.charAt(i)));
        state.addChar(Character.toUpperCase(image.charAt(i)));
      }

      state.next = finalState;
      state = finalState;
    }

    return new Nfa(startState, finalState);
  }

  /** Used for top level string literals. */
  public void generateDfa(ScannerGen scannerGen) {
    if (scannerGen.stringLiterals.maxStrKind <= ordinal) {
      scannerGen.stringLiterals.maxStrKind = ordinal + 1;
    }

    int len = image.length();
    if (len > scannerGen.stringLiterals.maxLen) {
      scannerGen.stringLiterals.maxLen = len;
    }
    for (int i = 0; i < len; i++) {
      char c;
      String s;
      if (Options.getIgnoreCase()) {
        s = ("" + (c = image.charAt(i))).toLowerCase();
      }
      else {
        s = "" + (c = image.charAt(i));
      }

      if (!scannerGen.nfaStates.unicodeWarningGiven && c > 0xff
          && !Options.getJavaUnicodeEscape()
          && !Options.getUserCharStream()) {
        scannerGen.nfaStates.unicodeWarningGiven = true;
        JavaCCErrors.warning(scannerGen.curRE, "Non-ASCII characters used in regular expression." +
            "Please make sure you use the correct Reader when you create the parser, " +
            "one that can handle your character set.");
      }

      Hashtable temp;
      if (i >= scannerGen.stringLiterals.charPosKind.size()) {
        // Kludge, but OK
        scannerGen.stringLiterals.charPosKind.add(temp = new Hashtable());
      }
      else {
        temp = (Hashtable) scannerGen.stringLiterals.charPosKind.get(i);
      }

      KindInfo info;
      if ((info = (KindInfo) temp.get(s)) == null) {
        temp.put(s, info = new KindInfo(scannerGen.maxOrdinal));
      }

      if (i + 1 == len) {
        info.insertFinalKind(ordinal);
      }
      else {
        info.insertValidKind(ordinal);
      }

      if (!Options.getIgnoreCase() && scannerGen.ignoreCase[ordinal]
          && c != Character.toLowerCase(c)) {
        s = String.valueOf(image.charAt(i)).toLowerCase();

        if (i >= scannerGen.stringLiterals.charPosKind.size()) {
          // Kludge, but OK
          scannerGen.stringLiterals.charPosKind.add(temp = new Hashtable());
        }
        else {
          temp = (Hashtable) scannerGen.stringLiterals.charPosKind.get(i);
        }

        if ((info = (KindInfo) temp.get(s)) == null) {
          temp.put(s, info = new KindInfo(scannerGen.maxOrdinal));
        }

        if (i + 1 == len) {
          info.insertFinalKind(ordinal);
        }
        else {
          info.insertValidKind(ordinal);
        }
      }

      if (!Options.getIgnoreCase()
          && scannerGen.ignoreCase[ordinal]
          && c != Character.toUpperCase(c)) {
        s = ("" + image.charAt(i)).toUpperCase();

        if (i >= scannerGen.stringLiterals.charPosKind.size()) {
          // Kludge, but OK
          scannerGen.stringLiterals.charPosKind.add(temp = new Hashtable());
        }
        else {
          temp = (Hashtable) scannerGen.stringLiterals.charPosKind.get(i);
        }

        if ((info = (KindInfo) temp.get(s)) == null) {
          temp.put(s, info = new KindInfo(scannerGen.maxOrdinal));
        }

        if (i + 1 == len) {
          info.insertFinalKind(ordinal);
        }
        else {
          info.insertValidKind(ordinal);
        }
      }
    }

    scannerGen.stringLiterals.maxLenForActive[ordinal / 64]
        = Math.max(scannerGen.stringLiterals.maxLenForActive[ordinal / 64], len - 1);
    scannerGen.stringLiterals.allImages[ordinal] = image;
  }

  /** Copied from {@link ScannerGen#isRegExp(int)}. */
  boolean isRegExp() {
    return (!toSkip && !toMore && !toToken)
        || toSkip
        || toMore;
    //|| canReachOnMore[lexStates[i]]; // TODO what does it mean?
  }

  public String toString() {
    return super.toString() + " - " + image;
  }
}

final class KindInfo {
  final long[] validKinds;
  final long[] finalKinds;
  int validKindCnt = 0;
  int finalKindCnt = 0;

  KindInfo(int maxKind) {
    validKinds = new long[maxKind / 64 + 1];
    finalKinds = new long[maxKind / 64 + 1];
  }

  public void insertValidKind(int kind) {
    validKinds[kind / 64] |= (1L << (kind % 64));
    validKindCnt++;
  }

  public void insertFinalKind(int kind) {
    finalKinds[kind / 64] |= (1L << (kind % 64));
    finalKindCnt++;
  }
}
