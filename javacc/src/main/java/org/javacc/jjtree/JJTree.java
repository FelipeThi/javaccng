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

package org.javacc.jjtree;

import org.javacc.parser.Main;
import org.javacc.utils.Tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

public final class JJTree {
  private IO io;

  private void p(String s) {
    io.getMsg().println(s);
  }

  public int main(String[] args) {
    // initialize static state for allowing repeat runs without exiting
    ASTNodeDescriptor.nodeIds = new ArrayList<String>();
    ASTNodeDescriptor.nodeNames = new ArrayList<String>();
    ASTNodeDescriptor.nodeSeen = new Hashtable<String, String>();
    Main.reInitAll();

    Tools.bannerLine("Tree Builder", "");

    io = new IO();
    try {
      return generate(args);
    }
    finally {
      io.closeAll();
    }
  }

  private int generate(String[] args) {
    initializeOptions();

    if (args.length == 0) {
      p("");
      usage();
      return 1;
    }
    else {
      p("(type \"jjtree\" with no arguments for help)");
    }

    String fn = args[args.length - 1];

    if (JJTreeOptions.isOption(fn)) {
      p("Last argument \"" + fn + "\" is not a filename");
      return 1;
    }

    for (int arg = 0; arg < args.length - 1; arg++) {
      if (!JJTreeOptions.isOption(args[arg])) {
        p("Argument \"" + args[arg] + "\" must be an option setting.");
        return 1;
      }
      JJTreeOptions.setCmdLineOption(args[arg]);
    }

    JJTreeOptions.validate();

    try {
      io.setInput(fn);
    }
    catch (IOException ex) {
      p("Error setting input: " + ex.getMessage());
      return 1;
    }

    p("Reading from file " + io.getInputFileName() + " . . .");

    try {
      JJTreeParser parser = new JJTreeParser(
          new JJTreeParserTokenManager(
              new JavaCharStream(io.getIn())));
      parser.javacc_input();

      ASTGrammar root = (ASTGrammar) parser.jjtree.rootNode();
      if (Boolean.getBoolean("jjtree-dump")) {
        root.dump(" ");
      }
      try {
        io.setOutput();
      }
      catch (IOException ex) {
        p("Error setting output: " + ex.getMessage());
        return 1;
      }

      root.generate(io);

      io.getOut().close();

      NodeFiles.generateTreeConstants_java();
      NodeFiles.generateVisitor_java();
      JJTreeState.generateTreeState_java();

      p("Annotated grammar generated successfully in " +
          io.getOutputFileName());
    }
    catch (ParseException ex) {
      p("Error parsing input: " + ex.toString());
      return 1;
    }
    catch (Exception ex) {
      p("Error parsing input: " + ex.toString());
      ex.printStackTrace(io.getMsg());
      return 1;
    }

    return 0;
  }

  /** Initialize for JJTree */
  private void initializeOptions() {
    JJTreeOptions.init();
    JJTreeGlobals.initialize();
  }

  private void usage() {
    p("Usage:");
    p("    jjtree option-settings inputfile");
    p("");
    p("\"option-settings\" is a sequence of settings separated by spaces.");
    p("Each option setting must be of one of the following forms:");
    p("");
    p("    -optionname=value (e.g., -VISITOR=false)");
    p("    -optionname:value (e.g., -VISITOR:false)");
    p("    -optionname       (equivalent to -optionname=true.  e.g., -VISITOR)");
    p("    -NOoptionname     (equivalent to -optionname=false. e.g., -NOVISITOR)");
    p("");
    p("Option settings are not case-sensitive, so one can say \"-nOvIsItOr\" instead");
    p("of \"-NOVISITOR\".  Option values must be appropriate for the corresponding");
    p("option, and must be either an integer or a string value.");
    p("");

    p("The boolean valued options are:");
    p("");
    p("    MULTI                    (default false)");
    p("    NODE_DEFAULT_VOID        (default false)");
    p("    NODE_SCOPE_HOOK          (default false)");
    p("    NODE_USES_PARSER         (default false)");
    p("    BUILD_NODE_FILES         (default true)");
    p("    TRACK_TOKENS             (default false)");
    p("    VISITOR                  (default false)");
    p("");
    p("The string valued options are:");
    p("");
    p("    JDK_VERSION              (default \"1.5\")");
    p("    NODE_CLASS               (default \"\")");
    p("    NODE_PREFIX              (default \"AST\")");
    p("    NODE_PACKAGE             (default \"\")");
    p("    NODE_EXTENDS             (default \"\")");
    p("    NODE_FACTORY             (default \"\")");
    p("    OUTPUT_FILE              (default remove input file suffix, add .jj)");
    p("    OUTPUT_DIRECTORY         (default \"\")");
    p("    JJTREE_OUTPUT_DIRECTORY  (default value of OUTPUT_DIRECTORY option)");
    p("    VISITOR_DATA_TYPE        (default \"\")");
    p("    VISITOR_RETURN_TYPE      (default \"Object\")");
    p("    VISITOR_EXCEPTION        (default \"\")");
    p("");
    p("JJTree also accepts JavaCC options, which it inserts into the generated file.");
    p("");

    p("EXAMPLES:");
    p("    jjtree -VISITOR=true mygrammar.jjt");
    p("");
    p("ABOUT JJTree:");
    p("    JJTree is a preprocessor for JavaCC that inserts actions into a");
    p("    JavaCC grammar to build parse trees for the input.");
    p("");
    p("    For more information, see the online JJTree documentation at ");
    p("    https://javacc.dev.java.net/doc/JJTree.html ");
    p("");
  }
}
