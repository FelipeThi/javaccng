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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains data created as a result of parsing and semanticizing a JavaCC input file.
 * This data is what is used by the back-ends of JavaCC as well as any other back-end
 * of JavaCC related tools such as JJTree.
 */
public final class JavaCCState {
  public static final String DEFAULT = "DEFAULT";
  /** The name of the grammar file being processed. */
  public String fileName;
  /** The name of the parser class (what appears in PARSER_BEGIN and PARSER_END). */
  public String cuName;
  /**
   * The list of tokens that appear after "PARSER_BEGIN(name)" all the
   * way until (but not including) the opening brace "{" of the class "name".
   */
  public List<Token> cuToInsertionPoint1
      = new ArrayList<Token>();
  /**
   * The list of all tokens that appear after the tokens in
   * "cu_to_insertion_point_1" and until (but not including) the closing brace "}"
   * of the class "name".
   */
  public List<Token> cuToInsertionPoint2
      = new ArrayList<Token>();
  /**
   * The list of all tokens that appear after the tokens in
   * "cu_to_insertion_point_2" and until "PARSER_END".
   */
  public List<Token> cuFromInsertionPoint2
      = new ArrayList<Token>();
  /**
   * A list of all grammar productions - normal and JAVACODE - in the order
   * they appear in the input file.  Each entry here will be a subclass of
   * "NormalProduction".
   */
  public List<NormalProduction> bnfProductions
      = new ArrayList<NormalProduction>();
  /**
   * A symbol table of all grammar productions - normal and JAVACODE.  The
   * symbol table is indexed by the name of the left hand side non-terminal.
   * Its contents are of type "NormalProduction".
   */
  public Map<String, NormalProduction> productionTable
      = new HashMap<String, NormalProduction>();
  /**
   * A mapping of lexical state strings to their integer internal representation.
   * Integers are stored as java.lang.Integer's.
   */
  public Map<String, Integer> lexStateS2I
      = new HashMap<String, Integer>();
  /**
   * A mapping of the internal integer representations of lexical states to
   * their strings.  Integers are stored as java.lang.Integer's.
   */
  public Map<Integer, String> lexStateI2S
      = new HashMap<Integer, String>();
  /** The declarations to be inserted into the Scanner class. */
  public List<Token> scannerDeclarations;
  /**
   * The list of all TokenProductions from the input file.  This list includes
   * implicit TokenProductions that are created for uses of regular expressions
   * within BNF productions.
   */
  public List<TokenProduction> regExpList
      = new ArrayList<TokenProduction>();
  /**
   * The total number of distinct tokens.  This is therefore one more than the
   * largest assigned token ordinal.
   */
  public int tokenCount;
  /**
   * This is a symbol table that contains all named tokens (those that are
   * defined with a label).  The index to the table is the image of the label
   * and the contents of the table are of type "RegularExpression".
   */
  public Map<String, RegularExpression> namedTokensTable
      = new HashMap<String, RegularExpression>();
  /**
   * Contains the same entries as "named_tokens_table", but this is an ordered
   * list which is ordered by the order of appearance in the input file.
   */
  public List<RegularExpression> orderedNamedTokens
      = new ArrayList<RegularExpression>();
  /**
   * A mapping of ordinal values (represented as objects of type "Integer") to
   * the corresponding labels (of type "String").  An entry exists for an ordinal
   * value only if there is a labeled token corresponding to this entry.
   * If there are multiple labels representing the same ordinal value, then
   * only one label is stored.
   */
  public Map<Integer, String> namesOfTokens
      = new HashMap<Integer, String>();
  /**
   * A mapping of ordinal values (represented as objects of type "Integer") to
   * the corresponding RegularExpression's.
   */
  public Map<Integer, RegularExpression> regExpsOfTokens
      = new HashMap<Integer, RegularExpression>();
  /**
   * This is a three-level symbol table that contains all simple tokens (those
   * that are defined using a single string (with or without a label).  The index
   * to the first level table is a lexical state which maps to a second level
   * hashtable.  The index to the second level hashtable is the string of the
   * simple token converted to upper case, and this maps to a third level hashtable.
   * This third level hashtable contains the actual string of the simple token
   * and maps it to its RegularExpression.
   */
  public Map<String, Map<String, RegularExpression>> simpleTokensTable
      = new HashMap<String, Map<String, RegularExpression>>();
  Action eofAction;
  String eofNextState;

  public JavaCCState() {
    lexStateS2I.put(DEFAULT, 0);
    lexStateI2S.put(0, DEFAULT);
    simpleTokensTable.put(DEFAULT,
        new HashMap<String, RegularExpression>());
  }

  public String constantsClass() {
    String name = cuName;
    if (name.endsWith("Parser")) {
      name = name.substring(0, name.length() - "Parser".length());
    }
    return name + "Constants";
  }

  public String scannerClass() {
    String name = cuName;
    if (name.endsWith("Parser")) {
      name = name.substring(0, name.length() - "Parser".length());
    }
    return name + "Scanner";
  }

  public String parserClass() {
    return cuName;
  }
}
