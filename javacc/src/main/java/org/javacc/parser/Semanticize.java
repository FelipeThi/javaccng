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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class Semanticize {
  private final JavaCCState state;
  private final LookaheadCalc lookaheadCalc = new LookaheadCalc();
  private final List<List> removeList = new ArrayList<List>();
  private final List itemList = new ArrayList();

  public Semanticize(JavaCCState state) {
    this.state = state;
  }

  void prepareToRemove(List vec, Object item) {
    removeList.add(vec);
    itemList.add(item);
  }

  void removePreparedItems() {
    for (int i = 0; i < removeList.size(); i++) {
      List list = removeList.get(i);
      list.remove(itemList.get(i));
    }
    removeList.clear();
    itemList.clear();
  }

  void start() throws MetaParseException {
    if (JavaCCErrors.getErrorCount() != 0) {
      throw new MetaParseException();
    }

    if (Options.getLookahead() > 1 && !Options.getForceLaCheck() && Options.getSanityCheck()) {
      JavaCCErrors.warning("Lookahead adequacy checking not being performed since option LOOKAHEAD " +
          "is more than 1.  Set option FORCE_LA_CHECK to true to force checking.");
    }

    /*
     * The following walks the entire parse tree to convert all LOOKAHEAD's
     * that are not at choice points (but at beginning of sequences) and converts
     * them to trivial choices.  This way, their semantic lookahead specification
     * can be evaluated during other lookahead evaluations.
     */
    for (NormalProduction production : state.bnfProductions) {
      ExpansionTreeWalker.postOrderWalk(production.getExpansion(),
          new LookaheadFixer());
    }

    /*
     * The following loop populates "production_table"
     */
    for (NormalProduction production : state.bnfProductions) {
      if (state.productionTable.put(production.getLhs(), production) != null) {
        JavaCCErrors.semanticError(production, production.getLhs() + " occurs on the left hand side of more than one production.");
      }
    }

    /*
     * The following walks the entire parse tree to make sure that all
     * non-terminals on RHS's are defined on the LHS.
     */
    for (NormalProduction production : state.bnfProductions) {
      ExpansionTreeWalker.preOrderWalk(production.getExpansion(),
          new ProductionDefinedChecker());
    }

    step93();

    removePreparedItems();

    step94();

    step95();

    removePreparedItems();

    step96();

    removePreparedItems();

    step97();

    removePreparedItems();

    step98();

    if (JavaCCErrors.getErrorCount() != 0) {
      throw new MetaParseException();
    }

    step99();

    step100();

    if (JavaCCErrors.getErrorCount() != 0) {
      throw new MetaParseException();
    }
  }

  private void step93() {
    // The following loop ensures that all target lexical states are
    // defined.  Also piggybacking on this loop is the detection of
    // <EOF> and <name> in token productions.  After reporting an
    // error, these entries are removed.  Also checked are definitions
    // on inline private regular expressions.
    // This loop works slightly differently when USER_SCANNER
    // is set to true.  In this case, <name> occurrences are OK, while
    // regular expression specs generate a warning.
    for (TokenProduction tp : state.regExpList) {
      List<RegExpSpec> reSpecs = tp.reSpecs;
      for (RegExpSpec reSpec : reSpecs) {
        if (reSpec.nextState != null) {
          if (state.lexStateS2I.get(reSpec.nextState) == null) {
            JavaCCErrors.semanticError(reSpec.nsToken, "Lexical state \"" + reSpec.nextState +
                "\" has not been defined.");
          }
        }
        if (reSpec.regExp instanceof REndOfFile) {
          //JavaCCErrors.semantic_error(res.rexp, "Badly placed <EOF>.");
          if (tp.lexStates != null) {
            JavaCCErrors.semanticError(reSpec.regExp, "EOF action/state change must be specified for all states, " +
                "i.e., <*>TOKEN:.");
          }
          if (tp.kind != TokenProduction.TOKEN) {
            JavaCCErrors.semanticError(reSpec.regExp, "EOF action/state change can be specified only in a " +
                "TOKEN specification.");
          }
          if (state.eofNextState != null || state.eofAction != null) {
            JavaCCErrors.semanticError(reSpec.regExp, "Duplicate action/state change specification for <EOF>.");
          }
          state.eofAction = reSpec.action;
          state.eofNextState = reSpec.nextState;
          prepareToRemove(reSpecs, reSpec);
        }
        else if (tp.explicit && Options.getUserScanner()) {
          JavaCCErrors.warning(reSpec.regExp, "Ignoring regular expression specification since " +
              "option USER_SCANNER has been set to true.");
        }
        else if (tp.explicit && !Options.getUserScanner() && reSpec.regExp instanceof RJustName) {
          JavaCCErrors.warning(reSpec.regExp, "Ignoring free-standing regular expression reference.  " +
              "If you really want this, you must give it a different label as <NEWLABEL:<"
              + reSpec.regExp.label + ">>.");
          prepareToRemove(reSpecs, reSpec);
        }
        else if (!tp.explicit && reSpec.regExp.isPrivate) {
          JavaCCErrors.semanticError(reSpec.regExp, "Private (#) regular expression cannot be defined within " +
              "grammar productions.");
        }
      }
    }
  }

  private void step94() {
    // The following loop inserts all names of regular expressions into
    // "named_tokens_table" and "ordered_named_tokens".
    // Duplications are flagged as errors.
    for (TokenProduction tp : state.regExpList) {
      List<RegExpSpec> reSpecs = tp.reSpecs;
      for (RegExpSpec reSpec : reSpecs) {
        if (!(reSpec.regExp instanceof RJustName) && !"".equals(reSpec.regExp.label)) {
          String s = reSpec.regExp.label;
          RegularExpression regExp = state.namedTokensTable.put(s, reSpec.regExp);
          if (regExp != null) {
            JavaCCErrors.semanticError(reSpec.regExp, "Multiply defined lexical token name \"" + s + "\".");
          }
          else {
            state.orderedNamedTokens.add(reSpec.regExp);
          }
          if (state.lexStateS2I.get(s) != null) {
            JavaCCErrors.semanticError(reSpec.regExp, "Lexical token name \"" + s + "\" is the same as " +
                "that of a lexical state.");
          }
        }
      }
    }
  }

  private void step95() {
    // The following code merges multiple uses of the same string in the same
    // lexical state and produces error messages when there are multiple
    // explicit occurrences (outside the BNF) of the string in the same
    // lexical state, or when within BNF occurrences of a string are duplicates
    // of those that occur as non-TOKEN's (SKIP, MORE, SPECIAL_TOKEN) or private
    // regular expressions.  While doing this, this code also numbers all
    // regular expressions (by setting their ordinal values), and populates the
    // table "names_of_tokens".

    state.tokenCount = 1;
    for (TokenProduction tp : state.regExpList) {
      List<RegExpSpec> reSpecs = tp.reSpecs;
      if (tp.lexStates == null) {
        tp.lexStates = new String[state.lexStateI2S.size()];
        int i = 0;
        for (String o : state.lexStateI2S.values()) {
          tp.lexStates[i++] = o;
        }
      }
      Map[] table = new Map[tp.lexStates.length];
      for (int i = 0; i < tp.lexStates.length; i++) {
        table[i] = state.simpleTokensTable.get(tp.lexStates[i]);
      }
      for (RegExpSpec reSpec : reSpecs) {
        if (reSpec.regExp instanceof RStringLiteral) {
          RStringLiteral sl = (RStringLiteral) reSpec.regExp;
          // This loop performs the checks and actions with respect to each lexical state.
          for (int i = 0; i < table.length; i++) {
            // Get table of all case variants of "sl.image" into table2.
            Map<String, RegularExpression> table2 = (Map<String, RegularExpression>) table[i].get(sl.image.toUpperCase());
            if (table2 == null) {
              // There are no case variants of "sl.image" earlier than the current one.
              // So go ahead and insert this item.
              if (sl.ordinal == 0) {
                sl.ordinal = state.tokenCount++;
              }
              table2 = new HashMap<String, RegularExpression>();
              table2.put(sl.image, sl);
              table[i].put(sl.image.toUpperCase(), table2);
            }
            else if (hasIgnoreCase(table2, sl.image)) {
              // hasIgnoreCase sets "other" if it is found.
              // Since IGNORE_CASE version exists, current one is useless and bad.
              if (!sl.tpContext.explicit) {
                // inline BNF string is used earlier with an IGNORE_CASE.
                JavaCCErrors.semanticError(sl, "String \"" + sl.image + "\" can never be matched " +
                    "due to presence of more general (IGNORE_CASE) regular expression " +
                    "at line " + other.getLine() + ", column " + other.getColumn() + ".");
              }
              else {
                // give the standard error message.
                JavaCCErrors.semanticError(sl, "Duplicate definition of string token \"" + sl.image + "\" " +
                    "can never be matched.");
              }
            }
            else if (sl.tpContext.ignoreCase) {
              // This has to be explicit.  A warning needs to be given with respect
              // to all previous strings.
              String pos = "";
              int count = 0;
              for (RegularExpression rexp : table2.values()) {
                if (count != 0) {
                  pos += ",";
                }
                pos += " line " + rexp.getLine();
                count++;
              }
              if (count == 1) {
                JavaCCErrors.warning(sl, "String with IGNORE_CASE is partially superceded by string at" + pos + ".");
              }
              else {
                JavaCCErrors.warning(sl, "String with IGNORE_CASE is partially superceded by strings at" + pos + ".");
              }
              // This entry is legitimate.  So insert it.
              if (sl.ordinal == 0) {
                sl.ordinal = state.tokenCount++;
              }
              table2.put(sl.image, sl);
              // The above "put" may override an existing entry (that is not IGNORE_CASE) and that's
              // the desired behavior.
            }
            else {
              // The rest of the cases do not involve IGNORE_CASE.
              RegularExpression re = table2.get(sl.image);
              if (re == null) {
                if (sl.ordinal == 0) {
                  sl.ordinal = state.tokenCount++;
                }
                table2.put(sl.image, sl);
              }
              else if (tp.explicit) {
                // This is an error even if the first occurrence was implicit.
                if (tp.lexStates[i].equals("DEFAULT")) {
                  JavaCCErrors.semanticError(sl, "Duplicate definition of string token \"" + sl.image + "\".");
                }
                else {
                  JavaCCErrors.semanticError(sl, "Duplicate definition of string token \"" + sl.image +
                      "\" in lexical state \"" + tp.lexStates[i] + "\".");
                }
              }
              else if (re.tpContext.kind != TokenProduction.TOKEN) {
                JavaCCErrors.semanticError(sl, "String token \"" + sl.image + "\" has been defined as a \"" +
                    TokenProduction.kindImage[re.tpContext.kind] + "\" token.");
              }
              else if (re.isPrivate) {
                JavaCCErrors.semanticError(sl, "String token \"" + sl.image +
                    "\" has been defined as a private regular expression.");
              }
              else {
                // This is now a legitimate reference to an existing RStringLiteral.
                // So we assign it a number and take it out of "rexprlist".
                // Therefore, if all is OK (no errors), then there will be only unequal
                // string literals in each lexical state.  Note that the only way
                // this can be legal is if this is a string declared inline within the
                // BNF.  Hence, it belongs to only one lexical state - namely "DEFAULT".
                sl.ordinal = re.ordinal;
                prepareToRemove(reSpecs, reSpec);
              }
            }
          }
        }
        else if (!(reSpec.regExp instanceof RJustName)) {
          reSpec.regExp.ordinal = state.tokenCount++;
        }
        if (!(reSpec.regExp instanceof RJustName) && !reSpec.regExp.label.equals("")) {
          state.namesOfTokens.put(reSpec.regExp.ordinal, reSpec.regExp.label);
        }
        if (!(reSpec.regExp instanceof RJustName)) {
          state.regExpsOfTokens.put(reSpec.regExp.ordinal, reSpec.regExp);
        }
      }
    }
  }

  private void step96() {// The following code performs a tree walk on all regular expressions
    // attaching links to "RJustName"s.  Error messages are given if
    // undeclared names are used, or if "RJustNames" refer to private
    // regular expressions or to regular expressions of any kind other
    // than TOKEN.  In addition, this loop also removes top level
    // "RJustName"s from "rexprlist".
    // This code is not executed if Options.getUserScanner() is set to
    // true.  Instead the following block of code is executed.

    if (!Options.getUserScanner()) {
      FixRJustNames frjn = new FixRJustNames();
      for (TokenProduction tp : state.regExpList) {
        List<RegExpSpec> reSpecs = tp.reSpecs;
        for (RegExpSpec reSpec : reSpecs) {
          frjn.root = reSpec.regExp;
          ExpansionTreeWalker.preOrderWalk(reSpec.regExp, frjn);
          if (reSpec.regExp instanceof RJustName) {
            prepareToRemove(reSpecs, reSpec);
          }
        }
      }
    }
  }

  private void step97() {
    // The following code is executed only if Options.getUserScanner() is
    // set to true.  This code visits all top-level "RJustName"s (ignores
    // "RJustName"s nested within regular expressions).  Since regular expressions
    // are optional in this case, "RJustName"s without corresponding regular
    // expressions are given ordinal values here.  If "RJustName"s refer to
    // a named regular expression, their ordinal values are set to reflect this.
    // All but one "RJustName" node is removed from the lists by the end of
    // execution of this code.

    if (Options.getUserScanner()) {
      for (TokenProduction tp : state.regExpList) {
        List<RegExpSpec> reSpecs = tp.reSpecs;
        for (RegExpSpec reSpec : reSpecs) {
          if (reSpec.regExp instanceof RJustName) {
            RJustName jn = (RJustName) reSpec.regExp;
            RegularExpression regExp = state.namedTokensTable.get(jn.label);
            if (regExp == null) {
              jn.ordinal = state.tokenCount++;
              state.namedTokensTable.put(jn.label, jn);
              state.orderedNamedTokens.add(jn);
              state.namesOfTokens.put(jn.ordinal, jn.label);
            }
            else {
              jn.ordinal = regExp.ordinal;
              prepareToRemove(reSpecs, reSpec);
            }
          }
        }
      }
    }
  }

  private void step98() {
    // The following code is executed only if Options.getUserScanner() is
    // set to true.  This loop labels any unlabeled regular expression and
    // prints a warning that it is doing so.  These labels are added to
    // "ordered_named_tokens" so that they may be generated into the ...Constants
    // file.
    if (Options.getUserScanner()) {
      for (TokenProduction tp : state.regExpList) {
        List<RegExpSpec> reSpecs = tp.reSpecs;
        for (RegExpSpec reSpec : reSpecs) {
          Integer ii = reSpec.regExp.ordinal;
          if (state.namesOfTokens.get(ii) == null) {
            JavaCCErrors.warning(reSpec.regExp, "Unlabeled regular expression cannot be referred to by " +
                "user generated scanner.");
          }
        }
      }
    }
  }

  private void step99() {
    // The following code sets the value of the "emptyPossible" field of NormalProduction
    // nodes.  This field is initialized to false, and then the entire list of
    // productions is processed.  This is repeated as long as at least one item
    // got updated from false to true in the pass.
    boolean emptyUpdate = true;
    while (emptyUpdate) {
      emptyUpdate = false;
      for (NormalProduction production : state.bnfProductions) {
        if (emptyExpansionExists(production.getExpansion())) {
          if (!production.isEmptyPossible()) {
            emptyUpdate = production.setEmptyPossible(true);
          }
        }
      }
    }
  }

  private void step100() {
    if (Options.getSanityCheck() && JavaCCErrors.getErrorCount() == 0) {
      // The following code checks that all ZeroOrMore, ZeroOrOne, and OneOrMore nodes
      // do not contain expansions that can expand to the empty token list.
      for (NormalProduction production : state.bnfProductions) {
        ExpansionTreeWalker.preOrderWalk(production.getExpansion(), new EmptyChecker());
      }

      // The following code goes through the productions and adds pointers to other
      // productions that it can expand to without consuming any tokens.  Once this is
      // done, a left-recursion check can be performed.
      for (NormalProduction production : state.bnfProductions) {
        addLeftMost(production, production.getExpansion());
      }

      // Now the following loop calls a recursive walk routine that searches for
      // actual left recursions.  The way the algorithm is coded, once a node has
      // been determined to participate in a left recursive loop, it is not tried
      // in any other loop.
      for (NormalProduction production : state.bnfProductions) {
        if (production.getWalkStatus() == 0) {
          prodWalk(production);
        }
      }

      // Now we do a similar, but much simpler walk for the regular expression part of
      // the grammar.  Here we are looking for any kind of loop, not just left recursions,
      // so we only need to do the equivalent of the above walk.
      // This is not done if option USER_SCANNER is set to true.
      if (!Options.getUserScanner()) {
        for (TokenProduction tp : state.regExpList) {
          List<RegExpSpec> reSpecs = tp.reSpecs;
          for (RegExpSpec reSpec : reSpecs) {
            RegularExpression regExp = reSpec.regExp;
            if (regExp.walkStatus == 0) {
              regExp.walkStatus = -1;
              if (regExpWalk(regExp)) {
                loopString = "..." + regExp.label + "... --> " + loopString;
                JavaCCErrors.semanticError(regExp, "Loop in regular expression detected: \"" + loopString + "\"");
              }
              regExp.walkStatus = 1;
            }
          }
        }
      }

      /*
       * The following code performs the lookahead ambiguity checking.
       */
      if (JavaCCErrors.getErrorCount() == 0) {
        for (NormalProduction bnfProduction : state.bnfProductions) {
          ExpansionTreeWalker.preOrderWalk(bnfProduction.getExpansion(),
              new LookaheadChecker());
        }
      }
    }
  }

  public RegularExpression other;

  // Checks to see if the "str" is superceded by another equal (except case) string
  // in table.
  public boolean hasIgnoreCase(Map<String, RegularExpression> table, String str) {
    RegularExpression rexp = table.get(str);
    if (rexp != null && !rexp.tpContext.ignoreCase) {
      return false;
    }
    for (RegularExpression regExp : table.values()) {
      rexp = regExp;
      if (rexp.tpContext.ignoreCase) {
        other = rexp;
        return true;
      }
    }
    return false;
  }

  // returns true if "exp" can expand to the empty string, returns false otherwise.
  public boolean emptyExpansionExists(Expansion exp) {
    if (exp instanceof NonTerminal) {
      return ((NonTerminal) exp).getProd().isEmptyPossible();
    }
    else if (exp instanceof Action) {
      return true;
    }
    else if (exp instanceof RegularExpression) {
      return false;
    }
    else if (exp instanceof OneOrMore) {
      return emptyExpansionExists(((OneOrMore) exp).expansion);
    }
    else if (exp instanceof ZeroOrMore || exp instanceof ZeroOrOne) {
      return true;
    }
    else if (exp instanceof Lookahead) {
      return true;
    }
    else if (exp instanceof Choice) {
      for (Iterator it = ((Choice) exp).getChoices().iterator(); it.hasNext(); ) {
        if (emptyExpansionExists((Expansion) it.next())) {
          return true;
        }
      }
      return false;
    }
    else if (exp instanceof Sequence) {
      for (Iterator it = ((Sequence) exp).units.iterator(); it.hasNext(); ) {
        if (!emptyExpansionExists((Expansion) it.next())) {
          return false;
        }
      }
      return true;
    }
    else if (exp instanceof TryBlock) {
      return emptyExpansionExists(((TryBlock) exp).expansion);
    }
    else {
      return false; // This should be dead code.
    }
  }

  // Updates prod.leftExpansions based on a walk of exp.
  private void addLeftMost(NormalProduction prod, Expansion exp) {
    if (exp instanceof NonTerminal) {
      for (int i = 0; i < prod.leIndex; i++) {
        if (prod.getLeftExpansions()[i] == ((NonTerminal) exp).getProd()) {
          return;
        }
      }
      if (prod.leIndex == prod.getLeftExpansions().length) {
        NormalProduction[] newle = new NormalProduction[prod.leIndex * 2];
        System.arraycopy(prod.getLeftExpansions(), 0, newle, 0, prod.leIndex);
        prod.setLeftExpansions(newle);
      }
      prod.getLeftExpansions()[prod.leIndex++] = ((NonTerminal) exp).getProd();
    }
    else if (exp instanceof OneOrMore) {
      addLeftMost(prod, ((OneOrMore) exp).expansion);
    }
    else if (exp instanceof ZeroOrMore) {
      addLeftMost(prod, ((ZeroOrMore) exp).expansion);
    }
    else if (exp instanceof ZeroOrOne) {
      addLeftMost(prod, ((ZeroOrOne) exp).expansion);
    }
    else if (exp instanceof Choice) {
      for (Iterator it = ((Choice) exp).getChoices().iterator(); it.hasNext(); ) {
        addLeftMost(prod, (Expansion) it.next());
      }
    }
    else if (exp instanceof Sequence) {
      for (Iterator it = ((Sequence) exp).units.iterator(); it.hasNext(); ) {
        Expansion e = (Expansion) it.next();
        addLeftMost(prod, e);
        if (!emptyExpansionExists(e)) {
          break;
        }
      }
    }
    else if (exp instanceof TryBlock) {
      addLeftMost(prod, ((TryBlock) exp).expansion);
    }
  }

  // The string in which the following methods store information.
  private String loopString;

  // Returns true to indicate an unraveling of a detected left recursion loop,
  // and returns false otherwise.
  private boolean prodWalk(NormalProduction prod) {
    prod.setWalkStatus(-1);
    for (int i = 0; i < prod.leIndex; i++) {
      if (prod.getLeftExpansions()[i].getWalkStatus() == -1) {
        prod.getLeftExpansions()[i].setWalkStatus(-2);
        loopString = prod.getLhs() + "... --> " + prod.getLeftExpansions()[i].getLhs() + "...";
        if (prod.getWalkStatus() == -2) {
          prod.setWalkStatus(1);
          JavaCCErrors.semanticError(prod, "Left recursion detected: \"" + loopString + "\"");
          return false;
        }
        else {
          prod.setWalkStatus(1);
          return true;
        }
      }
      else if (prod.getLeftExpansions()[i].getWalkStatus() == 0) {
        if (prodWalk(prod.getLeftExpansions()[i])) {
          loopString = prod.getLhs() + "... --> " + loopString;
          if (prod.getWalkStatus() == -2) {
            prod.setWalkStatus(1);
            JavaCCErrors.semanticError(prod, "Left recursion detected: \"" + loopString + "\"");
            return false;
          }
          else {
            prod.setWalkStatus(1);
            return true;
          }
        }
      }
    }
    prod.setWalkStatus(1);
    return false;
  }

  // Returns true to indicate an unraveling of a detected loop,
  // and returns false otherwise.
  private boolean regExpWalk(RegularExpression re) {
    if (re instanceof RJustName) {
      RJustName jn = (RJustName) re;
      if (jn.regExp.walkStatus == -1) {
        jn.regExp.walkStatus = -2;
        loopString = "..." + jn.regExp.label + "...";
        // Note: Only the regexpr's of RJustName nodes and the top leve
        // regexpr's can have labels.  Hence it is only in these cases that
        // the labels are checked for to be added to the loopString.
        return true;
      }
      else if (jn.regExp.walkStatus == 0) {
        jn.regExp.walkStatus = -1;
        if (regExpWalk(jn.regExp)) {
          loopString = "..." + jn.regExp.label + "... --> " + loopString;
          if (jn.regExp.walkStatus == -2) {
            jn.regExp.walkStatus = 1;
            JavaCCErrors.semanticError(jn.regExp, "Loop in regular expression detected: \"" + loopString + "\"");
            return false;
          }
          else {
            jn.regExp.walkStatus = 1;
            return true;
          }
        }
        else {
          jn.regExp.walkStatus = 1;
          return false;
        }
      }
    }
    else if (re instanceof RSequence) {
      for (RegularExpression unit : ((RSequence) re).units) {
        if (regExpWalk(unit)) {
          return true;
        }
      }
      return false;
    }
    else if (re instanceof RChoice) {
      for (RegularExpression unit : ((RChoice) re).getChoices()) {
        if (regExpWalk(unit)) {
          return true;
        }
      }
      return false;
    }
    else if (re instanceof RZeroOrOne) {
      return regExpWalk(((RZeroOrOne) re).regExp);
    }
    else if (re instanceof RZeroOrMore) {
      return regExpWalk(((RZeroOrMore) re).regExp);
    }
    else if (re instanceof ROneOrMore) {
      return regExpWalk(((ROneOrMore) re).regExp);
    }
    else if (re instanceof RRepetitionRange) {
      return regExpWalk(((RRepetitionRange) re).regExp);
    }
    return false;
  }

  /**
   * Objects of this class are created from class Semanticize to work on
   * references to regular expressions from RJustName's.
   */
  class FixRJustNames implements TreeWalkerOp {
    public RegularExpression root;

    @Override
    public boolean goDeeper(Expansion e) {
      return true;
    }

    @Override
    public void action(Expansion e) {
      if (e instanceof RJustName) {
        RJustName jn = (RJustName) e;
        RegularExpression regExp = state.namedTokensTable.get(jn.label);
        if (regExp == null) {
          JavaCCErrors.semanticError(e, "Undefined lexical token name \"" + jn.label + "\".");
        }
        else if (jn == root && !jn.tpContext.explicit && regExp.isPrivate) {
          JavaCCErrors.semanticError(e, "Token name \"" + jn.label + "\" refers to a private " +
              "(with a #) regular expression.");
        }
        else if (jn == root && !jn.tpContext.explicit && regExp.tpContext.kind != TokenProduction.TOKEN) {
          JavaCCErrors.semanticError(e, "Token name \"" + jn.label + "\" refers to a non-token " +
              "(SKIP, MORE, IGNORE_IN_BNF) regular expression.");
        }
        else {
          jn.ordinal = regExp.ordinal;
          jn.regExp = regExp;
        }
      }
    }
  }

  class LookaheadFixer implements TreeWalkerOp {
    @Override
    public boolean goDeeper(Expansion e) {
      if (e instanceof RegularExpression) {
        return false;
      }
      else {
        return true;
      }
    }

    @Override
    public void action(Expansion e) {
      if (e instanceof Sequence) {
        if (e.parent instanceof Choice || e.parent instanceof ZeroOrMore ||
            e.parent instanceof OneOrMore || e.parent instanceof ZeroOrOne) {
          return;
        }
        Sequence seq = (Sequence) e;
        Lookahead la = (Lookahead) (seq.units.get(0));
        if (!la.isExplicit()) {
          return;
        }
        // Create a singleton choice with an empty action.
        Choice ch = new Choice();
        ch.setLine(la.getLine());
        ch.setColumn(la.getColumn());
        ch.parent = seq;
        Sequence seq1 = new Sequence();
        seq1.setLine(la.getLine());
        seq1.setColumn(la.getColumn());
        seq1.parent = ch;
        seq1.units.add(la);
        la.parent = seq1;
        Action act = new Action();
        act.setLine(la.getLine());
        act.setColumn(la.getColumn());
        act.parent = seq1;
        seq1.units.add(act);
        ch.getChoices().add(seq1);
        if (la.getAmount() != 0) {
          if (la.getActionTokens().size() != 0) {
            JavaCCErrors.warning(la, "Encountered LOOKAHEAD(...) at a non-choice location.  " +
                "Only semantic lookahead will be considered here.");
          }
          else {
            JavaCCErrors.warning(la, "Encountered LOOKAHEAD(...) at a non-choice location.  This will be ignored.");
          }
        }
        // Now we have moved the lookahead into the singleton choice.  Now create
        // a new dummy lookahead node to replace this one at its original location.
        Lookahead la1 = new Lookahead();
        la1.setExplicit(false);
        la1.setLine(la.getLine());
        la1.setColumn(la.getColumn());
        la1.parent = seq;
        // Now set the la_expansion field of la and la1 with a dummy expansion (we use EOF).
        la.setLaExpansion(new REndOfFile());
        la1.setLaExpansion(new REndOfFile());
        seq.units.set(0, la1);
        seq.units.add(1, ch);
      }
    }
  }

  class ProductionDefinedChecker implements TreeWalkerOp {
    @Override
    public boolean goDeeper(Expansion e) {
      if (e instanceof RegularExpression) {
        return false;
      }
      else {
        return true;
      }
    }

    @Override
    public void action(Expansion exp) {
      if (exp instanceof NonTerminal) {
        NonTerminal nt = (NonTerminal) exp;
        if (nt.setProd(state.productionTable.get(nt.getName())) == null) {
          JavaCCErrors.semanticError(exp, "Non-terminal " + nt.getName() + " has not been defined.");
        }
        else {
          nt.getProd().getParents().add(nt);
        }
      }
    }
  }

  class EmptyChecker implements TreeWalkerOp {
    @Override
    public boolean goDeeper(Expansion e) {
      if (e instanceof RegularExpression) {
        return false;
      }
      else {
        return true;
      }
    }

    @Override
    public void action(Expansion e) {
      if (e instanceof OneOrMore) {
        if (emptyExpansionExists(((OneOrMore) e).expansion)) {
          JavaCCErrors.semanticError(e, "Expansion within \"(...)+\" can be matched by empty string.");
        }
      }
      else if (e instanceof ZeroOrMore) {
        if (emptyExpansionExists(((ZeroOrMore) e).expansion)) {
          JavaCCErrors.semanticError(e, "Expansion within \"(...)*\" can be matched by empty string.");
        }
      }
      else if (e instanceof ZeroOrOne) {
        if (emptyExpansionExists(((ZeroOrOne) e).expansion)) {
          JavaCCErrors.semanticError(e, "Expansion within \"(...)?\" can be matched by empty string.");
        }
      }
    }
  }

  class LookaheadChecker implements TreeWalkerOp {
    @Override
    public boolean goDeeper(Expansion e) {
      if (e instanceof RegularExpression) {
        return false;
      }
      else if (e instanceof Lookahead) {
        return false;
      }
      else {
        return true;
      }
    }

    @Override
    public void action(Expansion e) {
      if (e instanceof Choice) {
        if (Options.getLookahead() == 1 || Options.getForceLaCheck()) {
          lookaheadCalc.choiceCalc(state, Semanticize.this, (Choice) e);
        }
      }
      else if (e instanceof OneOrMore) {
        OneOrMore exp = (OneOrMore) e;
        if (Options.getForceLaCheck() || (implicitLA(exp.expansion) && Options.getLookahead() == 1)) {
          lookaheadCalc.ebnfCalc(state, exp, exp.expansion);
        }
      }
      else if (e instanceof ZeroOrMore) {
        ZeroOrMore exp = (ZeroOrMore) e;
        if (Options.getForceLaCheck() || (implicitLA(exp.expansion) && Options.getLookahead() == 1)) {
          lookaheadCalc.ebnfCalc(state, exp, exp.expansion);
        }
      }
      else if (e instanceof ZeroOrOne) {
        ZeroOrOne exp = (ZeroOrOne) e;
        if (Options.getForceLaCheck() || (implicitLA(exp.expansion) && Options.getLookahead() == 1)) {
          lookaheadCalc.ebnfCalc(state, exp, exp.expansion);
        }
      }
    }

    boolean implicitLA(Expansion exp) {
      if (!(exp instanceof Sequence)) {
        return true;
      }
      Sequence seq = (Sequence) exp;
      Expansion obj = seq.units.get(0);
      if (!(obj instanceof Lookahead)) {
        return true;
      }
      Lookahead la = (Lookahead) obj;
      return !la.isExplicit();
    }
  }
}
