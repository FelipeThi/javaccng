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

import org.javacc.utils.io.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

/** The state of a Non-deterministic Finite Automaton. */
public final class NfaState {
  final ScannerGen scannerGen;
  long[] asciiMoves = new long[2];
  char[] charMoves;
  char[] rangeMoves;
  NfaState next;
  NfaState stateForCase;
  Vector epsilonMoves = new Vector();
  String epsilonMovesString;
  NfaState[] epsilonMoveArray;
  int id;
  int stateName = -1;
  int kind = Integer.MAX_VALUE;
  int lookingFor;
  int usefulEpsilonMoves = 0;
  int inNextOf;
  int lexState;
  int nonAsciiMethod = -1;
  int kindToPrint = Integer.MAX_VALUE;
  boolean dummy = false;
  boolean isComposite = false;
  int[] compositeStates;
  boolean isFinal = false;
  Vector loByteVec;
  int[] nonAsciiMoveIndices;
  int round = 0;
  int onlyChar = 0;
  char matchSingleChar;

  NfaState(ScannerGen scannerGen) {
    this.scannerGen = scannerGen;
    id = scannerGen.nfaStates.idCnt++;
    scannerGen.nfaStates.allStates.add(this);
    lexState = scannerGen.lexStateIndex;
    lookingFor = scannerGen.curKind;
  }

  NfaState copy() {
    NfaState retVal = new NfaState(scannerGen);

    retVal.isFinal = isFinal;
    retVal.kind = kind;
    retVal.lookingFor = lookingFor;
    retVal.lexState = lexState;
    retVal.inNextOf = inNextOf;

    retVal.mergeMoves(this);

    return retVal;
  }

  void addMove(NfaState newState) {
    if (!epsilonMoves.contains(newState)) {
      scannerGen.nfaStates.insertInOrder(epsilonMoves, newState);
    }
  }

  void addASCIIMove(char c) {
    asciiMoves[c / 64] |= (1L << (c % 64));
  }

  void addChar(char c) {
    onlyChar++;
    matchSingleChar = c;
    int i;
    char temp;
    char temp1;

    if ((int) c < 128) // ASCII char
    {
      addASCIIMove(c);
      return;
    }

    if (charMoves == null) { charMoves = new char[10]; }

    int len = charMoves.length;

    if (charMoves[len - 1] != 0) {
      charMoves = scannerGen.nfaStates.expandCharArr(charMoves, 10);
      len += 10;
    }

    for (i = 0; i < len; i++) { if (charMoves[i] == 0 || charMoves[i] > c) { break; } }

    if (!scannerGen.nfaStates.unicodeWarningGiven && c > 0xff &&
        !Options.getJavaUnicodeEscape() &&
        !Options.getUserCharStream()) {
      scannerGen.nfaStates.unicodeWarningGiven = true;
      JavaCCErrors.warning(scannerGen.curRE, "Non-ASCII characters used in regular expression.\n" +
          "Please make sure you use the correct Reader when you create the parser, " +
          "one that can handle your character set.");
    }

    temp = charMoves[i];
    charMoves[i] = c;

    for (i++; i < len; i++) {
      if (temp == 0) { break; }

      temp1 = charMoves[i];
      charMoves[i] = temp;
      temp = temp1;
    }
  }

  void addRange(char left, char right) {
    onlyChar = 2;
    int i;
    char tempLeft1, tempLeft2, tempRight1, tempRight2;

    if (left < 128) {
      if (right < 128) {
        for (; left <= right; left++) { addASCIIMove(left); }

        return;
      }

      for (; left < 128; left++) { addASCIIMove(left); }
    }

    if (!scannerGen.nfaStates.unicodeWarningGiven && (left > 0xff || right > 0xff) &&
        !Options.getJavaUnicodeEscape() &&
        !Options.getUserCharStream()) {
      scannerGen.nfaStates.unicodeWarningGiven = true;
      JavaCCErrors.warning(scannerGen.curRE, "Non-ASCII characters used in regular expression.\n" +
          "Please make sure you use the correct Reader when you create the parser, " +
          "one that can handle your character set.");
    }

    if (rangeMoves == null) { rangeMoves = new char[20]; }

    int len = rangeMoves.length;

    if (rangeMoves[len - 1] != 0) {
      rangeMoves = scannerGen.nfaStates.expandCharArr(rangeMoves, 20);
      len += 20;
    }

    for (i = 0; i < len; i += 2) {
      if (rangeMoves[i] == 0 ||
          (rangeMoves[i] > left) ||
          ((rangeMoves[i] == left) && (rangeMoves[i + 1] > right))) { break; }
    }

    tempLeft1 = rangeMoves[i];
    tempRight1 = rangeMoves[i + 1];
    rangeMoves[i] = left;
    rangeMoves[i + 1] = right;

    for (i += 2; i < len; i += 2) {
      if (tempLeft1 == 0) { break; }

      tempLeft2 = rangeMoves[i];
      tempRight2 = rangeMoves[i + 1];
      rangeMoves[i] = tempLeft1;
      rangeMoves[i + 1] = tempRight1;
      tempLeft1 = tempLeft2;
      tempRight1 = tempRight2;
    }
  }

  // From hereon down all the functions are used for code generation
  boolean closureDone = false;

  /**
   * This function computes the closure and also updates the kind so that
   * any time there is a move to this state, it can go on epsilon to a
   * new state in the epsilon moves that might have a lower kind of token
   * number for the same length.
   */
  void epsilonClosure() {
    int i = 0;

    if (closureDone || scannerGen.nfaStates.mark[id]) { return; }

    scannerGen.nfaStates.mark[id] = true;

    // Recursively do closure
    for (i = 0; i < epsilonMoves.size(); i++) { ((NfaState) epsilonMoves.get(i)).epsilonClosure(); }

    Enumeration e = epsilonMoves.elements();

    while (e.hasMoreElements()) {
      NfaState tmp = (NfaState) e.nextElement();

      for (i = 0; i < tmp.epsilonMoves.size(); i++) {
        NfaState tmp1 = (NfaState) tmp.epsilonMoves.get(i);
        if (tmp1.usefulState() && !epsilonMoves.contains(tmp1)) {
          scannerGen.nfaStates.insertInOrder(epsilonMoves, tmp1);
          scannerGen.nfaStates.done = false;
        }
      }

      if (kind > tmp.kind) { kind = tmp.kind; }
    }

    if (hasTransitions() && !epsilonMoves.contains(this)) { scannerGen.nfaStates.insertInOrder(epsilonMoves, this); }
  }

