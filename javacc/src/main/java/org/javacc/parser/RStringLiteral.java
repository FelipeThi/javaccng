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
import java.util.Set;

final class KindInfo
{
   long[] validKinds;
   long[] finalKinds;
   int    validKindCnt = 0;
   int    finalKindCnt = 0;

   KindInfo(int maxKind)
   {
      validKinds = new long[maxKind / 64 + 1];
      finalKinds = new long[maxKind / 64 + 1];
   }

   public void InsertValidKind(int kind)
   {
      validKinds[kind / 64] |= (1L << (kind % 64));
      validKindCnt++;
   }

   public void InsertFinalKind(int kind)
   {
      finalKinds[kind / 64] |= (1L << (kind % 64));
      finalKindCnt++;
   }
};

/**
 * Describes string literals.
 */

public class RStringLiteral extends RegularExpression {

  /**
   * The string image of the literal.
   */
  public String image;

  public RStringLiteral() {
  }

  public RStringLiteral(Token t, String image) {
    this.setLine(t.getBeginLine());
    this.setColumn(t.getBeginColumn());
    this.image = image;
  }

  /**
   * Used for top level string literals.
   */
  public void GenerateDfa(LexGen lexGen, IndentingPrintWriter ostr, int kind)
  {
     String s;
     Hashtable temp;
     KindInfo info;
     int len;

     if (lexGen.stringLiterals.maxStrKind <= ordinal)
        lexGen.stringLiterals.maxStrKind = ordinal + 1;

     if ((len = image.length()) > lexGen.stringLiterals.maxLen)
        lexGen.stringLiterals.maxLen = len;

     char c;
     for (int i = 0; i < len; i++)
     {
        if (Options.getIgnoreCase())
           s = ("" + (c = image.charAt(i))).toLowerCase();
        else
           s = "" + (c = image.charAt(i));

        if (!lexGen.nfaStates.unicodeWarningGiven && c > 0xff &&
            !Options.getJavaUnicodeEscape() &&
            !Options.getUserCharStream())
        {
           lexGen.nfaStates.unicodeWarningGiven = true;
           JavaCCErrors.warning(lexGen.curRE, "Non-ASCII characters used in regular expression." +
              "Please make sure you use the correct Reader when you create the parser, " +
              "one that can handle your character set.");
        }

        if (i >= lexGen.stringLiterals.charPosKind.size()) // Kludge, but OK
           lexGen.stringLiterals.charPosKind.add(temp = new Hashtable());
        else
           temp = (Hashtable) lexGen.stringLiterals.charPosKind.get(i);

        if ((info = (KindInfo)temp.get(s)) == null)
           temp.put(s, info = new KindInfo(lexGen.maxOrdinal));

        if (i + 1 == len)
           info.InsertFinalKind(ordinal);
        else
           info.InsertValidKind(ordinal);

        if (!Options.getIgnoreCase() && lexGen.ignoreCase[ordinal] &&
            c != Character.toLowerCase(c))
        {
           s = ("" + image.charAt(i)).toLowerCase();

           if (i >= lexGen.stringLiterals.charPosKind.size()) // Kludge, but OK
              lexGen.stringLiterals.charPosKind.add(temp = new Hashtable());
           else
              temp = (Hashtable) lexGen.stringLiterals.charPosKind.get(i);

           if ((info = (KindInfo)temp.get(s)) == null)
              temp.put(s, info = new KindInfo(lexGen.maxOrdinal));

           if (i + 1 == len)
              info.InsertFinalKind(ordinal);
           else
              info.InsertValidKind(ordinal);
        }

        if (!Options.getIgnoreCase() && lexGen.ignoreCase[ordinal] &&
            c != Character.toUpperCase(c))
        {
           s = ("" + image.charAt(i)).toUpperCase();

           if (i >= lexGen.stringLiterals.charPosKind.size()) // Kludge, but OK
              lexGen.stringLiterals.charPosKind.add(temp = new Hashtable());
           else
              temp = (Hashtable) lexGen.stringLiterals.charPosKind.get(i);

           if ((info = (KindInfo)temp.get(s)) == null)
              temp.put(s, info = new KindInfo(lexGen.maxOrdinal));

           if (i + 1 == len)
              info.InsertFinalKind(ordinal);
           else
              info.InsertValidKind(ordinal);
        }
     }

     lexGen.stringLiterals.maxLenForActive[ordinal / 64] = Math.max(lexGen.stringLiterals.maxLenForActive[ordinal / 64],
                                                                        len -1);
     lexGen.stringLiterals.allImages[ordinal] = image;
  }

  public Nfa GenerateNfa(final LexGen lexGen, boolean ignoreCase)
  {
     if (image.length() == 1)
     {
        RCharacterList temp = new RCharacterList(image.charAt(0));
        return temp.GenerateNfa(lexGen, ignoreCase);
     }

     NfaState startState = new NfaState(lexGen);
     NfaState theStartState = startState;
     NfaState finalState = null;

     if (image.length() == 0)
        return new Nfa(theStartState, theStartState);

     int i;

     for (i = 0; i < image.length(); i++)
     {
        finalState = new NfaState(lexGen);
        startState.charMoves = new char[1];
        startState.AddChar(image.charAt(i));

        if (Options.getIgnoreCase() || ignoreCase)
        {
           startState.AddChar(Character.toLowerCase(image.charAt(i)));
           startState.AddChar(Character.toUpperCase(image.charAt(i)));
        }

        startState.next = finalState;
        startState = finalState;
     }

     return new Nfa(theStartState, finalState);
  }

  public StringBuffer dump(int indent, Set alreadyDumped) {
    StringBuffer sb = super.dump(indent, alreadyDumped).append(' ').append(image);
    return sb;
  }

  public String toString() {
    return super.toString() + " - " + image;
  }
}
