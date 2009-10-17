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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/** Generate lexer. */
public class LexGen extends JavaCCGlobals implements JavaCCParserConstants {
  static private IndentingPrintWriter ostr;
  static private String tokMgrClassName;

  // Hashtable of vectors
  static Hashtable allTpsForState = new Hashtable();
  public static int lexStateIndex = 0;
  static int[] kinds;
  public static int maxOrdinal = 1;
  public static String lexStateSuffix;
  static String[] newLexState;
  public static int[] lexStates;
  public static boolean[] ignoreCase;
  public static Action[] actions;
  public static Hashtable initStates = new Hashtable();
  public static int stateSetSize;
  public static int maxLexStates;
  public static String[] lexStateName;
  static NfaState[] singlesToSkip;
  public static long[] toSkip;
  public static long[] toSpecial;
  public static long[] toMore;
  public static long[] toToken;
  public static int defaultLexState;
  public static RegularExpression[] rexprs;
  public static int[] maxLongsReqd;
  public static int[] initMatch;
  public static int[] canMatchAnyChar;
  public static boolean hasEmptyMatch;
  public static boolean[] canLoop;
  public static boolean[] stateHasActions;
  public static boolean hasLoop = false;
  public static boolean[] canReachOnMore;
  public static boolean[] hasNfa;
  public static boolean[] mixed;
  public static NfaState initialState;
  public static int curKind;
  static boolean hasSkipActions = false;
  static boolean hasMoreActions = false;
  static boolean hasTokenActions = false;
  static boolean hasSpecial = false;
  static boolean hasSkip = false;
  static boolean hasMore = false;
  public static RegularExpression curRE;
  public static boolean keepLineCol;
  public static boolean keepImage;

  static void PrintClassHead() {
    int i, j;

    try {
      File tmp = new File(Options.getOutputDirectory(), tokMgrClassName + ".java");
      ostr = new IndentingPrintWriter(
          new java.io.BufferedWriter(
              new java.io.FileWriter(tmp),
              8092
          )
      );
      List tn = new ArrayList(toolNames);
      tn.add(toolName);

      ostr.println("/* " + getIdString(tn, tokMgrClassName + ".java") + " */");

      int l = 0, kind;
      i = 1;
      for (; ;) {
        if (cu_to_insertion_point_1.size() <= l) {
          break;
        }

        kind = ((Token) cu_to_insertion_point_1.get(l)).getKind();
        if (kind == PACKAGE || kind == IMPORT) {
          for (; i < cu_to_insertion_point_1.size(); i++) {
            kind = ((Token) cu_to_insertion_point_1.get(i)).getKind();
            if (kind == SEMICOLON ||
                kind == ABSTRACT ||
                kind == FINAL ||
                kind == PUBLIC ||
                kind == CLASS ||
                kind == INTERFACE) {
              cline = ((Token) (cu_to_insertion_point_1.get(l))).getBeginLine();
              ccol = ((Token) (cu_to_insertion_point_1.get(l))).getBeginColumn();
              for (j = l; j < i; j++) {
                printToken((Token) (cu_to_insertion_point_1.get(j)), ostr);
              }
              if (kind == SEMICOLON) {
                printToken((Token) (cu_to_insertion_point_1.get(j)), ostr);
              }
              ostr.println();
              break;
            }
          }
          l = ++i;
        }
        else {
          break;
        }
      }

      ostr.println();
      ostr.println("/** Token Manager. */");
      ostr.println("@SuppressWarnings(\"unused\")");
      if (Options.getSupportClassVisibilityPublic()) {
        ostr.print("public ");
      }
      ostr.println("class " + tokMgrClassName + " implements TokenManager, " +
          cu_name + "Constants");
      ostr.println("{"); // }
    }
    catch (java.io.IOException err) {
      JavaCCErrors.semantic_error("Could not create file : " + tokMgrClassName + ".java\n");
      throw new Error();
    }

    if (token_mgr_decls != null && token_mgr_decls.size() > 0) {
      boolean commonTokenActionSeen = false;
      boolean commonTokenActionNeeded = Options.getCommonTokenAction();

      printTokenSetup((Token) token_mgr_decls.get(0));
      ccol = 1;

      for (j = 0; j < token_mgr_decls.size(); j++) {
        Token t = (Token) token_mgr_decls.get(j);
        if (t.getKind() == IDENTIFIER &&
            commonTokenActionNeeded &&
            !commonTokenActionSeen) {
          commonTokenActionSeen = t.getImage().equals("commonTokenAction");
        }

        printToken(t, ostr);
      }

      ostr.println();
      if (commonTokenActionNeeded && !commonTokenActionSeen) {
        JavaCCErrors.warning("You have the COMMON_TOKEN_ACTION option set. " +
            "But it appears you have not defined the method :\n" +
            "      void commonTokenAction(Token t)\n" +
            "in your TOKEN_MGR_DECLS. The generated token manager will not compile.");
      }
    }
    else if (Options.getCommonTokenAction()) {
      JavaCCErrors.warning("You have the COMMON_TOKEN_ACTION option set. " +
          "But you have not defined the method :\n" +
          "      void commonTokenAction(Token t)\n" +
          "in your TOKEN_MGR_DECLS. The generated token manager will not compile.");
    }

    ostr.indent();

    ostr.println();
    if (Options.getDebugTokenManager()) {
      ostr.println("/** Debug output. */");
      ostr.println("private java.io.PrintWriter debugPrinter = new java.io.PrintWriter(System.out);");
      ostr.println();
      ostr.println("/** Set debug output. */");
      ostr.println("public  void setDebugPrinter(java.io.PrintWriter printer) { debugPrinter = printer; }");
      ostr.println();
    }

    if (Options.getTokenManagerUsesParser()) {
      ostr.println();
      ostr.println("/** The parser. */");
      ostr.println("public " + cu_name + " parser = null;");
    }
  }

