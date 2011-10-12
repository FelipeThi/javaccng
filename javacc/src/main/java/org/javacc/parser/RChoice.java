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
import java.util.List;

/**
 * Describes regular expressions which are choices from
 * from among included regular expressions.
 */
public final class RChoice extends RegularExpression {
  /**
   * The list of choices of this regular expression.  Each
   * list component will narrow to RegularExpression.
   */
  private List<RegularExpression> choices = new ArrayList<RegularExpression>();

  public List<RegularExpression> getChoices() {
    return choices;
  }

  @Override
  public Nfa generateNfa(LexGen lexGen, boolean ignoreCase) {
    compressCharLists(lexGen);

    if (getChoices().size() == 1) {
      return getChoices().get(0).generateNfa(lexGen, ignoreCase);
    }

    Nfa nfa = new Nfa(lexGen);
    NfaState startState = nfa.start;
    NfaState finalState = nfa.end;

    for (RegularExpression re : choices) {
      Nfa tmp = re.generateNfa(lexGen, ignoreCase);
      startState.addMove(tmp.start);
      tmp.end.addMove(finalState);
    }

    return nfa;
  }

  private void compressCharLists(LexGen lexGen) {
    compressChoices(); // Unroll nested choices

    RCharacterList cl = null;
    for (int i = 0; i < getChoices().size(); i++) {
      RegularExpression re = getChoices().get(i);

      while (re instanceof RJustName) {
        re = ((RJustName) re).regExp;
      }

      if (re instanceof RStringLiteral
          && ((RStringLiteral) re).image.length() == 1) {
        getChoices().set(i, re = new RCharacterList(
            ((RStringLiteral) re).image.charAt(0)));
      }

      if (re instanceof RCharacterList) {
        if (((RCharacterList) re).negatedList) {
          ((RCharacterList) re).removeNegation(lexGen);
        }

        List tmp = ((RCharacterList) re).descriptors;

        if (cl == null) {
          getChoices().set(i, re = cl = new RCharacterList());
        }
        else {
          getChoices().remove(i--);
        }

        for (int j = tmp.size(); j-- > 0; ) {
          cl.descriptors.add(tmp.get(j));
        }
      }
    }
  }

  private void compressChoices() {
    for (int i = 0; i < getChoices().size(); i++) {
      RegularExpression re = getChoices().get(i);

      while (re instanceof RJustName) {
        re = ((RJustName) re).regExp;
      }

      if (re instanceof RChoice) {
        getChoices().remove(i--);

        for (int j = ((RChoice) re).getChoices().size(); j-- > 0; ) {
          getChoices().add(((RChoice) re).getChoices().get(j));
        }
      }
    }
  }

  public void checkUnmatchability(LexGen lexGen) {
    for (int i = 0; i < getChoices().size(); i++) {
      RegularExpression curRE;
      if (!(curRE = getChoices().get(i)).isPrivate
          && curRE.ordinal > 0 && curRE.ordinal < ordinal
          && lexGen.lexStates[curRE.ordinal] == lexGen.lexStates[ordinal]) {
        if (label != null) {
          JavaCCErrors.warning(this,
              "Regular Expression choice : " + curRE.label + " can never be matched as : " + label);
        }
        else {
          JavaCCErrors.warning(this,
              "Regular Expression choice : " + curRE.label + " can never be matched as token of kind : " + ordinal);
        }
      }
    }
  }
}
