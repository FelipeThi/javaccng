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

package org.javacc.examples.vtransformer;

import java.io.IOException;

public class SimpleNode implements Node {
  protected Node parent;
  protected Node[] children;
  protected int id;
  protected JavaParser parser;
  protected Token first, last;

  public SimpleNode(int i) {
    id = i;
  }

  public SimpleNode(JavaParser p, int i) {
    this(i);
    parser = p;
  }

  public static Node jjtCreate(JavaParser p, int id) {
    return new SimpleNode(p, id);
  }

  public void jjtOpen() {
    try {
      first = parser.getToken(1);
    }
    catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public void jjtClose() {
    try {
      last = parser.getToken(0);
    }
    catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public Token getFirstToken() { return first; }

  public Token getLastToken() { return last; }

  public void jjtSetParent(Node n) { parent = n; }

  public Node jjtGetParent() { return parent; }

  public void jjtSetChild(Node n, int i) {
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

  public Node jjtGetChild(int i) {
    return children[i];
  }

  public int jjtGetChildCount() {
    return children == null ? 0 : children.length;
  }

  public Object jjtAccept(JavaVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

  public Object acceptChildren(JavaVisitor visitor, Object data) {
    if (children != null) {
      for (Node child : children) {
        child.jjtAccept(visitor, data);
      }
    }
    return data;
  }

  public String toString() {
    return JavaTreeConstants.jjtNodeName[id];
  }
}
