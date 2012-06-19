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

import java.util.HashMap;
import java.util.List;

abstract class JavaCCParserBase {
  static final class ModifierSet {
    /* Definitions of the bits in the modifiers field.  */
    public static final int PUBLIC = 0x0001;
    public static final int PROTECTED = 0x0002;
    public static final int PRIVATE = 0x0004;
    public static final int ABSTRACT = 0x0008;
    public static final int STATIC = 0x0010;
    public static final int FINAL = 0x0020;
    public static final int SYNCHRONIZED = 0x0040;
    public static final int NATIVE = 0x0080;
    public static final int TRANSIENT = 0x0100;
    public static final int VOLATILE = 0x0200;
    public static final int STRICTFP = 0x1000;

    /**
     * A set of accessors that indicate whether the specified modifier
     * is in the set.
     */
    public boolean isPublic(int modifiers) {
      return (modifiers & PUBLIC) != 0;
    }

    public boolean isProtected(int modifiers) {
      return (modifiers & PROTECTED) != 0;
    }

    public boolean isPrivate(int modifiers) {
      return (modifiers & PRIVATE) != 0;
    }

    public boolean isStatic(int modifiers) {
      return (modifiers & STATIC) != 0;
    }

    public boolean isAbstract(int modifiers) {
      return (modifiers & ABSTRACT) != 0;
    }

    public boolean isFinal(int modifiers) {
      return (modifiers & FINAL) != 0;
    }

    public boolean isNative(int modifiers) {
      return (modifiers & NATIVE) != 0;
    }

    public boolean isStrictfp(int modifiers) {
      return (modifiers & STRICTFP) != 0;
    }

    public boolean isSynchronized(int modifiers) {
      return (modifiers & SYNCHRONIZED) != 0;
    }

    public boolean isTransient(int modifiers) {
      return (modifiers & TRANSIENT) != 0;
    }

    public boolean isVolatile(int modifiers) {
      return (modifiers & VOLATILE) != 0;
    }

    static int removeModifier(int modifiers, int mod) {
      return modifiers & ~mod;
    }
  }

  private JavaCCState state;
  private List<Token> cuTokens;
  private Token firstCuToken;
  private boolean insertionPoint1Set;
  private boolean insertionPoint2Set;
  private int nextFreeLexState = 1;

  public void setState(JavaCCState state) {
    this.state = state;
    cuTokens = state.cuToInsertionPoint1;
  }

  protected void initialize() {}

  protected void setCuName(String name) {
    state.cuName = name;
  }

  protected void setInsertionPoint(Token t, int no) {
    do {
      cuTokens.add(firstCuToken);
      firstCuToken = firstCuToken.next;
    }
    while (firstCuToken != t);
    if (no == 1) {
      if (insertionPoint1Set) {
        JavaCCErrors.parseError(t, "Multiple declaration of parser class.");
      }
      else {
        insertionPoint1Set = true;
        cuTokens = state.cuToInsertionPoint2;
      }
    }
    else {
      cuTokens = state.cuFromInsertionPoint2;
      insertionPoint2Set = true;
    }
    firstCuToken = t;
  }

  protected void insertionPointErrors(Token t) {
    while (firstCuToken != t) {
      cuTokens.add(firstCuToken);
      firstCuToken = firstCuToken.next;
    }
    if (!insertionPoint1Set || !insertionPoint2Set) {
      JavaCCErrors.parseError(t, "Parser class has not been defined between PARSER_BEGIN and PARSER_END.");
    }
  }

  protected void setInitialCuToken(Token t) {
    firstCuToken = t;
  }

  protected void addScannerDeclarations(Token t, List<Token> decls) {
    if (state.scannerDeclarations != null) {
      JavaCCErrors.parseError(t, "Multiple occurrence of \"SCANNER_DECLS\".");
    }
    else {
      state.scannerDeclarations = decls;
      if (Options.getUserScanner()) {
        JavaCCErrors.warning(t, "Ignoring declarations in \"SCANNER_DECLS\" since option " +
            "USER_SCANNER has been set to true.");
      }
    }
  }

  protected void addProduction(NormalProduction p) {
    state.bnfProductions.add(p);
  }

  protected void addRegExp(TokenProduction production) {
    state.tokenProductions.add(production);
    if (Options.getUserScanner()) {
      if (production.lexStates == null
          || production.lexStates.length != 1
          || !production.lexStates[0].equals(JavaCCState.DEFAULT)) {
        JavaCCErrors.warning(production, "Ignoring lexical state specifications since option " +
            "USER_SCANNER has been set to true.");
      }
    }
    if (production.lexStates == null) {
      return;
    }
    for (int i = 0; i < production.lexStates.length; i++) {
      for (int j = 0; j < i; j++) {
        if (production.lexStates[i].equals(production.lexStates[j])) {
          JavaCCErrors.parseError(production, "Multiple occurrence of \"" + production.lexStates[i] + "\" in lexical state list.");
        }
      }
      if (state.lexStateS2I.get(production.lexStates[i]) == null) {
        Integer n = nextFreeLexState++;
        state.lexStateS2I.put(production.lexStates[i], n);
        state.lexStateI2S.put(n, production.lexStates[i]);
        state.simpleTokensTable.put(production.lexStates[i],
            new HashMap<String, RegularExpression>());
      }
    }
  }

  protected void addInlineRegExp(RegularExpression r) {
    if (!(r instanceof REndOfFile)) {
      TokenProduction p = new TokenProduction();
      p.explicit = false;
      p.lexStates = new String[]{JavaCCState.DEFAULT};
      p.kind = TokenProduction.TOKEN;
      RegExpSpec res = new RegExpSpec();
      res.regExp = r;
      res.regExp.tpContext = p;
      res.action = new Action();
      res.nextState = null;
      res.nsToken = null;
      p.reSpecs.add(res);
      state.tokenProductions.add(p);
    }
  }

  protected char character_descriptor_assign(Token t, String s) {
    if (s.length() != 1) {
      JavaCCErrors.parseError(t, "String in character list may contain only one character.");
      return ' ';
    }
    else {
      return s.charAt(0);
    }
  }

  protected char character_descriptor_assign(Token t, String s, String left) {
    if (s.length() != 1) {
      JavaCCErrors.parseError(t, "String in character list may contain only one character.");
      return ' ';
    }
    else if ((int) left.charAt(0) > (int) s.charAt(0)) {
      JavaCCErrors.parseError(t, "Right end of character range \'" + s +
          "\' has a lower ordinal value than the left end of character range \'" + left + "\'.");
      return left.charAt(0);
    }
    else {
      return s.charAt(0);
    }
  }

  protected void makeTryBlock(
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
    block.setLine(tryLoc.getLine());
    block.setColumn(tryLoc.getColumn());
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
