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

import org.javacc.utils.Parsers;
import org.javacc.utils.io.IndentingPrintWriter;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/** Generates the Constants file. */
final class ConstantsFile implements FileGenerator {
  private final JavaCCState state;
  private final ScannerGen scannerGen;

  ConstantsFile(JavaCCState state, ScannerGen scannerGen) {
    this.state = state;
    this.scannerGen = scannerGen;
  }

  @Override
  public void start() throws MetaParseException, IOException {
    if (JavaCCErrors.getErrorCount() != 0) {
      throw new MetaParseException();
    }

    enumFile();
    constantsFile();
  }

  private void enumFile() throws IOException {
    File path = new File(Options.getOutputDirectory(), "TokenKind" + ".java");
    OutputFile outputFile = new OutputFile(path);
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      generateEnum(out);
    }
    finally {
      out.close();
    }
  }

  private void constantsFile() throws IOException {
    File path = new File(Options.getOutputDirectory(), state.constantsClass() + ".java");
    OutputFile outputFile = new OutputFile(path);
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      generateConstants(out);
    }
    finally {
      out.close();
    }
  }

  private void generateEnum(IndentingPrintWriter out)
      throws IOException {
    TokenPrinter tp = new TokenPrinter();
    tp.packageDeclaration(state.cuToInsertionPoint1, out);
    out.println("import java.util.*;");
    out.println("/** Token literal values and constants. */");
    out.println("public enum TokenKind {");
    out.indent();
    Set<String> literals = new LinkedHashSet<String>();
    out.print("EOF");
    for (RegularExpression re : state.orderedNamedTokens) {
      if (re.isPrivate) {
        continue;
      }
      out.println(",");
      if (re instanceof RStringLiteral && !((RStringLiteral) re).isRegExp()) {
        literals.add(re.label);
        out.println("/** The string literal token '" + re.label + "' defined at [" + re.getLine() + ", " + re.getColumn() + "]. */");
        out.print(re.label).print("(\"").print(Parsers.escape(((RStringLiteral) re).image)).print("\")");
      }
      else {
        out.println("/** The regular expression token '" + re.label + "' defined at [" + re.getLine() + ", " + re.getColumn() + "]. */");
        out.print(re.label);
      }
    }
    out.println(";");
    if (!literals.isEmpty()) {
      out.println("/** Literal string tokens. */");
      out.println("public static final Set<TokenKind> LITERALS =");
      out.indent().indent();
      out.print("Collections.unmodifiableSet(EnumSet.of(");
      out.indent().indent();
      IndentingPrintWriter.ListPrinter list = out.list(", ");
      for (String literal : literals) {
        list.item(literal);
      }
      out.println("));");
      out.unindent().unindent().unindent().unindent();
    }
    out.println("private final String image;\n");
    out.println("private TokenKind() { this(null); }\n");
    out.println("private TokenKind(String image) { this.image = image; }\n");
    out.println("public String getImage() { return image; }");
    out.unindent();
    out.println("}");
  }

  private void generateConstants(IndentingPrintWriter out)
      throws IOException {
    TokenPrinter tp = new TokenPrinter();
    tp.packageDeclaration(state.cuToInsertionPoint1, out);
    out.println("/** Token literal values and constants. */");
    out.println("public interface " + state.constantsClass() + " {");
    out.indent();
    out.println("/** End of File. */");
    out.println("int EOF = 0;");
    for (RegularExpression re : state.orderedNamedTokens) {
      if (re.isPrivate) {
        continue;
      }
      out.println("/** The '" + re.label + "' token id. */");
      out.println("int " + re.label + " = " + re.ordinal + ";");
    }
    if (!Options.getUserScanner() && Options.getBuildScanner()) {
      for (int i = 0; i < scannerGen.lexStateName.length; i++) {
        out.println("/** Lexical state. */");
        out.println("int " + scannerGen.lexStateName[i] + " = " + i + ";");
      }
    }
    generateImages(out);
    out.unindent();
    out.println("}");
  }

  private void generateImages(IndentingPrintWriter out) {
    out.println("/** Literal token values. */");
    out.println("String[] tokenImage = {");
    out.indent();
    out.println("\"<EOF>\",");
    for (TokenProduction tp : state.tokenProductions) {
      for (RegExpSpec reSpec : tp.reSpecs) {
        RegularExpression re = reSpec.regExp;
        if (re instanceof RStringLiteral) {
          out.print("\"\\\"");
          out.print(Parsers.escape(Parsers.escape(((RStringLiteral) re).image)));
          out.print("\\\"\",");
          if (re.label == null || re.label.isEmpty()) {
            out.print(" // Anonymous token");
          }
          else {
            out.print(" // Literal image of token '" + re.label + "'");
          }
          out.println();
        }
        else if (!"".equals(re.label)) {
          out.println("\"<" + re.label + ">\",");
        }
        else {
          if (re.tpContext.kind == TokenProduction.TOKEN) {
            JavaCCErrors.warning(re, "Consider giving this non-string token a label for better error reporting.");
          }
          out.println("\"<token of kind " + re.ordinal + ">\",");
        }
      }
    }
    out.unindent();
    out.println("};");
  }
}
