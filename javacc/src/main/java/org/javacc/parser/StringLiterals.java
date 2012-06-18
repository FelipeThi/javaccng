package org.javacc.parser;

import org.javacc.utils.Parsers;
import org.javacc.utils.io.IndentingPrintWriter;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

final class StringLiterals {
  int maxStrKind;
  int maxLen;
  List charPosKind; // Elements are hashtables with single char keys;
  int[] maxLenForActive;
  String[] allImages;
  int[][] intermediateKinds;
  int[][] intermediateMatchedPos;
  int startStateCnt;
  boolean[] subString;
  boolean[] subStringAtPos;
  Hashtable[] statesForPos;
  boolean boilerPlateDumped;

  StringLiterals() {
    reInit();
  }

  /**
   * Initialize all the static variables, so that there is no interference
   * between the various states of the lexer.
   *
   * Need to call this method after generating code for each lexical state.
   */
  void reInit() {
    maxStrKind = 0;
    maxLen = 0;
    charPosKind = new ArrayList();
    maxLenForActive = new int[100]; // 6400 tokens
    intermediateKinds = null;
    intermediateMatchedPos = null;
    startStateCnt = 0;
    subString = null;
    subStringAtPos = null;
    statesForPos = null;
  }

  void dumpStrLiteralImages(ScannerGen scannerGen, IndentingPrintWriter ostr) {
    ostr.println("");
    ostr.println("/** Token literal values. */");
    ostr.println("public static final String[] jjLiteralImages = {");

    if (allImages == null || allImages.length == 0) {
      ostr.println("};");
      return;
    }

    allImages[0] = "";
    int charCnt = 0;
    String image;
    int i;
    for (i = 0; i < allImages.length; i++) {
      if ((image = allImages[i]) == null
          || scannerGen.isRegExp(i)
          || unnamed(scannerGen, image, i)) {
        allImages[i] = null;
        if ((charCnt += 6) > 80) {
          ostr.println();
          charCnt = 0;
        }

        ostr.print("null, ");
        continue;
      }

      String toPrint = "\"" + Parsers.escape(image) + "\", ";

      if ((charCnt += toPrint.length()) >= 80) {
        ostr.println("");
        charCnt = 0;
      }

      ostr.print(toPrint);
    }

    while (++i < scannerGen.maxOrdinal) {
      if ((charCnt += 6) > 80) {
        ostr.println("");
        charCnt = 0;
      }

      ostr.print("null, ");
    }

    ostr.println("};");
  }

  private boolean unnamed(ScannerGen scannerGen, String image, int i) {
    return (Options.getIgnoreCase() || scannerGen.ignoreCase[i])
        && (!image.equals(image.toLowerCase()) || !image.equals(image.toUpperCase()));
  }

  void dumpNullStrLiterals(ScannerGen scannerGen, IndentingPrintWriter ostr) {
    ostr.println("{");
    ostr.indent();
    if (scannerGen.nfaStates.generatedStates != 0) {
      ostr.println("return jjMoveNfa" + scannerGen.lexStateSuffix + "(" + scannerGen.nfaStates.initStateName(scannerGen) + ", 0);");
    }
    else { ostr.println("return 1;"); }

    ostr.unindent();
    ostr.println("}");
  }

  private int getStateSetForKind(ScannerGen scannerGen, int pos, int kind) {
    if (scannerGen.mixed[scannerGen.lexStateIndex] || scannerGen.nfaStates.generatedStates == 0) { return -1; }

    Hashtable allStateSets = statesForPos[pos];

    if (allStateSets == null) { return -1; }

    Enumeration e = allStateSets.keys();

    while (e.hasMoreElements()) {
      String s = (String) e.nextElement();
      long[] actives = (long[]) allStateSets.get(s);

      s = s.substring(s.indexOf(", ") + 2);
      s = s.substring(s.indexOf(", ") + 2);

      if (s.equals("null;")) { continue; }

      if (actives != null &&
          (actives[kind / 64] & (1L << (kind % 64))) != 0L) {
        return scannerGen.nfaStates.addStartStateSet(s);
      }
    }

    return -1;
  }

  String getLabel(ScannerGen scannerGen, int kind) {
    RegularExpression re = scannerGen.rexprs[kind];

    if (re instanceof RStringLiteral) {
      return " \"" + Parsers.escape(((RStringLiteral) re).image) + "\"";
    }
    else if (!re.label.equals("")) {
      return " <" + re.label + ">";
    }
    else {
      return " <token of kind " + kind + ">";
    }
  }

  int getLine(ScannerGen scannerGen, int kind) {
    return scannerGen.rexprs[kind].getLine();
  }

