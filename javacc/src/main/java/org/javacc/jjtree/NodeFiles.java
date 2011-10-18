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

import org.javacc.parser.FileGenerator;
import org.javacc.parser.MetaParseException;
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

final class NodeFiles implements FileGenerator {
  static Set<String> nodesGenerated = new HashSet<String>();

  @Override
  public void start() throws MetaParseException, IOException {
    //
  }

  static void ensure(String nodeType) throws IOException {
    if ("Node".equals(nodeType)) {}
    else if ("SimpleNode".equals(nodeType)) {
      ensure("Node");
    }
    else {
      ensure("SimpleNode");
    }

    /* Only build the node file if we're dealing with Node.java, or
       the NODE_BUILD_FILES option is set. */
    if (!("Node".equals(nodeType) || JJTreeOptions.getBuildNodeFiles())) {
      return;
    }

    File path = new File(JJTreeOptions.getJJTreeOutputDirectory(), nodeType + ".java");

    if (path.exists() && nodesGenerated.contains(path.getName())) {
      return;
    }

    nodesGenerated.add(path.getName());

    OutputFile outputFile = new OutputFile(path);
    if ("Node".equals(nodeType)) {
      generateNodeClass(outputFile);
    }
    else if ("SimpleNode".equals(nodeType)) {
      generateSimpleNodeClass(outputFile);
    }
    else {
      generateMultiNodeClass(outputFile, nodeType);
    }
  }

  static void generatePrologue(IndentingPrintWriter out) {
    // Output the node's package name. JJTreeGlobals.nodePackageName
    // will be the value of NODE_PACKAGE in OPTIONS; if that wasn't set it
    // will default to the parser's package name.
    // If the package names are different we will need to import classes
    // from the parser's package.
    if (!"".equals(JJTreeGlobals.nodePackageName)) {
      out.println("package " + JJTreeGlobals.nodePackageName + ";");
      out.println();
      if (!JJTreeGlobals.nodePackageName.equals(JJTreeGlobals.packageName)) {
        out.println("import " + JJTreeGlobals.packageName + ".*;");
        out.println();
      }
    }
  }

  static void generateTreeConstantsClass() throws IOException {
    File path = new File(JJTreeOptions.getJJTreeOutputDirectory(), JJTreeGlobals.treeConstantsClass() + ".java");
    OutputFile outputFile = new OutputFile(path);
    IndentingPrintWriter out = outputFile.getPrintWriter();

    List<String> nodeIds = ASTNodeDescriptor.getNodeIds();
    List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

    generatePrologue(out);

    out.println("public interface " + JJTreeGlobals.treeConstantsClass() + " {");

    for (int i = 0; i < nodeIds.size(); i++) {
      String n = nodeIds.get(i);
      out.println("  int " + n + " = " + i + ";");
    }

    out.println();

    out.println("  String[] jjtNodeName = {");
    for (String n : nodeNames) {
      out.println("    \"" + n + "\",");
    }
    out.println("  };");

    out.println("}");
    out.close();
  }

  static void generateVisitorClass() throws IOException {
    if (!JJTreeOptions.getVisitor()) {
      return;
    }

    File path = new File(JJTreeOptions.getJJTreeOutputDirectory(), JJTreeGlobals.visitorClass() + ".java");
    OutputFile outputFile = new OutputFile(path);
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      List<String> nodeNames = ASTNodeDescriptor.getNodeNames();

      generatePrologue(out);

      out.println("public interface " + JJTreeGlobals.visitorClass() + " {");

      String ve = mergeVisitorException();

      String argumentType = "Object";
      if (!"".equals(JJTreeOptions.getVisitorDataType())) {
        argumentType = JJTreeOptions.getVisitorDataType();
      }

      out.println("  " + JJTreeOptions.getVisitorReturnType() + " visit(SimpleNode node, " + argumentType + " data)" +
          ve + ";");
      if (JJTreeOptions.getMulti()) {
        for (String nodeName : nodeNames) {
          if ("void".equals(nodeName)) {
            continue;
          }
          String nodeType = JJTreeOptions.getNodePrefix() + nodeName;
          out.println("  " + JJTreeOptions.getVisitorReturnType() + " visit(" + nodeType +
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

  private static void generateNodeClass(OutputFile outputFile) throws IOException {
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      Map<String, Object> options = new HashMap<String, Object>(Options.getOptions());
      options.put("PARSER_NAME",
          JJTreeGlobals.parserName);
      options.put("VISITOR_TYPE",
          JJTreeGlobals.visitorClass());
      options.put("TREE_CONSTANTS_TYPE",
          JJTreeGlobals.treeConstantsClass());
      JavaFileGenerator generator = new JavaFileGenerator(
          "/templates/Node.template", options);
      generatePrologue(out);
      generator.generate(out);
    }
    finally {
      out.close();
    }
  }

  private static void generateSimpleNodeClass(OutputFile outputFile) throws IOException {
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      Map<String, Object> options = new HashMap<String, Object>(Options.getOptions());
      options.put("PARSER_NAME",
          JJTreeGlobals.parserName);
      options.put("VISITOR_TYPE",
          JJTreeGlobals.visitorClass());
      options.put("TREE_CONSTANTS_TYPE",
          JJTreeGlobals.treeConstantsClass());
      options.put("VISITOR_RETURN_TYPE_VOID",
          "void".equals(JJTreeOptions.getVisitorReturnType()));
      JavaFileGenerator generator = new JavaFileGenerator(
          "/templates/SimpleNode.template", options);
      generatePrologue(out);
      generator.generate(out);
    }
    finally {
      out.close();
    }
  }

  private static void generateMultiNodeClass(OutputFile outputFile, String nodeType)
      throws IOException {
    IndentingPrintWriter out = outputFile.getPrintWriter();
    try {
      Map<String, Object> options = new HashMap<String, Object>(Options.getOptions());
      options.put("PARSER_NAME",
          JJTreeGlobals.parserName);
      options.put("VISITOR_TYPE",
          JJTreeGlobals.visitorClass());
      options.put("TREE_CONSTANTS_TYPE",
          JJTreeGlobals.treeConstantsClass());
      options.put("NODE_TYPE",
          nodeType);
      options.put("VISITOR_RETURN_TYPE_VOID",
          "void".equals(JJTreeOptions.getVisitorReturnType()));
      JavaFileGenerator generator = new JavaFileGenerator(
          "/templates/MultiNode.template", options);
      generatePrologue(out);
      generator.generate(out);
    }
    finally {
      out.close();
    }
  }
}