  boolean usefulState() {
    return isFinal || hasTransitions();
  }

  public boolean hasTransitions() {
    return (asciiMoves[0] != 0L || asciiMoves[1] != 0L ||
        (charMoves != null && charMoves[0] != 0) ||
        (rangeMoves != null && rangeMoves[0] != 0));
  }

  void mergeMoves(NfaState other) {
    // Warning : This function does not merge epsilon moves
    if (asciiMoves == other.asciiMoves) {
      JavaCCErrors.semanticError("Bug in JavaCC : Please send " +
          "a report along with the input that caused this. Thank you.");
      throw new Error();
    }

    asciiMoves[0] = asciiMoves[0] | other.asciiMoves[0];
    asciiMoves[1] = asciiMoves[1] | other.asciiMoves[1];

    if (other.charMoves != null) {
      if (charMoves == null) { charMoves = other.charMoves; }
      else {
        char[] tmpCharMoves = new char[charMoves.length +
            other.charMoves.length];
        System.arraycopy(charMoves, 0, tmpCharMoves, 0, charMoves.length);
        charMoves = tmpCharMoves;

        for (int i = 0; i < other.charMoves.length; i++) { addChar(other.charMoves[i]); }
      }
    }

    if (other.rangeMoves != null) {
      if (rangeMoves == null) {
        rangeMoves = other.rangeMoves;
      }
      else {
        char[] tmpRangeMoves = new char[rangeMoves.length +
            other.rangeMoves.length];
        System.arraycopy(rangeMoves, 0, tmpRangeMoves,
            0, rangeMoves.length);
        rangeMoves = tmpRangeMoves;
        for (int i = 0; i < other.rangeMoves.length; i += 2) {
          addRange(other.rangeMoves[i], other.rangeMoves[i + 1]);
        }
      }
    }

    if (other.kind < kind) { kind = other.kind; }

    if (other.kindToPrint < kindToPrint) { kindToPrint = other.kindToPrint; }

    isFinal |= other.isFinal;
  }

  NfaState createEquivState(List states) {
    NfaState newState = ((NfaState) states.get(0)).copy();

    newState.next = new NfaState(scannerGen);

    scannerGen.nfaStates.insertInOrder(newState.next.epsilonMoves,
        ((NfaState) states.get(0)).next);

    for (int i = 1; i < states.size(); i++) {
      NfaState tmp2 = ((NfaState) states.get(i));

      if (tmp2.kind < newState.kind) {
        newState.kind = tmp2.kind;
      }

      newState.isFinal |= tmp2.isFinal;

      scannerGen.nfaStates.insertInOrder(newState.next.epsilonMoves, tmp2.next);
    }

    return newState;
  }

  NfaState getEquivalentRunTimeState() {
    Outer:
    for (int i = scannerGen.nfaStates.allStates.size(); i-- > 0; ) {
      NfaState other = (NfaState) scannerGen.nfaStates.allStates.get(i);

      if (this != other && other.stateName != -1 &&
          kindToPrint == other.kindToPrint &&
          asciiMoves[0] == other.asciiMoves[0] &&
          asciiMoves[1] == other.asciiMoves[1] &&
          scannerGen.nfaStates.equalCharArr(charMoves, other.charMoves) &&
          scannerGen.nfaStates.equalCharArr(rangeMoves, other.rangeMoves)) {
        if (next == other.next) { return other; }
        else if (next != null && other.next != null) {
          if (next.epsilonMoves.size() == other.next.epsilonMoves.size()) {
            for (int j = 0; j < next.epsilonMoves.size(); j++) {
              if (next.epsilonMoves.get(j) !=
                  other.next.epsilonMoves.get(j)) { continue Outer; }
            }

            return other;
          }
        }
      }
    }

    return null;
  }

  // generates code (without outputting it) and returns the name used.
  void generateCode() {
    if (stateName != -1) { return; }

    if (next != null) {
      next.generateCode();
      if (next.kind != Integer.MAX_VALUE) {
        kindToPrint = next.kind;
      }
    }

    if (stateName == -1 && hasTransitions()) {
      NfaState tmp = getEquivalentRunTimeState();

      if (tmp != null) {
        stateName = tmp.stateName;
//????
        //tmp.inNextOf += inNextOf;
//????
        dummy = true;
        return;
      }

      stateName = scannerGen.nfaStates.generatedStates++;
      scannerGen.nfaStates.indexedAllStates.add(this);
      generateNextStatesCode();
    }
  }

