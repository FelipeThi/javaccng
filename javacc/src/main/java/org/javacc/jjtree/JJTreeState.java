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

import org.javacc.parser.OutputFile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/** Generate the State of a tree. */
final class JJTreeState {

  private JJTreeState() {}

  static void insertParserMembers(IO io) {
    String s;

    if (JJTreeOptions.getStatic()) {
      s = "static ";
    }
    else {
      s = "";
    }

    io.println();
    io.println("  protected " + s + nameState() +
        " jjtree = new " + nameState() + "();");
    io.println();
  }

  private static String nameState() {
    return "JJT" + JJTreeGlobals.parserName + "State";
  }

  static void generateTreeState_java() {
    File file = new File(JJTreeOptions.getJJTreeOutputDirectory(), nameState() + ".java");

    try {
      OutputFile outputFile = new OutputFile(file);
      PrintWriter out = outputFile.getPrintWriter();
      NodeFiles.generatePrologue(out);
      insertState(out);
      outputFile.close();
    }
    catch (IOException e) {
      throw new Error(e.toString());
    }
  }

  private static void insertState(PrintWriter out) {
    out.println("public class " + nameState() + " {");

    if (!JJTreeOptions.getGenerateGenerics()) {
      out.println("  private java.util.List nodes;");
    }
    else {
      out.println("  private java.util.List<Node> nodes;");
    }

    if (!JJTreeOptions.getGenerateGenerics()) {
      out.println("  private java.util.List marks;");
    }
    else {
      out.println("  private java.util.List<Integer> marks;");
    }

    out.println("");
    out.println("  private int sp;        // number of nodes on stack");
    out.println("  private int mk;        // current mark");
    out.println("  private boolean node_created;");
    out.println("");
    out.println("  public " + nameState() + "() {");

    if (!JJTreeOptions.getGenerateGenerics()) {
      out.println("    nodes = new java.util.ArrayList();");
    }
    else {
      out.println("    nodes = new java.util.ArrayList<Node>();");
    }

    if (!JJTreeOptions.getGenerateGenerics()) {
      out.println("    marks = new java.util.ArrayList();");
    }
    else {
      out.println("    marks = new java.util.ArrayList<Integer>();");
    }

    out.println("    sp = 0;");
    out.println("    mk = 0;");
    out.println("  }");
    out.println("");
    out.println("  /* Determines whether the current node was actually closed and");
    out.println("     pushed.  This should only be called in the final user action of a");
    out.println("     node scope.  */");
    out.println("  public boolean nodeCreated() {");
    out.println("    return node_created;");
    out.println("  }");
    out.println("");
    out.println("  /* Call this to reinitialize the node stack.  It is called");
    out.println("     automatically by the parser's ReInit() method. */");
    out.println("  public void reset() {");
    out.println("    nodes.clear();");
    out.println("    marks.clear();");
    out.println("    sp = 0;");
    out.println("    mk = 0;");
    out.println("  }");
    out.println("");
    out.println("  /* Returns the root node of the AST.  It only makes sense to call");
    out.println("     this after a successful parse. */");
    out.println("  public Node rootNode() {");
    if (!JJTreeOptions.getGenerateGenerics()) {
      out.println("    return (Node)nodes.get(0);");
    }
    else {
      out.println("    return nodes.get(0);");
    }
    out.println("  }");
    out.println("");
    out.println("  /* Pushes a node on to the stack. */");
    out.println("  public void pushNode(Node n) {");
    out.println("    nodes.add(n);");
    out.println("    ++sp;");
    out.println("  }");
    out.println("");
    out.println("  /* Returns the node on the top of the stack, and remove it from the");
    out.println("     stack.  */");
    out.println("  public Node popNode() {");
    out.println("    if (--sp < mk) {");
    if (!JJTreeOptions.getGenerateGenerics()) {
      out.println("      mk = ((Integer)marks.remove(marks.size()-1)).intValue();");
    }
    else {
      out.println("      mk = marks.remove(marks.size()-1);");
    }
    out.println("    }");
    if (!JJTreeOptions.getGenerateGenerics()) {
      out.println("    return (Node)nodes.remove(nodes.size()-1);");
    }
    else {
      out.println("    return nodes.remove(nodes.size()-1);");
    }
    out.println("  }");
    out.println("");
    out.println("  /* Returns the node currently on the top of the stack. */");
    out.println("  public Node peekNode() {");
    if (!JJTreeOptions.getGenerateGenerics()) {
      out.println("    return (Node)nodes.get(nodes.size()-1);");
    }
    else {
      out.println("    return nodes.get(nodes.size()-1);");
    }
    out.println("  }");
    out.println("");
    out.println("  /* Returns the number of children on the stack in the current node");
    out.println("     scope. */");
    out.println("  public int nodeArity() {");
    out.println("    return sp - mk;");
    out.println("  }");
    out.println("");
    out.println("");
    out.println("  public void clearNodeScope(Node n) {");
    out.println("    while (sp > mk) {");
    out.println("      popNode();");
    out.println("    }");
    if (!JJTreeOptions.getGenerateGenerics()) {
      out.println("    mk = ((Integer)marks.remove(marks.size()-1)).intValue();");
    }
    else {
      out.println("    mk = marks.remove(marks.size()-1);");
    }
    out.println("  }");
    out.println("");
    out.println("");
    out.println("  public void openNodeScope(Node n) {");
    if (!JJTreeOptions.getGenerateGenerics()) {
      out.println("    marks.add(new Integer(mk));");
    }
    else {
      out.println("    marks.add(mk);");
    }
    out.println("    mk = sp;");
    out.println("    n.jjtOpen();");
    out.println("  }");
    out.println("");
    out.println("");
    out.println("  /* A definite node is constructed from a specified number of");
    out.println("     children.  That number of nodes are popped from the stack and");
    out.println("     made the children of the definite node.  Then the definite node");
    out.println("     is pushed on to the stack. */");
    out.println("  public void closeNodeScope(Node n, int num) {");
    if (!JJTreeOptions.getGenerateGenerics()) {
      out.println("    mk = ((Integer)marks.remove(marks.size()-1)).intValue();");
    }
    else {
      out.println("    mk = marks.remove(marks.size()-1);");
    }
    out.println("    while (num-- > 0) {");
    out.println("      Node c = popNode();");
    out.println("      c.jjtSetParent(n);");
    out.println("      n.jjtAddChild(c, num);");
    out.println("    }");
    out.println("    n.jjtClose();");
    out.println("    pushNode(n);");
    out.println("    node_created = true;");
    out.println("  }");
    out.println("");
    out.println("");
    out.println("  /* A conditional node is constructed if its condition is true.  All");
    out.println("     the nodes that have been pushed since the node was opened are");
    out.println("     made children of the conditional node, which is then pushed");
    out.println("     on to the stack.  If the condition is false the node is not");
    out.println("     constructed and they are left on the stack. */");
    out.println("  public void closeNodeScope(Node n, boolean condition) {");
    out.println("    if (condition) {");
    out.println("      int a = nodeArity();");
    if (!JJTreeOptions.getGenerateGenerics()) {
      out.println("      mk = ((Integer)marks.remove(marks.size()-1)).intValue();");
    }
    else {
      out.println("      mk = marks.remove(marks.size()-1);");
    }
    out.println("      while (a-- > 0) {");
    out.println("        Node c = popNode();");
    out.println("        c.jjtSetParent(n);");
    out.println("        n.jjtAddChild(c, a);");
    out.println("      }");
    out.println("      n.jjtClose();");
    out.println("      pushNode(n);");
    out.println("      node_created = true;");
    out.println("    } else {");
    if (!JJTreeOptions.getGenerateGenerics()) {
      out.println("      mk = ((Integer)marks.remove(marks.size()-1)).intValue();");
    }
    else {
      out.println("      mk = marks.remove(marks.size()-1);");
    }
    out.println("      node_created = false;");
    out.println("    }");
    out.println("  }");
    out.println("}");
  }
}

/*end*/
