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

package org.javacc.jjdoc;

import org.javacc.parser.Action;
import org.javacc.parser.BNFProduction;
import org.javacc.parser.CharacterRange;
import org.javacc.parser.Choice;
import org.javacc.parser.Expansion;
import org.javacc.parser.FileGenerator;
import org.javacc.parser.JavaCCState;
import org.javacc.parser.JavaCodeProduction;
import org.javacc.parser.Lookahead;
import org.javacc.parser.NonTerminal;
import org.javacc.parser.NormalProduction;
import org.javacc.parser.OneOrMore;
import org.javacc.parser.RCharacterList;
import org.javacc.parser.RChoice;
import org.javacc.parser.REndOfFile;
import org.javacc.parser.RJustName;
import org.javacc.parser.ROneOrMore;
import org.javacc.parser.RRepetitionRange;
import org.javacc.parser.RSequence;
import org.javacc.parser.RStringLiteral;
import org.javacc.parser.RZeroOrMore;
import org.javacc.parser.RZeroOrOne;
import org.javacc.parser.RegExpSpec;
import org.javacc.parser.RegularExpression;
import org.javacc.parser.Sequence;
import org.javacc.parser.SingleCharacter;
import org.javacc.parser.Token;
import org.javacc.parser.TokenPrinter;
import org.javacc.parser.TokenProduction;
import org.javacc.parser.TryBlock;
import org.javacc.parser.ZeroOrMore;
import org.javacc.parser.ZeroOrOne;
import org.javacc.utils.Parsers;

import java.util.Iterator;
import java.util.List;

public class JJDoc implements FileGenerator {
  private final JavaCCState state;
  private final Formatter f;

  public JJDoc(JavaCCState state, Formatter f) {
    this.state = state;
    this.f = f;
  }

  @Override
  public void start() {
    f.documentStart();
    emitTokenProductions(state.tokenProductions);
    emitNormalProductions(state.bnfProductions);
    f.documentEnd();
  }

  private Token getPrecedingSpecialToken(Token token) {
    Token t = token;
    while (t.specialToken != null) {
      t = t.specialToken;
    }
    return t != token ? t : null;
  }

  private void emitTopLevelSpecialTokens(Token token) {
    if (token == null) {
      // Strange ...
      return;
    }
    token = getPrecedingSpecialToken(token);
    String s = "";
    if (token != null) {
      TokenPrinter.cLine = token.getBeginLine();
      TokenPrinter.cCol = token.getBeginColumn();
      while (token != null) {
        s += TokenPrinter.printTokenOnly(token);
        token = token.next;
      }
    }
    if (!"".equals(s)) {
      f.specialTokens(s);
    }
  }

  private void emitTokenProductions(List<TokenProduction> prods) {
    f.tokensStart();
    // FIXME there are many empty productions here
    for (TokenProduction tp : prods) {
      emitTopLevelSpecialTokens(tp.firstToken);

      String token = "";
      if (tp.explicit) {
        if (tp.lexStates == null) {
          token += "<*> ";
        }
        else {
          token += "<";
          for (int i = 0; i < tp.lexStates.length; ++i) {
            token += tp.lexStates[i];
            if (i < tp.lexStates.length - 1) {
              token += ",";
            }
          }
          token += "> ";
        }
        token += TokenProduction.kindImage[tp.kind];
        if (tp.ignoreCase) {
          token += " [IGNORE_CASE]";
        }
        token += " : {\n";
        for (Iterator it2 = tp.reSpecs.iterator(); it2.hasNext(); ) {
          RegExpSpec res = (RegExpSpec) it2.next();

          token += emitRE(res.regExp);

          if (res.nsToken != null) {
            token += " : " + res.nsToken.getImage();
          }

          token += "\n";
          if (it2.hasNext()) {
            token += "| ";
          }
        }
        token += "}\n\n";
      }
      if (!"".equals(token)) {
        f.tokenStart(tp);
        f.text(token);
        f.tokenEnd(tp);
      }
    }
    f.tokensEnd();
  }