  void optimizeEpsilonMoves(boolean optReqd) {
    int i;

    // First do epsilon closure
    scannerGen.nfaStates.done = false;
    while (!scannerGen.nfaStates.done) {
      if (scannerGen.nfaStates.mark == null
          || scannerGen.nfaStates.mark.length < scannerGen.nfaStates.allStates.size()) {
        scannerGen.nfaStates.mark = new boolean[scannerGen.nfaStates.allStates.size()];
      }

      for (i = scannerGen.nfaStates.allStates.size(); i-- > 0; ) { scannerGen.nfaStates.mark[i] = false; }

      scannerGen.nfaStates.done = true;
      epsilonClosure();
    }

    for (i = scannerGen.nfaStates.allStates.size(); i-- > 0; ) {
      ((NfaState) scannerGen.nfaStates.allStates.get(i)).closureDone =
          scannerGen.nfaStates.mark[((NfaState) scannerGen.nfaStates.allStates.get(i)).id];
    }

    // Warning : The following piece of code is just an optimization.
    // in case of trouble, just remove this piece.

    boolean sometingOptimized = true;

    NfaState newState = null;
    NfaState tmp1, tmp2;
    int j;
    List equivStates = null;

    while (sometingOptimized) {
      sometingOptimized = false;
      for (i = 0; optReqd && i < epsilonMoves.size(); i++) {
        if ((tmp1 = (NfaState) epsilonMoves.get(i)).hasTransitions()) {
          for (j = i + 1; j < epsilonMoves.size(); j++) {
            if ((tmp2 = (NfaState) epsilonMoves.get(j)).
                hasTransitions() &&
                (tmp1.asciiMoves[0] == tmp2.asciiMoves[0] &&
                    tmp1.asciiMoves[1] == tmp2.asciiMoves[1] &&
                    scannerGen.nfaStates.equalCharArr(tmp1.charMoves, tmp2.charMoves) &&
                    scannerGen.nfaStates.equalCharArr(tmp1.rangeMoves, tmp2.rangeMoves))) {
              if (equivStates == null) {
                equivStates = new ArrayList();
                equivStates.add(tmp1);
              }

              scannerGen.nfaStates.insertInOrder(equivStates, tmp2);
              epsilonMoves.removeElementAt(j--);
            }
          }
        }

        if (equivStates != null) {
          sometingOptimized = true;
          String tmp = "";
          for (int l = 0; l < equivStates.size(); l++) {
            tmp += String.valueOf(
                ((NfaState) equivStates.get(l)).id) + ", ";
          }

          if ((newState = (NfaState) scannerGen.nfaStates.equivStatesTable.get(tmp)) == null) {
            newState = createEquivState(equivStates);
            scannerGen.nfaStates.equivStatesTable.put(tmp, newState);
          }

          epsilonMoves.removeElementAt(i--);
          epsilonMoves.add(newState);
          equivStates = null;
          newState = null;
        }
      }

      for (i = 0; i < epsilonMoves.size(); i++) {
        //if ((tmp1 = (NfaState)epsilonMoves.elementAt(i)).next == null)
        //continue;
        tmp1 = (NfaState) epsilonMoves.get(i);

        for (j = i + 1; j < epsilonMoves.size(); j++) {
          tmp2 = (NfaState) epsilonMoves.get(j);

          if (tmp1.next == tmp2.next) {
            if (newState == null) {
              newState = tmp1.copy();
              newState.next = tmp1.next;
              sometingOptimized = true;
            }

            newState.mergeMoves(tmp2);
            epsilonMoves.removeElementAt(j--);
          }
        }

        if (newState != null) {
          epsilonMoves.removeElementAt(i--);
          epsilonMoves.add(newState);
          newState = null;
        }
      }
    }

    // End Warning

    // Generate an array of states for epsilon moves (not vector)
    if (epsilonMoves.size() > 0) {
      for (i = 0; i < epsilonMoves.size(); i++) { // Since we are doing a closure, just epsilon moves are unncessary
        if (((NfaState) epsilonMoves.get(i)).hasTransitions()) {
          usefulEpsilonMoves++;
        }
        else {
          epsilonMoves.removeElementAt(i--);
        }
      }
    }
  }

  void generateNextStatesCode() {
    if (next.usefulEpsilonMoves > 0) {
      next.GetEpsilonMovesString();
    }
  }

  String GetEpsilonMovesString() {
    int[] stateNames = new int[usefulEpsilonMoves];
    int cnt = 0;

    if (epsilonMovesString != null) {
      return epsilonMovesString;
    }

    if (usefulEpsilonMoves > 0) {
      NfaState tempState;
      epsilonMovesString = "{ ";
      for (Object epsilonMove : epsilonMoves) {
        if ((tempState = (NfaState) epsilonMove).hasTransitions()) {
          if (tempState.stateName == -1) {
            tempState.generateCode();
          }

          ((NfaState) scannerGen.nfaStates.indexedAllStates.get(tempState.stateName)).inNextOf++;
          stateNames[cnt] = tempState.stateName;
          epsilonMovesString += tempState.stateName + ", ";
          if (cnt++ > 0 && cnt % 16 == 0) { epsilonMovesString += "\n"; }
        }
      }

      epsilonMovesString += "};";
    }

    usefulEpsilonMoves = cnt;
    if (epsilonMovesString != null &&
        scannerGen.nfaStates.allNextStates.get(epsilonMovesString) == null) {
      int[] statesToPut = new int[usefulEpsilonMoves];

      System.arraycopy(stateNames, 0, statesToPut, 0, cnt);
      scannerGen.nfaStates.allNextStates.put(epsilonMovesString, statesToPut);
    }

    return epsilonMovesString;
  }

  boolean canMoveUsingChar(char c) {
    int i;

    if (onlyChar == 1) { return c == matchSingleChar; }

    if (c < 128) { return ((asciiMoves[c / 64] & (1L << c % 64)) != 0L); }

    // Just check directly if there is a move for this char
    if (charMoves != null && charMoves[0] != 0) {
      for (i = 0; i < charMoves.length; i++) {
        if (c == charMoves[i]) { return true; }
        else if (c < charMoves[i] || charMoves[i] == 0) { break; }
      }
    }

    // For ranges, iterate thru the table to see if the current char
    // is in some range
    if (rangeMoves != null && rangeMoves[0] != 0) {
      for (i = 0; i < rangeMoves.length; i += 2) {
        if (c >= rangeMoves[i] && c <= rangeMoves[i + 1]) { return true; }
        else if (c < rangeMoves[i] || rangeMoves[i] == 0) { break; }
      }
    }

    //return (nextForNegatedList != null);
    return false;
  }

  public int getFirstValidPos(String s, int i, int len) {
    if (onlyChar == 1) {
      char c = matchSingleChar;
      while (c != s.charAt(i) && ++i < len) { ; }
      return i;
    }

    do {
      if (canMoveUsingChar(s.charAt(i))) { return i; }
    }
    while (++i < len);

    return i;
  }

  public int moveFrom(char c, List newStates) {
    if (canMoveUsingChar(c)) {
      for (int i = next.epsilonMoves.size(); i-- > 0; ) {
        scannerGen.nfaStates.insertInOrder(newStates, (NfaState) next.epsilonMoves.get(i));
      }

      return kindToPrint;
    }

    return Integer.MAX_VALUE;
  }

  /* This function generates the bit vectors of low and hi bytes for common
bit vectors and returns those that are not common with anything (in
loBytes) and returns an array of indices that can be used to generate
the function names for char matching using the common bit vectors.
It also generates code to match a char with the common bit vectors.
(Need a better comment). */