  static void DumpDebugMethods() {

    ostr.println("   int kindCnt = 0;");
    ostr.println("  protected  final String jjKindsForBitVector(int i, long vec)");
    ostr.println("  {");
    ostr.println("    String retVal = \"\";");
    ostr.println("    if (i == 0)");
    ostr.println("       kindCnt = 0;");
    ostr.println("    for (int j = 0; j < 64; j++)");
    ostr.println("    {");
    ostr.println("       if ((vec & (1L << j)) != 0L)");
    ostr.println("       {");
    ostr.println("          if (kindCnt++ > 0)");
    ostr.println("             retVal += \", \";");
    ostr.println("          if (kindCnt % 5 == 0)");
    ostr.println("             retVal += \"\\n     \";");
    ostr.println("          retVal += tokenImage[i * 64 + j];");
    ostr.println("       }");
    ostr.println("    }");
    ostr.println("    return retVal;");
    ostr.println("  }");
    ostr.println();

    ostr.println("  protected  final String jjKindsForStateVector(" +
        "int lexState, int[] vec, int start, int end)");
    ostr.println("  {");
    ostr.println("    boolean[] kindDone = new boolean[" + maxOrdinal + "];");
    ostr.println("    String retVal = \"\";");
    ostr.println("    int cnt = 0;");
    ostr.println("    for (int i = start; i < end; i++)");
    ostr.println("    {");
    ostr.println("     if (vec[i] == -1)");
    ostr.println("       continue;");
    ostr.println("     int[] stateSet = statesForState[jjLexState][vec[i]];");
    ostr.println("     for (int j = 0; j < stateSet.length; j++)");
    ostr.println("     {");
    ostr.println("       int state = stateSet[j];");
    ostr.println("       if (!kindDone[kindForState[lexState][state]])");
    ostr.println("       {");
    ostr.println("          kindDone[kindForState[lexState][state]] = true;");
    ostr.println("          if (cnt++ > 0)");
    ostr.println("             retVal += \", \";");
    ostr.println("          if (cnt % 5 == 0)");
    ostr.println("             retVal += \"\\n     \";");
    ostr.println("          retVal += tokenImage[kindForState[lexState][state]];");
    ostr.println("       }");
    ostr.println("     }");
    ostr.println("    }");
    ostr.println("    if (cnt == 0)");
    ostr.println("       return \"{  }\";");
    ostr.println("    else");
    ostr.println("       return \"{ \" + retVal + \" }\";");
    ostr.println("  }");
    ostr.println();
  }

