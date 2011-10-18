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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ParseEngine {
  /** Constants used in the following method "buildLookaheadChecker". */
  private static final int NO_OPEN_STM = 0;
  private static final int OPEN_IF = 1;
  private static final int OPEN_SWITCH = 2;

  /** This class stores information to pass from phase 2 to phase 3. */
  private static class Phase3Data {
    /** This is the expansion to generate the jj3 method for. */
    final Expansion expansion;
    /**
     * This is the number of tokens that can still be consumed.  This
     * number is used to limit the number of jj3 methods generated.
     */
    final int count;

    Phase3Data(Expansion e, int c) {
      expansion = e;
      count = c;
    }
  }

  private final JavaCCState state;
  private final Semanticize semanticize;
  /**
   * maskIndex, jj2index, maskValues are variables that are shared between
   * ParseEngine and ParserGen.
   */
  int maskIndex;
  int jj2index;
  boolean lookaheadNeeded;
  List<int[]> maskValues = new ArrayList<int[]>();
  private int genSymIndex;
  private boolean jj2LA;
  /**
   * These lists are used to maintain expansions for which code generation
   * in phase 2 and phase 3 is required.  Whenever a call is generated to
   * a phase 2 or phase 3 routine, a corresponding entry is added here if
   * it has not already been added.
   * The phase 3 routines have been optimized in version 0.7pre2.  Essentially
   * only those methods (and only those portions of these methods) are
   * generated that are required.  The lookahead amount is used to determine
   * this.  This change requires the use of a hash table because it is now
   * possible for the same phase 3 routine to be requested multiple times
   * with different lookaheads.  The hash table provides a easily searchable
   * capability to determine the previous requests.
   * The phase 3 routines now are performed in a two step process - the first
   * step gathers the requests (replacing requests with lower lookaheads with
   * those requiring larger lookaheads).  The second step then generates these
   * methods.
   * This optimization and the hashtable makes it look like we do not need
   * the flag "phase3done" any more.  But this has not been removed yet.
   */
  private final List<Lookahead> phase2list = new ArrayList<Lookahead>();
  private final List<Phase3Data> phase3list = new ArrayList<Phase3Data>();
  private final Map<Expansion, Phase3Data> phase3table = new HashMap<Expansion, Phase3Data>();

  public ParseEngine(JavaCCState state, Semanticize semanticize) {
    this.state = state;
    this.semanticize = semanticize;
  }

  void build(IndentingPrintWriter out) throws IOException {
    for (NormalProduction production : state.bnfProductions) {
      if (production instanceof JavaCodeProduction) {
        buildJavaCodeProduction(production, out);
      }
      else {
        buildPhase1Routine((BNFProduction) production, out);
      }
    }

    for (Lookahead lookahead : phase2list) {
      buildPhase2Routine(lookahead, out);
    }

    // The phase3list list will grow while it is being iterated over.
    int index = 0;
    while (index < phase3list.size()) {
      for (; index < phase3list.size(); index++) {
        setupPhase3Builds(phase3list.get(index));
      }
    }

    for (Phase3Data phase3Data : phase3table.values()) {
      buildPhase3Routine(phase3Data, out);
    }
  }

  /**
   * @return true if there is a JAVACODE production that the argument expansion
   *         may directly expand to (without consuming tokens or encountering lookahead).
   */
  private boolean javaCodeCheck(Expansion expansion) {
    if (expansion instanceof RegularExpression) {
      return false;
    }
    else if (expansion instanceof NonTerminal) {
      NormalProduction production = ((NonTerminal) expansion).getProd();
      return production instanceof JavaCodeProduction
          || javaCodeCheck(production.getExpansion());
    }
    else if (expansion instanceof Sequence) {
      Sequence sequence = (Sequence) expansion;
      for (int i = 0; i < sequence.units.size(); i++) {
        Expansion[] units = sequence.units.toArray(new Expansion[sequence.units.size()]);
        Expansion nested = units[i];
        if (nested instanceof Lookahead && ((Lookahead) nested).isExplicit()) {
          // An explicit lookahead (rather than one generated implicitly). Assume
          // the user knows what he / she is doing, e.g.
          //    "A" ( "B" | LOOKAHEAD("X") jcode() | "C" )* "D"
          return false;
        }
        if (javaCodeCheck(nested)) {
          return true;
        }
        if (!semanticize.emptyExpansionExists(nested)) {
          return false;
        }
      }
      return false;
    }
    else if (expansion instanceof Choice) {
      Choice choice = (Choice) expansion;
      for (Expansion nested : choice.getChoices()) {
        if (javaCodeCheck(nested)) {
          return true;
        }
      }
      return false;
    }
    else if (expansion instanceof ZeroOrOne) {
      return javaCodeCheck(((ZeroOrOne) expansion).expansion);
    }
    else if (expansion instanceof ZeroOrMore) {
      return javaCodeCheck(((ZeroOrMore) expansion).expansion);
    }
    else if (expansion instanceof OneOrMore) {
      return javaCodeCheck(((OneOrMore) expansion).expansion);
    }
    else if (expansion instanceof TryBlock) {
      return javaCodeCheck(((TryBlock) expansion).expansion);
    }
    else {
      return false;
    }
  }

  /**
   * An array used to store the first sets generated by the following method.
   * A true entry means that the corresponding token is in the first set.
   */
  private boolean[] firstSet;

  /**
   * Sets up the array "firstSet" above based on the Expansion argument
   * passed to it.  Since this is a recursive function, it assumes that
   * "firstSet" has been reset before the first call.
   */
  private void genFirstSet(Expansion expansion) {
    if (expansion instanceof RegularExpression) {
      firstSet[((RegularExpression) expansion).ordinal] = true;
    }
    else if (expansion instanceof NonTerminal) {
      if (!(((NonTerminal) expansion).getProd() instanceof JavaCodeProduction)) {
        genFirstSet(((NonTerminal) expansion).getProd().getExpansion());
      }
    }
    else if (expansion instanceof Choice) {
      Choice choice = (Choice) expansion;
      for (int i = 0; i < choice.getChoices().size(); i++) {
        genFirstSet(choice.getChoices().get(i));
      }
    }
    else if (expansion instanceof Sequence) {
      Sequence sequence = (Sequence) expansion;
      Expansion unit = sequence.units.get(0);
      if (unit instanceof Lookahead && ((Lookahead) unit).getActionTokens().size() != 0) {
        jj2LA = true;
      }
      for (int i = 0; i < sequence.units.size(); i++) {
        unit = sequence.units.get(i);
        // Javacode productions can not have FIRST sets. Instead we generate the FIRST set
        // for the preceding LOOKAHEAD (the semantic checks should have made sure that
        // the LOOKAHEAD is suitable).
        if (unit instanceof NonTerminal
            && ((NonTerminal) unit).getProd() instanceof JavaCodeProduction) {
          if (i > 0 && sequence.units.get(i - 1) instanceof Lookahead) {
            Lookahead la = (Lookahead) sequence.units.get(i - 1);
            genFirstSet(la.getLaExpansion());
          }
        }
        else {
          genFirstSet(sequence.units.get(i));
        }
        if (!semanticize.emptyExpansionExists(sequence.units.get(i))) {
          break;
        }
      }
    }
    else if (expansion instanceof ZeroOrOne) {
      genFirstSet(((ZeroOrOne) expansion).expansion);
    }
    else if (expansion instanceof ZeroOrMore) {
      genFirstSet(((ZeroOrMore) expansion).expansion);
    }
    else if (expansion instanceof OneOrMore) {
      genFirstSet(((OneOrMore) expansion).expansion);
    }
    else if (expansion instanceof TryBlock) {
      genFirstSet(((TryBlock) expansion).expansion);
    }
  }

  private void buildJavaCodeProduction(NormalProduction production, IndentingPrintWriter out)
      throws IOException {
    JavaCodeProduction jp = (JavaCodeProduction) production;
    Token t = jp.getReturnTypeTokens().get(0);
    TokenPrinter.printTokenSetup(t);
    TokenPrinter.cCol = 1;
    TokenPrinter.printLeadingComments(t, out);
    out.print(production.getAccessModifier() != null ? production.getAccessModifier() + " " : "");
    TokenPrinter.cLine = t.getBeginLine();
    TokenPrinter.cCol = t.getBeginColumn();
    TokenPrinter.printTokenOnly(t, out);
    for (int i = 1; i < jp.getReturnTypeTokens().size(); i++) {
      t = jp.getReturnTypeTokens().get(i);
      TokenPrinter.printToken(t, out);
    }
    TokenPrinter.printTrailingComments(t);
    out.print(" " + jp.getLhs() + "(");
    if (jp.getParameterListTokens().size() != 0) {
      TokenPrinter.printTokenSetup(jp.getParameterListTokens().get(0));
      for (Token token : jp.getParameterListTokens()) {
        t = token;
        TokenPrinter.printToken(t, out);
      }
      TokenPrinter.printTrailingComments(t);
    }
    out.print(") throws java.io.IOException, ParseException");
    for (List<Token> tokens : jp.getThrowsList()) {
      out.print(", ");
      for (Token token : tokens) {
        t = token;
        out.print(t.getImage());
      }
    }
    out.print(" {");
    if (Options.getDebugParser()) {
      out.println();
      out.println("trace_call(\"" + jp.getLhs() + "\");");
      out.print("try {");
    }
    if (jp.getCodeTokens().size() != 0) {
      TokenPrinter.printTokenSetup(jp.getCodeTokens().get(0));
      TokenPrinter.cLine--;
      TokenPrinter.printTokenList(jp.getCodeTokens(), out);
    }
    out.println();
    if (Options.getDebugParser()) {
      out.println("} finally {");
      out.println("trace_return(\"" + jp.getLhs() + "\");");
      out.println("}");
    }
    out.println("}");
    out.println();
  }

  private void buildPhase1Routine(BNFProduction p, IndentingPrintWriter out)
      throws IOException {
    Token t = p.getReturnTypeTokens().get(0);
    boolean voidReturn = t.getKind() == JavaCCConstants.VOID;
    TokenPrinter.printTokenSetup(t);
    TokenPrinter.cCol = 1;
    TokenPrinter.printLeadingComments(t, out);
    out.print((p.getAccessModifier() != null ? p.getAccessModifier() : "private") + " final ");
    TokenPrinter.cLine = t.getBeginLine();
    TokenPrinter.cCol = t.getBeginColumn();
    TokenPrinter.printTokenOnly(t, out);
    for (int i = 1; i < p.getReturnTypeTokens().size(); i++) {
      t = p.getReturnTypeTokens().get(i);
      TokenPrinter.printToken(t, out);
    }
    TokenPrinter.printTrailingComments(t);
    out.print(" " + p.getLhs() + "(");
    if (p.getParameterListTokens().size() != 0) {
      TokenPrinter.printTokenSetup(p.getParameterListTokens().get(0));
      for (Token token : p.getParameterListTokens()) {
        t = token;
        TokenPrinter.printToken(t, out);
      }
      TokenPrinter.printTrailingComments(t);
    }
    out.print(") throws java.io.IOException, ParseException");
    for (List<Token> tokens : p.getThrowsList()) {
      out.print(", ");
      for (Token token : tokens) {
        t = token;
        out.print(t.getImage());
      }
    }
    out.print(" {");

    out.indent();

    if (Options.getDebugParser()) {
      out.println();
      out.println("trace_call(\"" + p.getLhs() + "\");");
      out.print("try {");
    }

    if (p.getTokens().size() != 0) {
      TokenPrinter.printTokenSetup(p.getTokens().get(0));
      TokenPrinter.cLine--;
      for (Token token : p.getTokens()) {
        t = token;
        TokenPrinter.printToken(t, out);
      }
      TokenPrinter.printTrailingComments(t);
    }

    String code = phase1ExpansionGen(p.getExpansion(), out);

    out.println(code);

    out.println();

    if (p.isJumpPatched() && !voidReturn) {
      out.println("throw new Error(\"Missing return statement in function\");");
    }

    if (Options.getDebugParser()) {
      out.println("} finally {");
      out.println("trace_return(\"" + p.getLhs() + "\");");
      out.println("}");
    }

    out.unindent();

    out.println("}");
  }

  private String phase1ExpansionGen(Expansion expansion, IndentingPrintWriter out) {
    if (expansion instanceof NonTerminal) {
      return phase1_NonTerminal((NonTerminal) expansion, out);
    }
    else if (expansion instanceof Action) {
      return phase1_Action((Action) expansion, out);
    }
    else if (expansion instanceof RegularExpression) {
      return phase1_RegularExpression((RegularExpression) expansion, out);
    }
    else if (expansion instanceof Sequence) {
      return phase1_Sequence((Sequence) expansion, out);
    }
    else if (expansion instanceof Choice) {
      return phase1_Choice((Choice) expansion, out);
    }
    if (expansion instanceof ZeroOrOne) {
      return phase1_ZeroOrOne((ZeroOrOne) expansion, out);
    }
    else if (expansion instanceof ZeroOrMore) {
      return phase1_ZeroOrMore((ZeroOrMore) expansion, out);
    }
    else if (expansion instanceof OneOrMore) {
      return phase1_OneOrMore((OneOrMore) expansion, out);
    }
    else if (expansion instanceof TryBlock) {
      return phase1_TryBlock((TryBlock) expansion, out);
    }
    else {
      throw new IllegalStateException("unreachable");
    }
  }

  private String phase1_NonTerminal(NonTerminal expansion, IndentingPrintWriter out) {
    String s = "\n";
    if (expansion.getLhsTokens().size() != 0) {
      TokenPrinter.printTokenSetup(expansion.getLhsTokens().get(0));
      Token t = null;
      for (Token token : expansion.getLhsTokens()) {
        t = token;
        s += TokenPrinter.printToken(t);
      }
      s += TokenPrinter.printTrailingComments(t);
      s += " = ";
    }
    s += expansion.getName() + "(";
    if (expansion.getArgumentTokens().size() != 0) {
      TokenPrinter.printTokenSetup(expansion.getArgumentTokens().get(0));
      Token t = null;
      for (Token token : expansion.getArgumentTokens()) {
        t = token;
        s += TokenPrinter.printToken(t);
      }
      s += TokenPrinter.printTrailingComments(t);
    }
    s += ");";
    return s;
  }

  private String phase1_Action(Action expansion, IndentingPrintWriter out) {
    String s = "\n";
    if (expansion.getActionTokens().size() != 0) {
      TokenPrinter.printTokenSetup(expansion.getActionTokens().get(0));
      TokenPrinter.cCol = 1;
      Token t = null;
      for (Token token : expansion.getActionTokens()) {
        t = token;
        s += TokenPrinter.printToken(t);
      }
      s += TokenPrinter.printTrailingComments(t);
    }
    return s;
  }

  private String phase1_RegularExpression(RegularExpression expansion, IndentingPrintWriter out) {
    String s = "\n";
    if (expansion.lhsTokens.size() != 0) {
      Token t = null;
      TokenPrinter.printTokenSetup(expansion.lhsTokens.get(0));
      for (Token lhsToken : expansion.lhsTokens) {
        t = lhsToken;
        s += TokenPrinter.printToken(t);
      }
      s += TokenPrinter.printTrailingComments(t);
      s += " = ";
    }
    String tail = expansion.rhsToken == null ? ");" : ")." + expansion.rhsToken.getImage() + ";";
    if (expansion.label.equals("")) {
      String label = state.namesOfTokens.get(expansion.ordinal);
      if (label != null) {
        s += "jj_consume_token(" + label + tail;
      }
      else {
        s += "jj_consume_token(" + expansion.ordinal + tail;
      }
    }
    else {
      s += "jj_consume_token(" + expansion.label + tail;
    }
    return s;
  }

  private String phase1_Sequence(Sequence expansion, IndentingPrintWriter out) {
    // We skip the first element in the following iteration since it is the
    // Lookahead object.
    String s = "";
    for (int i = 1; i < expansion.units.size(); i++) {
      s += phase1ExpansionGen(expansion.units.get(i), out);
    }
    return s;
  }

  private String phase1_Choice(Choice expansion, IndentingPrintWriter out) {
    Lookahead[] lookaheads = new Lookahead[expansion.getChoices().size()];
    String[] actions = new String[expansion.getChoices().size() + 1];
    // In previous line, the "throw" never throws an exception since the
    // evaluation of jj_consume_token(-1) causes ParseException to be
    // thrown first.
    for (int i = 0; i < expansion.getChoices().size(); i++) {
      Sequence sequence = (Sequence) expansion.getChoices().get(i);
      actions[i] = phase1ExpansionGen(sequence, out);
      lookaheads[i] = (Lookahead) sequence.units.get(0);
    }
    actions[expansion.getChoices().size()] = "\njj_consume_token(-1);\nthrow new ParseException(\"unreachable\");";
    return buildLookaheadChecker(lookaheads, actions, out);
  }

  private String phase1_ZeroOrOne(ZeroOrOne expansion, IndentingPrintWriter out) {
    Expansion nested = expansion.expansion;
    Lookahead la;
    if (nested instanceof Sequence) {
      la = (Lookahead) ((Sequence) nested).units.get(0);
    }
    else {
      la = new Lookahead();
      la.setAmount(Options.getLookahead());
      la.setLaExpansion(nested);
    }
    Lookahead[] lookaheads = new Lookahead[1];
    lookaheads[0] = la;
    String[] actions = new String[2];
    actions[0] = phase1ExpansionGen(nested, out);
    actions[1] = "\n;";
    return buildLookaheadChecker(lookaheads, actions, out);
  }

  private String phase1_ZeroOrMore(ZeroOrMore expansion, IndentingPrintWriter out) {
    Expansion nested = expansion.expansion;
    Lookahead la;
    if (nested instanceof Sequence) {
      la = (Lookahead) ((Sequence) nested).units.get(0);
    }
    else {
      la = new Lookahead();
      la.setAmount(Options.getLookahead());
      la.setLaExpansion(nested);
    }
    String s = "\n";
    int labelIndex = ++genSymIndex;
    s += "label_" + labelIndex + ":\n";
    s += "while (true) {";
    Lookahead[] lookaheads = new Lookahead[1];
    lookaheads[0] = la;
    String[] actions = new String[2];
    actions[0] = "\n;";
    actions[1] = "\nbreak label_" + labelIndex + ";";
    s += buildLookaheadChecker(lookaheads, actions, out);
    s += phase1ExpansionGen(nested, out);
    s += "\n}";
    return s;
  }

  private String phase1_OneOrMore(OneOrMore expansion, IndentingPrintWriter out) {
    Expansion nested = expansion.expansion;
    Lookahead la;
    if (nested instanceof Sequence) {
      la = (Lookahead) ((Sequence) nested).units.get(0);
    }
    else {
      la = new Lookahead();
      la.setAmount(Options.getLookahead());
      la.setLaExpansion(nested);
    }
    String s = "\n";
    int labelIndex = ++genSymIndex;
    s += "label_" + labelIndex + ":\n";
    s += "while (true) {";
    s += phase1ExpansionGen(nested, out);
    Lookahead[] lookaheads = new Lookahead[1];
    lookaheads[0] = la;
    String[] actions = new String[2];
    actions[0] = "\n;";
    actions[1] = "\nbreak label_" + labelIndex + ";";
    s += buildLookaheadChecker(lookaheads, actions, out);
    s += "\n}";
    return s;
  }

  private String phase1_TryBlock(TryBlock expansion, IndentingPrintWriter out) {
    String s = "\n";
    s += "try {";
    s += phase1ExpansionGen(expansion.expansion, out);
    s += "\n}";
    for (int i = 0; i < expansion.catchBlocks.size(); i++) {
      s += " catch (";
      List<Token> list = expansion.types.get(i);
      Token t = null;
      if (list.size() != 0) {
        TokenPrinter.printTokenSetup(list.get(0));

        for (Token token : list) {
          t = token;
          s += TokenPrinter.printToken(t);
        }
        s += TokenPrinter.printTrailingComments(t);
      }
      s += " ";
      t = expansion.ids.get(i);
      TokenPrinter.printTokenSetup(t);
      s += TokenPrinter.printToken(t);
      s += TokenPrinter.printTrailingComments(t);
      s += ") {\n";
      list = expansion.catchBlocks.get(i);
      if (list.size() != 0) {
        TokenPrinter.printTokenSetup(list.get(0));
        TokenPrinter.cCol = 1;
        for (Token token : list) {
          t = token;
          s += TokenPrinter.printToken(t);
        }
        s += TokenPrinter.printTrailingComments(t);
      }
      s += "\n}";
    }
    if (expansion.finallyBlocks != null) {
      s += " finally {\n";
      if (expansion.finallyBlocks.size() != 0) {
        TokenPrinter.printTokenSetup(expansion.finallyBlocks.get(0));
        TokenPrinter.cCol = 1;
        Token t = null;
        for (Token token : expansion.finallyBlocks) {
          t = token;
          s += TokenPrinter.printToken(t);
        }
        s += TokenPrinter.printTrailingComments(t);
      }
      s += "\n}";
    }
    return s;
  }

  /**
   * This method takes two parameters - an array of Lookahead's
   * "lookaheads", and an array of String's "actions".  "actions" contains
   * exactly one element more than "lookaheads".  "actions" are Java source
   * code, and "lookaheads" translate to conditions - so lets say
   * "f(lookaheads[i])" is true if the lookahead required by "lookaheads[i]" is
   * indeed the case.  This method returns a string corresponding to
   * the Java code for:
   *
   * if (f(lookaheads[0]) actions[0]
   * else if (f(lookaheads[1]) actions[1]
   * . . .
   * else actions[action.length-1]
   *
   * A particular action entry ("actions[i]") can be null, in which
   * case, a noop is generated for that action.
   */
  private String buildLookaheadChecker(Lookahead[] lookaheads, String[] actions, IndentingPrintWriter out) {
    // The state variables.
    int state = NO_OPEN_STM;
    int indentAmt = 0;
    boolean[] casedValues = new boolean[this.state.tokenCount];
    String retval = "";

    int tokenMaskSize = (this.state.tokenCount - 1) / 32 + 1;
    int[] tokenMask = null;

    // Iterate over all the conditions.
    int index = 0;
    while (index < lookaheads.length) {
      Lookahead la = lookaheads[index];
      jj2LA = false;

      if (la.getAmount() == 0
          || semanticize.emptyExpansionExists(la.getLaExpansion())
          || javaCodeCheck(la.getLaExpansion())) {
        // This handles the following cases:
        // . If syntactic lookahead is not wanted (and hence explicitly specified
        //   as 0).
        // . If it is possible for the lookahead expansion to recognize the empty
        //   string - in which case the lookahead trivially passes.
        // . If the lookahead expansion has a JAVACODE production that it directly
        //   expands to - in which case the lookahead trivially passes.
        if (la.getActionTokens().size() == 0) {
          // In addition, if there is no semantic lookahead, then the
          // lookahead trivially succeeds.  So break the main loop and
          // treat this case as the default last action.
          break;
        }
        else {
          // This case is when there is only semantic lookahead
          // (without any preceding syntactic lookahead).  In this
          // case, an "if" statement is generated.
          switch (state) {
            case NO_OPEN_STM:
              retval += "\nif (";
              indentAmt++;
              break;
            case OPEN_IF:
              retval += "\n} else if (";
              break;
            case OPEN_SWITCH:
              retval += "\ndefault:";
              if (Options.getErrorReporting()) {
                retval += "\njj_la1[" + maskIndex + "] = jj_gen;";
                maskIndex++;
              }
              maskValues.add(tokenMask);
              retval += "\nif (";
              indentAmt++;
          }
          TokenPrinter.printTokenSetup(la.getActionTokens().get(0));
          Token t = null;
          for (Token token : la.getActionTokens()) {
            t = token;
            retval += TokenPrinter.printToken(t);
          }
          retval += TokenPrinter.printTrailingComments(t);
          retval += ") {" + actions[index];
          state = OPEN_IF;
        }
      }
      else if (la.getAmount() == 1 && la.getActionTokens().size() == 0) {
        // Special optimal processing when the lookahead is exactly 1, and there
        // is no semantic lookahead.

        if (firstSet == null) {
          firstSet = new boolean[this.state.tokenCount];
        }
        for (int i = 0; i < this.state.tokenCount; i++) {
          firstSet[i] = false;
        }

        // jj2LA is set to false at the beginning of the containing "if" statement.
        // It is checked immediately after the end of the same statement to determine
        // if lookaheads are to be performed using calls to the jj2 methods.
        genFirstSet(la.getLaExpansion());

        // genFirstSet may find that semantic attributes are appropriate for the next
        // token.  In which case, it sets jj2LA to true.
        if (!jj2LA) {
          // This case is if there is no applicable semantic lookahead and the lookahead
          // is one (excluding the earlier cases such as JAVACODE, etc.).
          switch (state) {
            case OPEN_IF:
              retval += "\n} else {";
              // Control flows through to next case.
            case NO_OPEN_STM:
              retval += "\nswitch (";
              if (Options.getCacheTokens()) {
                retval += "jj_nt.getKind()) {";
              }
              else {
                retval += "jj_ntk == -1 ? jj_ntk() : jj_ntk) {";
              }
              for (int i = 0; i < this.state.tokenCount; i++) {
                casedValues[i] = false;
              }
              indentAmt++;
              tokenMask = new int[tokenMaskSize];
              for (int i = 0; i < tokenMaskSize; i++) {
                tokenMask[i] = 0;
              }
              // Don't need to do anything if state is OPEN_SWITCH.
          }
          for (int i = 0; i < this.state.tokenCount; i++) {
            if (firstSet[i]) {
              if (!casedValues[i]) {
                casedValues[i] = true;
                retval += "\ncase ";
                int j1 = i / 32;
                int j2 = i % 32;
                tokenMask[j1] |= 1 << j2;
                String s = this.state.namesOfTokens.get(i);
                if (s == null) {
                  retval += i;
                }
                else {
                  retval += s;
                }
                retval += ":";
              }
            }
          }
          retval += actions[index];
          retval += "\nbreak;";
          state = OPEN_SWITCH;
        }
      }
      else {
        // This is the case when lookahead is determined through calls to
        // jj2 methods.  The other case is when lookahead is 1, but semantic
        // attributes need to be evaluated.  Hence this crazy control structure.
        jj2LA = true;
      }

      if (jj2LA) {
        // In this case lookahead is determined by the jj2 methods.
        switch (state) {
          case NO_OPEN_STM:
            retval += "\nif (";
            indentAmt++;
            break;
          case OPEN_IF:
            retval += "\n} else if (";
            break;
          case OPEN_SWITCH:
            retval += "\ndefault:";
            if (Options.getErrorReporting()) {
              retval += "\njj_la1[" + maskIndex + "] = jj_gen;";
              maskIndex++;
            }
            maskValues.add(tokenMask);
            retval += "\nif (";
            indentAmt++;
        }
        jj2index++;
        // At this point, la.la_expansion.internal_name must be "".
        la.getLaExpansion().internalName = "_" + jj2index;
        phase2list.add(la);
        retval += "jj_2" + la.getLaExpansion().internalName + "(" + la.getAmount() + ")";
        if (la.getActionTokens().size() != 0) {
          // In addition, there is also a semantic lookahead.  So concatenate
          // the semantic check with the syntactic one.
          retval += " && (";
          TokenPrinter.printTokenSetup(la.getActionTokens().get(0));
          Token t = null;
          for (Token token : la.getActionTokens()) {
            t = token;
            retval += TokenPrinter.printToken(t);
          }
          retval += TokenPrinter.printTrailingComments(t);
          retval += ")";
        }
        retval += ") {" + actions[index];
        state = OPEN_IF;
      }

      index++;
    }

    // Generate code for the default case.  Note this may not
    // be the last entry of "actions" if any condition can be
    // statically determined to be always "true".
    switch (state) {
      case NO_OPEN_STM:
        retval += actions[index];
        break;
      case OPEN_IF:
        retval += "\n} else {" + actions[index];
        break;
      case OPEN_SWITCH:
        retval += "\ndefault:";
        if (Options.getErrorReporting()) {
          retval += "\njj_la1[" + maskIndex + "] = jj_gen;";
          maskValues.add(tokenMask);
          maskIndex++;
        }
        retval += actions[index];
    }

    for (int i = 0; i < indentAmt; i++) {
      retval += "\n}";
    }

    return retval;
  }

  private void buildPhase2Routine(Lookahead la, IndentingPrintWriter out) {
    Expansion e = la.getLaExpansion();
    out.println("private boolean jj_2" + e.internalName + "(int xla) throws java.io.IOException {");
    out.indent();
    out.println("jj_la = xla; jj_lastPos = jj_scanPos = token;");
    out.println("try { return !jj_3" + e.internalName + "(); }");
    out.println("catch (LookaheadSuccess ls) { return true; }");
    if (Options.getErrorReporting()) {
      out.println("finally { jj_save(" + (Integer.parseInt(e.internalName.substring(1)) - 1) + ", xla); }");
    }
    out.unindent();
    out.println("}");
    out.println();
    Phase3Data p3d = new Phase3Data(e, la.getAmount());
    phase3list.add(p3d);
    phase3table.put(e, p3d);
  }

  private boolean xspDeclared;
  private Expansion jj3_expansion;

  private String genReturn(boolean value) {
    String s = value ? "true" : "false";
    if (Options.getDebugLookahead() && jj3_expansion != null) {
      String tracecode = "trace_return(\"" + ((NormalProduction) jj3_expansion.parent).getLhs() +
          "(LOOKAHEAD " + (value ? "FAILED" : "SUCCEEDED") + ")\");";
      if (Options.getErrorReporting()) {
        tracecode = "if (!jj_rescan) " + tracecode;
      }
      return "{ " + tracecode + " return " + s + "; }";
    }
    else {
      return "return " + s + ";";
    }
  }

  private void generate3R(Expansion e, Phase3Data data) {
    Expansion seq = e;
    if (e.internalName.equals("")) {
      while (true) {
        if (seq instanceof Sequence && ((Sequence) seq).units.size() == 2) {
          seq = ((Sequence) seq).units.get(1);
        }
        else if (seq instanceof NonTerminal) {
          NonTerminal e_nrw = (NonTerminal) seq;
          NormalProduction production = state.productionTable.get(e_nrw.getName());
          if (production instanceof JavaCodeProduction) {
            break; // nothing to do here
          }
          else {
            seq = production.getExpansion();
          }
        }
        else {
          break;
        }
      }

      if (seq instanceof RegularExpression) {
        e.internalName = "jj_scan_token(" + ((RegularExpression) seq).ordinal + ")";
        return;
      }

      genSymIndex++;

      e.internalName = "R_" + genSymIndex;
    }
    Phase3Data p3d = phase3table.get(e);
    if (p3d == null || p3d.count < data.count) {
      p3d = new Phase3Data(e, data.count);
      phase3list.add(p3d);
      phase3table.put(e, p3d);
    }
  }

  private void setupPhase3Builds(Phase3Data data) {
    Expansion expansion = data.expansion;
    if (expansion instanceof NonTerminal) {
      // All expansions of non-terminals have the "name" fields set.  So
      // there's no need to check it below for "e_nrw" and "ntexp".  In
      // fact, we rely here on the fact that the "name" fields of both these
      // variables are the same.
      NormalProduction production = state.productionTable.get(((NonTerminal) expansion).getName());
      if (production instanceof JavaCodeProduction) {
        // nothing to do here
      }
      else {
        generate3R(production.getExpansion(), data);
      }
    }
    else if (expansion instanceof RegularExpression) {
      // nothing to here
    }
    else if (expansion instanceof Sequence) {
      Sequence sequence = (Sequence) expansion;
      // We skip the first element in the following iteration since it is the
      // Lookahead object.
      int cnt = data.count;
      for (int i = 1; i < sequence.units.size(); i++) {
        Expansion unit = sequence.units.get(i);
        setupPhase3Builds(new Phase3Data(unit, cnt));
        cnt -= minimumSize(unit);
        if (cnt <= 0) {
          break;
        }
      }
    }
    else if (expansion instanceof Choice) {
      Choice choice = (Choice) expansion;
      for (int i = 0; i < choice.getChoices().size(); i++) {
        generate3R(choice.getChoices().get(i), data);
      }
    }
    else if (expansion instanceof ZeroOrOne) {
      generate3R(((ZeroOrOne) expansion).expansion, data);
    }
    else if (expansion instanceof ZeroOrMore) {
      generate3R(((ZeroOrMore) expansion).expansion, data);
    }
    else if (expansion instanceof OneOrMore) {
      generate3R(((OneOrMore) expansion).expansion, data);
    }
    else if (expansion instanceof TryBlock) {
      setupPhase3Builds(new Phase3Data(((TryBlock) expansion).expansion, data.count));
    }
  }

  private String genjj_3Call(Expansion e) {
    if (e.internalName.startsWith("jj_scan_token")) {
      return e.internalName;
    }
    else {
      return "jj_3" + e.internalName + "()";
    }
  }

  private void buildPhase3Routine(Phase3Data data, IndentingPrintWriter out)
      throws IOException {
    Expansion expansion = data.expansion;
    if (expansion.internalName.startsWith("jj_scan_token")) {
      return;
    }

    out.println("private boolean jj_3" + expansion.internalName + "() throws java.io.IOException {");
    out.indent();
    xspDeclared = false;
    if (Options.getDebugLookahead() && expansion.parent instanceof NormalProduction) {
      if (Options.getErrorReporting()) {
        out.print("if (!jj_rescan) ");
      }
      out.println("trace_call(\"" + ((NormalProduction) expansion.parent).getLhs() + "(LOOKING AHEAD...)\");");
      jj3_expansion = expansion;
    }
    else {
      jj3_expansion = null;
    }

    buildPhase3Statement(data, out);

    out.println(genReturn(false));
    out.unindent();
    out.println("}");
    out.println();
  }

  private void buildPhase3Statement(Phase3Data data, IndentingPrintWriter out)
      throws IOException {
    Expansion expansion = data.expansion;
    if (expansion.internalName.startsWith("jj_scan_token")) {
      return;
    }

    if (expansion instanceof NonTerminal) {
      phase3_NonTerminal((NonTerminal) expansion, out);
    }
    else if (expansion instanceof RegularExpression) {
      phase3_RegularExpression((RegularExpression) expansion, out);
    }
    else if (expansion instanceof Sequence) {
      phase3_Sequence((Sequence) expansion, data, out);
    }
    else if (expansion instanceof Choice) {
      phase3_Choice((Choice) expansion, out);
    }
    else if (expansion instanceof ZeroOrOne) {
      phase3_ZeroOrOne((ZeroOrOne) expansion, out);
    }
    else if (expansion instanceof ZeroOrMore) {
      phase3_ZeroOrMore((ZeroOrMore) expansion, out);
    }
    else if (expansion instanceof OneOrMore) {
      phase3_OneOrMore((OneOrMore) expansion, out);
    }
    else if (expansion instanceof TryBlock) {
      buildPhase3Statement(new Phase3Data(((TryBlock) expansion).expansion, data.count), out);
    }
  }

  private void phase3_NonTerminal(NonTerminal expansion, IndentingPrintWriter out) {
    // All expansions of non-terminals have the "name" fields set.  So
    // there's no need to check it below for "e_nrw" and "ntexp".  In
    // fact, we rely here on the fact that the "name" fields of both these
    // variables are the same.
    NormalProduction production = state.productionTable.get(expansion.getName());
    if (production instanceof JavaCodeProduction) {
      out.println("if (true) { jj_la = 0; jj_scanPos = jj_lastPos; " + genReturn(false) + "}");
    }
    else {
      Expansion nested = production.getExpansion();
      //out.println("if (jj_3" + nested.internal_name + "()) " + genReturn(true));
      out.println("if (" + genjj_3Call(nested) + ") " + genReturn(true));
      //out.println("if (jj_la == 0 && jj_scanPos == jj_lastPos) " + genReturn(false));
    }
  }

  private void phase3_RegularExpression(RegularExpression expansion, IndentingPrintWriter out) {
    if (expansion.label.equals("")) {
      String label = state.namesOfTokens.get(expansion.ordinal);
      if (label != null) {
        out.println("if (jj_scan_token(" + label + ")) " + genReturn(true));
      }
      else {
        out.println("if (jj_scan_token(" + expansion.ordinal + ")) " + genReturn(true));
      }
    }
    else {
      out.println("if (jj_scan_token(" + expansion.label + ")) " + genReturn(true));
    }
    //out.println("if (jj_la == 0 && jj_scanPos == jj_lastPos) " + genReturn(false));
  }

  private void phase3_Sequence(Sequence expansion, Phase3Data data, IndentingPrintWriter out)
      throws IOException {
    // We skip the first element in the following iteration since it is the
    // Lookahead object.
    int cnt = data.count;
    for (int i = 1; i < expansion.units.size(); i++) {
      Expansion unit = expansion.units.get(i);
      buildPhase3Statement(new Phase3Data(unit, cnt), out);
      cnt -= minimumSize(unit);
      if (cnt <= 0) {
        break;
      }
    }
  }

  private void phase3_Choice(Choice expansion, IndentingPrintWriter out)
      throws IOException {
    if (expansion.getChoices().size() != 1) {
      if (!xspDeclared) {
        xspDeclared = true;
        out.println("Token xsp;");
      }
      out.println("xsp = jj_scanPos;");
    }
    for (int i = 0; i < expansion.getChoices().size(); i++) {
      Sequence nested = (Sequence) expansion.getChoices().get(i);
      Lookahead la = (Lookahead) nested.units.get(0);
      if (la.getActionTokens().size() != 0) {
        // We have semantic lookahead that must be evaluated.
        lookaheadNeeded = true;
        out.println("jj_lookingAhead = true;");
        out.print("jj_semLA = ");
        TokenPrinter.printTokenSetup(la.getActionTokens().get(0));
        Token t = null;
        for (Token token : la.getActionTokens()) {
          t = token;
          TokenPrinter.printToken(t, out);
        }
        TokenPrinter.printTrailingComments(t);
        out.println(";");
        out.println("jj_lookingAhead = false;");
      }
      out.print("if (");
      if (la.getActionTokens().size() != 0) {
        out.print("!jj_semLA || ");
      }
      if (i != expansion.getChoices().size() - 1) {
        //out.println("jj_3" + nested.internal_name + "()) {");
        out.println(genjj_3Call(nested) + ") {");
        out.println("jj_scanPos = xsp;");
      }
      else {
        //out.println("jj_3" + nested.internal_name + "()) " + genReturn(true));
        out.println(genjj_3Call(nested) + ") " + genReturn(true));
        //out.println("if (jj_la == 0 && jj_scanPos == jj_lastPos) " + genReturn(false));
      }
    }
    for (int i = 1; i < expansion.getChoices().size(); i++) {
      //out.println("} else if (jj_la == 0 && jj_scanPos == jj_lastPos) " + genReturn(false));
      out.println("}");
    }
  }

  private void phase3_ZeroOrOne(ZeroOrOne expansion, IndentingPrintWriter out) {
    if (!xspDeclared) {
      xspDeclared = true;
      out.println("Token xsp;");
    }
    Expansion nested = expansion.expansion;
    out.println("xsp = jj_scanPos;");
    //out.println("if (jj_3" + nested.internal_name + "()) jj_scanPos = xsp;");
    out.println("if (" + genjj_3Call(nested) + ") jj_scanPos = xsp;");
    //out.println("else if (jj_la == 0 && jj_scanPos == jj_lastPos) " + genReturn(false));
  }

  private void phase3_ZeroOrMore(ZeroOrMore expansion, IndentingPrintWriter out) {
    if (!xspDeclared) {
      xspDeclared = true;
      out.println("Token xsp;");
    }
    Expansion nested = expansion.expansion;
    out.println("while (true) {");
    out.println("xsp = jj_scanPos;");
    //out.println("if (jj_3" + nested.internal_name + "()) { jj_scanPos = xsp; break; }");
    out.println("if (" + genjj_3Call(nested) + ") { jj_scanPos = xsp; break; }");
    //out.println("if (jj_la == 0 && jj_scanPos == jj_lastPos) " + genReturn(false));
    out.println("}");
  }

  private void phase3_OneOrMore(OneOrMore expansion, IndentingPrintWriter out) {
    if (!xspDeclared) {
      xspDeclared = true;
      out.println("Token xsp;");
    }
    Expansion nested = expansion.expansion;
    //out.println("if (jj_3" + nested.internal_name + "()) " + genReturn(true));
    out.println("if (" + genjj_3Call(nested) + ") " + genReturn(true));
    //out.println("if (jj_la == 0 && jj_scanPos == jj_lastPos) " + genReturn(false));
    out.println("while (true) {");
    out.println("xsp = jj_scanPos;");
    //out.println("if (jj_3" + nested.internal_name + "()) { jj_scanPos = xsp; break; }");
    out.println("if (" + genjj_3Call(nested) + ") { jj_scanPos = xsp; break; }");
    //out.println("if (jj_la == 0 && jj_scanPos == jj_lastPos) " + genReturn(false));
    out.println("}");
  }

  private int minimumSize(Expansion expansion) {
    return minimumSize(expansion, Integer.MAX_VALUE);
  }

  /** Returns the minimum number of tokens that can parse to this expansion. */
  private int minimumSize(Expansion expansion, int oldMin) {
    if (expansion.inMinimumSize) {
      // Recursive search for minimum size unnecessary.
      return Integer.MAX_VALUE;
    }
    expansion.inMinimumSize = true;
    int size = 0;
    if (expansion instanceof NonTerminal) {
      NonTerminal nonTerminal = (NonTerminal) expansion;
      NormalProduction production = state.productionTable.get(nonTerminal.getName());
      if (production instanceof JavaCodeProduction) {
        size = Integer.MAX_VALUE;
        // Make caller think this is unending (for we do not go beyond JAVACODE during
        // phase3 execution).
      }
      else {
        Expansion ntexp = production.getExpansion();
        size = minimumSize(ntexp);
      }
    }
    else if (expansion instanceof RegularExpression) {
      size = 1;
    }
    else if (expansion instanceof Sequence) {
      int min = 0;
      Sequence sequence = (Sequence) expansion;
      // We skip the first element in the following iteration since it is the
      // Lookahead object.
      for (int i = 1; i < sequence.units.size(); i++) {
        Expansion nested = sequence.units.get(i);
        int mineseq = minimumSize(nested);
        if (min == Integer.MAX_VALUE || mineseq == Integer.MAX_VALUE) {
          min = Integer.MAX_VALUE; // Adding infinity to something results in infinity.
        }
        else {
          min += mineseq;
          if (min > oldMin) {
            break;
          }
        }
      }
      size = min;
    }
    else if (expansion instanceof Choice) {
      int min = oldMin;
      Choice choice = (Choice) expansion;
      for (int i = 0; min > 1 && i < choice.getChoices().size(); i++) {
        Expansion nested = choice.getChoices().get(i);
        int min1 = minimumSize(nested, min);
        if (min > min1) {
          min = min1;
        }
      }
      size = min;
    }
    else if (expansion instanceof ZeroOrOne) {
      size = 0;
    }
    else if (expansion instanceof ZeroOrMore) {
      size = 0;
    }
    else if (expansion instanceof OneOrMore) {
      OneOrMore oneOrMore = (OneOrMore) expansion;
      size = minimumSize(oneOrMore.expansion);
    }
    else if (expansion instanceof TryBlock) {
      TryBlock tryBlock = (TryBlock) expansion;
      size = minimumSize(tryBlock.expansion);
    }
    else if (expansion instanceof Lookahead) {
      size = 0;
    }
    else if (expansion instanceof Action) {
      size = 0;
    }
    expansion.inMinimumSize = false;
    return size;
  }
}