  void generateNonAsciiMoves(IndentingPrintWriter out) {
    int i = 0, j = 0;
    char hiByte;
    int cnt = 0;
    long[][] loBytes = new long[256][4];

    if ((charMoves == null || charMoves[0] == 0) &&
        (rangeMoves == null || rangeMoves[0] == 0)) { return; }

    if (charMoves != null) {
      for (i = 0; i < charMoves.length; i++) {
        if (charMoves[i] == 0) { break; }

        hiByte = (char) (charMoves[i] >> 8);
        loBytes[hiByte][(charMoves[i] & 0xff) / 64] |=
            (1L << ((charMoves[i] & 0xff) % 64));
      }
    }

    if (rangeMoves != null) {
      for (i = 0; i < rangeMoves.length; i += 2) {
        if (rangeMoves[i] == 0) { break; }

        char c, r;

        r = (char) (rangeMoves[i + 1] & 0xff);
        hiByte = (char) (rangeMoves[i] >> 8);

        if (hiByte == (char) (rangeMoves[i + 1] >> 8)) {
          for (c = (char) (rangeMoves[i] & 0xff); c <= r; c++) { loBytes[hiByte][c / 64] |= (1L << (c % 64)); }

          continue;
        }

        for (c = (char) (rangeMoves[i] & 0xff); c <= 0xff; c++) { loBytes[hiByte][c / 64] |= (1L << (c % 64)); }

        while (++hiByte < (char) (rangeMoves[i + 1] >> 8)) {
          loBytes[hiByte][0] |= 0xffffffffffffffffL;
          loBytes[hiByte][1] |= 0xffffffffffffffffL;
          loBytes[hiByte][2] |= 0xffffffffffffffffL;
          loBytes[hiByte][3] |= 0xffffffffffffffffL;
        }

        for (c = 0; c <= r; c++) { loBytes[hiByte][c / 64] |= (1L << (c % 64)); }
      }
    }

    long[] common = null;
    boolean[] done = new boolean[256];

    for (i = 0; i <= 255; i++) {
      if (done[i] ||
          (done[i] =
              loBytes[i][0] == 0 &&
                  loBytes[i][1] == 0 &&
                  loBytes[i][2] == 0 &&
                  loBytes[i][3] == 0)) { continue; }

      for (j = i + 1; j < 256; j++) {
        if (done[j]) { continue; }

        if (loBytes[i][0] == loBytes[j][0] &&
            loBytes[i][1] == loBytes[j][1] &&
            loBytes[i][2] == loBytes[j][2] &&
            loBytes[i][3] == loBytes[j][3]) {
          done[j] = true;
          if (common == null) {
            done[i] = true;
            common = new long[4];
            common[i / 64] |= (1L << (i % 64));
          }

          common[j / 64] |= (1L << (j % 64));
        }
      }

      if (common != null) {
        Integer ind;
        String tmp;

        tmp = "{\n   0x" + Long.toHexString(common[0]) + "L, " +
            "0x" + Long.toHexString(common[1]) + "L, " +
            "0x" + Long.toHexString(common[2]) + "L, " +
            "0x" + Long.toHexString(common[3]) + "L\n};";
        if ((ind = (Integer) scannerGen.nfaStates.lohiByteTab.get(tmp)) == null) {
          scannerGen.nfaStates.allBitVectors.add(tmp);

          if (!scannerGen.nfaStates.allBitsSet(tmp)) {
            out.println("static final long[] jjbitVec" + scannerGen.nfaStates.lohiByteCnt + " = " + tmp);
          }
          scannerGen.nfaStates.lohiByteTab.put(tmp, ind = new Integer(scannerGen.nfaStates.lohiByteCnt++));
        }

        scannerGen.nfaStates.tmpIndices[cnt++] = ind.intValue();

        tmp = "{\n   0x" + Long.toHexString(loBytes[i][0]) + "L, " +
            "0x" + Long.toHexString(loBytes[i][1]) + "L, " +
            "0x" + Long.toHexString(loBytes[i][2]) + "L, " +
            "0x" + Long.toHexString(loBytes[i][3]) + "L\n};";
        if ((ind = (Integer) scannerGen.nfaStates.lohiByteTab.get(tmp)) == null) {
          scannerGen.nfaStates.allBitVectors.add(tmp);

          if (!scannerGen.nfaStates.allBitsSet(tmp)) {
            out.println("static final long[] jjbitVec" + scannerGen.nfaStates.lohiByteCnt + " = " + tmp);
          }
          scannerGen.nfaStates.lohiByteTab.put(tmp, ind = new Integer(scannerGen.nfaStates.lohiByteCnt++));
        }

        scannerGen.nfaStates.tmpIndices[cnt++] = ind.intValue();

        common = null;
      }
    }

    nonAsciiMoveIndices = new int[cnt];
    System.arraycopy(scannerGen.nfaStates.tmpIndices, 0, nonAsciiMoveIndices, 0, cnt);

/*
      System.out.println("state : " + stateName + " cnt : " + cnt);
      while (cnt > 0)
      {
         System.out.print(nonAsciiMoveIndices[cnt - 1] + ", " + nonAsciiMoveIndices[cnt - 2] + ", ");
         cnt -= 2;
      }
      System.out.println("");
*/

    for (i = 0; i < 256; i++) {
      if (done[i]) { loBytes[i] = null; }
      else {
        //System.out.print(i + ", ");
        String tmp;
        Integer ind;

        tmp = "{\n   0x" + Long.toHexString(loBytes[i][0]) + "L, " +
            "0x" + Long.toHexString(loBytes[i][1]) + "L, " +
            "0x" + Long.toHexString(loBytes[i][2]) + "L, " +
            "0x" + Long.toHexString(loBytes[i][3]) + "L\n};";

        if ((ind = (Integer) scannerGen.nfaStates.lohiByteTab.get(tmp)) == null) {
          scannerGen.nfaStates.allBitVectors.add(tmp);

          if (!scannerGen.nfaStates.allBitsSet(tmp)) {
            out.println("static final long[] jjbitVec" + scannerGen.nfaStates.lohiByteCnt + " = " + tmp);
          }
          scannerGen.nfaStates.lohiByteTab.put(tmp, ind = new Integer(scannerGen.nfaStates.lohiByteCnt++));
        }

        if (loByteVec == null) { loByteVec = new Vector(); }

        loByteVec.add(new Integer(i));
        loByteVec.add(ind);
      }
    }
    //System.out.println("");
    updateDuplicateNonAsciiMoves();
  }

  void updateDuplicateNonAsciiMoves() {
    for (int i = 0; i < scannerGen.nfaStates.nonAsciiTableForMethod.size(); i++) {
      NfaState tmp = (NfaState) scannerGen.nfaStates.nonAsciiTableForMethod.get(i);
      if (scannerGen.nfaStates.equalLoByteVectors(loByteVec, tmp.loByteVec) &&
          scannerGen.nfaStates.equalNonAsciiMoveIndices(nonAsciiMoveIndices, tmp.nonAsciiMoveIndices)) {
        nonAsciiMethod = i;
        return;
      }
    }

    nonAsciiMethod = scannerGen.nfaStates.nonAsciiTableForMethod.size();
    scannerGen.nfaStates.nonAsciiTableForMethod.add(this);
  }

