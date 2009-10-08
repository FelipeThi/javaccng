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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public class Semanticize extends JavaCCGlobals {
  static List removeList = new ArrayList();
  static List itemList = new ArrayList();

  static void prepareToRemove(List vec, Object item) {
    removeList.add(vec);
    itemList.add(item);
  }

  static void removePreparedItems() {
    for (int i = 0; i < removeList.size(); i++) {
      List list = (List) (removeList.get(i));
      list.remove(itemList.get(i));
    }
    removeList.clear();
    itemList.clear();
  }

  static public void start() throws MetaParseException {
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
    for (final Object bnfproduction : bnfproductions) {
      ExpansionTreeWalker.postOrderWalk(((NormalProduction) bnfproduction).getExpansion(),
                                        new LookaheadFixer());
    }

    /*
     * The following loop populates "production_table"
     */
    for (final Object bnfproduction : bnfproductions) {
      NormalProduction p = (NormalProduction) bnfproduction;
      if (production_table.put(p.getLhs(), p) != null) {
        JavaCCErrors.semanticError(p, p.getLhs() + " occurs on the left hand side of more than one production.");
      }
    }

    /*
     * The following walks the entire parse tree to make sure that all
     * non-terminals on RHS's are defined on the LHS.
     */
    for (final Object bnfproduction : bnfproductions) {
      ExpansionTreeWalker.preOrderWalk(((NormalProduction) bnfproduction).getExpansion(),
                                       new ProductionDefinedChecker());
    }

    /*
     * The following loop ensures that all target lexical states are
     * defined.  Also piggybacking on this loop is the detection of
     * <EOF> and <name> in token productions.  After reporting an
     * error, these entries are removed.  Also checked are definitions
     * on inline private regular expressions.
     * This loop works slightly differently when USER_TOKEN_MANAGER
     * is set to true.  In this case, <name> occurrences are OK, while
     * regular expression specs generate a warning.
     */
    for (final Object aRexprlist : rexprlist) {
      TokenProduction tp = (TokenProduction) (aRexprlist);
      List respecs = tp.respecs;
      for (final Object respec : respecs) {
        RegExpSpec res = (RegExpSpec) (respec);
        if (res.nextState != null) {
          if (lexstate_S2I.get(res.nextState) == null) {
            JavaCCErrors.semanticError(res.nextStateToken, "Lexical state \"" + res.nextState +
                "\" has not been defined.");
          }
        }
        if (res.regexp instanceof REndOfFile) {
          //JavaCCErrors.semantic_error(res.regexp, "Badly placed <EOF>.");
          if (tp.lexStates != null) {
            JavaCCErrors.semanticError(res.regexp, "EOF action/state change must be specified for all states, " +
                "i.e., <*>TOKEN:.");
          }
          if (tp.kind != TokenProduction.TOKEN) {
            JavaCCErrors.semanticError(res.regexp, "EOF action/state change can be specified only in a " +
                "TOKEN specification.");
          }
          if (nextStateForEof != null || actForEof != null) {
            JavaCCErrors.semanticError(res.regexp, "Duplicate action/state change specification for <EOF>.");
          }
          actForEof = res.action;
          nextStateForEof = res.nextState;
          prepareToRemove(respecs, res);
        }
        else if (tp.isExplicit && Options.getUserTokenManager()) {
          JavaCCErrors.warning(res.regexp, "Ignoring regular expression specification since " +
              "option USER_TOKEN_MANAGER has been set to true.");
        }
        else if (tp.isExplicit && !Options.getUserTokenManager() && res.regexp instanceof RJustName) {
          JavaCCErrors.warning(res.regexp, "Ignoring free-standing regular expression reference.  " +
              "If you really want this, you must give it a different label as <NEWLABEL:<"
              + res.regexp.label + ">>.");
          prepareToRemove(respecs, res);
        }
        else if (!tp.isExplicit && res.regexp.private_rexp) {
          JavaCCErrors.semanticError(res.regexp, "Private (#) regular expression cannot be defined within " +
              "grammar productions.");
        }
      }
    }

    removePreparedItems();

    /*
     * The following loop inserts all names of regular expressions into
     * "named_tokens_table" and "ordered_named_tokens".
     * Duplications are flagged as errors.
     */
    for (final Object aRexprlist : rexprlist) {
      TokenProduction tp = (TokenProduction) (aRexprlist);
      List respecs = tp.respecs;
      for (final Object respec : respecs) {
        RegExpSpec res = (RegExpSpec) (respec);
        if (!(res.regexp instanceof RJustName) && !res.regexp.label.equals("")) {
          String s = res.regexp.label;
          Object obj = named_tokens_table.put(s, res.regexp);
          if (obj != null) {
            JavaCCErrors.semanticError(res.regexp, "Multiply defined lexical token name \"" + s + "\".");
          }
          else {
            ordered_named_tokens.add(res.regexp);
          }
          if (lexstate_S2I.get(s) != null) {
            JavaCCErrors.semanticError(res.regexp, "Lexical token name \"" + s + "\" is the same as " +
                "that of a lexical state.");
          }
        }
      }
    }

    /*
     * The following code merges multiple uses of the same string in the same
     * lexical state and produces error messages when there are multiple
     * explicit occurrences (outside the BNF) of the string in the same
     * lexical state, or when within BNF occurrences of a string are duplicates
     * of those that occur as non-TOKEN's (SKIP, MORE, SPECIAL_TOKEN) or private
     * regular expressions.  While doing this, this code also numbers all
     * regular expressions (by setting their ordinal values), and populates the
     * table "names_of_tokens".
     */

    tokenCount = 1;
    for (final Object aRexprlist : rexprlist) {
      TokenProduction tp = (TokenProduction) (aRexprlist);
      List respecs = tp.respecs;
      if (tp.lexStates == null) {
        tp.lexStates = new String[lexstate_I2S.size()];
        int i = 0;
        for (Enumeration enum1 = lexstate_I2S.elements(); enum1.hasMoreElements();) {
          tp.lexStates[i++] = (String) (enum1.nextElement());
        }
      }
      Hashtable table[] = new Hashtable[tp.lexStates.length];
      for (int i = 0; i < tp.lexStates.length; i++) {
        table[i] = (Hashtable) simple_tokens_table.get(tp.lexStates[i]);
      }
      for (Iterator it1 = respecs.iterator(); it1.hasNext();) {
        RegExpSpec res = (RegExpSpec) (it1.next());
        if (res.regexp instanceof RStringLiteral) {
          RStringLiteral sl = (RStringLiteral) res.regexp;
          // This loop performs the checks and actions with respect to each lexical state.
          for (int i = 0; i < table.length; i++) {
            // Get table of all case variants of "sl.image" into table2.
            Hashtable table2 = (Hashtable) (table[i].get(sl.image.toUpperCase()));
            if (table2 == null) {
              // There are no case variants of "sl.image" earlier than the current one.
              // So go ahead and insert this item.
              if (sl.ordinal == 0) {
                sl.ordinal = tokenCount++;
              }
              table2 = new Hashtable();
              table2.put(sl.image, sl);
              table[i].put(sl.image.toUpperCase(), table2);
            }
            else if (hasIgnoreCase(table2, sl.image)) { // hasIgnoreCase sets "other" if it is found.
              // Since IGNORE_CASE version exists, current one is useless and bad.
              if (!sl.tpContext.isExplicit) {
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
              for (Enumeration enum2 = table2.elements(); enum2.hasMoreElements();) {
                RegularExpression regexp = (RegularExpression) (enum2.nextElement());
                if (count != 0) {
                  pos += ",";
                }
                pos += " line " + regexp.getLine();
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
                sl.ordinal = tokenCount++;
              }
              table2.put(sl.image, sl);
              // The above "put" may override an existing entry (that is not IGNORE_CASE) and that's
              // the desired behavior.
            }
            else {
              // The rest of the cases do not involve IGNORE_CASE.
              RegularExpression re = (RegularExpression) table2.get(sl.image);
              if (re == null) {
                if (sl.ordinal == 0) {
                  sl.ordinal = tokenCount++;
                }
                table2.put(sl.image, sl);
              }
              else if (tp.isExplicit) {
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
              else if (re.private_rexp) {
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
                prepareToRemove(respecs, res);
              }
            }
          }
        }
        else if (!(res.regexp instanceof RJustName)) {
          res.regexp.ordinal = tokenCount++;
        }
        if (!(res.regexp instanceof RJustName) && !res.regexp.label.equals("")) {
          names_of_tokens.put(new Integer(res.regexp.ordinal), res.regexp.label);
        }
        if (!(res.regexp instanceof RJustName)) {
          rexps_of_tokens.put(new Integer(res.regexp.ordinal), res.regexp);
        }
      }
    }

    removePreparedItems();

    /*
     * The following code performs a tree walk on all regular expressions
     * attaching links to "RJustName"s.  Error messages are given if
     * undeclared names are used, or if "RJustNames" refer to private
     * regular expressions or to regular expressions of any kind other
     * than TOKEN.  In addition, this loop also removes top level
     * "RJustName"s from "rexprlist".
     * This code is not executed if Options.getUserTokenManager() is set to
     * true.  Instead the following block of code is executed.
     */

    if (!Options.getUserTokenManager()) {
      FixRJustNames frjn = new FixRJustNames();
      for (final Object aRexprlist : rexprlist) {
        TokenProduction tp = (TokenProduction) (aRexprlist);
        List respecs = tp.respecs;
        for (final Object respec : respecs) {
          RegExpSpec res = (RegExpSpec) (respec);
          frjn.root = res.regexp;
          ExpansionTreeWalker.preOrderWalk(res.regexp, frjn);
          if (res.regexp instanceof RJustName) {
            prepareToRemove(respecs, res);
          }
        }
      }
    }

    removePreparedItems();

    /*
     * The following code is executed only if Options.getUserTokenManager() is
     * set to true.  This code visits all top-level "RJustName"s (ignores
     * "RJustName"s nested within regular expressions).  Since regular expressions
     * are optional in this case, "RJustName"s without corresponding regular
     * expressions are given ordinal values here.  If "RJustName"s refer to
     * a named regular expression, their ordinal values are set to reflect this.
     * All but one "RJustName" node is removed from the lists by the end of
     * execution of this code.
     */

    if (Options.getUserTokenManager()) {
      for (final Object aRexprlist : rexprlist) {
        TokenProduction tp = (TokenProduction) (aRexprlist);
        List respecs = tp.respecs;
        for (final Object respec : respecs) {
          RegExpSpec res = (RegExpSpec) (respec);
          if (res.regexp instanceof RJustName) {

            RJustName jn = (RJustName) res.regexp;
            RegularExpression regexp = (RegularExpression) named_tokens_table.get(jn.label);
            if (regexp == null) {
              jn.ordinal = tokenCount++;
              named_tokens_table.put(jn.label, jn);
              ordered_named_tokens.add(jn);
              names_of_tokens.put(new Integer(jn.ordinal), jn.label);
            }
            else {
              jn.ordinal = regexp.ordinal;
              prepareToRemove(respecs, res);
            }
          }
        }
      }
    }

    removePreparedItems();

    /*
     * The following code is executed only if Options.getUserTokenManager() is
     * set to true.  This loop labels any unlabeled regular expression and
     * prints a warning that it is doing so.  These labels are added to
     * "ordered_named_tokens" so that they may be generated into the ...Constants
     * file.
     */
    if (Options.getUserTokenManager()) {
      for (final Object aRexprlist : rexprlist) {
        TokenProduction tp = (TokenProduction) (aRexprlist);
        List respecs = tp.respecs;
        for (final Object respec : respecs) {
          RegExpSpec res = (RegExpSpec) (respec);
          Integer ii = new Integer(res.regexp.ordinal);
          if (names_of_tokens.get(ii) == null) {
            JavaCCErrors.warning(res.regexp, "Unlabeled regular expression cannot be referred to by " +
                "user generated token manager.");
          }
        }
      }
    }

    if (JavaCCErrors.getErrorCount() != 0) {
      throw new MetaParseException();
    }

    // The following code sets the value of the "emptyPossible" field of NormalProduction
    // nodes.  This field is initialized to false, and then the entire list of
    // productions is processed.  This is repeated as long as at least one item
    // got updated from false to true in the pass.
    boolean emptyUpdate = true;
    while (emptyUpdate) {
      emptyUpdate = false;
      for (final Object bnfproduction : bnfproductions) {
        NormalProduction prod = (NormalProduction) bnfproduction;
        if (emptyExpansionExists(prod.getExpansion())) {
          if (!prod.isEmptyPossible()) {
            emptyUpdate = prod.setEmptyPossible(true);
          }
        }
      }
    }

    if (Options.getSanityCheck() && JavaCCErrors.getErrorCount() == 0) {

      // The following code checks that all ZeroOrMore, ZeroOrOne, and OneOrMore nodes
      // do not contain expansions that can expand to the empty token list.
      for (final Object bnfproduction : bnfproductions) {
        ExpansionTreeWalker.preOrderWalk(((NormalProduction) bnfproduction).getExpansion(), new EmptyChecker());
      }

      // The following code goes through the productions and adds pointers to other
      // productions that it can expand to without consuming any tokens.  Once this is
      // done, a left-recursion check can be performed.
      for (final Object bnfproduction : bnfproductions) {
        NormalProduction prod = (NormalProduction) bnfproduction;
        addLeftMost(prod, prod.getExpansion());
      }

      // Now the following loop calls a recursive walk routine that searches for
      // actual left recursions.  The way the algorithm is coded, once a node has
      // been determined to participate in a left recursive loop, it is not tried
      // in any other loop.
      for (final Object bnfproduction : bnfproductions) {
        NormalProduction prod = (NormalProduction) bnfproduction;
        if (prod.getWalkStatus() == 0) {
          prodWalk(prod);
        }
      }

      // Now we do a similar, but much simpler walk for the regular expression part of
      // the grammar.  Here we are looking for any kind of loop, not just left recursions,
      // so we only need to do the equivalent of the above walk.
      // This is not done if option USER_TOKEN_MANAGER is set to true.
      if (!Options.getUserTokenManager()) {
        for (final Object aRexprlist : rexprlist) {
          TokenProduction tp = (TokenProduction) (aRexprlist);
          List respecs = tp.respecs;
          for (final Object respec : respecs) {
            RegExpSpec res = (RegExpSpec) (respec);
            RegularExpression regexp = res.regexp;
            if (regexp.walkStatus == 0) {
              regexp.walkStatus = -1;
              if (rexpWalk(regexp)) {
                loopString = "..." + regexp.label + "... --> " + loopString;
                JavaCCErrors.semanticError(regexp, "Loop in regular expression detected: \"" + loopString + "\"");
              }
              regexp.walkStatus = 1;
            }
          }
        }
      }

      /*
       * The following code performs the lookahead ambiguity checking.
       */
      if (JavaCCErrors.getErrorCount() == 0) {
        for (final Object bnfproduction : bnfproductions) {
          ExpansionTreeWalker.preOrderWalk(((NormalProduction) bnfproduction).getExpansion(),
                                           new LookaheadChecker());
        }
      }
    } // matches "if (Options.getSanityCheck()) {"

    if (JavaCCErrors.getErrorCount() != 0) {
      throw new MetaParseException();
    }
  }

  public static RegularExpression other;

  // Checks to see if the "str" is superceded by another equal (except case) string
  // in table.
  public static boolean hasIgnoreCase(Hashtable table, String str) {
    RegularExpression regexp = (RegularExpression) (table.get(str));
    if (regexp != null && !regexp.tpContext.ignoreCase) {
      return false;
    }
    for (Enumeration enumeration = table.elements(); enumeration.hasMoreElements();) {
      regexp = (RegularExpression) (enumeration.nextElement());
      if (regexp.tpContext.ignoreCase) {
        other = regexp;
        return true;
      }
    }
    return false;
  }

  // returns true if "expansion" can expand to the empty string, returns false otherwise.
  public static boolean emptyExpansionExists(Expansion exp) {
    if (exp instanceof NonTerminal) {
      return ((NonTerminal) exp).getProduction().isEmptyPossible();
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
      for (final Object o : ((Choice) exp).getChoices()) {
        if (emptyExpansionExists((Expansion) o)) {
          return true;
        }
      }
      return false;
    }
    else if (exp instanceof Sequence) {
      for (final Object unit : ((Sequence) exp).units) {
        if (!emptyExpansionExists((Expansion) unit)) {
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

  // Updates prod.leftExpansions based on a walk of expansion.
  static private void addLeftMost(NormalProduction prod, Expansion exp) {
    if (exp instanceof NonTerminal) {
      for (int i = 0; i < prod.leIndex; i++) {
        if (prod.getLeftExpansions()[i] == ((NonTerminal) exp).getProduction()) {
          return;
        }
      }
      if (prod.leIndex == prod.getLeftExpansions().length) {
        NormalProduction[] newle = new NormalProduction[prod.leIndex * 2];
        System.arraycopy(prod.getLeftExpansions(), 0, newle, 0, prod.leIndex);
        prod.setLeftExpansions(newle);
      }
      prod.getLeftExpansions()[prod.leIndex++] = ((NonTerminal) exp).getProduction();
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
      for (final Object o : ((Choice) exp).getChoices()) {
        addLeftMost(prod, (Expansion) o);
      }
    }
    else if (exp instanceof Sequence) {
      for (final Object unit : ((Sequence) exp).units) {
        Expansion e = (Expansion) unit;
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
  static private String loopString;

  // Returns true to indicate an unraveling of a detected left recursion loop,
  // and returns false otherwise.
  static private boolean prodWalk(NormalProduction prod) {
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
  static private boolean rexpWalk(RegularExpression regexp) {
    if (regexp instanceof RJustName) {
      RJustName jn = (RJustName) regexp;
      if (jn.regexp.walkStatus == -1) {
        jn.regexp.walkStatus = -2;
        loopString = "..." + jn.regexp.label + "...";
        // Note: Only the regexp's of RJustName nodes and the top leve
        // regexp's can have labels.  Hence it is only in these cases that
        // the labels are checked for to be added to the loopString.
        return true;
      }
      else if (jn.regexp.walkStatus == 0) {
        jn.regexp.walkStatus = -1;
        if (rexpWalk(jn.regexp)) {
          loopString = "..." + jn.regexp.label + "... --> " + loopString;
          if (jn.regexp.walkStatus == -2) {
            jn.regexp.walkStatus = 1;
            JavaCCErrors.semanticError(jn.regexp, "Loop in regular expression detected: \"" + loopString + "\"");
            return false;
          }
          else {
            jn.regexp.walkStatus = 1;
            return true;
          }
        }
        else {
          jn.regexp.walkStatus = 1;
          return false;
        }
      }
    }
    else if (regexp instanceof RChoice) {
      for (final Object o : ((RChoice) regexp).getChoices()) {
        if (rexpWalk((RegularExpression) o)) {
          return true;
        }
      }
      return false;
    }
    else if (regexp instanceof RSequence) {
      for (final Object unit : ((RSequence) regexp).units) {
        if (rexpWalk((RegularExpression) unit)) {
          return true;
        }
      }
      return false;
    }
    else if (regexp instanceof ROneOrMore) {
      return rexpWalk(((ROneOrMore) regexp).regexp);
    }
    else if (regexp instanceof RZeroOrMore) {
      return rexpWalk(((RZeroOrMore) regexp).regexp);
    }
    else if (regexp instanceof RZeroOrOne) {
      return rexpWalk(((RZeroOrOne) regexp).regexp);
    }
    else if (regexp instanceof RRepetitionRange) {
      return rexpWalk(((RRepetitionRange) regexp).regexp);
    }
    return false;
  }

  /**
   * Objects of this class are created from class Semanticize to work on
   * references to regular expressions from RJustName's.
   */
  static class FixRJustNames extends JavaCCGlobals implements TreeWalkerOp {
    public RegularExpression root;

    public boolean goDeeper(Expansion expansion) {
      return true;
    }

    public void action(Expansion expansion) {
      if (expansion instanceof RJustName) {
        RJustName jn = (RJustName) expansion;
        RegularExpression regexp = (RegularExpression) named_tokens_table.get(jn.label);
        if (regexp == null) {
          JavaCCErrors.semanticError(expansion, "Undefined lexical token name \"" + jn.label + "\".");
        }
        else if (jn == root && !jn.tpContext.isExplicit && regexp.private_rexp) {
          JavaCCErrors.semanticError(expansion, "Token name \"" + jn.label + "\" refers to a private " +
              "(with a #) regular expression.");
        }
        else if (jn == root && !jn.tpContext.isExplicit && regexp.tpContext.kind != TokenProduction.TOKEN) {
          JavaCCErrors.semanticError(expansion, "Token name \"" + jn.label + "\" refers to a non-token " +
              "(SKIP, MORE, IGNORE_IN_BNF) regular expression.");
        }
        else {
          jn.ordinal = regexp.ordinal;
          jn.regexp = regexp;
        }
      }
    }
  }

  static class LookaheadFixer extends JavaCCGlobals implements TreeWalkerOp {

    public boolean goDeeper(Expansion expansion) {
      if (expansion instanceof RegularExpression) {
        return false;
      }
      else {
        return true;
      }
    }

    public void action(Expansion expansion) {
      if (expansion instanceof Sequence) {
        if (expansion.parent instanceof Choice || expansion.parent instanceof ZeroOrMore ||
            expansion.parent instanceof OneOrMore || expansion.parent instanceof ZeroOrOne) {
          return;
        }
        Sequence seq = (Sequence) expansion;
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

  static class ProductionDefinedChecker extends JavaCCGlobals implements TreeWalkerOp {

    public boolean goDeeper(Expansion expansion) {
      if (expansion instanceof RegularExpression) {
        return false;
      }
      else {
        return true;
      }
    }

    public void action(Expansion expansion) {
      if (expansion instanceof NonTerminal) {
        NonTerminal nt = (NonTerminal) expansion;
        if ((nt.setProduction((NormalProduction) production_table.get(nt.getName()))) == null) {
          JavaCCErrors.semanticError(expansion, "Non-terminal " + nt.getName() + " has not been defined.");
        }
        else {
          nt.getProduction().getParents().add(nt);
        }
      }
    }
  }

  static class EmptyChecker extends JavaCCGlobals implements TreeWalkerOp {

    public boolean goDeeper(Expansion expansion) {
      if (expansion instanceof RegularExpression) {
        return false;
      }
      else {
        return true;
      }
    }

    public void action(Expansion expansion) {
      if (expansion instanceof OneOrMore) {
        if (Semanticize.emptyExpansionExists(((OneOrMore) expansion).expansion)) {
          JavaCCErrors.semanticError(expansion, "Expansion within \"(...)+\" can be matched by empty string.");
        }
      }
      else if (expansion instanceof ZeroOrMore) {
        if (Semanticize.emptyExpansionExists(((ZeroOrMore) expansion).expansion)) {
          JavaCCErrors.semanticError(expansion, "Expansion within \"(...)*\" can be matched by empty string.");
        }
      }
      else if (expansion instanceof ZeroOrOne) {
        if (Semanticize.emptyExpansionExists(((ZeroOrOne) expansion).expansion)) {
          JavaCCErrors.semanticError(expansion, "Expansion within \"(...)?\" can be matched by empty string.");
        }
      }
    }
  }

  static class LookaheadChecker extends JavaCCGlobals implements TreeWalkerOp {

    public boolean goDeeper(Expansion expansion) {
      if (expansion instanceof RegularExpression) {
        return false;
      }
      else if (expansion instanceof Lookahead) {
        return false;
      }
      else {
        return true;
      }
    }

    public void action(Expansion expansion) {
      if (expansion instanceof Choice) {
        if (Options.getLookahead() == 1 || Options.getForceLaCheck()) {
          LookaheadCalc.choiceCalc((Choice) expansion);
        }
      }
      else if (expansion instanceof OneOrMore) {
        OneOrMore exp = (OneOrMore) expansion;
        if (Options.getForceLaCheck() || (implicitLA(exp.expansion) && Options.getLookahead() == 1)) {
          LookaheadCalc.ebnfCalc(exp, exp.expansion);
        }
      }
      else if (expansion instanceof ZeroOrMore) {
        ZeroOrMore exp = (ZeroOrMore) expansion;
        if (Options.getForceLaCheck() || (implicitLA(exp.expansion) && Options.getLookahead() == 1)) {
          LookaheadCalc.ebnfCalc(exp, exp.expansion);
        }
      }
      else if (expansion instanceof ZeroOrOne) {
        ZeroOrOne exp = (ZeroOrOne) expansion;
        if (Options.getForceLaCheck() || (implicitLA(exp.expansion) && Options.getLookahead() == 1)) {
          LookaheadCalc.ebnfCalc(exp, exp.expansion);
        }
      }
    }

    static boolean implicitLA(Expansion exp) {
      if (!(exp instanceof Sequence)) {
        return true;
      }
      Sequence seq = (Sequence) exp;
      Object obj = seq.units.get(0);
      if (!(obj instanceof Lookahead)) {
        return true;
      }
      Lookahead la = (Lookahead) obj;
      return !la.isExplicit();
    }
  }

  public static void reInit() {
    removeList = new ArrayList();
    itemList = new ArrayList();
    other = null;
    loopString = null;
  }
}
