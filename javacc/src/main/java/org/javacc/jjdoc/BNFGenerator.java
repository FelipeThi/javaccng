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

import org.javacc.parser.Expansion;
import org.javacc.parser.JavaCodeProduction;
import org.javacc.parser.NonTerminal;
import org.javacc.parser.NormalProduction;
import org.javacc.parser.RCharacterList;
import org.javacc.parser.RJustName;
import org.javacc.parser.RegularExpression;
import org.javacc.parser.TokenProduction;
import org.javacc.utils.io.IndentingPrintWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class BNFGenerator implements Generator {
  protected IndentingPrintWriter out;
  private boolean printing = true;

  protected IndentingPrintWriter createOutputStream() {
    if (JJDocOptions.getOutputFile().equals("")) {
      if (JJDocGlobals.inputFile.equals("standard input")) {
        return new IndentingPrintWriter(
            new OutputStreamWriter(System.out));
      }
      else {
        String ext = ".bnf";
        int i = JJDocGlobals.inputFile.lastIndexOf('.');
        if (i == -1) {
          JJDocGlobals.outputFile = JJDocGlobals.inputFile + ext;
        }
        else {
          String suffix = JJDocGlobals.inputFile.substring(i);
          if (suffix.equals(ext)) {
            JJDocGlobals.outputFile = JJDocGlobals.inputFile + ext;
          }
          else {
            JJDocGlobals.outputFile = JJDocGlobals.inputFile.substring(0, i) + ext;
          }
        }
      }
    }
    else {
      JJDocGlobals.outputFile = JJDocOptions.getOutputFile();
    }
    try {
      out = new IndentingPrintWriter(
          new FileWriter(
              JJDocGlobals.outputFile));
    }
    catch (IOException ex) {
      error("JJDoc: can't open output stream on file "
          + JJDocGlobals.outputFile + ".  Using standard output.");
      out = new IndentingPrintWriter(new OutputStreamWriter(System.out));
    }

    return out;
  }

  private void println(String s) {
    print(s + "\n");
  }

  @Override
  public void text(String s) {
    if (printing && !(s.length() == 1 && (s.charAt(0) == '\n' || s.charAt(0) == '\r'))) {
      print(s);
    }
  }

  @Override
  public void print(String s) {
    out.print(s);
  }

  @Override
  public void documentStart() {
    out = createOutputStream();
  }

  @Override
  public void documentEnd() {
    out.close();
  }

  @Override
  public void specialTokens(String s) {
  }

  @Override
  public void tokenStart(TokenProduction tp) {
    printing = false;
  }

  @Override
  public void tokenEnd(TokenProduction tp) {
    printing = true;
  }

  @Override
  public void nonterminalsStart() { }

  @Override
  public void nonterminalsEnd() { }

  @Override
  public void tokensStart() { }

  @Override
  public void tokensEnd() { }

  @Override
  public void javacode(JavaCodeProduction jp) { }

  @Override
  public void expansionEnd(Expansion e, boolean first) { }

  @Override
  public void nonTerminalStart(NonTerminal nt) { }

  @Override
  public void nonTerminalEnd(NonTerminal nt) { }

  @Override
  public void productionStart(NormalProduction np) {
    println("");
    print(np.getLhs() + " ::= ");
  }

  @Override
  public void productionEnd(NormalProduction np) {
    println("");
  }

  @Override
  public void expansionStart(Expansion e, boolean first) {
    if (!first) {
      print(" | ");
    }
  }

  @Override
  public void reStart(RegularExpression r) {
    if (r.getClass().equals(RJustName.class) || r.getClass().equals(RCharacterList.class)) {
      printing = false;
    }
  }

  @Override
  public void reEnd(RegularExpression r) {
    printing = true;
  }

  @Override
  public void debug(String message) { System.err.println(message); }

  @Override
  public void info(String message) { System.err.println(message); }

  @Override
  public void warn(String message) { System.err.println(message); }

  @Override
  public void error(String message) { System.err.println(message); }
}