  public void generateInitMoves(IndentingPrintWriter out) {
    GetEpsilonMovesString();

    if (epsilonMovesString == null) { epsilonMovesString = "null;"; }

    scannerGen.nfaStates.addStartStateSet(epsilonMovesString);
  }

  boolean findCommonBlocks() {
    if (next == null || next.usefulEpsilonMoves <= 1) { return false; }

    if (scannerGen.nfaStates.stateDone == null) {
      scannerGen.nfaStates.stateDone = new boolean[scannerGen.nfaStates.generatedStates];
    }

    String set = next.epsilonMovesString;

    int[] nameSet = (int[]) scannerGen.nfaStates.allNextStates.get(set);

    if (nameSet.length <= 2 || scannerGen.nfaStates.compositeStateTable.get(set) != null) { return false; }

    int i;
    int[] freq = new int[nameSet.length];
    boolean[] live = new boolean[nameSet.length];
    int[] count = new int[scannerGen.nfaStates.allNextStates.size()];

    for (i = 0; i < nameSet.length; i++) {
      if (nameSet[i] != -1) {
        if (live[i] = !scannerGen.nfaStates.stateDone[nameSet[i]]) { count[0]++; }
      }
    }

    int j, blockLen = 0, commonFreq = 0;
    Enumeration e = scannerGen.nfaStates.allNextStates.keys();
    boolean needUpdate;

    while (e.hasMoreElements()) {
      int[] tmpSet = (int[]) scannerGen.nfaStates.allNextStates.get(e.nextElement());
      if (tmpSet == nameSet) { continue; }

      needUpdate = false;
      for (j = 0; j < nameSet.length; j++) {
        if (nameSet[j] == -1) { continue; }

        if (live[j] && scannerGen.nfaStates.elemOccurs(nameSet[j], tmpSet) >= 0) {
          if (!needUpdate) {
            needUpdate = true;
            commonFreq++;
          }

          count[freq[j]]--;
          count[commonFreq]++;
          freq[j] = commonFreq;
        }
      }

      if (needUpdate) {
        int foundFreq = -1;
        blockLen = 0;

        for (j = 0; j <= commonFreq; j++) {
          if (count[j] > blockLen) {
            foundFreq = j;
            blockLen = count[j];
          }
        }

        if (blockLen <= 1) { return false; }

        for (j = 0; j < nameSet.length; j++) {
          if (nameSet[j] != -1 && freq[j] != foundFreq) {
            live[j] = false;
            count[freq[j]]--;
          }
        }
      }
    }

    if (blockLen <= 1) { return false; }

    int[] commonBlock = new int[blockLen];
    int cnt = 0;
    //System.out.println("Common Block for " + set + " :");
    for (i = 0; i < nameSet.length; i++) {
      if (live[i]) {
        if (((NfaState) scannerGen.nfaStates.indexedAllStates.get(nameSet[i])).isComposite) { return false; }

        scannerGen.nfaStates.stateDone[nameSet[i]] = true;
        commonBlock[cnt++] = nameSet[i];
        //System.out.print(nameSet[i] + ", ");
      }
    }

    //System.out.println("");

    String s = scannerGen.nfaStates.getStateSetString(commonBlock);
    e = scannerGen.nfaStates.allNextStates.keys();

    Outer:
    while (e.hasMoreElements()) {
      int at;
      boolean firstOne = true;
      String stringToFix;
      int[] setToFix = (int[]) scannerGen.nfaStates.allNextStates.get(stringToFix = (String) e.nextElement());

      if (setToFix == commonBlock) { continue; }

      for (int k = 0; k < cnt; k++) {
        if ((at = scannerGen.nfaStates.elemOccurs(commonBlock[k], setToFix)) >= 0) {
          if (!firstOne) { setToFix[at] = -1; }
          firstOne = false;
        }
        else { continue Outer; }
      }

      if (scannerGen.nfaStates.stateSetsToFix.get(stringToFix) == null) {
        scannerGen.nfaStates.stateSetsToFix.put(stringToFix, setToFix);
      }
    }

    next.usefulEpsilonMoves -= blockLen - 1;
    scannerGen.nfaStates.addCompositeStateSet(s, false);
    return true;
  }

  boolean checkNextOccursTogether() {
    if (next == null || next.usefulEpsilonMoves <= 1) { return true; }

    String set = next.epsilonMovesString;

    int[] nameSet = (int[]) scannerGen.nfaStates.allNextStates.get(set);

    if (nameSet.length == 1 || scannerGen.nfaStates.compositeStateTable.get(set) != null ||
        scannerGen.nfaStates.stateSetsToFix.get(set) != null) { return false; }

    int i;
    Hashtable occursIn = new Hashtable();
    NfaState tmp = (NfaState) scannerGen.nfaStates.allStates.get(nameSet[0]);

    for (i = 1; i < nameSet.length; i++) {
      NfaState tmp1 = (NfaState) scannerGen.nfaStates.allStates.get(nameSet[i]);

      if (tmp.inNextOf != tmp1.inNextOf) { return false; }
    }

    int isPresent, j;
    Enumeration e = scannerGen.nfaStates.allNextStates.keys();
    while (e.hasMoreElements()) {
      String s;
      int[] tmpSet = (int[]) scannerGen.nfaStates.allNextStates.get(s = (String) e.nextElement());

      if (tmpSet == nameSet) { continue; }

      isPresent = 0;
      for (j = 0; j < nameSet.length; j++) {
        if (scannerGen.nfaStates.elemOccurs(nameSet[j], tmpSet) >= 0) { isPresent++; }
        else if (isPresent > 0) { return false; }
      }

      if (isPresent == j) {
        if (tmpSet.length > nameSet.length) { occursIn.put(s, tmpSet); }

        //May not need. But safe.
        if (scannerGen.nfaStates.compositeStateTable.get(s) != null ||
            scannerGen.nfaStates.stateSetsToFix.get(s) != null) { return false; }
      }
      else if (isPresent != 0) { return false; }
    }

    e = occursIn.keys();
    while (e.hasMoreElements()) {
      String s;
      int[] setToFix = (int[]) occursIn.get(s = (String) e.nextElement());

      if (scannerGen.nfaStates.stateSetsToFix.get(s) == null) { scannerGen.nfaStates.stateSetsToFix.put(s, setToFix); }

      for (int k = 0; k < setToFix.length; k++) {
        if (scannerGen.nfaStates.elemOccurs(setToFix[k], nameSet) > 0)  // Not >= since need the first one (0)
        { setToFix[k] = -1; }
      }
    }

    next.usefulEpsilonMoves = 1;
    scannerGen.nfaStates.addCompositeStateSet(next.epsilonMovesString, false);
    return true;
  }

