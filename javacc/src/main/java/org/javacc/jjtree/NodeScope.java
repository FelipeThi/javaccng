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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NodeScope {
  private final ASTProduction production;
  private final ASTNodeDescriptor nodeDescriptor;
  private final String closedVar;
  private final String exceptionVar;
  private final String nodeVar;
  private final int scopeNumber;

  NodeScope(ASTProduction p, ASTNodeDescriptor n) {
    production = p;

    if (n == null) {
      String name = production.name;
      if (JJTreeOptions.getNodeDefaultVoid()) {
        name = "void";
      }
      nodeDescriptor = ASTNodeDescriptor.indefinite(name);
    }
    else {
      nodeDescriptor = n;
    }

    scopeNumber = production.getNodeScopeNumber(this);
    nodeVar = constructVariable("n");
    closedVar = constructVariable("c");
    exceptionVar = constructVariable("e");
  }

  boolean isVoid() {
    return nodeDescriptor.isVoid();
  }

  ASTNodeDescriptor getNodeDescriptor() {
    return nodeDescriptor;
  }

  String getNodeDescriptorText() {
    return nodeDescriptor.getDescriptor();
  }

  String getNodeVariable() {
    return nodeVar;
  }

  private String constructVariable(String id) {
    String s = "000" + scopeNumber;
    return "jjt" + id + s.substring(s.length() - 3, s.length());
  }

  boolean usesCloseNodeVar() {
    return true;
  }

  void insertOpenNodeCode(IO io, String indent) throws IOException {
    String type = nodeDescriptor.getNodeType();
    String nodeClass;
    if (JJTreeOptions.getNodeClass().length() > 0 && !JJTreeOptions.getMulti()) {
      nodeClass = JJTreeOptions.getNodeClass();
    }
    else {
      nodeClass = type;
    }

    // Ensure that there is a template definition file for the node type.
    NodeFiles.ensure(type);

    io.print(indent + nodeClass + " " + nodeVar + " = ");
    String parserArg = JJTreeOptions.getNodeUsesParser() ? "this, " : "";

    if (JJTreeOptions.getNodeFactory().equals("*")) {
      // Old-style multiple-implementations.
      io.println("(" + nodeClass + ")" + nodeClass + ".jjtCreate(" + parserArg +
          nodeDescriptor.getNodeId() + ");");
    }
    else if (JJTreeOptions.getNodeFactory().length() > 0) {
      io.println("(" + nodeClass + ")" + JJTreeOptions.getNodeFactory() + ".jjtCreate(" + parserArg +
          nodeDescriptor.getNodeId() + ");");
    }
    else {
      io.println("new " + nodeClass + "(" + parserArg + nodeDescriptor.getNodeId() + ");");
    }

    if (usesCloseNodeVar()) {
      io.println(indent + "boolean " + closedVar + " = true;");
    }
    io.println(indent + nodeDescriptor.openNode(nodeVar));
    if (JJTreeOptions.getNodeScopeHook()) {
      io.println(indent + "jjtreeOpenNodeScope(" + nodeVar + ");");
    }

    if (JJTreeOptions.getTrackTokens()) {
      io.println(indent + nodeVar + ".jjtSetFirstToken(getToken(1));");
    }
  }

  void insertCloseNodeCode(IO io, String indent, boolean isFinal) {
    io.println(indent + nodeDescriptor.closeNode(nodeVar));
    if (usesCloseNodeVar() && !isFinal) {
      io.println(indent + closedVar + " = false;");
    }
    if (JJTreeOptions.getNodeScopeHook()) {
      io.println(indent + "jjtreeCloseNodeScope(" + nodeVar + ");");
    }

    if (JJTreeOptions.getTrackTokens()) {
      io.println(indent + nodeVar + ".jjtSetLastToken(getToken(0));");
    }
  }

  void insertOpenNodeAction(IO io, String indent) throws IOException {
    io.println(indent + "{");
    insertOpenNodeCode(io, indent + "  ");
    io.println(indent + "}");
  }

  void insertCloseNodeAction(IO io, String indent) {
    io.println(indent + "{");
    insertCloseNodeCode(io, indent + "  ", false);
    io.println(indent + "}");
  }

  private void insertCatchBlocks(IO io, Iterator<String> thrownNames, String indent) {
    String thrown;
    if (thrownNames.hasNext()) {
      io.println(indent + "} catch (Throwable " + exceptionVar + ") {");

      if (usesCloseNodeVar()) {
        io.println(indent + "  if (" + closedVar + ") {");
        io.println(indent + "    jjTree.clearNodeScope(" + nodeVar + ");");
        io.println(indent + "    " + closedVar + " = false;");
        io.println(indent + "  } else {");
        io.println(indent + "    jjTree.popNode();");
        io.println(indent + "  }");
      }

      while (thrownNames.hasNext()) {
        thrown = thrownNames.next();
        io.println(indent + "  if (" + exceptionVar + " instanceof " + thrown + ") {");
        io.println(indent + "    throw (" + thrown + ")" + exceptionVar + ";");
        io.println(indent + "  }");
      }
      /* This is either an Error or an undeclared Exception.  If it's
         an Error then the cast is good, otherwise we want to force
         the user to declare it by crashing on the bad cast. */
      io.println(indent + "  throw (Error)" + exceptionVar + ";");
    }
  }

  void tryTokenSequence(IO io, String indent, Token first, Token last) {
    io.println(indent + "try {");
    JJTreeNode.closeJJTreeComment(io);

    /* Print out all the tokens, converting all references to
       `jjtThis' into the current node variable. */
    for (Token t = first; t != last.next; t = t.next) {
      TokenUtils.print(t, io, "jjtThis", nodeVar);
    }

    JJTreeNode.openJJTreeComment(io, null);
    io.println();

    Iterator<String> thrown_names = production.throwsList.iterator();
    insertCatchBlocks(io, thrown_names, indent);

    io.println(indent + "} finally {");
    if (usesCloseNodeVar()) {
      io.println(indent + "  if (" + closedVar + ") {");
      insertCloseNodeCode(io, indent + "    ", true);
      io.println(indent + "  }");
    }
    io.println(indent + "}");
    JJTreeNode.closeJJTreeComment(io);
  }

  private static void findThrown(Map<String, String> thrownSet, JJTreeNode expansionUnit) {
    if (expansionUnit instanceof ASTBNFNonTerminal) {
      // Should really make the nonterminal explicitly maintain its name.
      String nt = expansionUnit.getFirstToken().getImage();
      ASTProduction prod = JJTreeGlobals.productions.get(nt);
      if (prod != null) {
        for (String t : prod.throwsList) {
          thrownSet.put(t, t);
        }
      }
    }
    for (int i = 0; i < expansionUnit.jjtGetChildCount(); ++i) {
      JJTreeNode n = (JJTreeNode) expansionUnit.jjtGetChild(i);
      findThrown(thrownSet, n);
    }
  }

  void tryExpansionUnit(IO io, String indent, JJTreeNode expansionUnit) {
    io.println(indent + "try {");
    JJTreeNode.closeJJTreeComment(io);

    expansionUnit.print(io);

    JJTreeNode.openJJTreeComment(io, null);
    io.println();

    Map<String, String> thrown_set = new HashMap<String, String>();
    findThrown(thrown_set, expansionUnit);
    Iterator<String> thrown_names = thrown_set.values().iterator();
    insertCatchBlocks(io, thrown_names, indent);

    io.println(indent + "} finally {");
    if (usesCloseNodeVar()) {
      io.println(indent + "  if (" + closedVar + ") {");
      insertCloseNodeCode(io, indent + "    ", true);
      io.println(indent + "  }");
    }
    io.println(indent + "}");
    JJTreeNode.closeJJTreeComment(io);
  }

  static NodeScope getEnclosingNodeScope(Node node) {
    if (node instanceof ASTBNFDeclaration) {
      return ((ASTBNFDeclaration) node).nodeScope;
    }
    for (Node n = node.jjtGetParent(); n != null; n = n.jjtGetParent()) {
      if (n instanceof ASTBNFDeclaration) {
        return ((ASTBNFDeclaration) n).nodeScope;
      }
      else if (n instanceof ASTBNFNodeScope) {
        return ((ASTBNFNodeScope) n).nodeScope;
      }
      else if (n instanceof ASTExpansionNodeScope) {
        return ((ASTExpansionNodeScope) n).nodeScope;
      }
    }
    return null;
  }
}
