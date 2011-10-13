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

/**
 * A set of routines that walk down the Expansion tree in
 * various ways.
 */
public final class ExpansionTreeWalker {
  private ExpansionTreeWalker() {}

  /**
   * Visits the nodes of the tree rooted at "node" in pre-order.
   * i.e., it executes op.action first and then visits the
   * children.
   */
  static void preOrderWalk(Expansion node, TreeWalkerOp op) {
    op.action(node);
    if (op.goDeeper(node)) {
      if (node instanceof Choice) {
        for (Expansion expansion : ((Choice) node).getChoices()) {
          preOrderWalk(expansion, op);
        }
      }
      else if (node instanceof Sequence) {
        for (Expansion unit : ((Sequence) node).units) {
          preOrderWalk(unit, op);
        }
      }
      else if (node instanceof OneOrMore) {
        preOrderWalk(((OneOrMore) node).expansion, op);
      }
      else if (node instanceof ZeroOrMore) {
        preOrderWalk(((ZeroOrMore) node).expansion, op);
      }
      else if (node instanceof ZeroOrOne) {
        preOrderWalk(((ZeroOrOne) node).expansion, op);
      }
      else if (node instanceof Lookahead) {
        Expansion nested = ((Lookahead) node).getLaExpansion();
        if (!(nested instanceof Sequence && ((Sequence) nested).units.get(0) == node)) {
          preOrderWalk(nested, op);
        }
      }
      else if (node instanceof TryBlock) {
        preOrderWalk(((TryBlock) node).expansion, op);
      }
      else if (node instanceof RChoice) {
        for (RegularExpression regularExpression : ((RChoice) node).getChoices()) {
          preOrderWalk(regularExpression, op);
        }
      }
      else if (node instanceof RSequence) {
        for (Object unit : ((RSequence) node).units) {
          preOrderWalk((Expansion) unit, op);
        }
      }
      else if (node instanceof ROneOrMore) {
        preOrderWalk(((ROneOrMore) node).regExp, op);
      }
      else if (node instanceof RZeroOrMore) {
        preOrderWalk(((RZeroOrMore) node).regExp, op);
      }
      else if (node instanceof RZeroOrOne) {
        preOrderWalk(((RZeroOrOne) node).regExp, op);
      }
      else if (node instanceof RRepetitionRange) {
        preOrderWalk(((RRepetitionRange) node).regExp, op);
      }
    }
  }

  /**
   * Visits the nodes of the tree rooted at "node" in post-order.
   * i.e., it visits the children first and then executes
   * op.action.
   */
  static void postOrderWalk(Expansion node, TreeWalkerOp op) {
    if (op.goDeeper(node)) {
      if (node instanceof Choice) {
        for (Expansion expansion : ((Choice) node).getChoices()) {
          postOrderWalk(expansion, op);
        }
      }
      else if (node instanceof Sequence) {
        for (Expansion unit : ((Sequence) node).units) {
          postOrderWalk(unit, op);
        }
      }
      else if (node instanceof OneOrMore) {
        postOrderWalk(((OneOrMore) node).expansion, op);
      }
      else if (node instanceof ZeroOrMore) {
        postOrderWalk(((ZeroOrMore) node).expansion, op);
      }
      else if (node instanceof ZeroOrOne) {
        postOrderWalk(((ZeroOrOne) node).expansion, op);
      }
      else if (node instanceof Lookahead) {
        Expansion nested_e = ((Lookahead) node).getLaExpansion();
        if (!(nested_e instanceof Sequence && ((Sequence) nested_e).units.get(0) == node)) {
          postOrderWalk(nested_e, op);
        }
      }
      else if (node instanceof TryBlock) {
        postOrderWalk(((TryBlock) node).expansion, op);
      }
      else if (node instanceof RChoice) {
        for (RegularExpression regularExpression : ((RChoice) node).getChoices()) {
          postOrderWalk(regularExpression, op);
        }
      }
      else if (node instanceof RSequence) {
        for (Object unit : ((RSequence) node).units) {
          postOrderWalk((Expansion) unit, op);
        }
      }
      else if (node instanceof ROneOrMore) {
        postOrderWalk(((ROneOrMore) node).regExp, op);
      }
      else if (node instanceof RZeroOrMore) {
        postOrderWalk(((RZeroOrMore) node).regExp, op);
      }
      else if (node instanceof RZeroOrOne) {
        postOrderWalk(((RZeroOrOne) node).regExp, op);
      }
      else if (node instanceof RRepetitionRange) {
        postOrderWalk(((RRepetitionRange) node).regExp, op);
      }
    }
    op.action(node);
  }
}
