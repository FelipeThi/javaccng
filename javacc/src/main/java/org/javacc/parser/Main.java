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

import org.javacc.utils.Tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public final class Main {
  public static void main(String[] args) throws Exception {
    System.exit(mainProgram(args));
  }

  public static int mainProgram(String[] args) throws Exception {
    reInitAll();

    Tools.bannerLine("Parser Generator", "");

    if (args.length == 0) {
      System.out.println("");
      usage();
      return 1;
    }

    System.out.println("(type \"javacc\" with no arguments for help)");

    if (Options.isOption(args[args.length - 1])) {
      System.out.println("Last argument \"" + args[args.length - 1] + "\" is not a filename.");
      return 1;
    }

    for (int arg = 0; arg < args.length - 1; arg++) {
      if (!Options.isOption(args[arg])) {
        System.out.println("Argument \"" + args[arg] + "\" must be an option setting.");
        return 1;
      }
      Options.setCmdLineOption(args[arg]);
    }

    return run(args);
  }

  private static int run(String[] args) throws IOException, ParseException {
    String path = args[args.length - 1];

    JavaCCParser parser;
    try {
      File file = new File(path);
      if (!file.exists()) {
        System.out.println("File " + path + " not found.");
        return 1;
      }
      if (file.isDirectory()) {
        System.out.println(path + " is a directory. Please use a valid file name.");
        return 1;
      }
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(
              new FileInputStream(path),
              Options.getGrammarEncoding()));

      parser = new JavaCCParser(
          new JavaCCScanner(
              new CharStream.Escaping(
                  new CharStream.ForReader(reader))));
    }
    catch (SecurityException ex) {
      System.out.println("Security violation while trying to open " + path);
      return 1;
    }
    catch (FileNotFoundException ex) {
      System.out.println("File " + path + " not found.");
      return 1;
    }

    try {
      System.out.println("Reading from file " + path + " . . .");

      JavaCCState state = new JavaCCState();

      state.fileName = path;

      parser.setState(state);

      parser.start();

      Tools.createOutputDir(Options.getOutputDirectory());

      Semanticize semanticize = new Semanticize(state);
      semanticize.start();

      ParserGen parserGen = new ParserGen(state, semanticize);
      parserGen.start();

      ScannerGen scannerGen = new ScannerGen(state);
      if (Options.getUnicodeInput()) {
        scannerGen.nfaStates.unicodeWarningGiven = true;
        System.out.println("Note: UNICODE_INPUT option is specified. " +
            "Please make sure you create the parser/lexer using a Reader with the correct character encoding.");
      }
      scannerGen.start();

      ConstantsFile constantsFile = new ConstantsFile(state, scannerGen);
      constantsFile.start();

      JavaFiles javaFiles = new JavaFiles(state);
      javaFiles.start();

      if (JavaCCErrors.getErrorCount() == 0
          && (Options.getBuildParser() || Options.getBuildScanner())) {
        if (JavaCCErrors.getWarningCount() == 0) {
          System.out.println("Parser generated successfully.");
        }
        else {
          System.out.println("Parser generated with 0 errors and "
              + JavaCCErrors.getWarningCount() + " warnings.");
        }
        return 0;
      }
      else {
        System.out.println("Detected " + JavaCCErrors.getErrorCount() + " errors and "
            + JavaCCErrors.getWarningCount() + " warnings.");
        return JavaCCErrors.getErrorCount() == 0 ? 0 : 1;
      }
    }
    catch (MetaParseException ex) {
      System.out.println("Detected " + JavaCCErrors.getErrorCount() + " errors and "
          + JavaCCErrors.getWarningCount() + " warnings.");
      return 1;
    }
    catch (ParseException ex) {
      System.out.println(ex.toString());
      System.out.println("Detected " + (JavaCCErrors.getErrorCount() + 1) + " errors and "
          + JavaCCErrors.getWarningCount() + " warnings.");
      return 1;
    }
  }

  @Deprecated
  public static void reInitAll() {
    JavaCCErrors.reInit();
    Options.init();
  }

  private static void usage() {
    System.out.println("Usage:");
    System.out.println("    javacc option-settings inputfile");
    System.out.println("");
    System.out.println("\"option-settings\" is a sequence of settings separated by spaces.");
    System.out.println("Each option setting must be of one of the following forms:");
    System.out.println("");
    System.out.println("    -optionname=value (e.g., -IGNORE_CASE=false)");
    System.out.println("    -optionname:value (e.g., -IGNORE_CASE:false)");
    System.out.println("    -optionname       (equivalent to -optionname=true.  e.g., -IGNORE_CASE)");
    System.out.println("    -NOoptionname     (equivalent to -optionname=false. e.g., -NOIGNORE_CASE)");
    System.out.println("");
    System.out.println("Option settings are not case-sensitive, so one can say \"-nOiGnOrE_cAsE\" instead");
    System.out.println("of \"-NOIGNORE_CASE\".  Option values must be appropriate for the corresponding");
    System.out.println("option, and must be either an integer, a boolean, or a string value.");
    System.out.println("");
    System.out.println("The integer valued options are:");
    System.out.println("");
    System.out.println("    LOOKAHEAD              (default 1)");
    System.out.println("    CHOICE_AMBIGUITY_CHECK (default 2)");
    System.out.println("    OTHER_AMBIGUITY_CHECK  (default 1)");
    System.out.println("");
    System.out.println("The boolean valued options are:");
    System.out.println("");
    System.out.println("    DEBUG_PARSER           (default false)");
    System.out.println("    DEBUG_LOOKAHEAD        (default false)");
    System.out.println("    DEBUG_SCANNER          (default false)");
    System.out.println("    ERROR_REPORTING        (default true)");
    System.out.println("    JAVA_UNICODE_ESCAPE    (default false)");
    System.out.println("    UNICODE_INPUT          (default false)");
    System.out.println("    IGNORE_CASE            (default false)");
    System.out.println("    COMMON_TOKEN_ACTION    (default false)");
    System.out.println("    USER_SCANNER           (default false)");
    System.out.println("    USER_CHAR_STREAM       (default false)");
    System.out.println("    BUILD_PARSER           (default true)");
    System.out.println("    BUILD_SCANNER          (default true)");
    System.out.println("    SCANNER_USES_PARSER    (default false)");
    System.out.println("    SANITY_CHECK           (default true)");
    System.out.println("    FORCE_LA_CHECK         (default false)");
    System.out.println("    CACHE_TOKENS           (default false)");
    System.out.println("    KEEP_LINE_COLUMN       (default true)");
    System.out.println("");
    System.out.println("The string valued options are:");
    System.out.println("");
    System.out.println("    OUTPUT_DIRECTORY       (default Current Directory)");
    System.out.println("    TOKEN_EXTENDS          (default java.lang.Object)");
    System.out.println("    TOKEN_FACTORY          (default none)");
    System.out.println("    JDK_VERSION            (default 1.5)");
    System.out.println("    GRAMMAR_ENCODING       (defaults to platform file encoding)");
    System.out.println("");
    System.out.println("EXAMPLE:");
    System.out.println("    javacc -IGNORE_CASE=false -LOOKAHEAD:2 -debug_parser mygrammar.jj");
    System.out.println("");
  }
}