  int getColumn(ScannerGen scannerGen, int kind) {
    return scannerGen.rexprs[kind].getColumn();
  }

  /** Returns true if s1 starts with s2 (ignoring case for each character). */
  private boolean startsWithIgnoreCase(String s1, String s2) {
    if (s1.length() < s2.length()) { return false; }

    for (int i = 0; i < s2.length(); i++) {
      char c1 = s1.charAt(i), c2 = s2.charAt(i);

      if (c1 != c2 && Character.toLowerCase(c2) != c1 &&
          Character.toUpperCase(c2) != c1) { return false; }
    }

    return true;
  }

  void FillSubString(ScannerGen scannerGen) {
    String image;
    subString = new boolean[maxStrKind + 1];
    subStringAtPos = new boolean[maxLen];

    for (int i = 0; i < maxStrKind; i++) {
      subString[i] = false;

      if ((image = allImages[i]) == null ||
          scannerGen.lexStates[i] != scannerGen.lexStateIndex) { continue; }

      if (scannerGen.mixed[scannerGen.lexStateIndex]) {
        // We will not optimize for mixed case
        subString[i] = true;
        subStringAtPos[image.length() - 1] = true;
        continue;
      }

      for (int j = 0; j < maxStrKind; j++) {
        if (j != i && scannerGen.lexStates[j] == scannerGen.lexStateIndex &&
            allImages[j] != null) {
          if (allImages[j].indexOf(image) == 0) {
            subString[i] = true;
            subStringAtPos[image.length() - 1] = true;
            break;
          }
          else if (Options.getIgnoreCase() &&
              startsWithIgnoreCase(allImages[j], image)) {
            subString[i] = true;
            subStringAtPos[image.length() - 1] = true;
            break;
          }
        }
      }
    }
  }

  void dumpStartWithStates(ScannerGen scannerGen, IndentingPrintWriter ostr) {
    ostr.println("private int " +
        "jjStartNfaWithStates" + scannerGen.lexStateSuffix + "(int pos, int kind, int state) throws java.io.IOException");
    ostr.println("{");
    ostr.indent();
    ostr.println("jjMatchedKind = kind;");
    ostr.println("jjMatchedPos = pos;");

    if (Options.getDebugScanner()) {
      ostr.println("debugPrinter.println(\"   No more string literal token matches are possible.\");");
      ostr.println("debugPrinter.println(\"   Currently matched the first \" " +
          "+ (jjMatchedPos + 1) + \" characters as a \" + tokenImage[jjMatchedKind] + \" token.\");");
    }

    ostr.println("jjChar = charStream.readChar();");
    ostr.println("if (jjChar == -1) { return pos + 1; }");

    if (Options.getDebugScanner()) {
      ostr.println("debugPrinter.println(" +
          (scannerGen.maxLexStates > 1 ? "\"<\" + jjLexStateNames[jjLexState] + \">\" + " : "") +
          "\"Current character : \" + " +
          "ScannerError.escape(String.valueOf(jjChar)) + \" (\" + jjChar + \") " +
          "at line \" + charStream.getEndLine() + \" column \" + charStream.getEndColumn());");
    }

    ostr.println("return jjMoveNfa" + scannerGen.lexStateSuffix + "(state, pos + 1);");
    ostr.unindent();
    ostr.println("}");
    ostr.println();
  }

  void dumpBoilerPlate(IndentingPrintWriter ostr) {
    ostr.println("private int " +
        "jjStopAtPos(int pos, int kind)");
    ostr.println("{");
    ostr.indent();
    ostr.println("jjMatchedKind = kind;");
    ostr.println("jjMatchedPos = pos;");

    if (Options.getDebugScanner()) {
      ostr.println("debugPrinter.println(\"   No more string literal token matches are possible.\");");
      ostr.println("debugPrinter.println(\"   Currently matched the first \" + (jjMatchedPos + 1) + " +
          "\" characters as a \" + tokenImage[jjMatchedKind] + \" token.\");");
    }

    ostr.println("return pos + 1;");
    ostr.unindent();
    ostr.println("}");
    ostr.println();
  }

  String[] reArrange(Hashtable tab) {
    String[] ret = new String[tab.size()];
    Enumeration e = tab.keys();
    int cnt = 0;

    while (e.hasMoreElements()) {
      int i = 0, j;
      String s;
      char c = (s = (String) e.nextElement()).charAt(0);

      while (i < cnt && ret[i].charAt(0) < c) { i++; }

      if (i < cnt) { for (j = cnt - 1; j >= i; j--) { ret[j + 1] = ret[j]; } }

      ret[i] = s;
      cnt++;
    }

    return ret;
  }