  private void emitNormalProductions(List<NormalProduction> prods) {
    f.nonterminalsStart();
    for (NormalProduction np : prods) {
      emitTopLevelSpecialTokens(np.getFirstToken());
      if (np instanceof BNFProduction) {
        f.productionStart(np);
        if (np.getExpansion() instanceof Choice) {
          boolean first = true;
          Choice c = (Choice) np.getExpansion();
          for (Expansion expansion : c.getChoices()) {
            f.expansionStart(expansion, first);
            emitExpansionTree(expansion);
            f.expansionEnd(expansion, first);
            first = false;
          }
        }
        else {
          f.expansionStart(np.getExpansion(), true);
          emitExpansionTree(np.getExpansion());
          f.expansionEnd(np.getExpansion(), true);
        }
        f.productionEnd(np);
      }
      else if (np instanceof JavaCodeProduction) {
        f.javacode((JavaCodeProduction) np);
      }
    }
    f.nonterminalsEnd();
  }

  private void emitExpansionTree(Expansion exp) {
    if (exp instanceof Action) {
      emitExpansionAction((Action) exp);
    }
    else if (exp instanceof Choice) {
      emitExpansionChoice((Choice) exp);
    }
    else if (exp instanceof Lookahead) {
      emitExpansionLookahead((Lookahead) exp);
    }
    else if (exp instanceof NonTerminal) {
      emitExpansionNonTerminal((NonTerminal) exp);
    }
    else if (exp instanceof OneOrMore) {
      emitExpansionOneOrMore((OneOrMore) exp);
    }
    else if (exp instanceof RegularExpression) {
      emitExpansionRegularExpression((RegularExpression) exp);
    }
    else if (exp instanceof Sequence) {
      emitExpansionSequence((Sequence) exp);
    }
    else if (exp instanceof TryBlock) {
      emitExpansionTryBlock((TryBlock) exp);
    }
    else if (exp instanceof ZeroOrMore) {
      emitExpansionZeroOrMore((ZeroOrMore) exp);
    }
    else if (exp instanceof ZeroOrOne) {
      emitExpansionZeroOrOne((ZeroOrOne) exp);
    }
    else {
      throw new IllegalStateException("Unreachable: unknown expansion type");
    }
  }

  private void emitExpansionAction(Action a) {}

  private void emitExpansionChoice(Choice c) {
    for (Iterator<Expansion> it = c.getChoices().iterator(); it.hasNext(); ) {
      Expansion e = it.next();
      emitExpansionTree(e);
      if (it.hasNext()) {
        f.text(" | ");
      }
    }
  }

  private void emitExpansionLookahead(Lookahead l) {}

  private void emitExpansionNonTerminal(NonTerminal nt) {
    f.nonTerminalStart(nt);
    f.text(nt.getName());
    f.nonTerminalEnd(nt);
  }

  private void emitExpansionOneOrMore(OneOrMore o) {
    f.text("( ");
    emitExpansionTree(o.expansion);
    f.text(" )+");
  }

  private void emitExpansionRegularExpression(RegularExpression r) {
    String reRendered = emitRE(r);
    if (!"".equals(reRendered)) {
      f.reStart(r);
      f.text(reRendered);
      f.reEnd(r);
    }
  }

  private void emitExpansionSequence(Sequence s) {
    boolean firstUnit = true;
    for (Expansion expansion : s.units) {
      if (expansion instanceof Lookahead || expansion instanceof Action) {
        continue;
      }
      if (!firstUnit) {
        f.text(" ");
      }
      boolean needParens = expansion instanceof Choice
          || expansion instanceof Sequence;
      if (needParens) {
        f.text("( ");
      }
      emitExpansionTree(expansion);
      if (needParens) {
        f.text(" )");
      }
      firstUnit = false;
    }
  }

  private void emitExpansionTryBlock(TryBlock t) {
    boolean needParens = t.expansion instanceof Choice;
    if (needParens) {
      f.text("( ");
    }
    emitExpansionTree(t.expansion);
    if (needParens) {
      f.text(" )");
    }
  }

  private void emitExpansionZeroOrMore(ZeroOrMore z) {
    f.text("( ");
    emitExpansionTree(z.expansion);
    f.text(" )*");
  }

  private void emitExpansionZeroOrOne(ZeroOrOne z) {
    f.text("( ");
    emitExpansionTree(z.expansion);
    f.text(" )?");
  }

