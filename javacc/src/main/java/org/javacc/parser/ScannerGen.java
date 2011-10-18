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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/** Generate lexer. */
final class ScannerGen implements FileGenerator, JavaCCConstants {
  private final JavaCCState state;
  // Hashtable of vectors
  Hashtable allTpsForState = new Hashtable();
  int lexStateIndex;
  int[] kinds;
  int maxOrdinal = 1;
  String lexStateSuffix;
  String[] newLexState;
  int[] lexStates;
  boolean[] ignoreCase;
  Action[] actions;
  Hashtable initStates = new Hashtable();
  int stateSetSize;
  int maxLexStates;
  String[] lexStateName;
  NfaState[] singlesToSkip;
  long[] toSkip;
  long[] toSpecial;
  long[] toMore;
  long[] toToken;
  int defaultLexState;
  RegularExpression[] rexprs;
  int[] maxLongsReqd;
  int[] initMatch;
  int[] canMatchAnyChar;
  boolean hasEmptyMatch;
  boolean[] canLoop;
  boolean[] stateHasActions;
  boolean hasLoop;
  boolean[] canReachOnMore;
  boolean[] hasNfa;
  boolean[] mixed;
  NfaState initialState;
  int curKind;
  boolean hasSkipActions;
  boolean hasMoreActions;
  boolean hasTokenActions;
  boolean hasSpecial;
  boolean hasSkip;
  boolean hasMore;
  RegularExpression curRE;
  boolean keepLineCol;
  public static boolean keepImage;
  final NfaStates nfaStates = new NfaStates();
  final StringLiterals stringLiterals = new StringLiterals();

  ScannerGen(JavaCCState state) {
    this.state = state;
  }