  void fixNextStates(int[] newSet) {
    next.usefulEpsilonMoves = newSet.length;
    //next.epsilonMovesString = getStateSetString(newSet);
  }

  String printNoBreak(IndentingPrintWriter out, int byteNum, boolean[] dumped) {
    if (inNextOf != 1) { throw new Error("JavaCC Bug: Please send mail to sankar@cs.stanford.edu"); }

    dumped[stateName] = true;

    if (byteNum >= 0) {
      if (asciiMoves[byteNum] != 0L) {
        out.println("               case " + stateName + ":");
        dumpAsciiMoveForCompositeState(out, byteNum, false);
        return "";
      }
    }
    else if (nonAsciiMethod != -1) {
      out.println("               case " + stateName + ":");
      dumpNonAsciiMoveForCompositeState(out);
      return "";
    }

    return ("               case " + stateName + ":\n");
  }

  boolean selfLoop() {
    if (next == null || next.epsilonMovesString == null) { return false; }

    int[] set = (int[]) scannerGen.nfaStates.allNextStates.get(next.epsilonMovesString);
    return scannerGen.nfaStates.elemOccurs(stateName, set) >= 0;
  }

  void dumpAsciiMoveForCompositeState(IndentingPrintWriter out, int byteNum, boolean elseNeeded) {
    boolean nextIntersects = selfLoop();

    for (int j = 0; j < scannerGen.nfaStates.allStates.size(); j++) {
      NfaState temp1 = (NfaState) scannerGen.nfaStates.allStates.get(j);

      if (this == temp1 || temp1.stateName == -1 || temp1.dummy ||
          stateName == temp1.stateName || temp1.asciiMoves[byteNum] == 0L) { continue; }

      if (!nextIntersects && scannerGen.nfaStates.entersect(temp1.next.epsilonMovesString,
          next.epsilonMovesString)) {
        nextIntersects = true;
        break;
      }
    }

    //System.out.println(stateName + " \'s nextIntersects : " + nextIntersects);
    String prefix = "";
    if (asciiMoves[byteNum] != 0xffffffffffffffffL) {
      int oneBit = scannerGen.nfaStates.onlyOneBitSet(asciiMoves[byteNum]);

      if (oneBit != -1) {
        out.println("                  " + (elseNeeded ? "else " : "") + "if (jjChar == " +
            (64 * byteNum + oneBit) + ")");
      }
      else {
        out.println("                  " + (elseNeeded ? "else " : "") +
            "if ((0x" + Long.toHexString(asciiMoves[byteNum]) + "L & l) != 0L)");
      }
      prefix = "   ";
    }

    if (kindToPrint != Integer.MAX_VALUE) {
      if (asciiMoves[byteNum] != 0xffffffffffffffffL) {
        out.println("                  {");
      }

      out.println("                  if (kind > " + kindToPrint + ")");
      out.println("                     kind = " + kindToPrint + ";");
    }

    if (next != null && next.usefulEpsilonMoves > 0) {
      int[] stateNames = (int[]) scannerGen.nfaStates.allNextStates.get(
          next.epsilonMovesString);
      if (next.usefulEpsilonMoves == 1) {
        int name = stateNames[0];

        if (nextIntersects) {
          out.println("                  jjCheckNAdd(" + name + ");");
        }
        else {
          out.println("                  jjStateSet[jjNewStateCount++] = " + name + ";");
        }
      }
      else if (next.usefulEpsilonMoves == 2 && nextIntersects) {
        out.println("                  jjCheckNAddTwoStates(" +
            stateNames[0] + ", " + stateNames[1] + ");");
      }
      else {
        int[] indices = scannerGen.nfaStates.getStateSetIndicesForUse(next.epsilonMovesString);
        boolean notTwo = (indices[0] + 1 != indices[1]);

        if (nextIntersects) {
          out.print("                  jjCheckNAddStates(" + indices[0]);
          if (notTwo) {
            scannerGen.nfaStates.jjCheckNAddStatesDualNeeded = true;
            out.print(", " + indices[1]);
          }
          else {
            scannerGen.nfaStates.jjCheckNAddStatesUnaryNeeded = true;
          }
          out.println(");");
        }
        else {
          out.println("                  jjAddStates(" +
              indices[0] + ", " + indices[1] + ");");
        }
      }
    }

    if (asciiMoves[byteNum] != 0xffffffffffffffffL && kindToPrint != Integer.MAX_VALUE) {
      out.println("                  }");
    }
  }

