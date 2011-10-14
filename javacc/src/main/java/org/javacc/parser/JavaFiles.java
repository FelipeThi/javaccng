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

import org.javacc.utils.JavaFileGenerator;
import org.javacc.utils.io.IndentingPrintWriter;

import java.io.File;
import java.io.IOException;

/** Generate CharStream, Scanner and Exceptions. */
public class JavaFiles implements JavaCCParserConstants {
  public void gen_JavaCharStream() throws IOException {
    File path = new File(Options.getOutputDirectory(), "JavaCharStream.java");
    OutputFile outputFile = new OutputFile(path);
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      if (JavaCCGlobals.cuToInsertionPoint1.size() != 0
          && JavaCCGlobals.cuToInsertionPoint1.get(0).getKind() == PACKAGE) {
        for (int i = 1; i < JavaCCGlobals.cuToInsertionPoint1.size(); i++) {
          if (JavaCCGlobals.cuToInsertionPoint1.get(i).getKind() == SEMICOLON) {
            JavaCCGlobals.cline = JavaCCGlobals.cuToInsertionPoint1.get(0).getBeginLine();
            JavaCCGlobals.ccol = JavaCCGlobals.cuToInsertionPoint1.get(0).getBeginColumn();
            for (int j = 0; j <= i; j++) {
              JavaCCGlobals.printToken(JavaCCGlobals.cuToInsertionPoint1.get(j), out);
            }
            out.println("");
            out.println("");
            break;
          }
        }
      }
      JavaFileGenerator generator = new JavaFileGenerator(
          "/templates/JavaCharStream.template", Options.getOptions());
      generator.generate(out);
    }
    finally {
      out.close();
    }
  }

  public void gen_SimpleCharStream() throws IOException {
    File path = new File(Options.getOutputDirectory(), "SimpleCharStream.java");
    OutputFile outputFile = new OutputFile(path);
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      if (JavaCCGlobals.cuToInsertionPoint1.size() != 0
          && JavaCCGlobals.cuToInsertionPoint1.get(0).getKind() == PACKAGE) {
        for (int i = 1; i < JavaCCGlobals.cuToInsertionPoint1.size(); i++) {
          if (JavaCCGlobals.cuToInsertionPoint1.get(i).getKind() == SEMICOLON) {
            JavaCCGlobals.cline = JavaCCGlobals.cuToInsertionPoint1.get(0).getBeginLine();
            JavaCCGlobals.ccol = JavaCCGlobals.cuToInsertionPoint1.get(0).getBeginColumn();
            for (int j = 0; j <= i; j++) {
              JavaCCGlobals.printToken(JavaCCGlobals.cuToInsertionPoint1.get(j), out);
            }
            out.println("");
            out.println("");
            break;
          }
        }
      }
      JavaFileGenerator generator = new JavaFileGenerator(
          "/templates/SimpleCharStream.template", Options.getOptions());
      generator.generate(out);
    }
    finally {
      out.close();
    }
  }

  public void gen_CharStream() throws IOException {
    File path = new File(Options.getOutputDirectory(), "CharStream.java");
    OutputFile outputFile = new OutputFile(path);
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      if (JavaCCGlobals.cuToInsertionPoint1.size() != 0
          && JavaCCGlobals.cuToInsertionPoint1.get(0).getKind() == PACKAGE) {
        for (int i = 1; i < JavaCCGlobals.cuToInsertionPoint1.size(); i++) {
          if (JavaCCGlobals.cuToInsertionPoint1.get(i).getKind() == SEMICOLON) {
            JavaCCGlobals.cline = JavaCCGlobals.cuToInsertionPoint1.get(0).getBeginLine();
            JavaCCGlobals.ccol = JavaCCGlobals.cuToInsertionPoint1.get(0).getBeginColumn();
            for (int j = 0; j <= i; j++) {
              JavaCCGlobals.printToken(JavaCCGlobals.cuToInsertionPoint1.get(j), out);
            }
            out.println("");
            out.println("");
            break;
          }
        }
      }
      JavaFileGenerator generator = new JavaFileGenerator(
          "/templates/CharStream.template", Options.getOptions());
      generator.generate(out);
    }
    finally {
      out.close();
    }
  }

  public void gen_ParseException() throws IOException {
    File path = new File(Options.getOutputDirectory(), "ParseException.java");
    OutputFile outputFile = new OutputFile(path);
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      if (JavaCCGlobals.cuToInsertionPoint1.size() != 0
          && JavaCCGlobals.cuToInsertionPoint1.get(0).getKind() == PACKAGE) {
        for (int i = 1; i < JavaCCGlobals.cuToInsertionPoint1.size(); i++) {
          if (JavaCCGlobals.cuToInsertionPoint1.get(i).getKind() == SEMICOLON) {
            JavaCCGlobals.cline = JavaCCGlobals.cuToInsertionPoint1.get(0).getBeginLine();
            JavaCCGlobals.ccol = JavaCCGlobals.cuToInsertionPoint1.get(0).getBeginColumn();
            for (int j = 0; j <= i; j++) {
              JavaCCGlobals.printToken(JavaCCGlobals.cuToInsertionPoint1.get(j), out);
            }
            out.println("");
            out.println("");
            break;
          }
        }
      }
      JavaFileGenerator generator = new JavaFileGenerator(
          "/templates/ParseException.template", Options.getOptions());
      generator.generate(out);
    }
    finally {
      out.close();
    }
  }

  public void gen_TokenMgrError() throws IOException {
    File path = new File(Options.getOutputDirectory(), "ScannerException.java");
    OutputFile outputFile = new OutputFile(path);
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      if (JavaCCGlobals.cuToInsertionPoint1.size() != 0
          && JavaCCGlobals.cuToInsertionPoint1.get(0).getKind() == PACKAGE) {
        for (int i = 1; i < JavaCCGlobals.cuToInsertionPoint1.size(); i++) {
          if (JavaCCGlobals.cuToInsertionPoint1.get(i).getKind() == SEMICOLON) {
            JavaCCGlobals.cline = JavaCCGlobals.cuToInsertionPoint1.get(0).getBeginLine();
            JavaCCGlobals.ccol = JavaCCGlobals.cuToInsertionPoint1.get(0).getBeginColumn();
            for (int j = 0; j <= i; j++) {
              JavaCCGlobals.printToken(JavaCCGlobals.cuToInsertionPoint1.get(j), out);
            }
            out.println("");
            out.println("");
            break;
          }
        }
      }
      JavaFileGenerator generator = new JavaFileGenerator(
          "/templates/ScannerException.template", Options.getOptions());
      generator.generate(out);
    }
    finally {
      out.close();
    }
  }

  public void gen_Token() throws IOException {
    File path = new File(Options.getOutputDirectory(), "Token.java");
    OutputFile outputFile = new OutputFile(path);
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      if (JavaCCGlobals.cuToInsertionPoint1.size() != 0
          && JavaCCGlobals.cuToInsertionPoint1.get(0).getKind() == PACKAGE) {
        for (int i = 1; i < JavaCCGlobals.cuToInsertionPoint1.size(); i++) {
          if (JavaCCGlobals.cuToInsertionPoint1.get(i).getKind() == SEMICOLON) {
            JavaCCGlobals.cline = JavaCCGlobals.cuToInsertionPoint1.get(0).getBeginLine();
            JavaCCGlobals.ccol = JavaCCGlobals.cuToInsertionPoint1.get(0).getBeginColumn();
            for (int j = 0; j <= i; j++) {
              JavaCCGlobals.printToken(JavaCCGlobals.cuToInsertionPoint1.get(j), out);
            }
            out.println("");
            out.println("");
            break;
          }
        }
      }
      JavaFileGenerator generator = new JavaFileGenerator(
          "/templates/Token.template", Options.getOptions());
      generator.generate(out);
    }
    finally {
      out.close();
    }
  }

  public void gen_Scanner() throws IOException {
    File path = new File(Options.getOutputDirectory(), "Scanner.java");
    OutputFile outputFile = new OutputFile(path);
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      if (JavaCCGlobals.cuToInsertionPoint1.size() != 0
          && JavaCCGlobals.cuToInsertionPoint1.get(0).getKind() == PACKAGE) {
        for (int i = 1; i < JavaCCGlobals.cuToInsertionPoint1.size(); i++) {
          if (JavaCCGlobals.cuToInsertionPoint1.get(i).getKind() == SEMICOLON) {
            JavaCCGlobals.cline = JavaCCGlobals.cuToInsertionPoint1.get(0).getBeginLine();
            JavaCCGlobals.ccol = JavaCCGlobals.cuToInsertionPoint1.get(0).getBeginColumn();
            for (int j = 0; j <= i; j++) {
              JavaCCGlobals.printToken(JavaCCGlobals.cuToInsertionPoint1.get(j), out);
            }
            out.println("");
            out.println("");
            break;
          }
        }
      }
      JavaFileGenerator generator = new JavaFileGenerator(
          "/templates/Scanner.template", Options.getOptions());
      generator.generate(out);
    }
    finally {
      out.close();
    }
  }
}