  @Override
  public void start() throws MetaParseException, IOException {
    if (JavaCCErrors.getErrorCount() != 0) {
      throw new MetaParseException();
    }

    if (!Options.getBuildScanner()
        || Options.getUserScanner()) {
      return;
    }

    File path = new File(Options.getOutputDirectory(), state.scannerClass() + ".java");
    OutputFile outputFile = new OutputFile(path);
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      generate(out);
    }
    finally {
      out.close();
    }
  }

  private void generate(IndentingPrintWriter out) throws IOException {
    keepLineCol = Options.getKeepLineColumn();
    keepImage = Options.getKeepImage();
    List choices = new ArrayList();
    Enumeration e;
    TokenProduction tp;
    int i, j;

    printClassHead(out);
    buildLexStatesTable();

    e = allTpsForState.keys();

    boolean ignoring;

    while (e.hasMoreElements()) {
      nfaStates.reInit();
      stringLiterals.ReInit();

      String key = (String) e.nextElement();

      lexStateIndex = getIndex(key);
      lexStateSuffix = "_" + lexStateIndex;
      List allTps = (List) allTpsForState.get(key);
      initStates.put(key, initialState = new NfaState(this));
      ignoring = false;

      singlesToSkip[lexStateIndex] = new NfaState(this);
      singlesToSkip[lexStateIndex].dummy = true;

      if (key.equals("DEFAULT")) {
        defaultLexState = lexStateIndex;
      }

      for (i = 0; i < allTps.size(); i++) {
        tp = (TokenProduction) allTps.get(i);
        int kind = tp.kind;
        boolean ignore = tp.ignoreCase;
        List<RegExpSpec> reSpecs = tp.reSpecs;

        if (i == 0) {
          ignoring = ignore;
        }

        for (j = 0; j < reSpecs.size(); j++) {
          RegExpSpec reSpec = reSpecs.get(j);
          curRE = reSpec.regExp;

          rexprs[curKind = curRE.ordinal] = curRE;
          lexStates[curRE.ordinal] = lexStateIndex;
          ignoreCase[curRE.ordinal] = ignore;

          if (curRE.isPrivate) {
            kinds[curRE.ordinal] = -1;
            continue;
          }

          if (curRE instanceof RStringLiteral
              && !((RStringLiteral) curRE).image.equals("")) {
            ((RStringLiteral) curRE).generateDfa(this);
            if (i != 0 && !mixed[lexStateIndex] && ignoring != ignore) {
              mixed[lexStateIndex] = true;
            }
          }
          else if (curRE.canMatchAnyChar()) {
            if (canMatchAnyChar[lexStateIndex] == -1
                || canMatchAnyChar[lexStateIndex] > curRE.ordinal) {
              canMatchAnyChar[lexStateIndex] = curRE.ordinal;
            }
          }
          else {
            Nfa temp;

            if (curRE instanceof RChoice) {
              choices.add(curRE);
            }

            temp = curRE.generateNfa(this, ignore);
            temp.end.isFinal = true;
            temp.end.kind = curRE.ordinal;
            initialState.addMove(temp.start);
          }

          if (kinds.length < curRE.ordinal) {
            int[] tmp = new int[curRE.ordinal + 1];

            System.arraycopy(kinds, 0, tmp, 0, kinds.length);
            kinds = tmp;
          }

          kinds[curRE.ordinal] = kind;

          if (reSpec.nextState != null &&
              !reSpec.nextState.equals(lexStateName[lexStateIndex])) {
            newLexState[curRE.ordinal] = reSpec.nextState;
          }

          if (reSpec.action != null && reSpec.action.getActionTokens() != null &&
              reSpec.action.getActionTokens().size() > 0) {
            actions[curRE.ordinal] = reSpec.action;
          }

          switch (kind) {
            case TokenProduction.SPECIAL:
              hasSkipActions |= (actions[curRE.ordinal] != null) ||
                  (newLexState[curRE.ordinal] != null);
              hasSpecial = true;
              toSpecial[curRE.ordinal / 64] |= 1L << (curRE.ordinal % 64);
              toSkip[curRE.ordinal / 64] |= 1L << (curRE.ordinal % 64);
              break;
            case TokenProduction.SKIP:
              hasSkipActions |= (actions[curRE.ordinal] != null);
              hasSkip = true;
              toSkip[curRE.ordinal / 64] |= 1L << (curRE.ordinal % 64);
              break;
            case TokenProduction.MORE:
              hasMoreActions |= (actions[curRE.ordinal] != null);
              hasMore = true;
              toMore[curRE.ordinal / 64] |= 1L << (curRE.ordinal % 64);

              if (newLexState[curRE.ordinal] != null) {
                canReachOnMore[getIndex(newLexState[curRE.ordinal])] = true;
              }
              else {
                canReachOnMore[lexStateIndex] = true;
              }

              break;
            case TokenProduction.TOKEN:
              hasTokenActions |= (actions[curRE.ordinal] != null);
              toToken[curRE.ordinal / 64] |= 1L << (curRE.ordinal % 64);
              break;
          }
        }
      }

      // Generate a static block for initializing the nfa transitions
      nfaStates.computeClosures();

      for (i = 0; i < initialState.epsilonMoves.size(); i++) {
        ((NfaState) initialState.epsilonMoves.elementAt(i)).generateCode();
      }

      if (hasNfa[lexStateIndex] = (nfaStates.generatedStates != 0)) {
        initialState.generateCode();
        initialState.generateInitMoves(out);
      }

      if (initialState.kind != Integer.MAX_VALUE && initialState.kind != 0) {
        if ((toSkip[initialState.kind / 64] & (1L << initialState.kind)) != 0L ||
            (toSpecial[initialState.kind / 64] & (1L << initialState.kind)) != 0L) {
          hasSkipActions = true;
        }
        else if ((toMore[initialState.kind / 64] & (1L << initialState.kind)) != 0L) {
          hasMoreActions = true;
        }
        else {
          hasTokenActions = true;
        }

        if (initMatch[lexStateIndex] == 0 ||
            initMatch[lexStateIndex] > initialState.kind) {
          initMatch[lexStateIndex] = initialState.kind;
          hasEmptyMatch = true;
        }
      }
      else if (initMatch[lexStateIndex] == 0) {
        initMatch[lexStateIndex] = Integer.MAX_VALUE;
      }

      stringLiterals.FillSubString(this);

      if (hasNfa[lexStateIndex] && !mixed[lexStateIndex]) {
        stringLiterals.GenerateNfaStartStates(this, out, initialState);
      }

      stringLiterals.DumpDfaCode(this, out);

      if (hasNfa[lexStateIndex]) {
        nfaStates.dumpMoveNfa(this, out);
      }

      if (stateSetSize < nfaStates.generatedStates) {
        stateSetSize = nfaStates.generatedStates;
      }
    }

    for (i = 0; i < choices.size(); i++) {
      ((RChoice) choices.get(i)).checkUnmatchability(this);
    }

    nfaStates.dumpStateSets(out);
    checkEmptyStringMatch();
    nfaStates.dumpNonAsciiMoveMethods(out);
    stringLiterals.DumpStrLiteralImages(this, out);
    dumpStaticVarDeclarations(out);
    dumpFillToken(out);
    dumpGetNextToken(out);

    if (Options.getDebugScanner()) {
      nfaStates.dumpStatesForKind(out);
      dumpDebugMethods(out);
    }

    if (hasLoop) {
      out.println("int[] jjEmptyLineNo = new int[" + maxLexStates + "];");
      out.println("int[] jjEmptyColumnNo = new int[" + maxLexStates + "];");
      out.println("boolean[] jjBeenHere = new boolean[" + maxLexStates + "];");
    }

    if (hasSkipActions) {
      dumpSkipActions(out);
    }
    if (hasMoreActions) {
      dumpMoreActions(out);
    }
    if (hasTokenActions) {
      dumpTokenActions(out);
    }

    nfaStates.printBoilerPlate(out);
    out.unindent();
    out.println("}");
  }

  private void printClassHead(IndentingPrintWriter out)
      throws IOException {
    int i, j;

    int l = 0, kind;
    i = 1;
    List<Token> tokens = state.cuToInsertionPoint1;
    while (true) {
      if (tokens.size() <= l) {
        break;
      }

      kind = tokens.get(l).getKind();
      if (kind == PACKAGE || kind == IMPORT) {
        for (; i < tokens.size(); i++) {
          kind = tokens.get(i).getKind();
          if (kind == SEMICOLON
              || kind == ABSTRACT
              || kind == FINAL
              || kind == PUBLIC
              || kind == CLASS
              || kind == INTERFACE) {
            TokenPrinter.cLine = tokens.get(l).getBeginLine();
            TokenPrinter.cCol = tokens.get(l).getBeginColumn();
            for (j = l; j < i; j++) {
              TokenPrinter.printToken(tokens.get(j), out);
            }
            if (kind == SEMICOLON) {
              TokenPrinter.printToken(tokens.get(j), out);
            }
            out.println();
            break;
          }
        }
        l = ++i;
      }
      else {
        break;
      }
    }

    out.println();
    out.println("@SuppressWarnings(\"unused\")");
    out.print("public class " + state.scannerClass() + " implements Scanner, " +
        state.constantsClass() + " {");

    writeScannerDeclarations(out);

    out.indent();

    out.println();

    if (Options.getDebugScanner()) {
      out.println("/** Debug output. */");
      out.println("private java.io.PrintWriter debugPrinter = new java.io.PrintWriter(System.out);");
      out.println();
      out.println("/** Set debug output. */");
      out.println("public void setDebugPrinter(java.io.PrintWriter printer) { debugPrinter = printer; }");
      out.println();
    }

    if (Options.getScannerUsesParser()) {
      out.println();
      out.println("/** The parser. */");
      out.println("public " + state.parserClass() + " parser = null;");
    }
  }

  private void writeScannerDeclarations(IndentingPrintWriter out)
      throws IOException {
    List<Token> tokens = state.scannerDeclarations;
    if (tokens != null
        && tokens.size() > 0) {
      boolean commonTokenActionSeen = false;

      TokenPrinter.printTokenSetup(tokens.get(0));
      TokenPrinter.cCol = 1;

      for (Token t : tokens) {
        if (t.getKind() == IDENTIFIER
            && Options.getCommonTokenAction()
            && !commonTokenActionSeen) {
          commonTokenActionSeen = t.getImage().equals("commonTokenAction");
        }

        TokenPrinter.printToken(t, out);
      }

      out.println();

      if (Options.getCommonTokenAction() && !commonTokenActionSeen) {
        JavaCCErrors.warning("You have the COMMON_TOKEN_ACTION option set. " +
            "But it appears you have not defined the method :\n" +
            "      void commonTokenAction(Token t)\n" +
            "in your SCANNER_DECLS. The generated scanner will not compile.");
      }
    }
    else if (Options.getCommonTokenAction()) {
      JavaCCErrors.warning("You have the COMMON_TOKEN_ACTION option set. " +
          "But you have not defined the method :\n" +
          "      void commonTokenAction(Token t)\n" +
          "in your SCANNER_DECLS. The generated scanner will not compile.");
    }
  }

  private void dumpDebugMethods(IndentingPrintWriter out) {
    out.println("   int kindCnt = 0;");
    out.println("  protected final String jjKindsForBitVector(int i, long vec) {");
    out.println("    String retVal = \"\";");
    out.println("    if (i == 0)");
    out.println("       kindCnt = 0;");
    out.println("    for (int j = 0; j < 64; j++) {");
    out.println("       if ((vec & (1L << j)) != 0L) {");
    out.println("          if (kindCnt++ > 0)");
    out.println("             retVal += \", \";");
    out.println("          if (kindCnt % 5 == 0)");
    out.println("             retVal += \"\\n     \";");
    out.println("          retVal += tokenImage[i * 64 + j];");
    out.println("       }");
    out.println("    }");
    out.println("    return retVal;");
    out.println("  }");
    out.println();

    out.println("  protected final String jjKindsForStateVector(" +
        "int lexState, int[] vec, int start, int end) {");
    out.println("    boolean[] kindDone = new boolean[" + maxOrdinal + "];");
    out.println("    String retVal = \"\";");
    out.println("    int cnt = 0;");
    out.println("    for (int i = start; i < end; i++) {");
    out.println("     if (vec[i] == -1)");
    out.println("       continue;");
    out.println("     int[] stateSet = statesForState[jjLexState][vec[i]];");
    out.println("     for (int j = 0; j < stateSet.length; j++) {");
    out.println("       int state = stateSet[j];");
    out.println("       if (!kindDone[kindForState[lexState][state]]) {");
    out.println("          kindDone[kindForState[lexState][state]] = true;");
    out.println("          if (cnt++ > 0)");
    out.println("             retVal += \", \";");
    out.println("          if (cnt % 5 == 0)");
    out.println("             retVal += \"\\n     \";");
    out.println("          retVal += tokenImage[kindForState[lexState][state]];");
    out.println("       }");
    out.println("     }");
    out.println("    }");
    out.println("    if (cnt == 0)");
    out.println("       return \"{  }\";");
    out.println("    else");
    out.println("       return \"{ \" + retVal + \" }\";");
    out.println("  }");
    out.println();
  }

  private void buildLexStatesTable() {
    Iterator<TokenProduction> it = state.regExpList.iterator();
    TokenProduction tp;
    int i;

    String[] tmpLexStateName = new String[state.lexStateI2S.size()];
    while (it.hasNext()) {
      tp = it.next();
      List respecs = tp.reSpecs;
      List tps;

      for (i = 0; i < tp.lexStates.length; i++) {
        if ((tps = (List) allTpsForState.get(tp.lexStates[i])) == null) {
          tmpLexStateName[maxLexStates++] = tp.lexStates[i];
          allTpsForState.put(tp.lexStates[i], tps = new ArrayList());
        }

        tps.add(tp);
      }

      if (respecs == null || respecs.size() == 0) {
        continue;
      }

      RegularExpression re;
      for (i = 0; i < respecs.size(); i++) {
        if (maxOrdinal <= (re = ((RegExpSpec) respecs.get(i)).regExp).ordinal) {
          maxOrdinal = re.ordinal + 1;
        }
      }
    }

    kinds = new int[maxOrdinal];
    toSkip = new long[maxOrdinal / 64 + 1];
    toSpecial = new long[maxOrdinal / 64 + 1];
    toMore = new long[maxOrdinal / 64 + 1];
    toToken = new long[maxOrdinal / 64 + 1];
    toToken[0] = 1L;
    actions = new Action[maxOrdinal];
    actions[0] = state.eofAction;
    hasTokenActions = state.eofAction != null;
    initStates = new Hashtable();
    canMatchAnyChar = new int[maxLexStates];
    canLoop = new boolean[maxLexStates];
    stateHasActions = new boolean[maxLexStates];
    lexStateName = new String[maxLexStates];
    singlesToSkip = new NfaState[maxLexStates];
    System.arraycopy(tmpLexStateName, 0, lexStateName, 0, maxLexStates);

    for (i = 0; i < maxLexStates; i++) {
      canMatchAnyChar[i] = -1;
    }

    hasNfa = new boolean[maxLexStates];
    mixed = new boolean[maxLexStates];
    maxLongsReqd = new int[maxLexStates];
    initMatch = new int[maxLexStates];
    newLexState = new String[maxOrdinal];
    newLexState[0] = state.eofNextState;
    hasEmptyMatch = false;
    lexStates = new int[maxOrdinal];
    ignoreCase = new boolean[maxOrdinal];
    rexprs = new RegularExpression[maxOrdinal];
    stringLiterals.allImages = new String[maxOrdinal];
    canReachOnMore = new boolean[maxLexStates];
  }

  private int getIndex(String name) {
    for (int i = 0; i < lexStateName.length; i++) {
      if (lexStateName[i] != null && lexStateName[i].equals(name)) {
        return i;
      }
    }

    throw new Error(); // Should never come here
  }

  public void addCharToSkip(char c, int kind) {
    singlesToSkip[lexStateIndex].addChar(c);
    singlesToSkip[lexStateIndex].kind = kind;
  }

  private void checkEmptyStringMatch() {
    int i, j, k, len;
    boolean[] seen = new boolean[maxLexStates];
    boolean[] done = new boolean[maxLexStates];
    String cycle;
    String reList;

    Outer:
    for (i = 0; i < maxLexStates; i++) {
      if (done[i] || initMatch[i] == 0 || initMatch[i] == Integer.MAX_VALUE ||
          canMatchAnyChar[i] != -1) {
        continue;
      }

      done[i] = true;
      len = 0;
      cycle = "";
      reList = "";

      for (k = 0; k < maxLexStates; k++) {
        seen[k] = false;
      }

      j = i;
      seen[i] = true;
      cycle += lexStateName[j] + "-->";
      while (newLexState[initMatch[j]] != null) {
        cycle += newLexState[initMatch[j]];
        if (seen[j = getIndex(newLexState[initMatch[j]])]) {
          break;
        }

        cycle += "-->";
        done[j] = true;
        seen[j] = true;
        if (initMatch[j] == 0 || initMatch[j] == Integer.MAX_VALUE ||
            canMatchAnyChar[j] != -1) {
          continue Outer;
        }
        if (len != 0) {
          reList += "; ";
        }
        reList += "line " + rexprs[initMatch[j]].getLine() + ", column " +
            rexprs[initMatch[j]].getColumn();
        len++;
      }

      if (newLexState[initMatch[j]] == null) {
        cycle += lexStateName[lexStates[initMatch[j]]];
      }

      for (k = 0; k < maxLexStates; k++) {
        canLoop[k] |= seen[k];
      }

      hasLoop = true;
      if (len == 0) {
        JavaCCErrors.warning(rexprs[initMatch[i]],
            "Regular expression" + ((rexprs[initMatch[i]].label.equals(""))
                ? "" : (" for " + rexprs[initMatch[i]].label)) +
                " can be matched by the empty string (\"\") in lexical state " +
                lexStateName[i] + ". This can result in an endless loop of " +
                "empty string matches.");
      }
      else {
        JavaCCErrors.warning(rexprs[initMatch[i]],
            "Regular expression" + ((rexprs[initMatch[i]].label.equals(""))
                ? "" : (" for " + rexprs[initMatch[i]].label)) +
                " can be matched by the empty string (\"\") in lexical state " +
                lexStateName[i] + ". This regular expression along with the " +
                "regular expressions at " + reList + " forms the cycle \n   " +
                cycle + "\ncontaining regular expressions with empty matches." +
                " This can result in an endless loop of empty string matches.");
      }
    }
  }

  private void printArrayInitializer(int noElems, IndentingPrintWriter out) {
    out.print("{");
    for (int i = 0; i < noElems; i++) {
      if (i % 25 == 0) {
        out.print("\n   ");
      }
      out.print("0, ");
    }
    out.println("\n};");
  }

  private void dumpStaticVarDeclarations(IndentingPrintWriter out) {
    int i;

    out.println();
    out.println("/** Lexer state names. */");
    out.println("public static final String[] jjLexStateNames = {");
    out.indent();
    for (i = 0; i < maxLexStates; i++) {
      out.println("\"" + lexStateName[i] + "\",");
    }
    out.unindent();
    out.println("};");

    if (maxLexStates > 1) {
      out.println();
      out.println("/** Lex State array. */");
      out.print("public static final int[] jjNewLexState = {");
      out.indent();
      for (i = 0; i < maxOrdinal; i++) {
        if (i % 25 == 0) {
          out.print("\n");
        }

        if (newLexState[i] == null) {
          out.print("-1, ");
        }
        else {
          out.print(getIndex(newLexState[i]) + ", ");
        }
      }
      out.unindent();
      out.println("\n};");
    }

    if (hasSkip || hasMore || hasSpecial) {
      // Bit vector for TOKEN
      out.print("static final long[] jjToToken = {");
      for (i = 0; i < maxOrdinal / 64 + 1; i++) {
        if (i % 4 == 0) {
          out.print("\n   ");
        }
        out.print("0x" + Long.toHexString(toToken[i]) + "L, ");
      }
      out.println("\n};");
    }

    if (hasSkip || hasSpecial) {
      // Bit vector for SKIP
      out.print("static final long[] jjToSkip = {");
      for (i = 0; i < maxOrdinal / 64 + 1; i++) {
        if (i % 4 == 0) {
          out.print("\n   ");
        }
        out.print("0x" + Long.toHexString(toSkip[i]) + "L, ");
      }
      out.println("\n};");
    }

    if (hasSpecial) {
      // Bit vector for SPECIAL
      out.print("static final long[] jjToSpecial = {");
      for (i = 0; i < maxOrdinal / 64 + 1; i++) {
        if (i % 4 == 0) {
          out.print("\n   ");
        }
        out.print("0x" + Long.toHexString(toSpecial[i]) + "L, ");
      }
      out.println("\n};");
    }

    if (hasMore) {
      // Bit vector for MORE
      out.print("static final long[] jjToMore = {");
      for (i = 0; i < maxOrdinal / 64 + 1; i++) {
        if (i % 4 == 0) {
          out.print("\n   ");
        }
        out.print("0x" + Long.toHexString(toMore[i]) + "L, ");
      }
      out.println("\n};");
    }

    out.println("protected final CharStream charStream;");

    out.println("private final int[] jjRounds = " +
        "new int[" + stateSetSize + "];");
    out.println("private final int[] jjStateSet = " +
        "new int[" + (2 * stateSetSize) + "];");

    if (hasMoreActions || hasSkipActions || hasTokenActions) {
      if (keepImage) {
        out.println("private final StringBuilder jjImage = new StringBuilder();");
        out.println("private StringBuilder image = jjImage;");
        out.println("private int jjImageLength;");
      }
      out.println("private int lengthOfMatch;");
    }

    out.println("protected int jjChar;");

    if (Options.getScannerUsesParser()) {
      out.println();
      out.println("/** Constructor with parser. */");
      out.println("public " + state.scannerClass() + "(" + state.parserClass() + " parserArg, CharStream stream) {");
      out.println("   parser = parserArg;");
    }
    else {
      out.println("/** Constructor. */");
      out.println("public " + state.scannerClass() + "(CharStream stream) {");
    }

    out.println("   charStream = stream;");

    out.println("}");

    if (Options.getScannerUsesParser()) {
      out.println();
      out.println("/** Constructor with parser. */");
      out.println("public " + state.scannerClass() + "(" + state.parserClass() + " parserArg, CharStream stream, int lexState) {");
      out.println("   this(parserArg, stream);");
    }
    else {
      out.println();
      out.println("/** Constructor. */");
      out.println("public " + state.scannerClass() + "(CharStream stream, int lexState) {");
      out.println("   this(stream);");
    }
    out.println("   switchTo(lexState);");
    out.println("}");

    // Method to reinitialize the jjRounds array.
    out.println("private void ReInitRounds() {");
    out.println("   int i;");
    out.println("   jjRound = 0x" + Integer.toHexString(Integer.MIN_VALUE + 1) + ";");
    out.println("   for (i = " + stateSetSize + "; i-- > 0;)");
    out.println("      jjRounds[i] = 0x" + Integer.toHexString(Integer.MIN_VALUE) + ";");
    out.println("}");

    out.println();
    out.println("/** Switch to specified lex state. */");
    out.println("public void switchTo(int lexState) {");
    out.println("   if (lexState < 0 || lexState >= " + lexStateName.length + ")");
    out.println("      throw new IllegalArgumentException(\"Invalid lexical state: \" + lexState + \"\");");
    out.println("   jjLexState = lexState;");
    out.println("}");

    out.println();
  }

  private char maxChar(long l) {
    // Assumes l != 0L
    for (int i = 64; i-- > 0; ) {
      if ((l & (1L << i)) != 0L) {
        return (char) i;
      }
    }

    return 0xffff;
  }

  private void dumpFillToken(IndentingPrintWriter out) {
    out.println("protected Token jjFillToken() {");
    out.indent();
    if (keepImage) {
      out.println("String curTokenImage;");
    }

    if (hasEmptyMatch) {
      out.println("if (jjMatchedPos < 0) {");
      if (keepImage) {
        out.println("   if (image == null)");
        out.println("      curTokenImage = \"\";");
        out.println("   else");
        out.println("      curTokenImage = image.toString();");
      }

      if (keepLineCol) {
        out.println("int beginLine = charStream.getBeginLine();");
        out.println("int beginColumn = charStream.getBeginColumn();");
        out.println("int endLine = charStream.getBeginLine();");
        out.println("int endColumn = charStream.getBeginColumn();");
      }

      out.println("}");
      out.println("else {");
      if (keepImage) {
        out.println("   String literalImage = jjLiteralImages[jjMatchedKind];");
        out.println("   curTokenImage = literalImage == null ? charStream.getImage() : literalImage;");
      }

      if (keepLineCol) {
        out.println("   int beginLine = charStream.getBeginLine();");
        out.println("   int beginColumn = charStream.getBeginColumn();");
        out.println("   int endLine = charStream.getEndLine();");
        out.println("   int endColumn = charStream.getEndColumn();");
      }

      out.println("}");
    }
    else {
      if (keepImage) {
        out.println("String literalImage = jjLiteralImages[jjMatchedKind];");
        out.println("curTokenImage = literalImage == null ? charStream.getImage() : literalImage;");
      }
      if (keepLineCol) {
        out.println("int beginLine = charStream.getBeginLine();");
        out.println("int beginColumn = charStream.getBeginColumn();");
        out.println("int endLine = charStream.getEndLine();");
        out.println("int endColumn = charStream.getEndColumn();");
      }
    }

    if (keepImage) {
      if (Options.getTokenFactory().length() > 0) {
        out.println("Token t = " + Options.getTokenFactory() + ".newToken(jjMatchedKind, curTokenImage);");
      }
      else {
        out.println("Token t = Token.newToken(jjMatchedKind, curTokenImage);");
      }
    }
    else {
      if (Options.getTokenFactory().length() > 0) {
        out.println("Token t = " + Options.getTokenFactory() + ".newToken(jjMatchedKind);");
      }
      else {
        out.println("Token t = Token.newToken(jjMatchedKind);");
      }
    }

    out.println("t.setOffset(charStream.getBeginOffset(), charStream.getEndOffset());");
    if (keepLineCol) {
      out.println("t.setLineColumn(beginLine, beginColumn, endLine, endColumn);");
    }

    out.println("return t;");
    out.unindent();
    out.println("}");
  }

  private void dumpGetNextToken(IndentingPrintWriter out) {
    int i;

    out.println();
    out.println("int jjLexState = " + defaultLexState + ";");
    out.println("int jjNewStateCount;");
    out.println("int jjRound;");
    out.println("int jjMatchedPos;");
    out.println("int jjMatchedKind;");

    out.println();
    out.println("/** Get the next token that is not special. */");
    out.println("public Token getNextToken() throws java.io.IOException {");
    out.indent();
    if (hasSpecial) {
      out.println("Token token = getAnyNextToken();")
          .println("Token specialToken = null;")
          .println("while (isSpecial(token.getKind())) {")
          .indent()
          .println("if (specialToken == null) {")
          .indent()
          .println("specialToken = token;")
          .unindent()
          .println("}")
          .println("else {")
          .indent()
          .println("token.specialToken = specialToken;")
          .println("specialToken = specialToken.next = token;")
          .unindent()
          .println("}")
          .println("token = getAnyNextToken();")
          .unindent()
          .println("}")
          .println("token.specialToken = specialToken;")
          .println("return token;");
    }
    else {
      out.println("return getAnyNextToken();");
    }
    out.unindent();
    out.println("}");

    out.println();
    out.println("/** Get the next normal or special, but not skip token. */");
    out.println("public Token getAnyNextToken() throws java.io.IOException {");
    out.indent();
    out.println("Token token;");
    out.println("int curPos = 0;");
    out.println();
    out.println("loop:\nwhile (true) {");
    out.indent();
    out.println("charStream.beginToken();");
    out.println("jjChar = charStream.readChar();");
    out.println("if (jjChar == -1) {");
    out.indent();

    if (Options.getDebugScanner()) {
      out.println("debugPrinter.println(\"Returning the <EOF> token.\");");
    }

    out.println("jjMatchedKind = 0;");
    out.println("token = jjFillToken();");

    if (state.eofNextState != null || state.eofAction != null) {
      out.println("tokenLexicalActions(token);");
    }

    if (Options.getCommonTokenAction()) {
      out.println("commonTokenAction(token);");
    }

    out.println("return token;");
    out.unindent();
    out.println("}"); // if (jjChar == -1)

    if (hasMoreActions || hasSkipActions || hasTokenActions) {
      if (keepImage) {
        out.println("image = jjImage;");
        out.println("image.setLength(0);");
        out.println("jjImageLength = 0;");
      }
    }

    out.println();

    if (hasMore) {
      out.println("while (true) {");
      out.indent();
    }

    // this also sets up the start state of the nfa
    if (maxLexStates > 1) {
      out.println("switch (jjLexState) {");
      out.indent();
    }

    for (i = 0; i < maxLexStates; i++) {
      if (maxLexStates > 1) {
        out.println("case " + i + ":");
        out.indent();
      }

      if (singlesToSkip[i].hasTransitions()) {
        // added the backup(0) to make JIT happy
        out.println("// added the backup(0) to make JIT happy");
        out.println("charStream.backup(0);");
        if (singlesToSkip[i].asciiMoves[0] != 0L &&
            singlesToSkip[i].asciiMoves[1] != 0L) {
          out.println("while ((jjChar < 64" + " && (0x" +
              Long.toHexString(singlesToSkip[i].asciiMoves[0]) +
              "L & (1L << jjChar)) != 0L) || \n" +
              "          (jjChar >> 6) == 1" +
              " && (0x" +
              Long.toHexString(singlesToSkip[i].asciiMoves[1]) +
              "L & (1L << (jjChar & 077))) != 0L)");
        }
        else if (singlesToSkip[i].asciiMoves[1] == 0L) {
          out.println("while (jjChar <= " +
              (int) maxChar(singlesToSkip[i].asciiMoves[0]) + " && (0x" +
              Long.toHexString(singlesToSkip[i].asciiMoves[0]) +
              "L & (1L << jjChar)) != 0L)");
        }
        else if (singlesToSkip[i].asciiMoves[0] == 0L) {
          out.println("while (jjChar > 63 && jjChar <= " +
              ((int) maxChar(singlesToSkip[i].asciiMoves[1]) + 64) +
              " && (0x" +
              Long.toHexString(singlesToSkip[i].asciiMoves[1]) +
              "L & (1L << (jjChar & 077))) != 0L)");
        }

        out.println("{");
        out.indent();
        if (Options.getDebugScanner()) {
          out.println("debugPrinter.println(" +
              (maxLexStates > 1 ?
                  "\"<\" + jjLexStateNames[jjLexState] + \">\" + " : "") +
              "\"Skipping character : \" + " +
              "ScannerException.escape(String.valueOf(jjChar)) + \" (\" + jjChar + \")\");");
        }
        out.println("charStream.beginToken();");
        out.println("jjChar = charStream.readChar();");
        out.println("if (jjChar == -1) { continue loop; }");
        out.unindent();
        out.println("}");
      }

      if (initMatch[i] != Integer.MAX_VALUE && initMatch[i] != 0) {
        if (Options.getDebugScanner()) {
          out.println("debugPrinter.println(\"   Matched the empty string as \" + tokenImage[" +
              initMatch[i] + "] + \" token.\");");
        }

        out.println("jjMatchedKind = " + initMatch[i] + ";");
        out.println("jjMatchedPos = -1;");
        out.println("curPos = 0;");
      }
      else {
        out.println("jjMatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
        out.println("jjMatchedPos = 0;");
      }

      if (Options.getDebugScanner()) {
        out.println("debugPrinter.println(" +
            (maxLexStates > 1 ? "\"<\" + jjLexStateNames[jjLexState] + \">\" + " : "") +
            "\"Current character : \" + " +
            "ScannerException.escape(String.valueOf(jjChar)) + \" (\" + jjChar + \") " +
            "at line \" + charStream.getEndLine() + \" column \" + charStream.getEndColumn());");
      }

      out.println("curPos = jjMoveStringLiteralDfa0_" + i + "();");

      if (canMatchAnyChar[i] != -1) {
        if (initMatch[i] != Integer.MAX_VALUE && initMatch[i] != 0) {
          out.println("if (jjMatchedPos < 0 || (jjMatchedPos == 0 && jjMatchedKind > " +
              canMatchAnyChar[i] + "))");
        }
        else {
          out.println("if (jjMatchedPos == 0 && jjMatchedKind > " +
              canMatchAnyChar[i] + ")");
        }
        out.println("{");
        out.indent();

        if (Options.getDebugScanner()) {
          out.println("debugPrinter.println(\"   Current character matched as a \" + tokenImage[" +
              canMatchAnyChar[i] + "] + \" token.\");");
        }
        out.println("jjMatchedKind = " + canMatchAnyChar[i] + ";");

        if (initMatch[i] != Integer.MAX_VALUE && initMatch[i] != 0) {
          out.println("jjMatchedPos = 0;");
        }

        out.unindent();
        out.println("}");
      }

      if (maxLexStates > 1) {
        out.println("break;");
        out.unindent();
      }
    }

    if (maxLexStates > 1) {
      out.unindent();
      out.println("}");
    }
    else if (maxLexStates == 0) {
      out.println("jjMatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
    }

    if (maxLexStates > 1) {
      out.unindent();
    }

    out.indent();

    if (maxLexStates > 0) {
      out.println("if (jjMatchedKind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ") {");
      out.indent();
      out.println("if (jjMatchedPos + 1 < curPos)");

      if (Options.getDebugScanner()) {
        out.println("{");
        out.println("debugPrinter.println(" +
            "\"   Putting back \" + (curPos - jjMatchedPos - 1) + \" characters into the input stream.\");");
      }

      out.println("charStream.backup(curPos - jjMatchedPos - 1);");

      if (Options.getDebugScanner()) {
        out.println("}");
      }

      if (Options.getDebugScanner()) {
        out.println("debugPrinter.println(" +
            "\"****** FOUND A \" + tokenImage[jjMatchedKind] + \" MATCH " +
            "(\" + ScannerException.escape(new String(charStream.getSuffix(jjMatchedPos + 1))) + " +
            "\") ******\\n\");");
      }

      if (hasSkip || hasMore || hasSpecial) {
        out.println("if (isToken(jjMatchedKind)) {");
        out.indent();
      }

      out.println("token = jjFillToken();");

      if (hasTokenActions) {
        out.println("tokenLexicalActions(token);");
      }

      if (maxLexStates > 1) {
        out.println("if (jjNewLexState[jjMatchedKind] != -1)")
            .indent()
            .println("jjLexState = jjNewLexState[jjMatchedKind];")
            .unindent();
      }

      if (Options.getCommonTokenAction()) {
        out.println("commonTokenAction(token);");
      }

      out.println("return token;");

      if (hasSkip || hasMore || hasSpecial) {
        out.unindent();
        out.println("}");

        if (hasSkip || hasSpecial) {
          if (hasMore) {
            out.println("else if (isSkip(jjMatchedKind))");
          }
          else {
            out.println("else");
          }

          out.println("{");
          out.indent();

          if (hasSpecial) {
            out.println("if (isSpecial(jjMatchedKind)) {");
            out.indent();
            out.println("token = jjFillToken();");

            if (hasSkipActions) {
              out.println("skipLexicalActions(token);");
            }

            if (maxLexStates > 1) {
              out.println("if (jjNewLexState[jjMatchedKind] != -1)")
                  .indent()
                  .println("jjLexState = jjNewLexState[jjMatchedKind];")
                  .unindent();
            }

            out.println("return token;");
            out.unindent();
            out.println("}");

            if (hasSkipActions) {
              out.println("skipLexicalActions(null);");
            }
          }
          else if (hasSkipActions) {
            out.println("skipLexicalActions(null);");
          }

          if (maxLexStates > 1) {
            out.println("if (jjNewLexState[jjMatchedKind] != -1)")
                .indent()
                .println("jjLexState = jjNewLexState[jjMatchedKind];")
                .unindent();
          }

          out.println("continue loop;");

          out.unindent();
          out.println("}");
        }

        if (hasMore) {
          if (hasMoreActions) {
            out.println("moreLexicalActions();");
          }
          else if (hasSkipActions || hasTokenActions) {
            if (keepImage) {
              out.println("jjImageLength += jjMatchedPos + 1;");
            }
          }

          if (maxLexStates > 1) {
            out.println("if (jjNewLexState[jjMatchedKind] != -1)")
                .indent()
                .println("jjLexState = jjNewLexState[jjMatchedKind];")
                .unindent();
          }
          out.println("curPos = 0;");
          out.println("jjMatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");

          out.println("jjChar = charStream.readChar();");
          out.println("if (jjChar != -1) {");
          out.indent();
          if (Options.getDebugScanner()) {
            out.println("debugPrinter.println(" +
                (maxLexStates > 1 ? "\"<\" + jjLexStateNames[jjLexState] + \">\" + " : "") +
                "\"Current character : \" + " +
                "ScannerException.escape(String.valueOf(jjChar)) + \" (\" + jjChar + \") " +
                "at line \" + charStream.getEndLine() + \" column \" + charStream.getEndColumn());");
          }
          out.println("continue;");
          out.unindent();
          out.println("}");
        }
      }
      out.unindent();
      out.println("}");

      out.println("reportLexicalError(curPos);");
    }

    if (hasMore) {
      out.unindent();
      out.println("}");
    }

    out.unindent();
    out.println("}");

    out.unindent();
    out.println("}");

    if (hasMore || hasSkip || hasSpecial) {
      out.println()
          .println("/**")
          .println(" * Verify whether the specified is a normal token kind.")
          .println(" *")
          .println(" * @param kind A token kind.")
          .println(" * @return <code>true</code> if a normal token, </code>false</code> otherwise.")
          .println(" */")
          .println("public static boolean isToken(int kind) {")
          .indent()
          .println("return (jjToToken[kind >> 6] & (1L << (kind & 077))) != 0L;")
          .unindent()
          .println("}");
    }

    if (hasSkip || hasSpecial) {
      out.println()
          .println("/**")
          .println(" * Verify whether to ignore the specified token.")
          .println(" *")
          .println(" * @param kind A token kind.")
          .println(" * @return <code>true</code> if ignore token, </code>false</code> otherwise.")
          .println(" */")
          .println("public static boolean isSkip(int kind) {")
          .indent()
          .println("return (jjToSkip[kind >> 6] & (1L << (kind & 077))) != 0L;")
          .unindent()
          .println("}");
    }

    if (hasSpecial) {
      out.println()
          .println("/**")
          .println(" * Verify whether to ignore the specified special token.")
          .println(" *")
          .println(" * @param kind A token kind.")
          .println(" * @return <code>true</code> if ignore special token, </code>false</code> otherwise.")
          .println(" */")
          .println("public static boolean isSpecial(int kind) {")
          .indent()
          .println("return (jjToSpecial[kind >> 6] & (1L << (kind & 077))) != 0L;")
          .unindent()
          .println("}");
    }

    out.println();
    out.println("void reportLexicalError(int curPos) throws java.io.IOException {");
    out.println("   throw new ScannerException(jjLexState,");
    out.println("        ScannerException.LEXICAL_ERROR,");
    if (keepLineCol) {
      out.println("        charStream.getEndLine(),");
      out.println("        charStream.getEndColumn(),");
    }
    if (keepImage) {
      out.println("        charStream.getImage(),");
    }
    out.println("        jjChar);");

    out.println("}");
    out.println();
  }

  public void dumpSkipActions(IndentingPrintWriter out)
      throws IOException {
    Action act;

    out.println("void skipLexicalActions(Token matchedToken) {");
    out.println("   switch(jjMatchedKind) {");

    Outer:
    for (int i = 0; i < maxOrdinal; i++) {
      if ((toSkip[i / 64] & (1L << (i % 64))) == 0L) {
        continue;
      }

      for (; ; ) {
        if (((act = actions[i]) == null ||
            act.getActionTokens() == null ||
            act.getActionTokens().size() == 0) && !canLoop[lexStates[i]]) {
          continue Outer;
        }

        out.println("      case " + i + " :");

        if (initMatch[lexStates[i]] == i && canLoop[lexStates[i]]) {
          out.println("         if (jjMatchedPos == -1) {");
          out.println("            if (jjBeenHere[" + lexStates[i] + "] &&");
          out.println("                jjEmptyLineNo[" + lexStates[i] + "] == charStream.getBeginLine() &&");
          out.println("                jjEmptyColumnNo[" + lexStates[i] + "] == charStream.getBeginColumn())");
          out.println("               throw new ScannerException(" +
              "(\"Bailing out of infinite loop caused by repeated empty string matches " +
              "at line \" + charStream.getBeginLine() + \", " +
              "column \" + charStream.getBeginColumn() + \".\"), ScannerException.LOOP_DETECTED);");
          out.println("            jjEmptyLineNo[" + lexStates[i] + "] = charStream.getBeginLine();");
          out.println("            jjEmptyColumnNo[" + lexStates[i] + "] = charStream.getBeginColumn();");
          out.println("            jjBeenHere[" + lexStates[i] + "] = true;");
          out.println("         }");
        }

        if ((act = actions[i]) == null ||
            act.getActionTokens().size() == 0) {
          break;
        }

        if (keepImage) {
          out.print("         image.append");
          if (stringLiterals.allImages[i] != null) {
            out.println("(jjLiteralImages[" + i + "]);");
            out.println("        lengthOfMatch = jjLiteralImages[" + i + "].length();");
          }
          else {
            out.println("(charStream.getSuffix(jjImageLength + (lengthOfMatch = jjMatchedPos + 1)));");
          }
        }

        TokenPrinter.printTokenSetup(act.getActionTokens().get(0));
        TokenPrinter.cCol = 1;

        for (int j = 0; j < act.getActionTokens().size(); j++) {
          TokenPrinter.printToken(act.getActionTokens().get(j), out);
        }
        out.println();

        break;
      }

      out.println("         break;");
    }

    out.println("      default:");
    out.println("         break;");
    out.println("   }");
    out.println("}");
  }

  public void dumpMoreActions(IndentingPrintWriter out)
      throws IOException {
    Action act;

    out.println("void moreLexicalActions() {");
    if (keepImage) {
      out.println("   jjImageLength += (lengthOfMatch = jjMatchedPos + 1);");
    }
    out.println("   switch(jjMatchedKind) {");

    Outer:
    for (int i = 0; i < maxOrdinal; i++) {
      if ((toMore[i / 64] & (1L << (i % 64))) == 0L) {
        continue;
      }

      for (; ; ) {
        if (((act = actions[i]) == null ||
            act.getActionTokens() == null ||
            act.getActionTokens().size() == 0) && !canLoop[lexStates[i]]) {
          continue Outer;
        }

        out.println("      case " + i + " :");

        if (initMatch[lexStates[i]] == i && canLoop[lexStates[i]]) {
          out.println("         if (jjMatchedPos == -1) {");
          out.println("            if (jjBeenHere[" + lexStates[i] + "] &&");
          out.println("                jjEmptyLineNo[" + lexStates[i] + "] == charStream.getBeginLine() &&");
          out.println("                jjEmptyColumnNo[" + lexStates[i] + "] == charStream.getBeginColumn())");
          out.println("               throw new ScannerException(" +
              "(\"Bailing out of infinite loop caused by repeated empty string matches " +
              "at line \" + charStream.getBeginLine() + \", " +
              "column \" + charStream.getBeginColumn() + \".\"), ScannerException.LOOP_DETECTED);");
          out.println("            jjEmptyLineNo[" + lexStates[i] + "] = charStream.getBeginLine();");
          out.println("            jjEmptyColumnNo[" + lexStates[i] + "] = charStream.getBeginColumn();");
          out.println("            jjBeenHere[" + lexStates[i] + "] = true;");
          out.println("         }");
        }

        if ((act = actions[i]) == null ||
            act.getActionTokens().size() == 0) {
          break;
        }

        if (keepImage) {
          out.print("         image.append");
          if (stringLiterals.allImages[i] != null) {
            out.println("(jjLiteralImages[" + i + "]);");
          }
          else {
            out.println("(charStream.getSuffix(jjImageLength));");
          }
        }

        out.println("         jjImageLength = 0;");
        TokenPrinter.printTokenSetup(act.getActionTokens().get(0));
        TokenPrinter.cCol = 1;

        for (int j = 0; j < act.getActionTokens().size(); j++) {
          TokenPrinter.printToken(act.getActionTokens().get(j), out);
        }
        out.println();

        break;
      }

      out.println("         break;");
    }

    out.println("      default:");
    out.println("         break;");

    out.println("   }");
    out.println("}");
  }

  public void dumpTokenActions(IndentingPrintWriter out)
      throws IOException {
    Action act;
    int i;

    out.println("void tokenLexicalActions(Token matchedToken) {");
    out.println("   switch(jjMatchedKind) {");

    Outer:
    for (i = 0; i < maxOrdinal; i++) {
      if ((toToken[i / 64] & (1L << (i % 64))) == 0L) {
        continue;
      }

      for (; ; ) {
        if (((act = actions[i]) == null ||
            act.getActionTokens() == null ||
            act.getActionTokens().size() == 0) && !canLoop[lexStates[i]]) {
          continue Outer;
        }

        out.println("      case " + i + " :");

        if (initMatch[lexStates[i]] == i && canLoop[lexStates[i]]) {
          out.println("         if (jjMatchedPos == -1) {");
          out.println("            if (jjBeenHere[" + lexStates[i] + "] &&");
          out.println("                jjEmptyLineNo[" + lexStates[i] + "] == charStream.getBeginLine() &&");
          out.println("                jjEmptyColumnNo[" + lexStates[i] + "] == charStream.getBeginColumn())");
          out.println("               throw new ScannerException(" +
              "(\"Bailing out of infinite loop caused by repeated empty string matches " +
              "at line \" + charStream.getBeginLine() + \", " +
              "column \" + charStream.getBeginColumn() + \".\"), ScannerException.LOOP_DETECTED);");
          out.println("            jjEmptyLineNo[" + lexStates[i] + "] = charStream.getBeginLine();");
          out.println("            jjEmptyColumnNo[" + lexStates[i] + "] = charStream.getBeginColumn();");
          out.println("            jjBeenHere[" + lexStates[i] + "] = true;");
          out.println("         }");
        }

        if ((act = actions[i]) == null
            || act.getActionTokens().size() == 0) {
          break;
        }

        if (keepImage) {
          if (i == 0) {
            out.println("      image.setLength(0);"); // For EOF no image is there
          }
          else {
            out.print("        image.append");

            if (stringLiterals.allImages[i] != null) {
              out.println("(jjLiteralImages[" + i + "]);");
              out.println("        lengthOfMatch = jjLiteralImages[" + i + "].length();");
            }
            else {
              out.print("        image.append");

              if (stringLiterals.allImages[i] != null) {
                out.println("(jjLiteralImages[" + i + "]);");
                out.println("        lengthOfMatch = jjLiteralImages[" + i + "].length();");
              }
              else {
                out.println("(charStream.getSuffix(jjImageLength + (lengthOfMatch = jjMatchedPos + 1)));");
              }
            }
          }
        }

        TokenPrinter.printTokenSetup(act.getActionTokens().get(0));
        TokenPrinter.cCol = 1;

        for (int j = 0; j < act.getActionTokens().size(); j++) {
          TokenPrinter.printToken(act.getActionTokens().get(j), out);
        }
        out.println();

        break;
      }

      out.println("         break;");
    }

    out.println("      default:");
    out.println("         break;");
    out.println("   }");
    out.println("}");
  }
}
