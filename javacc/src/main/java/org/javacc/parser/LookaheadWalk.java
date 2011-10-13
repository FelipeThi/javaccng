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

public final class LookaheadWalk {
  public boolean considerSemanticLA;
  public ArrayList<MatchInfo> sizeLimitedMatches;
  /**
   * To avoid right-recursive loops when calculating follow sets, we use
   * a generation number which indicates if this expansion was visited
   * by LookaheadWalk.genFollowSet in the same generation.  New generations
   * are obtained by incrementing the static counter below, and the current
   * generation is stored in the non-static variable below.
   */
  public long nextGenerationIndex = 1;
  public int laLimit;

  public List<MatchInfo> genFirstSet(List<MatchInfo> partialMatches, Expansion exp) {
    if (exp instanceof RegularExpression) {
      List<MatchInfo> retval = new ArrayList<MatchInfo>();
      for (MatchInfo partialMatch : partialMatches) {
        MatchInfo mnew = new MatchInfo(laLimit);
        for (int j = 0; j < partialMatch.firstFreeLoc; j++) {
          mnew.match[j] = partialMatch.match[j];
        }
        mnew.firstFreeLoc = partialMatch.firstFreeLoc;
        mnew.match[mnew.firstFreeLoc++] = ((RegularExpression) exp).ordinal;
        if (mnew.firstFreeLoc == laLimit) {
          sizeLimitedMatches.add(mnew);
        }
        else {
          retval.add(mnew);
        }
      }
      return retval;
    }
    else if (exp instanceof NonTerminal) {
      NormalProduction production = ((NonTerminal) exp).getProd();
      if (production instanceof JavaCodeProduction) {
        return new ArrayList<MatchInfo>();
      }
      else {
        return genFirstSet(partialMatches, production.getExpansion());
      }
    }
    else if (exp instanceof Choice) {
      List<MatchInfo> retval = new ArrayList<MatchInfo>();
      Choice choice = (Choice) exp;
      for (int i = 0; i < choice.getChoices().size(); i++) {
        List<MatchInfo> v = genFirstSet(partialMatches, choice.getChoices().get(i));
        retval.addAll(v);
      }
      return retval;
    }
    else if (exp instanceof Sequence) {
      List<MatchInfo> v = partialMatches;
      Sequence seq = (Sequence) exp;
      for (int i = 0; i < seq.units.size(); i++) {
        v = genFirstSet(v, seq.units.get(i));
        if (v.size() == 0) {
          break;
        }
      }
      return v;
    }
    else if (exp instanceof OneOrMore) {
      List<MatchInfo> retval = new ArrayList<MatchInfo>();
      List<MatchInfo> v = partialMatches;
      OneOrMore oneOrMore = (OneOrMore) exp;
      while (true) {
        v = genFirstSet(v, oneOrMore.expansion);
        if (v.size() == 0) {
          break;
        }
        retval.addAll(v);
      }
      return retval;
    }
    else if (exp instanceof ZeroOrMore) {
      List<MatchInfo> retval = new ArrayList<MatchInfo>();
      retval.addAll(partialMatches);
      List<MatchInfo> v = partialMatches;
      ZeroOrMore zeroOrMore = (ZeroOrMore) exp;
      while (true) {
        v = genFirstSet(v, zeroOrMore.expansion);
        if (v.size() == 0) {
          break;
        }
        retval.addAll(v);
      }
      return retval;
    }
    else if (exp instanceof ZeroOrOne) {
      List<MatchInfo> retval = new ArrayList<MatchInfo>();
      retval.addAll(partialMatches);
      retval.addAll(genFirstSet(partialMatches, ((ZeroOrOne) exp).expansion));
      return retval;
    }
    else if (exp instanceof TryBlock) {
      return genFirstSet(partialMatches, ((TryBlock) exp).expansion);
    }
    else if (considerSemanticLA
        && exp instanceof Lookahead
        && ((Lookahead) exp).getActionTokens().size() != 0) {
      return new ArrayList<MatchInfo>();
    }
    else {
      List<MatchInfo> retval = new ArrayList<MatchInfo>();
      retval.addAll(partialMatches);
      return retval;
    }
  }

  private void listSplit(List<MatchInfo> toSplit, List mask, List partInMask, List rest) {
    outer:
    for (Object aToSplit : toSplit) {
      for (Object aMask : mask) {
        if (aToSplit == aMask) {
          partInMask.add(aToSplit);
          continue outer;
        }
      }
      rest.add(aToSplit);
    }
  }

  public List<MatchInfo> genFollowSet(List<MatchInfo> partialMatches, Expansion exp, long generation) {
    if (exp.myGeneration == generation) {
      return new ArrayList<MatchInfo>();
    }
    exp.myGeneration = generation;
    if (exp.parent == null) {
      List<MatchInfo> retval = new ArrayList<MatchInfo>();
      retval.addAll(partialMatches);
      return retval;
    }
    else if (exp.parent instanceof NormalProduction) {
      List parents = ((NormalProduction) exp.parent).getParents();
      List<MatchInfo> retval = new ArrayList<MatchInfo>();
      for (Object parent : parents) {
        List<MatchInfo> v = genFollowSet(partialMatches, (Expansion) parent, generation);
        retval.addAll(v);
      }
      return retval;
    }
    else if (exp.parent instanceof Sequence) {
      Sequence seq = (Sequence) exp.parent;
      List<MatchInfo> v = partialMatches;
      for (int i = exp.ordinal + 1; i < seq.units.size(); i++) {
        v = genFirstSet(v, seq.units.get(i));
        if (v.size() == 0) { return v; }
      }
      List<MatchInfo> v1 = new ArrayList<MatchInfo>();
      List<MatchInfo> v2 = new ArrayList<MatchInfo>();
      listSplit(v, partialMatches, v1, v2);
      if (v1.size() != 0) {
        v1 = genFollowSet(v1, seq, generation);
      }
      if (v2.size() != 0) {
        v2 = genFollowSet(v2, seq, nextGenerationIndex++);
      }
      v2.addAll(v1);
      return v2;
    }
    else if (exp.parent instanceof OneOrMore
        || exp.parent instanceof ZeroOrMore) {
      List<MatchInfo> moreMatches = new ArrayList<MatchInfo>();
      moreMatches.addAll(partialMatches);
      List v = partialMatches;
      while (true) {
        v = genFirstSet(v, exp);
        if (v.size() == 0) { break; }
        moreMatches.addAll(v);
      }
      List<MatchInfo> v1 = new ArrayList<MatchInfo>();
      List<MatchInfo> v2 = new ArrayList<MatchInfo>();
      listSplit(moreMatches, partialMatches, v1, v2);
      if (v1.size() != 0) {
        v1 = genFollowSet(v1, (Expansion) exp.parent, generation);
      }
      if (v2.size() != 0) {
        v2 = genFollowSet(v2, (Expansion) exp.parent, nextGenerationIndex++);
      }
      v2.addAll(v1);
      return v2;
    }
    else {
      return genFollowSet(partialMatches, (Expansion) exp.parent, generation);
    }
  }
}
