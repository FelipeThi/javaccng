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

import java.util.Hashtable;
import java.util.List;

/** Utilities. */
public abstract class JavaCCParserInternals {
  protected static void initialize() {
    Integer i = new Integer(0);
    JavaCCGlobals.lexStateS2I.put("DEFAULT", i);
    JavaCCGlobals.lexStateI2S.put(i, "DEFAULT");
    JavaCCGlobals.simpleTokensTable.put("DEFAULT", new Hashtable());
  }

  protected static void addCuName(String id) {
    JavaCCGlobals.cuName = id;
  }

  protected static void compare(Token t, String id1, String id2) {
    if (!id2.equals(id1)) {
      JavaCCErrors.parseError(t, "Name " + id2 + " must be the same as that used at PARSER_BEGIN (" + id1 + ")");
    }
  }

  private List addCuTokenHere = JavaCCGlobals.cuToInsertionPoint1;
  private Token firstCuToken;
  private boolean insertionPoint1Set = false;
  private boolean insertionPoint2Set = false;

  protected void setInsertionPoint(Token t, int no) {
    do {
      addCuTokenHere.add(firstCuToken);
      firstCuToken = firstCuToken.next;
    }
    while (firstCuToken != t);
    if (no == 1) {
      if (insertionPoint1Set) {
        JavaCCErrors.parseError(t, "Multiple declaration of parser class.");
      }
      else {
        insertionPoint1Set = true;
        addCuTokenHere = JavaCCGlobals.cuToInsertionPoint2;
      }
    }
    else {
      addCuTokenHere = JavaCCGlobals.cuFromInsertionPoint2;
      insertionPoint2Set = true;
    }
    firstCuToken = t;
  }

  protected void insertionpointerrors(Token t) {
    while (firstCuToken != t) {
      addCuTokenHere.add(firstCuToken);
      firstCuToken = firstCuToken.next;
    }
    if (!insertionPoint1Set || !insertionPoint2Set) {
      JavaCCErrors.parseError(t, "Parser class has not been defined between PARSER_BEGIN and PARSER_END.");
    }
  }

  protected void set_initial_cu_token(Token t) {
    firstCuToken = t;
  }

  protected void addproduction(NormalProduction p) {
    JavaCCGlobals.bnfProductions.add(p);
  }

  protected static void production_addexpansion(BNFProduction p, Expansion e) {
    e.parent = p;
    p.setExpansion(e);
  }

  private int nextFreeLexState = 1;

  protected void addregexpr(TokenProduction p) {
    Integer ii;
    JavaCCGlobals.regExpList.add(p);
    if (Options.getUserTokenManager()) {
      if (p.lexStates == null || p.lexStates.length != 1 || !p.lexStates[0].equals("DEFAULT")) {
        JavaCCErrors.warning(p, "Ignoring lexical state specifications since option " +
            "USER_TOKEN_MANAGER has been set to true.");
      }
    }
    if (p.lexStates == null) {
      return;
    }
    for (int i = 0; i < p.lexStates.length; i++) {
      for (int j = 0; j < i; j++) {
        if (p.lexStates[i].equals(p.lexStates[j])) {
          JavaCCErrors.parseError(p, "Multiple occurrence of \"" + p.lexStates[i] + "\" in lexical state list.");
        }
      }
      if (JavaCCGlobals.lexStateS2I.get(p.lexStates[i]) == null) {
        ii = new Integer(nextFreeLexState++);
        JavaCCGlobals.lexStateS2I.put(p.lexStates[i], ii);
        JavaCCGlobals.lexStateI2S.put(ii, p.lexStates[i]);
        JavaCCGlobals.simpleTokensTable.put(p.lexStates[i], new Hashtable());
      }
    }
  }

  protected static void add_token_manager_decls(Token t, java.util.List decls) {
    if (JavaCCGlobals.tokenManagerDeclarations != null) {
      JavaCCErrors.parseError(t, "Multiple occurrence of \"TOKEN_MGR_DECLS\".");
    }
    else {
      JavaCCGlobals.tokenManagerDeclarations = decls;
      if (Options.getUserTokenManager()) {
        JavaCCErrors.warning(t, "Ignoring declarations in \"TOKEN_MGR_DECLS\" since option " +
            "USER_TOKEN_MANAGER has been set to true.");
      }
    }
  }

  protected static void add_inline_regexpr(RegularExpression r) {
    if (!(r instanceof REndOfFile)) {
      TokenProduction p = new TokenProduction();
      p.explicit = false;
      p.lexStates = new String[]{"DEFAULT"};
      p.kind = TokenProduction.TOKEN;
      RegExpSpec res = new RegExpSpec();
      res.regExp = r;
      res.regExp.tpContext = p;
      res.action = new Action();
      res.nextState = null;
      res.nsToken = null;
      p.reSpecs.add(res);
      JavaCCGlobals.regExpList.add(p);
    }
  }