  private String emitRE(RegularExpression re) {
    String returnString = "";
    boolean hasLabel = !"".equals(re.label);
    boolean justName = re instanceof RJustName;
    boolean eof = re instanceof REndOfFile;
    boolean isString = re instanceof RStringLiteral;
    boolean toplevelRE = re.tpContext != null;
    boolean needBrackets
        = justName || eof || hasLabel || !isString && toplevelRE;
    if (needBrackets) {
      returnString += "<";
      if (!justName) {
        if (re.isPrivate) {
          returnString += "#";
        }
        if (hasLabel) {
          returnString += re.label;
          returnString += ": ";
        }
      }
    }
    if (re instanceof RCharacterList) {
      RCharacterList cl = (RCharacterList) re;
      if (cl.negatedList) {
        returnString += "~";
      }
      returnString += "[";
      for (Iterator it = cl.descriptors.iterator(); it.hasNext(); ) {
        Object o = it.next();
        if (o instanceof SingleCharacter) {
          returnString += "\"";
          char[] s = {((SingleCharacter) o).ch};
          returnString += Parsers.escape(new String(s));
          returnString += "\"";
        }
        else if (o instanceof CharacterRange) {
          returnString += "\"";
          char[] s = {((CharacterRange) o).getLeft()};
          returnString += Parsers.escape(new String(s));
          returnString += "\"-\"";
          s[0] = ((CharacterRange) o).getRight();
          returnString += Parsers.escape(new String(s));
          returnString += "\"";
        }
        else {
          throw new IllegalStateException("Unreachable: unknown character list element type");
        }
        if (it.hasNext()) {
          returnString += ",";
        }
      }
      returnString += "]";
    }
    else if (re instanceof RChoice) {
      RChoice c = (RChoice) re;
      for (Iterator<RegularExpression> it = c.getChoices().iterator(); it.hasNext(); ) {
        RegularExpression sub = it.next();
        returnString += emitRE(sub);
        if (it.hasNext()) {
          returnString += " | ";
        }
      }
    }
    else if (re instanceof REndOfFile) {
      returnString += "EOF";
    }
    else if (re instanceof RJustName) {
      RJustName jn = (RJustName) re;
      returnString += jn.label;
    }
    else if (re instanceof ROneOrMore) {
      ROneOrMore om = (ROneOrMore) re;
      returnString += "(";
      returnString += emitRE(om.regExp);
      returnString += ")+";
    }
    else if (re instanceof RSequence) {
      RSequence s = (RSequence) re;
      for (Iterator<RegularExpression> it = s.units.iterator(); it.hasNext(); ) {
        RegularExpression sub = it.next();
        boolean needParens = false;
        if (sub instanceof RChoice) {
          needParens = true;
        }
        if (needParens) {
          returnString += "(";
        }
        returnString += emitRE(sub);
        if (needParens) {
          returnString += ")";
        }
        if (it.hasNext()) {
          returnString += " ";
        }
      }
    }
    else if (re instanceof RStringLiteral) {
      RStringLiteral sl = (RStringLiteral) re;
      returnString += "\"" + Parsers.escape(sl.image) + "\"";
    }
    else if (re instanceof RZeroOrMore) {
      RZeroOrMore zm = (RZeroOrMore) re;
      returnString += "(";
      returnString += emitRE(zm.regExp);
      returnString += ")*";
    }
    else if (re instanceof RZeroOrOne) {
      RZeroOrOne zo = (RZeroOrOne) re;
      returnString += "(";
      returnString += emitRE(zo.regExp);
      returnString += ")?";
    }
    else if (re instanceof RRepetitionRange) {
      RRepetitionRange zo = (RRepetitionRange) re;
      returnString += "(";
      returnString += emitRE(zo.regExp);
      returnString += ")";
      returnString += "{";
      if (zo.hasMax) {
        returnString += zo.min;
        returnString += ",";
        returnString += zo.max;
      }
      else {
        returnString += zo.min;
      }
      returnString += "}";
    }
    else {
      throw new IllegalStateException("Unreachable: unknown regular expression type");
    }
    if (needBrackets) {
      returnString += ">";
    }
    return returnString;
  }
}