  void dumpAsciiMove(IndentingPrintWriter out, int byteNum, boolean[] dumped) {
    boolean nextIntersects = selfLoop() && isComposite;
    boolean onlyState = true;

    for (int j = 0; j < scannerGen.nfaStates.allStates.size(); j++) {
      NfaState temp1 = (NfaState) scannerGen.nfaStates.allStates.get(j);

      if (this == temp1 || temp1.stateName == -1 || temp1.dummy ||
          stateName == temp1.stateName || temp1.asciiMoves[byteNum] == 0L) { continue; }

      if (onlyState && (asciiMoves[byteNum] & temp1.asciiMoves[byteNum]) != 0L) { onlyState = false; }

      if (!nextIntersects && scannerGen.nfaStates.entersect(temp1.next.epsilonMovesString,
          next.epsilonMovesString)) { nextIntersects = true; }

      if (!dumped[temp1.stateName] && !temp1.isComposite &&
          asciiMoves[byteNum] == temp1.asciiMoves[byteNum] &&
          kindToPrint == temp1.kindToPrint &&
          (next.epsilonMovesString == temp1.next.epsilonMovesString ||
              (next.epsilonMovesString != null &&
                  temp1.next.epsilonMovesString != null &&
                  next.epsilonMovesString.equals(
                      temp1.next.epsilonMovesString)))) {
        dumped[temp1.stateName] = true;
        out.println("               case " + temp1.stateName + ":");
      }
    }

    //if (onlyState)
    //nextIntersects = false;

    int oneBit = scannerGen.nfaStates.onlyOneBitSet(asciiMoves[byteNum]);
    if (asciiMoves[byteNum] != 0xffffffffffffffffL) {
      if ((next == null || next.usefulEpsilonMoves == 0) &&
          kindToPrint != Integer.MAX_VALUE) {
        String kindCheck = "";

        if (!onlyState) { kindCheck = " && kind > " + kindToPrint; }

        if (oneBit != -1) {
          out.println("                  if (jjChar == " +
              (64 * byteNum + oneBit) + kindCheck + ")");
        }
        else {
          out.println("                  if ((0x" +
              Long.toHexString(asciiMoves[byteNum]) +
              "L & l) != 0L" + kindCheck + ")");
        }

        out.println("                     kind = " + kindToPrint + ";");

        if (onlyState) { out.println("                  break;"); }
        else { out.println("                  break;"); }

        return;
      }
    }

    String prefix = "";
    if (kindToPrint != Integer.MAX_VALUE) {

      if (oneBit != -1) {
        out.println("                  if (jjChar != " +
            (64 * byteNum + oneBit) + ")");
        out.println("                     break;");
      }
      else if (asciiMoves[byteNum] != 0xffffffffffffffffL) {
        out.println("                  if ((0x" + Long.toHexString(asciiMoves[byteNum]) + "L & l) == 0L)");
        out.println("                     break;");
      }

      if (onlyState) {
        out.println("                  kind = " + kindToPrint + ";");
      }
      else {
        out.println("                  if (kind > " + kindToPrint + ")");
        out.println("                     kind = " + kindToPrint + ";");
      }
    }
    else {
      if (oneBit != -1) {
        out.println("                  if (jjChar == " +
            (64 * byteNum + oneBit) + ")");
        prefix = "   ";
      }
      else if (asciiMoves[byteNum] != 0xffffffffffffffffL) {
        out.println("                  if ((0x" + Long.toHexString(asciiMoves[byteNum]) + "L & l) != 0L)");
        prefix = "   ";
      }
    }

    if (next != null && next.usefulEpsilonMoves > 0) {
      int[] stateNames = (int[]) scannerGen.nfaStates.allNextStates.get(
          next.epsilonMovesString);
      if (next.usefulEpsilonMoves == 1) {
        int name = stateNames[0];
        if (nextIntersects) { out.println("                  jjCheckNAdd(" + name + ");"); }
        else { out.println("                  jjStateSet[jjNewStateCount++] = " + name + ";"); }
      }
      else if (next.usefulEpsilonMoves == 2 && nextIntersects) {
        out.println("                  jjCheckNAddTwoStates(" +
            stateNames[0] + ", " + stateNames[1] + ");");
      }
      else {
        int[] indices = scannerGen.nfaStates.getStateSetIndicesForUse(next.epsilonMovesString);
        boolean notTwo = (indices[0] + 1 != indices[1]);

        if (nextIntersects) {
          out.print("                  jjCheckNAddStates(" + indices[0]);
          if (notTwo) {
            scannerGen.nfaStates.jjCheckNAddStatesDualNeeded = true;
            out.print(", " + indices[1]);
          }
          else {
            scannerGen.nfaStates.jjCheckNAddStatesUnaryNeeded = true;
          }
          out.println(");");
        }
        else {
          out.println("                  jjAddStates(" +
              indices[0] + ", " + indices[1] + ");");
        }
      }
    }

    if (onlyState) { out.println("                  break;"); }
    else { out.println("                  break;"); }
  }

  void dumpNonAsciiMoveForCompositeState(IndentingPrintWriter out) {
    boolean nextIntersects = selfLoop();
    for (int j = 0; j < scannerGen.nfaStates.allStates.size(); j++) {
      NfaState temp1 = (NfaState) scannerGen.nfaStates.allStates.get(j);

      if (this == temp1 || temp1.stateName == -1 || temp1.dummy ||
          stateName == temp1.stateName || (temp1.nonAsciiMethod == -1)) { continue; }

      if (!nextIntersects && scannerGen.nfaStates.entersect(temp1.next.epsilonMovesString,
          next.epsilonMovesString)) {
        nextIntersects = true;
        break;
      }
    }

    if (!Options.getJavaUnicodeEscape() && !scannerGen.nfaStates.unicodeWarningGiven) {
      if (loByteVec != null && loByteVec.size() > 1) {
        out.println("                  if ((jjbitVec" +
            ((Integer) loByteVec.get(1)).intValue() + "[i2" +
            "] & l2) != 0L)");
      }
    }
    else {
      out.println("                  if (jjCanMove_" + nonAsciiMethod +
          "(hiByte, i1, i2, l1, l2))");
    }

    if (kindToPrint != Integer.MAX_VALUE) {
      out.println("                  {");
      out.println("                     if (kind > " + kindToPrint + ")");
      out.println("                        kind = " + kindToPrint + ";");
    }

    if (next != null && next.usefulEpsilonMoves > 0) {
      int[] stateNames = (int[]) scannerGen.nfaStates.allNextStates.get(
          next.epsilonMovesString);
      if (next.usefulEpsilonMoves == 1) {
        int name = stateNames[0];
        if (nextIntersects) { out.println("                     jjCheckNAdd(" + name + ");"); }
        else { out.println("                     jjStateSet[jjNewStateCount++] = " + name + ";"); }
      }
      else if (next.usefulEpsilonMoves == 2 && nextIntersects) {
        out.println("                     jjCheckNAddTwoStates(" +
            stateNames[0] + ", " + stateNames[1] + ");");
      }
      else {
        int[] indices = scannerGen.nfaStates.getStateSetIndicesForUse(next.epsilonMovesString);
        boolean notTwo = (indices[0] + 1 != indices[1]);

        if (nextIntersects) {
          out.print("                     jjCheckNAddStates(" + indices[0]);
          if (notTwo) {
            scannerGen.nfaStates.jjCheckNAddStatesDualNeeded = true;
            out.print(", " + indices[1]);
          }
          else {
            scannerGen.nfaStates.jjCheckNAddStatesUnaryNeeded = true;
          }
          out.println(");");
        }
        else { out.println("                     jjAddStates(" + indices[0] + ", " + indices[1] + ");"); }
      }
    }

    if (kindToPrint != Integer.MAX_VALUE) { out.println("                  }"); }
  }