  static void BuildLexStatesTable() {
    Iterator it = rexprlist.iterator();
    TokenProduction tp;
    int i;

    String[] tmpLexStateName = new String[lexstate_I2S.size()];
    while (it.hasNext()) {
      tp = (TokenProduction) it.next();
      List respecs = tp.respecs;
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
        if (maxOrdinal <= (re = ((RegExprSpec) respecs.get(i)).rexp).ordinal) {
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
    actions[0] = actForEof;
    hasTokenActions = actForEof != null;
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
    newLexState[0] = nextStateForEof;
    hasEmptyMatch = false;
    lexStates = new int[maxOrdinal];
    ignoreCase = new boolean[maxOrdinal];
    rexprs = new RegularExpression[maxOrdinal];
    RStringLiteral.allImages = new String[maxOrdinal];
    canReachOnMore = new boolean[maxLexStates];
  }

  static int GetIndex(String name) {
    for (int i = 0; i < lexStateName.length; i++) {
      if (lexStateName[i] != null && lexStateName[i].equals(name)) {
        return i;
      }
    }

    throw new Error(); // Should never come here
  }

  public static void AddCharToSkip(char c, int kind) {
    singlesToSkip[lexStateIndex].AddChar(c);
    singlesToSkip[lexStateIndex].kind = kind;
  }

  public static void start() {
    if (!Options.getBuildTokenManager() ||
        Options.getUserTokenManager() ||
        JavaCCErrors.get_error_count() > 0) {
      return;
    }

    keepLineCol = Options.getKeepLineColumn();
    keepImage = Options.getKeepImage();
    List choices = new ArrayList();
    Enumeration e;
    TokenProduction tp;
    int i, j;

    tokMgrClassName = cu_name + "TokenManager";

    PrintClassHead();
    BuildLexStatesTable();

    e = allTpsForState.keys();

    boolean ignoring = false;

    while (e.hasMoreElements()) {
      NfaState.ReInit();
      RStringLiteral.ReInit();

      String key = (String) e.nextElement();

      lexStateIndex = GetIndex(key);
      lexStateSuffix = "_" + lexStateIndex;
      List allTps = (List) allTpsForState.get(key);
      initStates.put(key, initialState = new NfaState());
      ignoring = false;

      singlesToSkip[lexStateIndex] = new NfaState();
      singlesToSkip[lexStateIndex].dummy = true;

      if (key.equals("DEFAULT")) {
        defaultLexState = lexStateIndex;
      }

      for (i = 0; i < allTps.size(); i++) {
        tp = (TokenProduction) allTps.get(i);
        int kind = tp.kind;
        boolean ignore = tp.ignoreCase;
        List rexps = tp.respecs;

        if (i == 0) {
          ignoring = ignore;
        }

        for (j = 0; j < rexps.size(); j++) {
          RegExprSpec respec = (RegExprSpec) rexps.get(j);
          curRE = respec.rexp;

          rexprs[curKind = curRE.ordinal] = curRE;
          lexStates[curRE.ordinal] = lexStateIndex;
          ignoreCase[curRE.ordinal] = ignore;

          if (curRE.private_rexp) {
            kinds[curRE.ordinal] = -1;
            continue;
          }

          if (curRE instanceof RStringLiteral &&
              !((RStringLiteral) curRE).image.equals("")) {
            ((RStringLiteral) curRE).GenerateDfa(ostr, curRE.ordinal);
            if (i != 0 && !mixed[lexStateIndex] && ignoring != ignore) {
              mixed[lexStateIndex] = true;
            }
          }
          else if (curRE.CanMatchAnyChar()) {
            if (canMatchAnyChar[lexStateIndex] == -1 ||
                canMatchAnyChar[lexStateIndex] > curRE.ordinal) {
              canMatchAnyChar[lexStateIndex] = curRE.ordinal;
            }
          }
          else {
            Nfa temp;

            if (curRE instanceof RChoice) {
              choices.add(curRE);
            }

            temp = curRE.GenerateNfa(ignore);
            temp.end.isFinal = true;
            temp.end.kind = curRE.ordinal;
            initialState.AddMove(temp.start);
          }

          if (kinds.length < curRE.ordinal) {
            int[] tmp = new int[curRE.ordinal + 1];

            System.arraycopy(kinds, 0, tmp, 0, kinds.length);
            kinds = tmp;
          }
          //System.out.println("   ordina : " + curRE.ordinal);

          kinds[curRE.ordinal] = kind;

          if (respec.nextState != null &&
              !respec.nextState.equals(lexStateName[lexStateIndex])) {
            newLexState[curRE.ordinal] = respec.nextState;
          }

          if (respec.act != null && respec.act.getActionTokens() != null &&
              respec.act.getActionTokens().size() > 0) {
            actions[curRE.ordinal] = respec.act;
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
                canReachOnMore[GetIndex(newLexState[curRE.ordinal])] = true;
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
      NfaState.ComputeClosures();

      for (i = 0; i < initialState.epsilonMoves.size(); i++) {
        ((NfaState) initialState.epsilonMoves.elementAt(i)).GenerateCode();
      }

      if (hasNfa[lexStateIndex] = (NfaState.generatedStates != 0)) {
        initialState.GenerateCode();
        initialState.GenerateInitMoves(ostr);
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

      RStringLiteral.FillSubString();

      if (hasNfa[lexStateIndex] && !mixed[lexStateIndex]) {
        RStringLiteral.GenerateNfaStartStates(ostr, initialState);
      }

      RStringLiteral.DumpDfaCode(ostr);

      if (hasNfa[lexStateIndex]) {
        NfaState.DumpMoveNfa(ostr);
      }

      if (stateSetSize < NfaState.generatedStates) {
        stateSetSize = NfaState.generatedStates;
      }
    }

    for (i = 0; i < choices.size(); i++) {
      ((RChoice) choices.get(i)).CheckUnmatchability();
    }

    NfaState.DumpStateSets(ostr);
    CheckEmptyStringMatch();
    NfaState.DumpNonAsciiMoveMethods(ostr);
    RStringLiteral.DumpStrLiteralImages(ostr);
    DumpStaticVarDeclarations();
    DumpFillToken();
    DumpGetNextToken();

    if (Options.getDebugTokenManager()) {
      NfaState.DumpStatesForKind(ostr);
      DumpDebugMethods();
    }

    if (hasLoop) {
      ostr.println("int[] jjEmptyLineNo = new int[" + maxLexStates + "];");
      ostr.println("int[] jjEmptyColumnNo = new int[" + maxLexStates + "];");
      ostr.println("boolean[] jjBeenHere = new boolean[" + maxLexStates + "];");
    }

    if (hasSkipActions) {
      DumpSkipActions();
    }
    if (hasMoreActions) {
      DumpMoreActions();
    }
    if (hasTokenActions) {
      DumpTokenActions();
    }

    NfaState.PrintBoilerPlate(ostr);
    ostr.unindent();
    ostr.println(/*{*/ "}");
    ostr.close();
  }

  static void CheckEmptyStringMatch() {
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
        if (seen[j = GetIndex(newLexState[initMatch[j]])]) {
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

  static void PrintArrayInitializer(int noElems) {
    ostr.print("{");
    for (int i = 0; i < noElems; i++) {
      if (i % 25 == 0) {
        ostr.print("\n   ");
      }
      ostr.print("0, ");
    }
    ostr.println("\n};");
  }

  static void DumpStaticVarDeclarations() {
    int i;

    ostr.println();
    ostr.println("/** Lexer state names. */");
    ostr.println("public static final String[] jjLexStateNames = {");
    ostr.indent();
    for (i = 0; i < maxLexStates; i++) {
      ostr.println("\"" + lexStateName[i] + "\",");
    }
    ostr.unindent();
    ostr.println("};");

    if (maxLexStates > 1) {
      ostr.println();
      ostr.println("/** Lex State array. */");
      ostr.print("public static final int[] jjNewLexState = {");
      ostr.indent();
      for (i = 0; i < maxOrdinal; i++) {
        if (i % 25 == 0) {
          ostr.print("\n");
        }

        if (newLexState[i] == null) {
          ostr.print("-1, ");
        }
        else {
          ostr.print(GetIndex(newLexState[i]) + ", ");
        }
      }
      ostr.unindent();
      ostr.println("\n};");
    }

    if (hasSkip || hasMore || hasSpecial) {
      // Bit vector for TOKEN
      ostr.print("static final long[] jjToToken = {");
      for (i = 0; i < maxOrdinal / 64 + 1; i++) {
        if (i % 4 == 0) {
          ostr.print("\n   ");
        }
        ostr.print("0x" + Long.toHexString(toToken[i]) + "L, ");
      }
      ostr.println("\n};");
    }

    if (hasSkip || hasSpecial) {
      // Bit vector for SKIP
      ostr.print("static final long[] jjToSkip = {");
      for (i = 0; i < maxOrdinal / 64 + 1; i++) {
        if (i % 4 == 0) {
          ostr.print("\n   ");
        }
        ostr.print("0x" + Long.toHexString(toSkip[i]) + "L, ");
      }
      ostr.println("\n};");
    }

    if (hasSpecial) {
      // Bit vector for SPECIAL
      ostr.print("static final long[] jjToSpecial = {");
      for (i = 0; i < maxOrdinal / 64 + 1; i++) {
        if (i % 4 == 0) {
          ostr.print("\n   ");
        }
        ostr.print("0x" + Long.toHexString(toSpecial[i]) + "L, ");
      }
      ostr.println("\n};");
    }

    if (hasMore) {
      // Bit vector for MORE
      ostr.print("static final long[] jjToMore = {");
      for (i = 0; i < maxOrdinal / 64 + 1; i++) {
        if (i % 4 == 0) {
          ostr.print("\n   ");
        }
        ostr.print("0x" + Long.toHexString(toMore[i]) + "L, ");
      }
      ostr.println("\n};");
    }

    ostr.println("protected final CharStream charStream;");

    ostr.println("private final int[] jjRounds = " +
        "new int[" + stateSetSize + "];");
    ostr.println("private final int[] jjStateSet = " +
        "new int[" + (2 * stateSetSize) + "];");

    if (hasMoreActions || hasSkipActions || hasTokenActions) {
      if (keepImage) {
        ostr.println("private final " + Options.stringBufOrBuild() + " jjImage = new " + Options.stringBufOrBuild() + "();");
        ostr.println("private " + Options.stringBufOrBuild() + " image = jjImage;");
        ostr.println("private int jjImageLength;");
      }
      ostr.println("private int lengthOfMatch;");
    }

    ostr.println("protected int jjChar;");

    if (Options.getTokenManagerUsesParser()) {
      ostr.println();
      ostr.println("/** Constructor with parser. */");
      ostr.println("public " + tokMgrClassName + "(" + cu_name + " parserArg, CharStream stream){");
      ostr.println("   parser = parserArg;");
    }
    else {
      ostr.println("/** Constructor. */");
      ostr.println("public " + tokMgrClassName + "(CharStream stream){");
    }

    ostr.println("   charStream = stream;");

    ostr.println("}");

    if (Options.getTokenManagerUsesParser()) {
      ostr.println();
      ostr.println("/** Constructor with parser. */");
      ostr.println("public " + tokMgrClassName + "(" + cu_name + " parserArg, CharStream stream, int lexState){");
      ostr.println("   this(parserArg, stream);");
    }
    else {
      ostr.println();
      ostr.println("/** Constructor. */");
      ostr.println("public " + tokMgrClassName + "(CharStream stream, int lexState){");
      ostr.println("   this(stream);");
    }
    ostr.println("   switchTo(lexState);");
    ostr.println("}");

    // Method to reinitialize the jjRounds array.
    ostr.println("private void ReInitRounds()");
    ostr.println("{");
    ostr.println("   int i;");
    ostr.println("   jjRound = 0x" + Integer.toHexString(Integer.MIN_VALUE + 1) + ";");
    ostr.println("   for (i = " + stateSetSize + "; i-- > 0;)");
    ostr.println("      jjRounds[i] = 0x" + Integer.toHexString(Integer.MIN_VALUE) + ";");
    ostr.println("}");

    ostr.println();
    ostr.println("/** Switch to specified lex state. */");
    ostr.println("public void switchTo(int lexState)");
    ostr.println("{");
    ostr.println("   if (lexState >= " + lexStateName.length + " || lexState < 0)");
    ostr.println("      throw new TokenManagerError(\"Error: Ignoring invalid lexical state : \"" +
        " + lexState + \". State unchanged.\", TokenManagerError.INVALID_LEXICAL_STATE);");
    ostr.println("   else");
    ostr.println("      jjLexState = lexState;");
    ostr.println("}");

    ostr.println();
  }

  // Assumes l != 0L
  static char MaxChar(long l) {
    for (int i = 64; i-- > 0;) {
      if ((l & (1L << i)) != 0L) {
        return (char) i;
      }
    }

    return 0xffff;
  }

  static void DumpFillToken() {
    ostr.println("protected Token jjFillToken()");
    ostr.println("{");
    ostr.indent();
    if (keepImage) {
      ostr.println("final String curTokenImage;");
    }

    if (hasEmptyMatch) {
      ostr.println("if (jjMatchedPos < 0)");
      ostr.println("{");
      if (keepImage) {
        ostr.println("   if (image == null)");
        ostr.println("      curTokenImage = \"\";");
        ostr.println("   else");
        ostr.println("      curTokenImage = image.toString();");
      }

      if (keepLineCol) {
        ostr.println("final int beginLine = charStream.getBeginLine();");
        ostr.println("final int beginColumn = charStream.getBeginColumn();");
        ostr.println("final int endLine = charStream.getBeginLine();");
        ostr.println("final int endColumn = charStream.getBeginColumn();");
      }

      ostr.println("}");
      ostr.println("else");
      ostr.println("{");
      if (keepImage) {
        ostr.println("   final String literalImage = jjLiteralImages[jjMatchedKind];");
        ostr.println("   curTokenImage = literalImage == null ? charStream.getImage() : literalImage;");
      }

      if (keepLineCol) {
        ostr.println("   final int beginLine = charStream.getBeginLine();");
        ostr.println("   final int beginColumn = charStream.getBeginColumn();");
        ostr.println("   final int endLine = charStream.getEndLine();");
        ostr.println("   final int endColumn = charStream.getEndColumn();");
      }

      ostr.println("}");
    }
    else {
      if (keepImage) {
        ostr.println("final String literalImage = jjLiteralImages[jjMatchedKind];");
        ostr.println("curTokenImage = literalImage == null ? charStream.getImage() : literalImage;");
      }
      if (keepLineCol) {
        ostr.println("final int beginLine = charStream.getBeginLine();");
        ostr.println("final int beginColumn = charStream.getBeginColumn();");
        ostr.println("final int endLine = charStream.getEndLine();");
        ostr.println("final int endColumn = charStream.getEndColumn();");
      }
    }

    if (keepImage) {
      if (Options.getTokenFactory().length() > 0) {
        ostr.println("final Token t = " + Options.getTokenFactory() + ".newToken(jjMatchedKind, curTokenImage);");
      }
      else {
        ostr.println("final Token t = Token.newToken(jjMatchedKind, curTokenImage);");
      }
    }
    else {
      if (Options.getTokenFactory().length() > 0) {
        ostr.println("final Token t = " + Options.getTokenFactory() + ".newToken(jjMatchedKind);");
      }
      else {
        ostr.println("final Token t = Token.newToken(jjMatchedKind);");
      }
    }

    ostr.println("t.setOffset(charStream.getBeginOffset(), charStream.getEndOffset());");
    if (keepLineCol) {
      ostr.println("t.setLineColumn(beginLine, beginColumn, endLine, endColumn);");
    }

    ostr.println("return t;");
    ostr.unindent();
    ostr.println("}");
  }

  static void DumpGetNextToken() {
    int i;

    ostr.println();
    ostr.println("int jjLexState = " + defaultLexState + ";");
    ostr.println("int jjNewStateCount;");
    ostr.println("int jjRound;");
    ostr.println("int jjMatchedPos;");
    ostr.println("int jjMatchedKind;");

    ostr.println();
    ostr.println("/** Get the next token that is not special. */");
    ostr.println("public Token getNextToken() throws java.io.IOException {");
    ostr.indent();
    if (hasSpecial) {
      ostr.println("Token token = getAnyNextToken();")
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
      ostr.println("return getAnyNextToken();");
    }
    ostr.unindent();
    ostr.println("}");

    ostr.println();
    ostr.println("/** Get the next normal or special, but not skip token. */");
    ostr.println("public Token getAnyNextToken() throws java.io.IOException {");
    ostr.indent();
    ostr.println("Token token;");
    ostr.println("int curPos = 0;");
    ostr.println();
    ostr.println("loop:\nwhile (true)");
    ostr.println("{");
    ostr.indent();
    ostr.println("charStream.beginToken();");
    ostr.println("jjChar = charStream.readChar();");
    ostr.println("if (jjChar == -1)");
    ostr.println("{");
    ostr.indent();

    if (Options.getDebugTokenManager()) {
      ostr.println("debugPrinter.println(\"Returning the <EOF> token.\");");
    }

    ostr.println("jjMatchedKind = 0;");
    ostr.println("token = jjFillToken();");

    if (nextStateForEof != null || actForEof != null) {
      ostr.println("tokenLexicalActions(token);");
    }

    if (Options.getCommonTokenAction()) {
      ostr.println("commonTokenAction(token);");
    }

    ostr.println("return token;");
    ostr.unindent();
    ostr.println("}"); // if (jjChar == -1)

    if (hasMoreActions || hasSkipActions || hasTokenActions) {
      if (keepImage) {
        ostr.println("image = jjImage;");
        ostr.println("image.setLength(0);");
        ostr.println("jjImageLength = 0;");
      }
    }

    ostr.println();

    if (hasMore) {
      ostr.println("while (true)");
      ostr.println("{");
      ostr.indent();
    }

    // this also sets up the start state of the nfa
    if (maxLexStates > 1) {
      ostr.println("switch (jjLexState)");
      ostr.println("{");
      ostr.indent();
    }

    for (i = 0; i < maxLexStates; i++) {
      if (maxLexStates > 1) {
        ostr.println("case " + i + ":");
        ostr.indent();
      }

      if (singlesToSkip[i].HasTransitions()) {
        // added the backup(0) to make JIT happy
        ostr.println("// added the backup(0) to make JIT happy");
        ostr.println("charStream.backup(0);");
        if (singlesToSkip[i].asciiMoves[0] != 0L &&
            singlesToSkip[i].asciiMoves[1] != 0L) {
          ostr.println("while ((jjChar < 64" + " && (0x" +
              Long.toHexString(singlesToSkip[i].asciiMoves[0]) +
              "L & (1L << jjChar)) != 0L) || \n" +
              "          (jjChar >> 6) == 1" +
              " && (0x" +
              Long.toHexString(singlesToSkip[i].asciiMoves[1]) +
              "L & (1L << (jjChar & 077))) != 0L)");
        }
        else if (singlesToSkip[i].asciiMoves[1] == 0L) {
          ostr.println("while (jjChar <= " +
              (int) MaxChar(singlesToSkip[i].asciiMoves[0]) + " && (0x" +
              Long.toHexString(singlesToSkip[i].asciiMoves[0]) +
              "L & (1L << jjChar)) != 0L)");
        }
        else if (singlesToSkip[i].asciiMoves[0] == 0L) {
          ostr.println("while (jjChar > 63 && jjChar <= " +
              ((int) MaxChar(singlesToSkip[i].asciiMoves[1]) + 64) +
              " && (0x" +
              Long.toHexString(singlesToSkip[i].asciiMoves[1]) +
              "L & (1L << (jjChar & 077))) != 0L)");
        }

        ostr.println("{");
        ostr.indent();
        if (Options.getDebugTokenManager()) {
          ostr.println("debugPrinter.println(" +
              (maxLexStates > 1 ?
                  "\"<\" + jjLexStateNames[jjLexState] + \">\" + " : "") +
              "\"Skipping character : \" + " +
              "TokenManagerError.escape(String.valueOf(jjChar)) + \" (\" + jjChar + \")\");");
        }
        ostr.println("charStream.beginToken();");
        ostr.println("jjChar = charStream.readChar();");
        ostr.println("if (jjChar == -1) { continue loop; }");
        ostr.unindent();
        ostr.println("}");
      }

      if (initMatch[i] != Integer.MAX_VALUE && initMatch[i] != 0) {
        if (Options.getDebugTokenManager()) {
          ostr.println("debugPrinter.println(\"   Matched the empty string as \" + tokenImage[" +
              initMatch[i] + "] + \" token.\");");
        }

        ostr.println("jjMatchedKind = " + initMatch[i] + ";");
        ostr.println("jjMatchedPos = -1;");
        ostr.println("curPos = 0;");
      }
      else {
        ostr.println("jjMatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
        ostr.println("jjMatchedPos = 0;");
      }

      if (Options.getDebugTokenManager()) {
        ostr.println("debugPrinter.println(" +
            (maxLexStates > 1 ? "\"<\" + jjLexStateNames[jjLexState] + \">\" + " : "") +
            "\"Current character : \" + " +
            "TokenManagerError.escape(String.valueOf(jjChar)) + \" (\" + jjChar + \") " +
            "at line \" + charStream.getEndLine() + \" column \" + charStream.getEndColumn());");
      }

      ostr.println("curPos = jjMoveStringLiteralDfa0_" + i + "();");

      if (canMatchAnyChar[i] != -1) {
        if (initMatch[i] != Integer.MAX_VALUE && initMatch[i] != 0) {
          ostr.println("if (jjMatchedPos < 0 || (jjMatchedPos == 0 && jjMatchedKind > " +
              canMatchAnyChar[i] + "))");
        }
        else {
          ostr.println("if (jjMatchedPos == 0 && jjMatchedKind > " +
              canMatchAnyChar[i] + ")");
        }
        ostr.println("{");
        ostr.indent();

        if (Options.getDebugTokenManager()) {
          ostr.println("debugPrinter.println(\"   Current character matched as a \" + tokenImage[" +
              canMatchAnyChar[i] + "] + \" token.\");");
        }
        ostr.println("jjMatchedKind = " + canMatchAnyChar[i] + ";");

        if (initMatch[i] != Integer.MAX_VALUE && initMatch[i] != 0) {
          ostr.println("jjMatchedPos = 0;");
        }

        ostr.unindent();
        ostr.println("}");
      }

      if (maxLexStates > 1) {
        ostr.println("break;");
        ostr.unindent();
      }
    }

    if (maxLexStates > 1) {
      ostr.unindent();
      ostr.println("}");
    }
    else if (maxLexStates == 0) {
      ostr.println("jjMatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
    }

    if (maxLexStates > 1) {
      ostr.unindent();
    }

    ostr.indent();

    if (maxLexStates > 0) {
      ostr.println("if (jjMatchedKind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
      ostr.println("{");
      ostr.indent();
      ostr.println("if (jjMatchedPos + 1 < curPos)");

      if (Options.getDebugTokenManager()) {
        ostr.println("{");
        ostr.println("debugPrinter.println(" +
            "\"   Putting back \" + (curPos - jjMatchedPos - 1) + \" characters into the input stream.\");");
      }

      ostr.println("charStream.backup(curPos - jjMatchedPos - 1);");

      if (Options.getDebugTokenManager()) {
        ostr.println("}");
      }

      if (Options.getDebugTokenManager()) {
        ostr.println("debugPrinter.println(" +
            "\"****** FOUND A \" + tokenImage[jjMatchedKind] + \" MATCH " +
            "(\" + TokenManagerError.escape(new String(charStream.getSuffix(jjMatchedPos + 1))) + " +
            "\") ******\\n\");");
      }

      if (hasSkip || hasMore || hasSpecial) {
        ostr.println("if (isToken(jjMatchedKind))");
        ostr.println("{");
        ostr.indent();
      }

      ostr.println("token = jjFillToken();");

      if (hasTokenActions) {
        ostr.println("tokenLexicalActions(token);");
      }

      if (maxLexStates > 1) {
        ostr.println("if (jjNewLexState[jjMatchedKind] != -1)")
          .indent()
          .println("jjLexState = jjNewLexState[jjMatchedKind];")
          .unindent();
      }

      if (Options.getCommonTokenAction()) {
        ostr.println("commonTokenAction(token);");
      }

      ostr.println("return token;");

      if (hasSkip || hasMore || hasSpecial) {
        ostr.unindent();
        ostr.println("}");

        if (hasSkip || hasSpecial) {
          if (hasMore) {
            ostr.println("else if (isSkip(jjMatchedKind))");
          }
          else {
            ostr.println("else");
          }

          ostr.println("{");
          ostr.indent();

          if (hasSpecial) {
            ostr.println("if (isSpecial(jjMatchedKind))");
            ostr.println("{");
            ostr.indent();
            ostr.println("token = jjFillToken();");

            if (hasSkipActions) {
              ostr.println("skipLexicalActions(token);");
            }

            if (maxLexStates > 1) {
              ostr.println("if (jjNewLexState[jjMatchedKind] != -1)")
                .indent()
                .println("jjLexState = jjNewLexState[jjMatchedKind];")
                .unindent();
            }

            ostr.println("return token;");
            ostr.unindent();
            ostr.println("}");

            if (hasSkipActions) {
              ostr.println("skipLexicalActions(null);");
            }
          }
          else if (hasSkipActions) {
            ostr.println("skipLexicalActions(null);");
          }

          if (maxLexStates > 1) {
            ostr.println("if (jjNewLexState[jjMatchedKind] != -1)")
              .indent()
              .println("jjLexState = jjNewLexState[jjMatchedKind];")
              .unindent();
          }

          ostr.println("continue loop;");

          ostr.unindent();
          ostr.println("}");
        }

        if (hasMore) {
          if (hasMoreActions) {
            ostr.println("moreLexicalActions();");
          }
          else if (hasSkipActions || hasTokenActions) {
            if (keepImage) {
              ostr.println("jjImageLength += jjMatchedPos + 1;");
            }
          }

          if (maxLexStates > 1) {
            ostr.println("if (jjNewLexState[jjMatchedKind] != -1)")
                .indent()
                .println("jjLexState = jjNewLexState[jjMatchedKind];")
                .unindent();
          }
          ostr.println("curPos = 0;");
          ostr.println("jjMatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");

          ostr.println("jjChar = charStream.readChar();");
          ostr.println("if (jjChar != -1) {");
          ostr.indent();
          if (Options.getDebugTokenManager()) {
            ostr.println("debugPrinter.println(" +
                (maxLexStates > 1 ? "\"<\" + jjLexStateNames[jjLexState] + \">\" + " : "") +
                "\"Current character : \" + " +
                "TokenManagerError.escape(String.valueOf(jjChar)) + \" (\" + jjChar + \") " +
                "at line \" + charStream.getEndLine() + \" column \" + charStream.getEndColumn());");
          }
          ostr.println("continue;");
          ostr.unindent();
          ostr.println("}");
        }
      }
      ostr.unindent();
      ostr.println("}");

      ostr.println("reportLexicalError(curPos);");
    }

    if (hasMore) {
      ostr.unindent();
      ostr.println("}");
    }

    ostr.unindent();
    ostr.println("}");

    ostr.unindent();
    ostr.println("}");

    if (hasMore || hasSkip || hasSpecial) {
      ostr.println()
          .println("/**")
          .println(" * Verify whether the specified is a normal token kind.")
          .println(" *")
          .println(" * @param kind A token kind.")
          .println(" * @return <code>true</code> if a normal token, </code>false</code> otherwise.")
          .println(" */")
          .println("public static boolean isToken(final int kind) {")
          .indent()
          .println("return (jjToToken[kind >> 6] & (1L << (kind & 077))) != 0L;")
          .unindent()
          .println("}");
    }

    if (hasSkip || hasSpecial) {
      ostr.println()
      .println("/**")
      .println(" * Verify whether to ignore the specified token.")
      .println(" *")
      .println(" * @param kind A token kind.")
      .println(" * @return <code>true</code> if ignore token, </code>false</code> otherwise.")
      .println(" */")
      .println("public static boolean isSkip(final int kind) {")
      .indent()
      .println("return (jjToSkip[kind >> 6] & (1L << (kind & 077))) != 0L;")
      .unindent()
      .println("}");
    }

    if (hasSpecial) {
      ostr.println()
      .println("/**")
      .println(" * Verify whether to ignore the specified special token.")
      .println(" *")
      .println(" * @param kind A token kind.")
      .println(" * @return <code>true</code> if ignore special token, </code>false</code> otherwise.")
      .println(" */")
      .println("public static boolean isSpecial(final int kind) {")
      .indent()
      .println("return (jjToSpecial[kind >> 6] & (1L << (kind & 077))) != 0L;")
      .unindent()
      .println("}");
    }

    ostr.println();
    ostr.println("void reportLexicalError(final int curPos) throws java.io.IOException {");
    ostr.println("   String prefix = null;");
    ostr.println("   boolean eof = false;");
    if (keepLineCol) {
      ostr.println("   int errorLine = charStream.getEndLine();");
      ostr.println("   int errorColumn = charStream.getEndColumn();");
      ostr.println("   int c = charStream.readChar();");
      ostr.println("   if (c != -1) {");
      ostr.println("      charStream.backup(1);");
      ostr.println("   } else {");
      ostr.println("      eof = true;");
      if (keepImage) {
        ostr.println("      prefix = curPos <= 1 ? \"\" : charStream.getImage();");
      }
      ostr.println("      errorLine = charStream.getEndLine();");
      ostr.println("      errorColumn = charStream.getEndColumn();");
      ostr.println("   }");
      ostr.println("   throw new TokenManagerError(" +
          "eof, jjLexState, errorLine, errorColumn, prefix, jjChar, TokenManagerError.LEXICAL_ERROR);");
    }
    else {
      ostr.println("   int c = charStream.readChar();");
      ostr.println("   if (c != -1) {");
      ostr.println("      charStream.backup(1);");
      ostr.println("   } else {");
      ostr.println("      eof = true;");
      if (keepImage) {
        ostr.println("      prefix = curPos <= 1 ? \"\" : charStream.getImage();");
      }
      ostr.println("   }");
      ostr.println("   throw new TokenManagerError(" +
          "eof, jjLexState, -1, -1, prefix, jjChar, TokenManagerError.LEXICAL_ERROR);");
    }
    ostr.println("}");
    ostr.println();
  }

  public static void DumpSkipActions() {
    Action act;

    ostr.println("void skipLexicalActions(final Token matchedToken)");
    ostr.println("{");
    ostr.println("   switch(jjMatchedKind)");
    ostr.println("   {");

    Outer:
    for (int i = 0; i < maxOrdinal; i++) {
      if ((toSkip[i / 64] & (1L << (i % 64))) == 0L) {
        continue;
      }

      for (; ;) {
        if (((act = (Action) actions[i]) == null ||
            act.getActionTokens() == null ||
            act.getActionTokens().size() == 0) && !canLoop[lexStates[i]]) {
          continue Outer;
        }

        ostr.println("      case " + i + " :");

        if (initMatch[lexStates[i]] == i && canLoop[lexStates[i]]) {
          ostr.println("         if (jjMatchedPos == -1)");
          ostr.println("         {");
          ostr.println("            if (jjBeenHere[" + lexStates[i] + "] &&");
          ostr.println("                jjEmptyLineNo[" + lexStates[i] + "] == charStream.getBeginLine() &&");
          ostr.println("                jjEmptyColumnNo[" + lexStates[i] + "] == charStream.getBeginColumn())");
          ostr.println("               throw new TokenManagerError(" +
              "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
              "at line \" + charStream.getBeginLine() + \", " +
              "column \" + charStream.getBeginColumn() + \".\"), TokenManagerError.LOOP_DETECTED);");
          ostr.println("            jjEmptyLineNo[" + lexStates[i] + "] = charStream.getBeginLine();");
          ostr.println("            jjEmptyColumnNo[" + lexStates[i] + "] = charStream.getBeginColumn();");
          ostr.println("            jjBeenHere[" + lexStates[i] + "] = true;");
          ostr.println("         }");
        }

        if ((act = (Action) actions[i]) == null ||
            act.getActionTokens().size() == 0) {
          break;
        }

        if (keepImage) {
          ostr.print("         image.append");
          if (RStringLiteral.allImages[i] != null) {
            ostr.println("(jjLiteralImages[" + i + "]);");
            ostr.println("        lengthOfMatch = jjLiteralImages[" + i + "].length();");
          }
          else {
            ostr.println("(charStream.getSuffix(jjImageLength + (lengthOfMatch = jjMatchedPos + 1)));");
          }
        }

        printTokenSetup((Token) act.getActionTokens().get(0));
        ccol = 1;

        for (int j = 0; j < act.getActionTokens().size(); j++) {
          printToken((Token) act.getActionTokens().get(j), ostr);
        }
        ostr.println();

        break;
      }

      ostr.println("         break;");
    }

    ostr.println("      default:");
    ostr.println("         break;");
    ostr.println("   }");
    ostr.println("}");
  }

  public static void DumpMoreActions() {
    Action act;

    ostr.println("void moreLexicalActions()");
    ostr.println("{");
    if (keepImage) {
      ostr.println("   jjImageLength += (lengthOfMatch = jjMatchedPos + 1);");
    }
    ostr.println("   switch(jjMatchedKind)");
    ostr.println("   {");

    Outer:
    for (int i = 0; i < maxOrdinal; i++) {
      if ((toMore[i / 64] & (1L << (i % 64))) == 0L) {
        continue;
      }

      for (; ;) {
        if (((act = (Action) actions[i]) == null ||
            act.getActionTokens() == null ||
            act.getActionTokens().size() == 0) && !canLoop[lexStates[i]]) {
          continue Outer;
        }

        ostr.println("      case " + i + " :");

        if (initMatch[lexStates[i]] == i && canLoop[lexStates[i]]) {
          ostr.println("         if (jjMatchedPos == -1)");
          ostr.println("         {");
          ostr.println("            if (jjBeenHere[" + lexStates[i] + "] &&");
          ostr.println("                jjEmptyLineNo[" + lexStates[i] + "] == charStream.getBeginLine() &&");
          ostr.println("                jjEmptyColumnNo[" + lexStates[i] + "] == charStream.getBeginColumn())");
          ostr.println("               throw new TokenManagerError(" +
              "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
              "at line \" + charStream.getBeginLine() + \", " +
              "column \" + charStream.getBeginColumn() + \".\"), TokenManagerError.LOOP_DETECTED);");
          ostr.println("            jjEmptyLineNo[" + lexStates[i] + "] = charStream.getBeginLine();");
          ostr.println("            jjEmptyColumnNo[" + lexStates[i] + "] = charStream.getBeginColumn();");
          ostr.println("            jjBeenHere[" + lexStates[i] + "] = true;");
          ostr.println("         }");
        }

        if ((act = (Action) actions[i]) == null ||
            act.getActionTokens().size() == 0) {
          break;
        }

        if (keepImage) {
          ostr.print("         image.append");
          if (RStringLiteral.allImages[i] != null) {
            ostr.println("(jjLiteralImages[" + i + "]);");
          }
          else {
            ostr.println("(charStream.getSuffix(jjImageLength));");
          }

          ostr.println("         jjImageLength = 0;");
        }

        printTokenSetup((Token) act.getActionTokens().get(0));
        ccol = 1;

        for (int j = 0; j < act.getActionTokens().size(); j++) {
          printToken((Token) act.getActionTokens().get(j), ostr);
        }
        ostr.println();

        break;
      }

      ostr.println("         break;");
    }

    ostr.println("      default:");
    ostr.println("         break;");

    ostr.println("   }");
    ostr.println("}");
  }

  public static void DumpTokenActions() {
    Action act;
    int i;

    ostr.println("void tokenLexicalActions(final Token matchedToken)");
    ostr.println("{");
    ostr.println("   switch(jjMatchedKind)");
    ostr.println("   {");

    Outer:
    for (i = 0; i < maxOrdinal; i++) {
      if ((toToken[i / 64] & (1L << (i % 64))) == 0L) {
        continue;
      }

      for (; ;) {
        if (((act = (Action) actions[i]) == null ||
            act.getActionTokens() == null ||
            act.getActionTokens().size() == 0) && !canLoop[lexStates[i]]) {
          continue Outer;
        }

        ostr.println("      case " + i + " :");

        if (initMatch[lexStates[i]] == i && canLoop[lexStates[i]]) {
          ostr.println("         if (jjMatchedPos == -1)");
          ostr.println("         {");
          ostr.println("            if (jjBeenHere[" + lexStates[i] + "] &&");
          ostr.println("                jjEmptyLineNo[" + lexStates[i] + "] == charStream.getBeginLine() &&");
          ostr.println("                jjEmptyColumnNo[" + lexStates[i] + "] == charStream.getBeginColumn())");
          ostr.println("               throw new TokenManagerError(" +
              "(\"Error: Bailing out of infinite loop caused by repeated empty string matches " +
              "at line \" + charStream.getBeginLine() + \", " +
              "column \" + charStream.getBeginColumn() + \".\"), TokenManagerError.LOOP_DETECTED);");
          ostr.println("            jjEmptyLineNo[" + lexStates[i] + "] = charStream.getBeginLine();");
          ostr.println("            jjEmptyColumnNo[" + lexStates[i] + "] = charStream.getBeginColumn();");
          ostr.println("            jjBeenHere[" + lexStates[i] + "] = true;");
          ostr.println("         }");
        }

        if ((act = (Action) actions[i]) == null ||
            act.getActionTokens().size() == 0) {
          break;
        }

        if (keepImage) {
          if (i == 0) {
              ostr.println("      image.setLength(0);"); // For EOF no image is there
          }
          else {
            ostr.print("        image.append");

            if (RStringLiteral.allImages[i] != null) {
              ostr.println("(jjLiteralImages[" + i + "]);");
              ostr.println("        lengthOfMatch = jjLiteralImages[" + i + "].length();");
            }
            else {
              ostr.println("(charStream.getSuffix(jjImageLength + (lengthOfMatch = jjMatchedPos + 1)));");
            }
          }
        }

        printTokenSetup((Token) act.getActionTokens().get(0));
        ccol = 1;

        for (int j = 0; j < act.getActionTokens().size(); j++) {
          printToken((Token) act.getActionTokens().get(j), ostr);
        }
        ostr.println();

        break;
      }

      ostr.println("         break;");
    }

    ostr.println("      default:");
    ostr.println("         break;");
    ostr.println("   }");
    ostr.println("}");
  }

  public static void reInit() {
    ostr = null;
    tokMgrClassName = null;
    allTpsForState = new Hashtable();
    lexStateIndex = 0;
    kinds = null;
    maxOrdinal = 1;
    lexStateSuffix = null;
    newLexState = null;
    lexStates = null;
    ignoreCase = null;
    actions = null;
    initStates = new Hashtable();
    stateSetSize = 0;
    maxLexStates = 0;
    lexStateName = null;
    singlesToSkip = null;
    toSkip = null;
    toSpecial = null;
    toMore = null;
    toToken = null;
    defaultLexState = 0;
    rexprs = null;
    maxLongsReqd = null;
    initMatch = null;
    canMatchAnyChar = null;
    hasEmptyMatch = false;
    canLoop = null;
    stateHasActions = null;
    hasLoop = false;
    canReachOnMore = null;
    hasNfa = null;
    mixed = null;
    initialState = null;
    curKind = 0;
    hasSkipActions = false;
    hasMoreActions = false;
    hasTokenActions = false;
    hasSpecial = false;
    hasSkip = false;
    hasMore = false;
    curRE = null;
  }
}
