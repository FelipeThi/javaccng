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
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/** Generate the parser. */
final class ParseGen implements FileGenerator, JavaCCConstants {
  private final Semanticize semanticize;

  ParseGen(Semanticize semanticize) {
    this.semanticize = semanticize;
  }

  @Override
  public void start() throws MetaParseException, IOException {
    if (JavaCCErrors.getErrorCount() != 0) {
      throw new MetaParseException();
    }

    if (!Options.getBuildParser()) {
      return;
    }

    File path = new File(Options.getOutputDirectory(), JavaCCGlobals.parserClass() + ".java");
    OutputFile outputFile = new OutputFile(path);
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      generate(out);
    }
    finally {
      out.close();
    }
  }

  private void generate(IndentingPrintWriter out) throws IOException {
    boolean implementsExists = false;

    List<Token> tokens = JavaCCGlobals.cuToInsertionPoint1;
    if (tokens.size() != 0) {
      TokenPrinter.printTokenSetup(tokens.get(0));
      TokenPrinter.cCol = 1;
      for (Token t : tokens) {
        if (t.getKind() == IMPLEMENTS) {
          implementsExists = true;
        }
        else if (t.getKind() == CLASS) {
          implementsExists = false;
        }
        TokenPrinter.printToken(t, out);
      }
    }
    if (implementsExists) {
      out.print(", ");
    }
    else {
      out.print(" implements ");
    }
    out.print(JavaCCGlobals.constantsClass() + " ");
    if (JavaCCGlobals.cuToInsertionPoint2.size() != 0) {
      TokenPrinter.printTokenSetup(JavaCCGlobals.cuToInsertionPoint2.get(0));
      for (Token t : JavaCCGlobals.cuToInsertionPoint2) {
        TokenPrinter.printToken(t, out);
      }
    }

    out.println();
    out.println();

    ParseEngine parseEngine = new ParseEngine(semanticize);
    parseEngine.build(out);

    printBoilerplate(out);

    List<Token> cuFromInsertionPoint2 = JavaCCGlobals.cuFromInsertionPoint2;
    if (cuFromInsertionPoint2.size() != 0) {
      TokenPrinter.printTokenSetup(cuFromInsertionPoint2.get(0));
      TokenPrinter.cCol = 1;
      Token t = null;
      for (Token token : cuFromInsertionPoint2) {
        t = token;
        TokenPrinter.printToken(t, out);
      }
      TokenPrinter.printTrailingComments(t);
    }

    out.println();
  }

  private void printBoilerplate(IndentingPrintWriter out) {
    out.println("  /** Either generated or user defined Token Manager. */");
    out.println("  public final Scanner scanner;");
    out.println("  /** Current token. */");
    out.println("  public Token token;");
    out.println("  /** Next token. */");
    out.println("  public Token jj_nt;");
    if (!Options.getCacheTokens()) {
      out.println("  private int jj_ntk;");
    }
    if (JavaCCGlobals.jj2index != 0) {
      out.println("  private Token jj_scanPos, jj_lastPos;");
      out.println("  private int jj_la;");
      if (JavaCCGlobals.lookaheadNeeded) {
        out.println("  /** Whether we are looking ahead. */");
        out.println("  private boolean jj_lookingAhead = false;");
        out.println("  private boolean jj_semLA;");
      }
    }
    if (Options.getErrorReporting()) {
      out.println("  private int jj_gen;");
      out.println("  private final int[] jj_la1 = new int[" + JavaCCGlobals.maskIndex + "];");
      int tokenMaskSize = (JavaCCGlobals.tokenCount - 1) / 32 + 1;
      for (int i = 0; i < tokenMaskSize; i++) {
        out.println("  static private int[] jj_la1_" + i + ";");
      }
      out.println("  static {");
      for (int i = 0; i < tokenMaskSize; i++) {
        out.println("      jj_la1_init_" + i + "();");
      }
      out.println("   }");
      for (int i = 0; i < tokenMaskSize; i++) {
        out.println("   private static void jj_la1_init_" + i + "() {");
        out.print("      jj_la1_" + i + " = new int[] {");
        for (Iterator it = JavaCCGlobals.maskVals.iterator(); it.hasNext(); ) {
          int[] tokenMask = (int[]) it.next();
          out.print("0x" + Integer.toHexString(tokenMask[i]) + ",");
        }
        out.println("};");
        out.println("   }");
      }
    }
    if (JavaCCGlobals.jj2index != 0 && Options.getErrorReporting()) {
      out.println("  final private JJCalls[] jj_2_rtns = new JJCalls[" + JavaCCGlobals.jj2index + "];");
      out.println("  private boolean jj_rescan = false;");
      out.println("  private int jj_gc = 0;");
    }
    out.println();

    out.println("  /** Constructor with either generated or user provided Token Manager. */");
    out.println("  public " + JavaCCGlobals.parserClass() + "(Scanner s) throws java.io.IOException, ParseException {");
    out.println("    scanner = s;");
    out.println("    token = new Token();");
    if (Options.getCacheTokens()) {
      out.println("    token.next = jj_nt = scanner.getNextToken();");
    }
    else {
      out.println("    jj_ntk = -1;");
    }
    if (Options.getErrorReporting()) {
      out.println("    jj_gen = 0;");
      out.println("    for (int i = 0; i < " + JavaCCGlobals.maskIndex + "; i++) jj_la1[i] = -1;");
      if (JavaCCGlobals.jj2index != 0) {
        out.println("    for (int i = 0; i < jj_2_rtns.length; i++) jj_2_rtns[i] = new JJCalls();");
      }
    }
    out.println("  }");
    out.println();
    out.println("  private Token jj_consume_token(int kind) throws java.io.IOException, ParseException {");
    if (Options.getCacheTokens()) {
      out.println("    Token oldToken = token;");
      out.println("    if ((token = jj_nt).next != null) jj_nt = jj_nt.next;");
      out.println("    else jj_nt = jj_nt.next = scanner.getNextToken();");
    }
    else {
      out.println("    Token oldToken;");
      out.println("    if ((oldToken = token).next != null) token = token.next;");
      out.println("    else token = token.next = scanner.getNextToken();");
      out.println("    jj_ntk = -1;");
    }
    out.println("    if (token.getKind() == kind) {");
    if (Options.getErrorReporting()) {
      out.println("      jj_gen++;");
      if (JavaCCGlobals.jj2index != 0) {
        out.println("      if (++jj_gc > 100) {");
        out.println("        jj_gc = 0;");
        out.println("        for (int i = 0; i < jj_2_rtns.length; i++) {");
        out.println("          JJCalls c = jj_2_rtns[i];");
        out.println("          while (c != null) {");
        out.println("            if (c.gen < jj_gen) c.first = null;");
        out.println("            c = c.next;");
        out.println("          }");
        out.println("        }");
        out.println("      }");
      }
    }
    if (Options.getDebugParser()) {
      out.println("      trace_token(token, \"\");");
    }
    out.println("      return token;");
    out.println("    }");
    if (Options.getCacheTokens()) {
      out.println("    jj_nt = token;");
    }
    out.println("    token = oldToken;");
    if (Options.getErrorReporting()) {
      out.println("    jj_kind = kind;");
    }
    out.println("    throw generateParseException();");
    out.println("  }");
    out.println();
    if (JavaCCGlobals.jj2index != 0) {
      out.println("  @SuppressWarnings(\"serial\")");
      out.println("  private static final class LookaheadSuccess extends Error {}");
      out.println("  private final LookaheadSuccess jj_ls = new LookaheadSuccess();");
      out.println("  private boolean jj_scan_token(int kind) throws java.io.IOException {");
      out.println("    if (jj_scanPos == jj_lastPos) {");
      out.println("      jj_la--;");
      out.println("      if (jj_scanPos.next == null) {");
      out.println("        jj_lastPos = jj_scanPos = jj_scanPos.next = scanner.getNextToken();");
      out.println("      } else {");
      out.println("        jj_lastPos = jj_scanPos = jj_scanPos.next;");
      out.println("      }");
      out.println("    } else {");
      out.println("      jj_scanPos = jj_scanPos.next;");
      out.println("    }");
      if (Options.getErrorReporting()) {
        out.println("    if (jj_rescan) {");
        out.println("      int i = 0; Token tok = token;");
        out.println("      while (tok != null && tok != jj_scanPos) { i++; tok = tok.next; }");
        out.println("      if (tok != null) jj_add_error_token(kind, i);");
        if (Options.getDebugLookahead()) {
          out.println("    } else {");
          out.println("      trace_scan(jj_scanPos, kind);");
        }
        out.println("    }");
      }
      else if (Options.getDebugLookahead()) {
        out.println("    trace_scan(jj_scanPos, kind);");
      }
      out.println("    if (jj_scanPos.getKind() != kind) return true;");
      out.println("    if (jj_la == 0 && jj_scanPos == jj_lastPos) throw jj_ls;");
      out.println("    return false;");
      out.println("  }");
      out.println();
    }
    out.println();
    out.println("/** Get the next Token. */");
    out.println("  final public Token getNextToken() throws java.io.IOException {");
    if (Options.getCacheTokens()) {
      out.println("    if ((token = jj_nt).next != null) jj_nt = jj_nt.next;");
      out.println("    else jj_nt = jj_nt.next = scanner.getNextToken();");
    }
    else {
      out.println("    if (token.next != null) token = token.next;");
      out.println("    else token = token.next = scanner.getNextToken();");
      out.println("    jj_ntk = -1;");
    }
    if (Options.getErrorReporting()) {
      out.println("    jj_gen++;");
    }
    if (Options.getDebugParser()) {
      out.println("      trace_token(token, \" (in getNextToken)\");");
    }
    out.println("    return token;");
    out.println("  }");
    out.println();
    out.println("/** Get the specific Token. */");
    out.println("  final public Token getToken(int index) throws java.io.IOException {");
    if (JavaCCGlobals.lookaheadNeeded) {
      out.println("    Token t = jj_lookingAhead ? jj_scanPos : token;");
    }
    else {
      out.println("    Token t = token;");
    }
    out.println("    for (int i = 0; i < index; i++) {");
    out.println("      if (t.next != null) t = t.next;");
    out.println("      else t = t.next = scanner.getNextToken();");
    out.println("    }");
    out.println("    return t;");
    out.println("  }");
    out.println();
    if (!Options.getCacheTokens()) {
      out.println("  private int jj_ntk() throws java.io.IOException {");
      out.println("    if ((jj_nt = token.next) == null)");
      out.println("      return (jj_ntk = (token.next = scanner.getNextToken()).getKind());");
      out.println("    else");
      out.println("      return (jj_ntk = jj_nt.getKind());");
      out.println("  }");
      out.println();
    }
    if (Options.getErrorReporting()) {
      if (!Options.getGenerateGenerics()) {
        out.println("  private java.util.List jj_expentries = new java.util.ArrayList();");
      }
      else {
        out.println("  private final java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();");
      }
      out.println("  private int[] jj_expentry;");
      out.println("  private int jj_kind = -1;");
      if (JavaCCGlobals.jj2index != 0) {
        out.println("  private int[] jj_lasttokens = new int[100];");
        out.println("  private int jj_endpos;");
        out.println();
        out.println("  private void jj_add_error_token(int kind, int pos) {");
        out.println("    if (pos >= 100) return;");
        out.println("    if (pos == jj_endpos + 1) {");
        out.println("      jj_lasttokens[jj_endpos++] = kind;");
        out.println("    } else if (jj_endpos != 0) {");
        out.println("      jj_expentry = new int[jj_endpos];");
        out.println("      for (int i = 0; i < jj_endpos; i++) {");
        out.println("        jj_expentry[i] = jj_lasttokens[i];");
        out.println("      }");
        if (!Options.getGenerateGenerics()) {
          out.println("      jj_entries_loop: for (java.util.Iterator it = jj_expentries.iterator(); it.hasNext();) {");
        }
        else {
          out.println("      jj_entries_loop: for (java.util.Iterator<?> it = jj_expentries.iterator(); it.hasNext();) {");
        }
        out.println("        int[] oldentry = (int[])(it.next());");
        out.println("        if (oldentry.length == jj_expentry.length) {");
        out.println("          for (int i = 0; i < jj_expentry.length; i++) {");
        out.println("            if (oldentry[i] != jj_expentry[i]) {");
        out.println("              continue jj_entries_loop;");
        out.println("            }");
        out.println("          }");
        out.println("          jj_expentries.add(jj_expentry);");
        out.println("          break jj_entries_loop;");
        out.println("        }");
        out.println("      }");
        out.println("      if (pos != 0) jj_lasttokens[(jj_endpos = pos) - 1] = kind;");
        out.println("    }");
        out.println("  }");
      }
      out.println();
      out.println("  /** Generate ParseException. */");
      out.println("  public ParseException generateParseException() throws java.io.IOException {");
      out.println("    jj_expentries.clear();");
      out.println("    boolean[] la1tokens = new boolean[" + JavaCCGlobals.tokenCount + "];");
      out.println("    if (jj_kind >= 0) {");
      out.println("      la1tokens[jj_kind] = true;");
      out.println("      jj_kind = -1;");
      out.println("    }");
      out.println("    for (int i = 0; i < " + JavaCCGlobals.maskIndex + "; i++) {");
      out.println("      if (jj_la1[i] == jj_gen) {");
      out.println("        for (int j = 0; j < 32; j++) {");
      for (int i = 0; i < (JavaCCGlobals.tokenCount - 1) / 32 + 1; i++) {
        out.println("          if ((jj_la1_" + i + "[i] & (1<<j)) != 0) {");
        out.print("            la1tokens[");
        if (i != 0) {
          out.print((32 * i) + "+");
        }
        out.println("j] = true;");
        out.println("          }");
      }
      out.println("        }");
      out.println("      }");
      out.println("    }");
      out.println("    for (int i = 0; i < " + JavaCCGlobals.tokenCount + "; i++) {");
      out.println("      if (la1tokens[i]) {");
      out.println("        jj_expentry = new int[1];");
      out.println("        jj_expentry[0] = i;");
      out.println("        jj_expentries.add(jj_expentry);");
      out.println("      }");
      out.println("    }");
      if (JavaCCGlobals.jj2index != 0) {
        out.println("    jj_endpos = 0;");
        out.println("    jj_rescan_token();");
        out.println("    jj_add_error_token(0, 0);");
      }
      out.println("    int[][] exptokseq = new int[jj_expentries.size()][];");
      out.println("    for (int i = 0; i < jj_expentries.size(); i++) {");
      if (!Options.getGenerateGenerics()) {
        out.println("      exptokseq[i] = (int[])jj_expentries.get(i);");
      }
      else {
        out.println("      exptokseq[i] = jj_expentries.get(i);");
      }
      out.println("    }");
      out.println("    return new ParseException(token, exptokseq, tokenImage);");
      out.println("  }");
    }
    else {
      out.println("  /** Generate ParseException. */");
      out.println("  public ParseException generateParseException() throws java.io.IOException {");
      out.println("    Token errortok = token.next;");
      if (Options.getKeepLineColumn()) {
        out.println("    int line = errortok.beginLine, column = errortok.beginColumn;");
      }
      out.println("    String mess = (errortok.getKind() == 0) ? tokenImage[0] : errortok.image;");
      if (Options.getKeepLineColumn()) {
        out.println("    return new ParseException(" +
            "\"Parse error at line \" + line + \", column \" + column + \".  " +
            "Encountered: \" + mess);");
      }
      else {
        out.println("    return new ParseException(\"Parse error at <unknown location>.  " +
            "Encountered: \" + mess);");
      }
      out.println("  }");
    }
    out.println();

    if (Options.getDebugParser()) {
      out.println("  private int trace_indent = 0;");
      out.println("  private boolean trace_enabled = true;");
      out.println();
      out.println("/** Enable tracing. */");
      out.println("  final public void enable_tracing() {");
      out.println("    trace_enabled = true;");
      out.println("  }");
      out.println();
      out.println("/** Disable tracing. */");
      out.println("  final public void disable_tracing() {");
      out.println("    trace_enabled = false;");
      out.println("  }");
      out.println();
      out.println("  private void trace_call(String s) {");
      out.println("    if (trace_enabled) {");
      out.println("      for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
      out.println("      System.out.println(\"Call:   \" + s);");
      out.println("    }");
      out.println("    trace_indent = trace_indent + 2;");
      out.println("  }");
      out.println();
      out.println("  private void trace_return(String s) {");
      out.println("    trace_indent = trace_indent - 2;");
      out.println("    if (trace_enabled) {");
      out.println("      for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
      out.println("      System.out.println(\"Return: \" + s);");
      out.println("    }");
      out.println("  }");
      out.println();
      out.println("  private void trace_token(Token t, String where) {");
      out.println("    if (trace_enabled) {");
      out.println("      for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
      out.println("      System.out.print(\"Consumed token: <\" + tokenImage[t.getKind()]);");
      out.println("      if (t.getKind() != 0 && !tokenImage[t.getKind()].equals(\"\\\"\" + t.image + \"\\\"\")) {");
      out.println("        System.out.print(\": \\\"\" + t.image + \"\\\"\");");
      out.println("      }");
      out.println("      System.out.println(\" at line \" + t.beginLine + " +
          "\" column \" + t.beginColumn + \">\" + where);");
      out.println("    }");
      out.println("  }");
      out.println();
      out.println("  private void trace_scan(Token t1, int t2) {");
      out.println("    if (trace_enabled) {");
      out.println("      for (int i = 0; i < trace_indent; i++) { System.out.print(\" \"); }");
      out.println("      System.out.print(\"Visited token: <\" + tokenImage[t1.getKind()]);");
      out.println("      if (t1.getKind() != 0 && !tokenImage[t1.getKind()].equals(\"\\\"\" + t1.image + \"\\\"\")) {");
      out.println("        System.out.print(\": \\\"\" + t1.image + \"\\\"\");");
      out.println("      }");
      out.println("      System.out.println(\" at line \" + t1.beginLine + \"" +
          " column \" + t1.beginColumn + \">; Expected token: <\" + tokenImage[t2] + \">\");");
      out.println("    }");
      out.println("  }");
      out.println();
    }
    else {
      out.println("  /** Enable tracing. */");
      out.println("  final public void enable_tracing() {");
      out.println("  }");
      out.println();
      out.println("  /** Disable tracing. */");
      out.println("  final public void disable_tracing() {");
      out.println("  }");
      out.println();
    }

    if (JavaCCGlobals.jj2index != 0 && Options.getErrorReporting()) {
      out.println("  private void jj_rescan_token() throws java.io.IOException {");
      out.println("    jj_rescan = true;");
      out.println("    for (int i = 0; i < " + JavaCCGlobals.jj2index + "; i++) {");
      out.println("    try {");
      out.println("      JJCalls p = jj_2_rtns[i];");
      out.println("      do {");
      out.println("        if (p.gen > jj_gen) {");
      out.println("          jj_la = p.arg; jj_lastPos = jj_scanPos = p.first;");
      out.println("          switch (i) {");
      for (int i = 0; i < JavaCCGlobals.jj2index; i++) {
        out.println("            case " + i + ": jj_3_" + (i + 1) + "(); break;");
      }
      out.println("          }");
      out.println("        }");
      out.println("        p = p.next;");
      out.println("      } while (p != null);");
      out.println("      } catch(LookaheadSuccess ls) {}");
      out.println("    }");
      out.println("    jj_rescan = false;");
      out.println("  }");
      out.println();
      out.println("  private void jj_save(int index, int xla) throws java.io.IOException {");
      out.println("    JJCalls p = jj_2_rtns[index];");
      out.println("    while (p.gen > jj_gen) {");
      out.println("      if (p.next == null) { p = p.next = new JJCalls(); break; }");
      out.println("      p = p.next;");
      out.println("    }");
      out.println("    p.gen = jj_gen + xla - jj_la; p.first = token; p.arg = xla;");
      out.println("  }");
      out.println();
    }

    if (JavaCCGlobals.jj2index != 0 && Options.getErrorReporting()) {
      out.println("  static final class JJCalls {");
      out.println("    int gen;");
      out.println("    Token first;");
      out.println("    int arg;");
      out.println("    JJCalls next;");
      out.println("  }");
      out.println();
    }
  }
}
