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

public class JJTreeNode extends SimpleNode {
  private int ordinal;

  public JJTreeNode(int id) {
    super(id);
  }

  @Override
  public void jjtSetChild(Node n, int i) {
    super.jjtSetChild(n, i);
    ((JJTreeNode) n).setOrdinal(i);
  }

  public int getOrdinal() {
    return ordinal;
  }

  public void setOrdinal(int o) {
    ordinal = o;
  }

  /**
   * The following is added manually to enhance all tree nodes with
   * attributes that store the first and last tokens corresponding to
   * each node, as well as to print the tokens back to the specified
   * output stream.
   */
  private Token first, last;

  public Token getFirstToken() { return first; }

  public void setFirstToken(Token t) { first = t; }

  public Token getLastToken() { return last; }

  public void setLastToken(Token t) { last = t; }

  /**
   * This method prints the tokens corresponding to this node
   * recursively calling the print methods of its children.
   * Overriding this print method in appropriate nodes gives the
   * output the added stuff not in the input.
   */
  public void print(IO io) {
    /* Some productions do not consume any tokens.  In that case their
       first and last tokens are a bit strange. */
    if (!TokenUtils.hasTokens(this)) {
      return;
    }

    Token t = new Token(0, 0, 0, null);
    t.next = getFirstToken();
    JJTreeNode node;
    for (int n = 0; n < jjtGetChildCount(); n++) {
      node = (JJTreeNode) jjtGetChild(n);
      while (true) {
        t = t.next;
        if (t == node.getFirstToken()) {
          break;
        }
        print(io, t);
      }
      node.print(io);
      t = node.getLastToken();
    }
    while (t != getLastToken()) {
      t = t.next;
      print(io, t);
    }
  }

  String translateImage(Token t) {
    return t.getImage();
  }

  String whiteOut(Token t) {
    StringBuilder sb = new StringBuilder(t.getImage().length());

    for (int i = 0; i < t.getImage().length(); ++i) {
      char ch = t.getImage().charAt(i);
      if (ch != '\t' && ch != '\n' && ch != '\r' && ch != '\f') {
        sb.append(' ');
      }
      else {
        sb.append(ch);
      }
    }

    return sb.toString();
  }

  /**
   * Indicates whether the token should be replaced by white space or
   * replaced with the actual node variable.
   */
  private boolean whitingOut;

  protected void print(IO io, Token t) {
    Token st = t.specialToken;
    if (st != null) {
      while (st.specialToken != null) {
        st = st.specialToken;
      }
      while (st != null) {
        io.print(TokenUtils.escape(translateImage(st)));
        st = st.next;
      }
    }

    /* If we're within a node scope we modify the source in the
       following ways:

       1) we rename all references to `jjtThis' to be references to
       the actual node variable.

       2) we replace all calls to `jjTree.currentNode()' with
       references to the node variable. */

    NodeScope s = NodeScope.getEnclosingNodeScope(this);
    if (s == null) {
      /* Not within a node scope so we don't need to modify the
         source. */
      io.print(TokenUtils.escape(translateImage(t)));
      return;
    }

    if (t.getImage().equals("jjtThis")) {
      io.print(s.getNodeVariable());
      return;
    }
    else if (t.getImage().equals("jjTree")) {
      if (t.next.getImage().equals(".")) {
        if (t.next.next.getImage().equals("currentNode")) {
          if (t.next.next.next.getImage().equals("(")) {
            if (t.next.next.next.next.getImage().equals(")")) {
              /* Found `jjTree.currentNode()' so go into white out
                 mode.  We'll stay in this mode until we find the
                 closing parenthesis. */
              whitingOut = true;
            }
          }
        }
      }
    }
    if (whitingOut) {
      if (t.getImage().equals("jjTree")) {
        io.print(s.getNodeVariable());
        io.print(" ");
      }
      else if (t.getImage().equals(")")) {
        io.print(" ");
        whitingOut = false;
      }
      else {
        for (int i = 0; i < t.getImage().length(); ++i) {
          io.print(" ");
        }
      }
      return;
    }

    io.print(TokenUtils.escape(translateImage(t)));
  }

  static void openJJTreeComment(IO io, String arg) {
    if (arg != null) {
      io.print("/*@bgen(jjtree) " + arg + " */");
    }
    else {
      io.print("/*@bgen(jjtree)*/");
    }
  }

  static void closeJJTreeComment(IO io) {
    io.print("/*@egen*/");
  }

  String getIndentation(JJTreeNode n) {
    return getIndentation(n, 0);
  }

  String getIndentation(JJTreeNode n, int offset) {
    String s = "";
    for (int i = offset + 1; i < n.getFirstToken().getColumn(); ++i) {
      s += " ";
    }
    return s;
  }
}
