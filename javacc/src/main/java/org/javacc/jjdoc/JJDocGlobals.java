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

import org.javacc.utils.io.IndentingPrintWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

/** Global variables for JJDoc. */
public class JJDocGlobals {
  /** The name of the input file. */
  public static String inputFile;
  /** The name of the output file. */
  public static String outputFile;

  public static Formatter createFormatter(IndentingPrintWriter out) {
    if (JJDocOptions.getText()) {
      return new TextFormatter(out);
    }
    if (JJDocOptions.getBNF()) {
      return new BNFFormatter(out);
    }
    return new HTMLFormatter(out);
  }

  public static IndentingPrintWriter createOutputStream()
      throws IOException {
    if (JJDocOptions.getOutputFile().equals("")) {
      if (JJDocGlobals.inputFile.equals("standard input")) {
        return new IndentingPrintWriter(
            new OutputStreamWriter(System.out));
      }
      else {
        String ext = ".bnf";
        int i = JJDocGlobals.inputFile.lastIndexOf('.');
        if (i == -1) {
          outputFile = inputFile + ext;
        }
        else {
          String suffix = JJDocGlobals.inputFile.substring(i);
          if (suffix.equals(ext)) {
            outputFile = inputFile + ext;
          }
          else {
            outputFile = JJDocGlobals.inputFile.substring(0, i) + ext;
          }
        }
      }
    }
    else {
      outputFile = JJDocOptions.getOutputFile();
    }
    return new IndentingPrintWriter(
        new FileWriter(
            outputFile));
  }

  public static void info(String message) {
    System.err.println(message);
  }

  public static void error(String message) {
    System.err.println(message);
  }
}