  void dumpNonAsciiMove(IndentingPrintWriter out, boolean[] dumped) {
    boolean nextIntersects = selfLoop() && isComposite;

    for (int j = 0; j < scannerGen.nfaStates.allStates.size(); j++) {
      NfaState temp1 = (NfaState) scannerGen.nfaStates.allStates.get(j);

      if (this == temp1 || temp1.stateName == -1 || temp1.dummy ||
          stateName == temp1.stateName || (temp1.nonAsciiMethod == -1)) { continue; }

      if (!nextIntersects && scannerGen.nfaStates.entersect(temp1.next.epsilonMovesString,
          next.epsilonMovesString)) { nextIntersects = true; }

      if (!dumped[temp1.stateName] && !temp1.isComposite &&
          nonAsciiMethod == temp1.nonAsciiMethod &&
          kindToPrint == temp1.kindToPrint &&
          (next.epsilonMovesString == temp1.next.epsilonMovesString ||
              (next.epsilonMovesString != null &&
                  temp1.next.epsilonMovesString != null &&
                  next.epsilonMovesString.equals(temp1.next.epsilonMovesString)))) {
        dumped[temp1.stateName] = true;
        out.println("               case " + temp1.stateName + ":");
      }
    }

    if (next == null || next.usefulEpsilonMoves <= 0) {
      String kindCheck = " && kind > " + kindToPrint;

      if (!Options.getJavaUnicodeEscape() && !scannerGen.nfaStates.unicodeWarningGiven) {
        if (loByteVec != null && loByteVec.size() > 1) {
          out.println("                  if ((jjbitVec" +
              ((Integer) loByteVec.get(1)).intValue() + "[i2" +
              "] & l2) != 0L" + kindCheck + ")");
        }
      }
      else {
        out.println("                  if (jjCanMove_" + nonAsciiMethod +
            "(hiByte, i1, i2, l1, l2)" + kindCheck + ")");
      }
      out.println("                     kind = " + kindToPrint + ";");
      out.println("                  break;");
      return;
    }

    String prefix = "   ";
    if (kindToPrint != Integer.MAX_VALUE) {
      if (!Options.getJavaUnicodeEscape() && !scannerGen.nfaStates.unicodeWarningGiven) {
        if (loByteVec != null && loByteVec.size() > 1) {
          out.println("                  if ((jjbitVec" +
              ((Integer) loByteVec.get(1)).intValue() + "[i2" +
              "] & l2) == 0L)");
          out.println("                     break;");
        }
      }
      else {
        out.println("                  if (!jjCanMove_" + nonAsciiMethod +
            "(hiByte, i1, i2, l1, l2))");
        out.println("                     break;");
      }

      out.println("                  if (kind > " + kindToPrint + ")");
      out.println("                     kind = " + kindToPrint + ";");
      prefix = "";
    }
    else if (!Options.getJavaUnicodeEscape() && !scannerGen.nfaStates.unicodeWarningGiven) {
      if (loByteVec != null && loByteVec.size() > 1) {
        out.println("                  if ((jjbitVec" +
            ((Integer) loByteVec.get(1)).intValue() + "[i2" +
            "] & l2) != 0L)");
      }
    }
    else {
      out.println("                  if (jjCanMove_" + nonAsciiMethod +
          "(hiByte, i1, i2, l1, l2))");
    }

    if (next != null && next.usefulEpsilonMoves > 0) {
      int[] stateNames = (int[]) scannerGen.nfaStates.allNextStates.get(
          next.epsilonMovesString);
      if (next.usefulEpsilonMoves == 1) {
        int name = stateNames[0];
        if (nextIntersects) { out.println("                  jjCheckNAdd(" + name + ");"); }
        else { out.println("                  jjStateSet[jjNewStateCount++] = " + name + ";"); }
      }
      else if (next.usefulEpsilonMoves == 2 && nextIntersects) {
        out.println("                  jjCheckNAddTwoStates(" +
            stateNames[0] + ", " + stateNames[1] + ");");
      }
      else {
        int[] indices = scannerGen.nfaStates.getStateSetIndicesForUse(next.epsilonMovesString);
        boolean notTwo = (indices[0] + 1 != indices[1]);

        if (nextIntersects) {
          out.print("                  jjCheckNAddStates(" + indices[0]);
          if (notTwo) {
            scannerGen.nfaStates.jjCheckNAddStatesDualNeeded = true;
            out.print(", " + indices[1]);
          }
          else {
            scannerGen.nfaStates.jjCheckNAddStatesUnaryNeeded = true;
          }
          out.println(");");
        }
        else { out.println("                  jjAddStates(" + indices[0] + ", " + indices[1] + ");"); }
      }
    }

    out.println("                  break;");
  }

  void dumpNonAsciiMoveMethod(IndentingPrintWriter out) {
    int j;
    out.println("private static boolean jjCanMove_" + nonAsciiMethod +
        "(int hiByte, int i1, int i2, long l1, long l2)");
    out.println("{");
    out.println("   switch(hiByte)");
    out.println("   {");

    if (loByteVec != null && loByteVec.size() > 0) {
      for (j = 0; j < loByteVec.size(); j += 2) {
        out.println("      case " +
            ((Integer) loByteVec.get(j)).intValue() + ":");
        if (!scannerGen.nfaStates.allBitsSet((String) scannerGen.nfaStates.allBitVectors.get(
            ((Integer) loByteVec.get(j + 1)).intValue()))) {
          out.println("         return ((jjbitVec" +
              ((Integer) loByteVec.get(j + 1)).intValue() + "[i2" +
              "] & l2) != 0L);");
        }
        else { out.println("            return true;"); }
      }
    }

    out.println("      default:");

    if (nonAsciiMoveIndices != null &&
        (j = nonAsciiMoveIndices.length) > 0) {
      do {
        if (!scannerGen.nfaStates.allBitsSet((String) scannerGen.nfaStates.allBitVectors.get(
            nonAsciiMoveIndices[j - 2]))) {
          out.println("         if ((jjbitVec" + nonAsciiMoveIndices[j - 2] +
              "[i1] & l1) != 0L)");
        }
        if (!scannerGen.nfaStates.allBitsSet((String) scannerGen.nfaStates.allBitVectors.get(
            nonAsciiMoveIndices[j - 1]))) {
          out.println("            if ((jjbitVec" + nonAsciiMoveIndices[j - 1] +
              "[i2] & l2) == 0L)");
          out.println("               return false;");
          out.println("            else");
        }
        out.println("            return true;");
      }
      while ((j -= 2) > 0);
    }

    out.println("         return false;");
    out.println("   }");
    out.println("}");
  }
}
