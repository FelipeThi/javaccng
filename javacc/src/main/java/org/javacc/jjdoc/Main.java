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

import org.javacc.parser.JavaCCErrors;
import org.javacc.parser.JavaCCParser;
import org.javacc.parser.JavaCCScanner;
import org.javacc.parser.JavaCCState;
import org.javacc.parser.JavaCharStream;
import org.javacc.parser.MetaParseException;
import org.javacc.parser.ParseException;
import org.javacc.utils.Tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

public final class Main {
  private Main() {}

  public static void main(String[] args) throws Exception {
    System.exit(mainProgram(args));
  }

  public static int mainProgram(String[] args) throws Exception {
    org.javacc.parser.Main.reInitAll();
    JJDocOptions.init();

    Tools.bannerLine("Documentation Generator", "0.1.4");

    JavaCCParser parser;
    if (args.length == 0) {
      usage();
      return 1;
    }
    else {
      JJDocGlobals.info("(type \"jjdoc\" with no arguments for help)");
    }

    if (JJDocOptions.isOption(args[args.length - 1])) {
      JJDocGlobals.error("Last argument \"" + args[args.length - 1] + "\" is not a filename or \"-\".  ");
      return 1;
    }
    for (int arg = 0; arg < args.length - 1; arg++) {
      if (!JJDocOptions.isOption(args[arg])) {
        JJDocGlobals.error("Argument \"" + args[arg] + "\" must be an option setting.  ");
        return 1;
      }
      JJDocOptions.setCmdLineOption(args[arg]);
    }

    if (args[args.length - 1].equals("-")) {
      JJDocGlobals.info("Reading from standard input . . .");
      parser = new JavaCCParser(
          new JavaCCScanner(
              new JavaCharStream(
                  new InputStreamReader(System.in))));
      JJDocGlobals.inputFile = "standard input";
      JJDocGlobals.outputFile = "standard output";
    }
    else {
      JJDocGlobals.info("Reading from file " + args[args.length - 1] + " . . .");
      try {
        File fp = new File(args[args.length - 1]);
        if (!fp.exists()) {
          JJDocGlobals.error("File " + args[args.length - 1] + " not found.");
          return 1;
        }
        if (fp.isDirectory()) {
          JJDocGlobals.error(args[args.length - 1] + " is a directory. Please use a valid file name.");
          return 1;
        }
        JJDocGlobals.inputFile = fp.getName();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(
                new FileInputStream(args[args.length - 1]),
                JJDocOptions.getGrammarEncoding()));
        parser = new JavaCCParser(new JavaCCScanner(new JavaCharStream(reader)));
      }
      catch (SecurityException se) {
        JJDocGlobals.error("Security violation while trying to open " + args[args.length - 1]);
        return 1;
      }
      catch (FileNotFoundException e) {
        JJDocGlobals.error("File " + args[args.length - 1] + " not found.");
        return 1;
      }
    }
    try {
      JavaCCState state = new JavaCCState();

      parser.setState(state);

      parser.start();

      JJDoc doc = new JJDoc(state);
      doc.start();

      if (JavaCCErrors.getErrorCount() == 0) {
        if (JavaCCErrors.getWarningCount() == 0) {
          JJDocGlobals.info("Grammar documentation generated successfully in " + JJDocGlobals.outputFile);
        }
        else {
          JJDocGlobals.info("Grammar documentation generated with 0 errors and "
              + JavaCCErrors.getWarningCount() + " warnings.");
        }
        return 0;
      }
      else {
        JJDocGlobals.error("Detected " + JavaCCErrors.getErrorCount() + " errors and "
            + JavaCCErrors.getWarningCount() + " warnings.");
        return JavaCCErrors.getErrorCount() == 0 ? 0 : 1;
      }
    }
    catch (MetaParseException e) {
      JJDocGlobals.error(e.toString());
      JJDocGlobals.error("Detected " + JavaCCErrors.getErrorCount() + " errors and "
          + JavaCCErrors.getWarningCount() + " warnings.");
      return 1;
    }
    catch (ParseException e) {
      JJDocGlobals.error(e.toString());
      JJDocGlobals.error("Detected " + (JavaCCErrors.getErrorCount() + 1) + " errors and "
          + JavaCCErrors.getWarningCount() + " warnings.");
      return 1;
    }
  }

  private static void usage() {
    JJDocGlobals.info("");
    JJDocGlobals.info("    jjdoc option-settings - (to read from standard input)");
    JJDocGlobals.info("OR");
    JJDocGlobals.info("    jjdoc option-settings inputfile (to read from a file)");
    JJDocGlobals.info("");
    JJDocGlobals.info("WHERE");
    JJDocGlobals.info("    \"option-settings\" is a sequence of settings separated by spaces.");
    JJDocGlobals.info("");

    JJDocGlobals.info("Each option setting must be of one of the following forms:");
    JJDocGlobals.info("");
    JJDocGlobals.info("    -optionname=value (e.g., -TEXT=false)");
    JJDocGlobals.info("    -optionname:value (e.g., -TEXT:false)");
    JJDocGlobals.info("    -optionname       (equivalent to -optionname=true.  e.g., -TEXT)");
    JJDocGlobals.info("    -NOoptionname     (equivalent to -optionname=false. e.g., -NOTEXT)");
    JJDocGlobals.info("");
    JJDocGlobals.info("Option settings are not case-sensitive, so one can say \"-nOtExT\" instead");
    JJDocGlobals.info("of \"-NOTEXT\".  Option values must be appropriate for the corresponding");
    JJDocGlobals.info("option, and must be either an integer, boolean or string value.");
    JJDocGlobals.info("");
    JJDocGlobals.info("The string valued options are:");
    JJDocGlobals.info("");
    JJDocGlobals.info("    OUTPUT_FILE");
    JJDocGlobals.info("    CSS");
    JJDocGlobals.info("");
    JJDocGlobals.info("The boolean valued options are:");
    JJDocGlobals.info("");
    JJDocGlobals.info("    ONE_TABLE              (default true)");
    JJDocGlobals.info("    TEXT                   (default false)");
    JJDocGlobals.info("    BNF                    (default false)");
    JJDocGlobals.info("");

    JJDocGlobals.info("");
    JJDocGlobals.info("EXAMPLES:");
    JJDocGlobals.info("    jjdoc -ONE_TABLE=false mygrammar.jj");
    JJDocGlobals.info("    jjdoc - < mygrammar.jj");
    JJDocGlobals.info("");
    JJDocGlobals.info("ABOUT JJDoc:");
    JJDocGlobals.info("    JJDoc generates JavaDoc documentation from JavaCC grammar files.");
    JJDocGlobals.info("");
    JJDocGlobals.info("    For more information, see the online JJDoc documentation at");
    JJDocGlobals.info("    https://javacc.dev.java.net/doc/JJDoc.html");
  }
}
