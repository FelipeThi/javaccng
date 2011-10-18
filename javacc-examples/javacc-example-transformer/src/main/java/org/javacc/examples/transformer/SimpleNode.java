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

/* JJT: 0.3pre1 */

package org.javacc.examples.transformer;

import java.io.PrintWriter;

public class SimpleNode implements Node {
  protected Node parent;
  protected Node[] children;
  protected int id;

  public SimpleNode(int i) {
    id = i;
  }

  @Override
  public void jjtOpen() {}

  @Override
  public void jjtClose() {}

  @Override
  public void jjtSetParent(Node n) { parent = n; }

  @Override
  public Node jjtGetParent() { return parent; }

  @Override
  public void jjtAddChild(Node n, int i) {
    if (children == null) {
      children = new Node[i + 1];
    }
    else if (i >= children.length) {
      Node c[] = new Node[i + 1];
      System.arraycopy(children, 0, c, 0, children.length);
      children = c;
    }
    children[i] = n;
  }

  @Override
  public Node jjtGetChild(int i) {
    return children[i];
  }

  @Override
  public int jjtGetNumChildren() {
    return children == null ? 0 : children.length;
  }

  protected Token begin, end;

  public void setFirstToken(Token t) { begin = t; }

  public void setLastToken(Token t) { end = t; }

  public void process(PrintWriter out) {
    throw new UnsupportedOperationException();
  }

  /**
   * The following method prints token t, as well as all preceding
   * special tokens (essentially, white space and comments).
   */
  protected void print(Token t, PrintWriter out) {
    Token tt = t.specialToken;
    if (tt != null) {
      while (tt.specialToken != null) {
        tt = tt.specialToken;
      }
      while (tt != null) {
        out.print(addUnicodeEscapes(tt.getImage()));
        tt = tt.next;
      }
    }
    out.print(addUnicodeEscapes(t.getImage()));
  }

  private String addUnicodeEscapes(String str) {
    String retval = "";
    char ch;
    for (int i = 0; i < str.length(); i++) {
      ch = str.charAt(i);
      if ((ch < 0x20 || ch > 0x7e) && ch != '\t' && ch != '\n' && ch != '\r' && ch != '\f') {
        String s = "0000" + Integer.toString(ch, 16);
        retval += "\\u" + s.substring(s.length() - 4, s.length());
      }
      else {
        retval += ch;
      }
    }
    return retval;
  }

  public String toString() {
    return ToyTreeConstants.jjtNodeName[id];
  }

  public String toString(String prefix) {
    return prefix + toString();
  }

  public void dump(String prefix) {
    System.out.println(toString(prefix));
    if (children != null) {
      for (Node child : children) {
        SimpleNode n = (SimpleNode) child;
        if (n != null) {
          n.dump(prefix + " ");
        }
      }
    }
  }
}