  protected static boolean hexchar(char ch) {
    if (ch >= '0' && ch <= '9') { return true; }
    if (ch >= 'A' && ch <= 'F') { return true; }
    if (ch >= 'a' && ch <= 'f') { return true; }
    return false;
  }

  protected static int hexval(char ch) {
    if (ch >= '0' && ch <= '9') { return ((int) ch) - ((int) '0'); }
    if (ch >= 'A' && ch <= 'F') { return ((int) ch) - ((int) 'A') + 10; }
    return ((int) ch) - ((int) 'a') + 10;
  }

  protected static String remove_escapes_and_quotes(Token t, String str) {
    String retval = "";
    int index = 1;
    char ch, ch1;
    int ordinal;
    while (index < str.length() - 1) {
      if (str.charAt(index) != '\\') {
        retval += str.charAt(index);
        index++;
        continue;
      }
      index++;
      ch = str.charAt(index);
      if (ch == 'b') {
        retval += '\b';
        index++;
        continue;
      }
      if (ch == 't') {
        retval += '\t';
        index++;
        continue;
      }
      if (ch == 'n') {
        retval += '\n';
        index++;
        continue;
      }
      if (ch == 'f') {
        retval += '\f';
        index++;
        continue;
      }
      if (ch == 'r') {
        retval += '\r';
        index++;
        continue;
      }
      if (ch == '"') {
        retval += '\"';
        index++;
        continue;
      }
      if (ch == '\'') {
        retval += '\'';
        index++;
        continue;
      }
      if (ch == '\\') {
        retval += '\\';
        index++;
        continue;
      }
      if (ch >= '0' && ch <= '7') {
        ordinal = ((int) ch) - ((int) '0');
        index++;
        ch1 = str.charAt(index);
        if (ch1 >= '0' && ch1 <= '7') {
          ordinal = ordinal * 8 + ((int) ch1) - ((int) '0');
          index++;
          ch1 = str.charAt(index);
          if (ch <= '3' && ch1 >= '0' && ch1 <= '7') {
            ordinal = ordinal * 8 + ((int) ch1) - ((int) '0');
            index++;
          }
        }
        retval += (char) ordinal;
        continue;
      }
      if (ch == 'u') {
        index++;
        ch = str.charAt(index);
        if (hexchar(ch)) {
          ordinal = hexval(ch);
          index++;
          ch = str.charAt(index);
          if (hexchar(ch)) {
            ordinal = ordinal * 16 + hexval(ch);
            index++;
            ch = str.charAt(index);
            if (hexchar(ch)) {
              ordinal = ordinal * 16 + hexval(ch);
              index++;
              ch = str.charAt(index);
              if (hexchar(ch)) {
                ordinal = ordinal * 16 + hexval(ch);
                index++;
                continue;
              }
            }
          }
        }
        JavaCCErrors.parseError(t, "Encountered non-hex character '" + ch +
            "' at position " + index + " of string " +
            "- Unicode escape must have 4 hex digits after it.");
        return retval;
      }
      JavaCCErrors.parseError(t, "Illegal escape sequence '\\" + ch +
          "' at position " + index + " of string.");
      return retval;
    }
    return retval;
  }

  protected static char character_descriptor_assign(Token t, String s) {
    if (s.length() != 1) {
      JavaCCErrors.parseError(t, "String in character list may contain only one character.");
      return ' ';
    }
    else {
      return s.charAt(0);
    }
  }

  protected static char character_descriptor_assign(Token t, String s, String left) {
    if (s.length() != 1) {
      JavaCCErrors.parseError(t, "String in character list may contain only one character.");
      return ' ';
    }
    else if ((int) (left.charAt(0)) > (int) (s.charAt(0))) {
      JavaCCErrors.parseError(t, "Right end of character range \'" + s +
          "\' has a lower ordinal value than the left end of character range \'" + left + "\'.");
      return left.charAt(0);
    }
    else {
      return s.charAt(0);
    }
  }

  protected static void makeTryBlock(
      Token tryLoc,
      Container<Expansion> result,
      Container<Expansion> nestedExp,
      List types,
      List ids,
      List catchBlocks,
      List finallyBlocks
  ) {
    if (catchBlocks.size() == 0 && finallyBlocks == null) {
      JavaCCErrors.parseError(tryLoc, "Try block must contain at least one catch or finally block.");
      return;
    }
    TryBlock block = new TryBlock();
    block.setLine(tryLoc.getBeginLine());
    block.setColumn(tryLoc.getBeginColumn());
    block.expansion = nestedExp.member;
    block.expansion.parent = block;
    block.expansion.ordinal = 0;
    block.types = types;
    block.ids = ids;
    block.catchBlocks = catchBlocks;
    block.finallyBlocks = finallyBlocks;
    result.member = block;
  }
}
