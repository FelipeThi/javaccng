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

import org.javacc.parser.Options;
import org.javacc.parser.OutputFile;
import org.javacc.utils.JavaFileGenerator;
import org.javacc.utils.io.IndentingPrintWriter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class NodeFiles {
  static Set<String> nodesGenerated = new HashSet<String>();

  static void ensure(IO io, String nodeType) throws IOException {
    File file = new File(JJTreeOptions.getJJTreeOutputDirectory(), nodeType + ".java");

    if ("Node".equals(nodeType)) {}
    else if ("SimpleNode".equals(nodeType)) {
      ensure(io, "Node");
    }
    else {
      ensure(io, "SimpleNode");
    }

    /* Only build the node file if we're dealing with Node.java, or
       the NODE_BUILD_FILES option is set. */
    if (!(nodeType.equals("Node") || JJTreeOptions.getBuildNodeFiles())) {
      return;
    }

    if (file.exists() && nodesGenerated.contains(file.getName())) {
      return;
    }

    nodesGenerated.add(file.getName());

    OutputFile outputFile = new OutputFile(file);
    if (nodeType.equals("Node")) {
      generateNode_java(outputFile);
    }
    else if (nodeType.equals("SimpleNode")) {
      generateSimpleNode_java(outputFile);
    }
    else {
      generateMultiNode_java(outputFile, nodeType);
    }
  }

  static void generatePrologue(IndentingPrintWriter out) {
    // Output the node's package name. JJTreeGlobals.nodePackageName
    // will be the value of NODE_PACKAGE in OPTIONS; if that wasn't set it
    // will default to the parser's package name.
    // If the package names are different we will need to import classes
    // from the parser's package.
    if (!JJTreeGlobals.nodePackageName.equals("")) {
      out.println("package " + JJTreeGlobals.nodePackageName + ";");
      out.println();
      if (!JJTreeGlobals.nodePackageName.equals(JJTreeGlobals.packageName)) {
        out.println("import " + JJTreeGlobals.packageName + ".*;");
        out.println();
      }
    }
  }

  static String nodeConstants() {
    return JJTreeGlobals.parserName + "TreeConstants";
  }

  static void generateTreeConstants_java() throws IOException {
    String name = nodeConstants();
    File path = new File(JJTreeOptions.getJJTreeOutputDirectory(), name + ".java");

    OutputFile outputFile = new OutputFile(path);
    IndentingPrintWriter out = outputFile.getPrintWriter();

    List<String> nodeIds = ASTNodeDescriptor.getNodeIds();
    List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

    generatePrologue(out);
    out.println("public interface " + name);
    out.println("{");

    for (int i = 0; i < nodeIds.size(); i++) {
      String n = nodeIds.get(i);
      out.println("  public int " + n + " = " + i + ";");
    }

    out.println();
    out.println();

    out.println("  public String[] jjtNodeName = {");
    for (int i = 0; i < nodeNames.size(); i++) {
      String n = nodeNames.get(i);
      out.println("    \"" + n + "\",");
    }
    out.println("  };");

    out.println("}");
    out.close();
  }

  static String visitorClass() {
    return JJTreeGlobals.parserName + "Visitor";
  }

  static void generateVisitor_java() throws IOException {
    if (!JJTreeOptions.getVisitor()) {
      return;
    }

    String name = visitorClass();
    File file = new File(JJTreeOptions.getJJTreeOutputDirectory(), name + ".java");

    OutputFile outputFile = new OutputFile(file);
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      List nodeNames = ASTNodeDescriptor.getNodeNames();

      generatePrologue(out);

      out.println("public interface " + name);
      out.println("{");

      String ve = mergeVisitorException();

      String argumentType = "Object";
      if (!JJTreeOptions.getVisitorDataType().equals("")) {
        argumentType = JJTreeOptions.getVisitorDataType();
      }

      out.println("  public " + JJTreeOptions.getVisitorReturnType() + " visit(SimpleNode node, " + argumentType + " data)" +
          ve + ";");
      if (JJTreeOptions.getMulti()) {
        for (int i = 0; i < nodeNames.size(); ++i) {
          String n = (String) nodeNames.get(i);
          if (n.equals("void")) {
            continue;
          }
          String nodeType = JJTreeOptions.getNodePrefix() + n;
          out.println("  public " + JJTreeOptions.getVisitorReturnType() + " visit(" + nodeType +
              " node, " + argumentType + " data)" + ve + ";");
        }
      }
      out.println("}");
    }
    finally {
      out.close();
    }
  }

  private static String mergeVisitorException() {
    String ve = JJTreeOptions.getVisitorException();
    if (!"".equals(ve)) {
      ve = " throws " + ve;
    }
    return ve;
  }

  private static void generateNode_java(OutputFile outputFile) throws IOException {
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      generatePrologue(out);

      Map options = new HashMap(Options.getOptions());
      options.put("PARSER_NAME",
          JJTreeGlobals.parserName);

      JavaFileGenerator generator = new JavaFileGenerator(
          "/templates/Node.template", options);

      generator.generate(out);
    }
    finally {
      out.close();
    }
  }

  private static void generateSimpleNode_java(OutputFile outputFile) throws IOException {
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      generatePrologue(out);
      Map<String, Object> options = new HashMap<String, Object>(Options.getOptions());
      options.put("PARSER_NAME",
          JJTreeGlobals.parserName);
      options.put("VISITOR_RETURN_TYPE_VOID",
          "void".equals(JJTreeOptions.getVisitorReturnType()));
      JavaFileGenerator generator = new JavaFileGenerator(
          "/templates/SimpleNode.template", options);
      generator.generate(out);
    }
    finally {
      out.close();
    }
  }

  private static void generateMultiNode_java(OutputFile outputFile, String nodeType)
      throws IOException {
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      generatePrologue(out);
      Map<String, Object> options = new HashMap<String, Object>(Options.getOptions());
      options.put("PARSER_NAME",
          JJTreeGlobals.parserName);
      options.put("NODE_TYPE",
          nodeType);
      options.put("VISITOR_RETURN_TYPE_VOID",
          "void".equals(JJTreeOptions.getVisitorReturnType()));
      JavaFileGenerator generator = new JavaFileGenerator(
          "/templates/MultiNode.template", options);
      generator.generate(out);
    }
    finally {
      out.close();
    }
  }
}