  void dumpDfaCode(ScannerGen scannerGen, IndentingPrintWriter ostr) {
    Hashtable tab;
    String key;
    KindInfo info;
    int maxLongsReqd = maxStrKind / 64 + 1;
    int i, j, k;
    boolean ifGenerated;
    scannerGen.maxLongsReqd[scannerGen.lexStateIndex] = maxLongsReqd;

    if (maxLen == 0) {
      ostr.println("private int " +
          "jjMoveStringLiteralDfa0" + scannerGen.lexStateSuffix + "() throws java.io.IOException");

      dumpNullStrLiterals(scannerGen, ostr);
      ostr.println();
      return;
    }

    if (!boilerPlateDumped) {
      dumpBoilerPlate(ostr);
      boilerPlateDumped = true;
    }

    boolean createStartNfa = false;
    for (i = 0; i < maxLen; i++) {
      boolean atLeastOne = false;
      boolean startNfaNeeded = false;
      tab = (Hashtable) charPosKind.get(i);
      String[] keys = reArrange(tab);

      ostr.print("private int " +
          "jjMoveStringLiteralDfa" + i + scannerGen.lexStateSuffix + "(");

      if (i != 0) {
        if (i == 1) {
          for (j = 0; j < maxLongsReqd - 1; j++) {
            if (i <= maxLenForActive[j]) {
              if (atLeastOne) { ostr.print(", "); }
              else { atLeastOne = true; }
              ostr.print("long active" + j);
            }
          }

          if (i <= maxLenForActive[j]) {
            if (atLeastOne) { ostr.print(", "); }
            ostr.print("long active" + j);
          }
        }
        else {
          for (j = 0; j < maxLongsReqd - 1; j++) {
            if (i <= maxLenForActive[j] + 1) {
              if (atLeastOne) { ostr.print(", "); }
              else { atLeastOne = true; }
              ostr.print("long old" + j + ", long active" + j);
            }
          }

          if (i <= maxLenForActive[j] + 1) {
            if (atLeastOne) { ostr.print(", "); }
            ostr.print("long old" + j + ", long active" + j);
          }
        }
      }
      ostr.println(") throws java.io.IOException");
      ostr.println("{");
      ostr.indent();
      if (i != 0) {
        if (i > 1) {
          atLeastOne = false;
          ostr.print("if ((");

          for (j = 0; j < maxLongsReqd - 1; j++) {
            if (i <= maxLenForActive[j] + 1) {
              if (atLeastOne) { ostr.print(" | "); }
              else { atLeastOne = true; }
              ostr.print("(active" + j + " &= old" + j + ")");
            }
          }

          if (i <= maxLenForActive[j] + 1) {
            if (atLeastOne) { ostr.print(" | "); }
            ostr.print("(active" + j + " &= old" + j + ")");
          }

          ostr.println(") == 0L)");
          ostr.indent();
          if (!scannerGen.mixed[scannerGen.lexStateIndex] && scannerGen.nfaStates.generatedStates != 0) {
            ostr.print("return jjStartNfa" + scannerGen.lexStateSuffix +
                "(" + (i - 2) + ", ");
            for (j = 0; j < maxLongsReqd - 1; j++) {
              if (i <= maxLenForActive[j] + 1) { ostr.print("old" + j + ", "); }
              else { ostr.print("0L, "); }
            }
            if (i <= maxLenForActive[j] + 1) { ostr.println("old" + j + ");"); }
            else { ostr.println("0L);"); }
          }
          else if (scannerGen.nfaStates.generatedStates != 0) {
            ostr.println("return jjMoveNfa" + scannerGen.lexStateSuffix +
                "(" + scannerGen.nfaStates.initStateName(scannerGen) + ", " + (i - 1) + ");");
          }
          else { ostr.println("return " + i + ";"); }
          ostr.unindent();
        }

        if (i != 0 && Options.getDebugScanner()) {
          ostr.println("if (jjMatchedKind != 0 && jjMatchedKind != 0x" +
              Integer.toHexString(Integer.MAX_VALUE) + ")");
          ostr.println("debugPrinter.println(\"   Currently matched the first \" + " +
              "(jjMatchedPos + 1) + \" characters as a \" + tokenImage[jjMatchedKind] + \" token.\");");

          ostr.print("debugPrinter.println(\"   Possible string literal matches : { \"");

          for (int vecs = 0; vecs < maxStrKind / 64 + 1; vecs++) {
            if (i <= maxLenForActive[vecs]) {
              ostr.print(" + ");
              ostr.print("jjKindsForBitVector(" + vecs + ", ");
              ostr.print("active" + vecs + ") ");
            }
          }

          ostr.print(" + \" } \");");
          ostr.println();
        }

        ostr.println("jjChar = charStream.readChar();");
        ostr.println("if (jjChar == -1)");
        ostr.println("{");
        ostr.indent();

        if (!scannerGen.mixed[scannerGen.lexStateIndex] && scannerGen.nfaStates.generatedStates != 0) {
          ostr.print("jjStopStringLiteralDfa" + scannerGen.lexStateSuffix + "(" + (i - 1) + ", ");
          for (k = 0; k < maxLongsReqd - 1; k++) {
            if (i <= maxLenForActive[k]) { ostr.print("active" + k + ", "); }
            else { ostr.print("0L, "); }
          }

          if (i <= maxLenForActive[k]) { ostr.println("active" + k + ");"); }
          else { ostr.println("0L);"); }

          if (i != 0 && Options.getDebugScanner()) {
            ostr.println("if (jjMatchedKind != 0 && jjMatchedKind != 0x" +
                Integer.toHexString(Integer.MAX_VALUE) + ")");
            ostr.println("debugPrinter.println(\"   Currently matched the first \" + " +
                "(jjMatchedPos + 1) + \" characters as a \" + tokenImage[jjMatchedKind] + \" token.\");");
          }
          ostr.println("return " + i + ";");
        }
        else if (scannerGen.nfaStates.generatedStates != 0) {
          ostr.println("return jjMoveNfa" + scannerGen.lexStateSuffix + "(" + scannerGen.nfaStates.initStateName(scannerGen) +
              ", " + (i - 1) + ");");
        }
        else { ostr.println("return " + i + ";"); }

        ostr.unindent();
        ostr.println("}");
      }

      if (i != 0 && Options.getDebugScanner()) {
        ostr.println("debugPrinter.println(" +
            (scannerGen.maxLexStates > 1 ? "\"<\" + jjLexStateNames[jjLexState] + \">\" + " : "") +
            "\"Current character : \" + " +
            "ScannerError.escape(String.valueOf(jjChar)) + \" (\" + jjChar + \") " +
            "at line \" + charStream.getEndLine() + \" column \" + charStream.getEndColumn());");
      }

      ostr.println("switch(jjChar)");
      ostr.println("{");
      ostr.indent();

      CaseLoop:
      for (int q = 0; q < keys.length; q++) {
        key = keys[q];
        info = (KindInfo) tab.get(key);
        ifGenerated = false;
        char c = key.charAt(0);

        if (i == 0 && c < 128 && info.finalKindCnt != 0 &&
            (scannerGen.nfaStates.generatedStates == 0 || !scannerGen.nfaStates.canStartNfaUsingAscii(scannerGen, c))) {
          int kind;
          for (j = 0; j < maxLongsReqd; j++) { if (info.finalKinds[j] != 0L) { break; } }

          for (k = 0; k < 64; k++) {
            if ((info.finalKinds[j] & (1L << k)) != 0L &&
                !subString[kind = (j * 64 + k)]) {
              if ((intermediateKinds != null &&
                  intermediateKinds[(j * 64 + k)] != null &&
                  intermediateKinds[(j * 64 + k)][i] < (j * 64 + k) &&
                  intermediateMatchedPos != null &&
                  intermediateMatchedPos[(j * 64 + k)][i] == i) ||
                  (scannerGen.canMatchAnyChar[scannerGen.lexStateIndex] >= 0 &&
                      scannerGen.canMatchAnyChar[scannerGen.lexStateIndex] < (j * 64 + k))) { break; }
              else if ((scannerGen.toSkip[kind / 64] & (1L << (kind % 64))) != 0L &&
                  (scannerGen.toSpecial[kind / 64] & (1L << (kind % 64))) == 0L &&
                  scannerGen.actions[kind] == null &&
                  scannerGen.newLexState[kind] == null) {
                scannerGen.addCharToSkip(c, kind);

                if (Options.getIgnoreCase()) {
                  if (c != Character.toUpperCase(c)) { scannerGen.addCharToSkip(Character.toUpperCase(c), kind); }

                  if (c != Character.toLowerCase(c)) { scannerGen.addCharToSkip(Character.toLowerCase(c), kind); }
                }
                continue CaseLoop;
              }
            }
          }
        }

        // Since we know key is a single character ...
        if (Options.getIgnoreCase()) {
          if (c != Character.toUpperCase(c)) { ostr.println("case " + (int) Character.toUpperCase(c) + ":"); }

          if (c != Character.toLowerCase(c)) { ostr.println("case " + (int) Character.toLowerCase(c) + ":"); }
        }

        ostr.println("case " + (int) c + ":");
        ostr.indent();

        long matchedKind;

        if (info.finalKindCnt != 0) {
          for (j = 0; j < maxLongsReqd; j++) {
            if ((matchedKind = info.finalKinds[j]) == 0L) { continue; }

            for (k = 0; k < 64; k++) {
              if ((matchedKind & (1L << k)) == 0L) { continue; }

              if (ifGenerated) {
                ostr.print("else if ");
              }
              else if (i != 0) { ostr.print("if "); }

              ifGenerated = true;

              int kindToPrint;
              if (i != 0) {
                ostr.println("((active" + j +
                    " & 0x" + Long.toHexString(1L << k) + "L) != 0L)");
              }

              if (intermediateKinds != null &&
                  intermediateKinds[(j * 64 + k)] != null &&
                  intermediateKinds[(j * 64 + k)][i] < (j * 64 + k) &&
                  intermediateMatchedPos != null &&
                  intermediateMatchedPos[(j * 64 + k)][i] == i) {
                JavaCCErrors.warning(" \"" +
                    Parsers.escape(allImages[j * 64 + k]) +
                    "\" cannot be matched as a string literal token " +
                    "at line " + getLine(scannerGen, j * 64 + k) + ", column " + getColumn(scannerGen, j * 64 + k) +
                    ". It will be matched as " +
                    getLabel(scannerGen, intermediateKinds[(j * 64 + k)][i]) + ".");
                kindToPrint = intermediateKinds[(j * 64 + k)][i];
              }
              else if (i == 0 &&
                  scannerGen.canMatchAnyChar[scannerGen.lexStateIndex] >= 0 &&
                  scannerGen.canMatchAnyChar[scannerGen.lexStateIndex] < (j * 64 + k)) {
                JavaCCErrors.warning(" \"" +
                    Parsers.escape(allImages[j * 64 + k]) +
                    "\" cannot be matched as a string literal token " +
                    "at line " + getLine(scannerGen, j * 64 + k) + ", column " + getColumn(scannerGen, j * 64 + k) +
                    ". It will be matched as " +
                    getLabel(scannerGen, scannerGen.canMatchAnyChar[scannerGen.lexStateIndex]) + ".");
                kindToPrint = scannerGen.canMatchAnyChar[scannerGen.lexStateIndex];
              }
              else { kindToPrint = j * 64 + k; }

              if (!subString[(j * 64 + k)]) {
                int stateSetName = getStateSetForKind(scannerGen, i, j * 64 + k);

                if (stateSetName != -1) {
                  createStartNfa = true;
                  ostr.indent();
                  ostr.println("return jjStartNfaWithStates" +
                      scannerGen.lexStateSuffix + "(" + i +
                      ", " + kindToPrint + ", " + stateSetName + ");");
                  ostr.unindent();
                }
                else {
                  ostr.indent();
                  ostr.println("return jjStopAtPos" + "(" + i + ", " + kindToPrint + ");");
                  ostr.unindent();
                }
              }
              else {
                if ((scannerGen.initMatch[scannerGen.lexStateIndex] != 0 &&
                    scannerGen.initMatch[scannerGen.lexStateIndex] != Integer.MAX_VALUE) ||
                    i != 0) {
                  ostr.println("{");
                  ostr.indent();
                  ostr.println("jjMatchedKind = " +
                      kindToPrint + ";");
                  ostr.println("jjMatchedPos = " + i + ";");
                  ostr.unindent();
                  ostr.println("}");
                }
                else {
                  ostr.println("jjMatchedKind = " +
                      kindToPrint + ";");
                }
              }
            }
          }
        }

        if (info.validKindCnt != 0) {
          atLeastOne = false;

          if (i == 0) {

            ostr.print("return jjMoveStringLiteralDfa" + (i + 1) +
                scannerGen.lexStateSuffix + "(");
            for (j = 0; j < maxLongsReqd - 1; j++) {
              if ((i + 1) <= maxLenForActive[j]) {
                if (atLeastOne) { ostr.print(", "); }
                else { atLeastOne = true; }

                ostr.print("0x" + Long.toHexString(info.validKinds[j]) + "L");
              }
            }

            if ((i + 1) <= maxLenForActive[j]) {
              if (atLeastOne) { ostr.print(", "); }

              ostr.print("0x" + Long.toHexString(info.validKinds[j]) + "L");
            }
            ostr.println(");");
            //out.unindent();
          }
          else {
            ostr.print("return jjMoveStringLiteralDfa" + (i + 1) +
                scannerGen.lexStateSuffix + "(");

            for (j = 0; j < maxLongsReqd - 1; j++) {
              if ((i + 1) <= maxLenForActive[j] + 1) {
                if (atLeastOne) { ostr.print(", "); }
                else { atLeastOne = true; }

                if (info.validKinds[j] != 0L) {
                  ostr.print("active" + j + ", 0x" +
                      Long.toHexString(info.validKinds[j]) + "L");
                }
                else { ostr.print("active" + j + ", 0L"); }
              }
            }

            if ((i + 1) <= maxLenForActive[j] + 1) {
              if (atLeastOne) { ostr.print(", "); }
              if (info.validKinds[j] != 0L) {
                ostr.print("active" + j + ", 0x" +
                    Long.toHexString(info.validKinds[j]) + "L");
              }
              else { ostr.print("active" + j + ", 0L"); }
            }

            ostr.println(");");
            //out.unindent();
          }
        }
        else {
          // A very special case.
          if (i == 0 && scannerGen.mixed[scannerGen.lexStateIndex]) {

            if (scannerGen.nfaStates.generatedStates != 0) {
              ostr.println("return jjMoveNfa" + scannerGen.lexStateSuffix +
                  "(" + scannerGen.nfaStates.initStateName(scannerGen) + ", 0);");
            }
            else { ostr.println("return 1;"); }
          }
          else if (i != 0) // No more str literals to look for
          {
            ostr.println("break;");
            //out.unindent();
            startNfaNeeded = true;
          }
        }

        ostr.unindent();
      }

      /* default means that the current character is not in any of the
  strings at this position. */
      ostr.println("default:");

      if (Options.getDebugScanner()) {
        ostr.println("debugPrinter.println(\"   No string literal matches possible.\");");
      }

      if (scannerGen.nfaStates.generatedStates != 0) {
        if (i == 0) {
          /* This means no string literal is possible. Just move nfa with
      this guy and return. */
          ostr.println("return jjMoveNfa" + scannerGen.lexStateSuffix +
              "(" + scannerGen.nfaStates.initStateName(scannerGen) + ", 0);");
        }
        else {
          ostr.println("break;");
          //out.unindent();
          startNfaNeeded = true;
        }
      }
      else {
        ostr.println("return " + (i + 1) + ";");
      }

      ostr.unindent();
      ostr.println("}");

      if (i != 0) {
        if (startNfaNeeded) {
          if (!scannerGen.mixed[scannerGen.lexStateIndex] && scannerGen.nfaStates.generatedStates != 0) {
            /* Here, a string literal is successfully matched and no more
   string literals are possible. So set the kind and state set
   upto and including this position for the matched string. */

            ostr.print("return jjStartNfa" + scannerGen.lexStateSuffix + "(" + (i - 1) + ", ");
            for (k = 0; k < maxLongsReqd - 1; k++) {
              if (i <= maxLenForActive[k]) { ostr.print("active" + k + ", "); }
              else { ostr.print("0L, "); }
            }
            if (i <= maxLenForActive[k]) { ostr.println("active" + k + ");"); }
            else { ostr.println("0L);"); }
          }
          else if (scannerGen.nfaStates.generatedStates != 0) {
            ostr.println("return jjMoveNfa" + scannerGen.lexStateSuffix +
                "(" + scannerGen.nfaStates.initStateName(scannerGen) + ", " + i + ");");
          }
          else { ostr.println("return " + (i + 1) + ";"); }
        }
      }

      ostr.unindent();
      ostr.println("}");
      ostr.println();
    }

    if (!scannerGen.mixed[scannerGen.lexStateIndex] && scannerGen.nfaStates.generatedStates != 0 && createStartNfa) {
      dumpStartWithStates(scannerGen, ostr);
    }
  }

