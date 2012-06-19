/* Copyright (c) 2007, Sun Microsystems, Inc.
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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public final class ExpansionTest {
  private Token t;
  private Expansion e;

  @Before
  public void setUp() {
    t = new Token(0, 0, 0, null);
    t.setLineColumn(3, 2);
    e = new Expansion() {
      //
    };
    e.setColumn(5);
    e.setLine(6);
  }

  @Test
  public void testZeroOrOneConstructor() {
    ZeroOrOne zoo = new ZeroOrOne(t, e);
    assertEquals(t.getColumn(), zoo.getColumn());
    assertEquals(t.getLine(), zoo.getLine());
    assertEquals(e, zoo.expansion);
    assertSame(e.parent, zoo);
  }

  @Test
  public void testZeroOrMoreConstructor() {
    ZeroOrMore zom = new ZeroOrMore(t, e);
    assertEquals(t.getColumn(), zom.getColumn());
    assertEquals(t.getLine(), zom.getLine());
    assertEquals(e, zom.expansion);
    assertEquals(e.parent, zom);
  }

  @Test
  public void testRZeroOrMoreConstructor() {
    RegularExpression r = new RChoice();
    RZeroOrMore rzom = new RZeroOrMore(t, r);
    assertEquals(t.getColumn(), rzom.getColumn());
    assertEquals(t.getLine(), rzom.getLine());
    assertEquals(r, rzom.regExp);
  }

  @Test
  public void testROneOrMoreConstructor() {
    RegularExpression r = new RChoice();
    ROneOrMore room = new ROneOrMore(t, r);
    assertEquals(t.getColumn(), room.getColumn());
    assertEquals(t.getLine(), room.getLine());
    assertEquals(r, room.regExp);
  }

  @Test
  public void testOneOrMoreConstructor() {
    Expansion rce = new RChoice();
    OneOrMore oom = new OneOrMore(t, rce);
    assertEquals(t.getColumn(), oom.getColumn());
    assertEquals(t.getLine(), oom.getLine());
    assertEquals(rce, oom.expansion);
    assertEquals(rce.parent, oom);
  }

  @Test
  public void testRStringLiteralConstructor() {
    RStringLiteral r = new RStringLiteral(t, "hey");
    assertEquals(t.getColumn(), r.getColumn());
    assertEquals(t.getLine(), r.getLine());
    assertEquals("hey", r.image);
  }

  @Test
  public void testChoiceConstructor() {
    Choice c = new Choice(t);
    assertEquals(t.getColumn(), c.getColumn());
    assertEquals(t.getLine(), c.getLine());
    c = new Choice(e);
    assertEquals(e.getColumn(), c.getColumn());
    assertEquals(e.getLine(), c.getLine());
    assertEquals(e, c.getChoices().get(0));
  }

  @Test
  public void testRJustNameConstructor() {
    RJustName r = new RJustName(t, "hey");
    assertEquals(t.getColumn(), r.getColumn());
    assertEquals(t.getLine(), r.getLine());
    assertEquals("hey", r.label);
  }

  @Test
  public void testSequenceConstructor() {
    Lookahead la = new Lookahead();
    Sequence s = new Sequence(t, la);
    assertEquals(t.getColumn(), s.getColumn());
    assertEquals(t.getLine(), s.getLine());
    assertSame(la, s.units.get(0));
  }
}