  final int getStrKind(ScannerGen scannerGen, String str) {
    for (int i = 0; i < maxStrKind; i++) {
      if (scannerGen.lexStates[i] != scannerGen.lexStateIndex) { continue; }

      String image = allImages[i];
      if (image != null && image.equals(str)) { return i; }
    }

    return Integer.MAX_VALUE;
  }

  void generateNfaStartStates(ScannerGen scannerGen, IndentingPrintWriter ostr,
                              NfaState initialState) {
    boolean[] seen = new boolean[scannerGen.nfaStates.generatedStates];
    Hashtable stateSets = new Hashtable();
    String stateSetString = "";
    int i, j, kind, jjMatchedPos = 0;
    int maxKindsReqd = maxStrKind / 64 + 1;
    long[] actives;
    List newStates = new ArrayList();
    List oldStates = null, jjtmpStates;

    statesForPos = new Hashtable[maxLen];
    intermediateKinds = new int[maxStrKind + 1][];
    intermediateMatchedPos = new int[maxStrKind + 1][];

    for (i = 0; i < maxStrKind; i++) {
      if (scannerGen.lexStates[i] != scannerGen.lexStateIndex) { continue; }

      String image = allImages[i];

      if (image == null || image.length() < 1) { continue; }

      try {
        if ((oldStates = (List) initialState.epsilonMoves.clone()) == null ||
            oldStates.size() == 0) {
          dumpNfaStartStatesCode(scannerGen, statesForPos, ostr);
          return;
        }
      }
      catch (Exception e) {
        JavaCCErrors.semanticError("Error cloning state vector");
      }

      intermediateKinds[i] = new int[image.length()];
      intermediateMatchedPos[i] = new int[image.length()];
      jjMatchedPos = 0;
      kind = Integer.MAX_VALUE;

      for (j = 0; j < image.length(); j++) {
        if (oldStates == null || oldStates.size() <= 0) {
          // Here, j > 0
          kind = intermediateKinds[i][j] = intermediateKinds[i][j - 1];
          jjMatchedPos = intermediateMatchedPos[i][j] = intermediateMatchedPos[i][j - 1];
        }
        else {
          kind = scannerGen.nfaStates.moveFromSet(image.charAt(j), oldStates, newStates);
          oldStates.clear();

          if (j == 0 && kind != Integer.MAX_VALUE &&
              scannerGen.canMatchAnyChar[scannerGen.lexStateIndex] != -1 &&
              kind > scannerGen.canMatchAnyChar[scannerGen.lexStateIndex]) {
            kind = scannerGen.canMatchAnyChar[scannerGen.lexStateIndex];
          }

          if (getStrKind(scannerGen, image.substring(0, j + 1)) < kind) {
            intermediateKinds[i][j] = kind = Integer.MAX_VALUE;
            jjMatchedPos = 0;
          }
          else if (kind != Integer.MAX_VALUE) {
            intermediateKinds[i][j] = kind;
            jjMatchedPos = intermediateMatchedPos[i][j] = j;
          }
          else if (j == 0) { kind = intermediateKinds[i][j] = Integer.MAX_VALUE; }
          else {
            kind = intermediateKinds[i][j] = intermediateKinds[i][j - 1];
            jjMatchedPos = intermediateMatchedPos[i][j] = intermediateMatchedPos[i][j - 1];
          }

          stateSetString = scannerGen.nfaStates.getStateSetString(newStates);
        }

        if (kind == Integer.MAX_VALUE &&
            (newStates == null || newStates.size() == 0)) { continue; }

        int p;
        if (stateSets.get(stateSetString) == null) {
          stateSets.put(stateSetString, stateSetString);
          for (p = 0; p < newStates.size(); p++) {
            if (seen[((NfaState) newStates.get(p)).stateName]) { ((NfaState) newStates.get(p)).inNextOf++; }
            else { seen[((NfaState) newStates.get(p)).stateName] = true; }
          }
        }
        else {
          for (p = 0; p < newStates.size(); p++) { seen[((NfaState) newStates.get(p)).stateName] = true; }
        }

        jjtmpStates = oldStates;
        oldStates = newStates;
        (newStates = jjtmpStates).clear();

        if (statesForPos[j] == null) { statesForPos[j] = new Hashtable(); }

        if ((actives = ((long[]) statesForPos[j].get(kind + ", " +
            jjMatchedPos + ", " + stateSetString))) == null) {
          actives = new long[maxKindsReqd];
          statesForPos[j].put(kind + ", " + jjMatchedPos + ", " +
              stateSetString, actives);
        }

        actives[i / 64] |= 1L << (i % 64);
        //String name = NfaState.StoreStateSet(stateSetString);
      }
    }

    dumpNfaStartStatesCode(scannerGen, statesForPos, ostr);
  }

  void dumpNfaStartStatesCode(ScannerGen scannerGen, Hashtable[] statesForPos,
                              IndentingPrintWriter ostr) {
    if (maxStrKind == 0) { // No need to generate this function
      return;
    }

    int i, maxKindsReqd = maxStrKind / 64 + 1;
    boolean condGenerated = false;
    int ind = 0;

    ostr.print("private int jjStopStringLiteralDfa" +
        scannerGen.lexStateSuffix + "(int pos, ");
    for (i = 0; i < maxKindsReqd - 1; i++) { ostr.print("long active" + i + ", "); }
    ostr.println("long active" + i + ")\n{");

    if (Options.getDebugScanner()) {
      ostr.println("debugPrinter.println(\"   No more string literal token matches are possible.\");");
    }

    ostr.println("   switch (pos)\n   {");

    for (i = 0; i < maxLen - 1; i++) {
      if (statesForPos[i] == null) { continue; }

      ostr.println("      case " + i + ":");

      Enumeration e = statesForPos[i].keys();
      while (e.hasMoreElements()) {
        String stateSetString = (String) e.nextElement();
        long[] actives = (long[]) statesForPos[i].get(stateSetString);

        for (int j = 0; j < maxKindsReqd; j++) {
          if (actives[j] == 0L) { continue; }

          if (condGenerated) { ostr.print(" || "); }
          else { ostr.print("         if ("); }

          condGenerated = true;

          ostr.print("(active" + j + " & 0x" +
              Long.toHexString(actives[j]) + "L) != 0L");
        }

        if (condGenerated) {
          ostr.println(")");

          String kindStr = stateSetString.substring(0,
              ind = stateSetString.indexOf(", "));
          String afterKind = stateSetString.substring(ind + 2);
          int jjMatchedPos = Integer.parseInt(
              afterKind.substring(0, afterKind.indexOf(", ")));

          if (!kindStr.equals(String.valueOf(Integer.MAX_VALUE))) { ostr.println("         {"); }

          if (!kindStr.equals(String.valueOf(Integer.MAX_VALUE))) {
            if (i == 0) {
              ostr.println("            jjMatchedKind = " + kindStr + ";");

              if ((scannerGen.initMatch[scannerGen.lexStateIndex] != 0 &&
                  scannerGen.initMatch[scannerGen.lexStateIndex] != Integer.MAX_VALUE)) {
                ostr.println("            jjMatchedPos = 0;");
              }
            }
            else if (i == jjMatchedPos) {
              if (subStringAtPos[i]) {
                ostr.println("            if (jjMatchedPos != " + i + ")");
                ostr.println("            {");
                ostr.println("               jjMatchedKind = " + kindStr + ";");
                ostr.println("               jjMatchedPos = " + i + ";");
                ostr.println("            }");
              }
              else {
                ostr.println("            jjMatchedKind = " + kindStr + ";");
                ostr.println("            jjMatchedPos = " + i + ";");
              }
            }
            else {
              if (jjMatchedPos > 0) { ostr.println("            if (jjMatchedPos < " + jjMatchedPos + ")"); }
              else { ostr.println("            if (jjMatchedPos == 0)"); }
              ostr.println("            {");
              ostr.println("               jjMatchedKind = " + kindStr + ";");
              ostr.println("               jjMatchedPos = " + jjMatchedPos + ";");
              ostr.println("            }");
            }
          }

          kindStr = stateSetString.substring(0,
              ind = stateSetString.indexOf(", "));
          afterKind = stateSetString.substring(ind + 2);
          stateSetString = afterKind.substring(
              afterKind.indexOf(", ") + 2);

          if (stateSetString.equals("null;")) { ostr.println("            return -1;"); }
          else {
            ostr.println("            return " +
                scannerGen.nfaStates.addStartStateSet(stateSetString) + ";");
          }

          if (!kindStr.equals(String.valueOf(Integer.MAX_VALUE))) { ostr.println("         }"); }
          condGenerated = false;
        }
      }

      ostr.println("         return -1;");
    }

    ostr.println("      default:");
    ostr.println("         return -1;");
    ostr.println("   }");
    ostr.println("}");

    ostr.println();
    ostr.print("private int jjStartNfa" +
        scannerGen.lexStateSuffix + "(int pos, ");
    for (i = 0; i < maxKindsReqd - 1; i++) { ostr.print("long active" + i + ", "); }
    ostr.println("long active" + i + ") throws java.io.IOException \n{");

    if (scannerGen.mixed[scannerGen.lexStateIndex]) {
      if (scannerGen.nfaStates.generatedStates != 0) {
        ostr.println("   return jjMoveNfa" + scannerGen.lexStateSuffix +
            "(" + scannerGen.nfaStates.initStateName(scannerGen) + ", pos + 1);");
      }
      else { ostr.println("   return pos + 1;"); }

      ostr.println("}");
      return;
    }

    ostr.print("   return jjMoveNfa" + scannerGen.lexStateSuffix + "(" +
        "jjStopStringLiteralDfa" + scannerGen.lexStateSuffix + "(pos, ");
    for (i = 0; i < maxKindsReqd - 1; i++) { ostr.print("active" + i + ", "); }
    ostr.print("active" + i + ")");
    ostr.println(", pos + 1);");
    ostr.println("}");
    ostr.println();
  }
}
