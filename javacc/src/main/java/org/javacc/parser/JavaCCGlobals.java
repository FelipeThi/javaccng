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
public final class JavaCCGlobals {
  /** The name of the grammar file being processed. */
  public static String fileName;
  /**
   * The name of the original file (before processing by JJTree).
   * Currently this is the same as fileName.
   */
  public static String origFileName;
  /** The name of the parser class (what appears in PARSER_BEGIN and PARSER_END). */
  public static String cuName;
  /**
   * This is a list of tokens that appear after "PARSER_BEGIN(name)" all the
   * way until (but not including) the opening brace "{" of the class "name".
   */
  public static List<Token> cuToInsertionPoint1
      = new ArrayList<Token>();
  /**
   * This is the list of all tokens that appear after the tokens in
   * "cu_to_insertion_point_1" and until (but not including) the closing brace "}"
   * of the class "name".
   */
  public static List<Token> cuToInsertionPoint2
      = new ArrayList<Token>();
  /**
   * This is the list of all tokens that appear after the tokens in
   * "cu_to_insertion_point_2" and until "PARSER_END(name)".
   */
  public static List<Token> cuFromInsertionPoint2
      = new ArrayList<Token>();
  /**
   * A list of all grammar productions - normal and JAVACODE - in the order
   * they appear in the input file.  Each entry here will be a subclass of
   * "NormalProduction".
   */
  public static List<NormalProduction> bnfProductions
      = new ArrayList<NormalProduction>();
  /**
   * A symbol table of all grammar productions - normal and JAVACODE.  The
   * symbol table is indexed by the name of the left hand side non-terminal.
   * Its contents are of type "NormalProduction".
   */
  public static Map<String, NormalProduction> productionTable
      = new HashMap<String, NormalProduction>();
  /**
   * A mapping of lexical state strings to their integer internal representation.
   * Integers are stored as java.lang.Integer's.
   */
  public static Map<String, Integer> lexStateS2I
      = new HashMap<String, Integer>();
  /**
   * A mapping of the internal integer representations of lexical states to
   * their strings.  Integers are stored as java.lang.Integer's.
   */
  public static Map<Integer, String> lexStateI2S
      = new HashMap<Integer, String>();
  /** The declarations to be inserted into the Scanner class. */
  public static List<Token> scannerDeclarations;
  /**
   * The list of all TokenProductions from the input file.  This list includes
   * implicit TokenProductions that are created for uses of regular expressions
   * within BNF productions.
   */
  public static List<TokenProduction> regExpList
      = new ArrayList<TokenProduction>();
  /**
   * The total number of distinct tokens.  This is therefore one more than the
   * largest assigned token ordinal.
   */
  public static int tokenCount;
  /**
   * This is a symbol table that contains all named tokens (those that are
   * defined with a label).  The index to the table is the image of the label
   * and the contents of the table are of type "RegularExpression".
   */
  public static Map<String, RegularExpression> namedTokensTable
      = new HashMap<String, RegularExpression>();
  /**
   * Contains the same entries as "named_tokens_table", but this is an ordered
   * list which is ordered by the order of appearance in the input file.
   */
  public static List<RegularExpression> orderedNamedTokens
      = new ArrayList<RegularExpression>();
  /**
   * A mapping of ordinal values (represented as objects of type "Integer") to
   * the corresponding labels (of type "String").  An entry exists for an ordinal
   * value only if there is a labeled token corresponding to this entry.
   * If there are multiple labels representing the same ordinal value, then
   * only one label is stored.
   */
  public static Map<Integer, String> namesOfTokens
      = new HashMap<Integer, String>();
  /**
   * A mapping of ordinal values (represented as objects of type "Integer") to
   * the corresponding RegularExpression's.
   */
  public static Map<Integer, RegularExpression> regExpsOfTokens
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
  public static Map<String, Map<String, RegularExpression>> simpleTokensTable
      = new HashMap<String, Map<String, RegularExpression>>();
  /**
   * maskIndex, jj2index, maskVals are variables that are shared between
   * ParseEngine and ParseGen.
   */
  protected static int maskIndex = 0;
  protected static int jj2index = 0;
  public static boolean lookaheadNeeded;
  protected static List maskVals = new ArrayList();
  static Action eofAction;
  static String eofNextState;

  @Deprecated
  public static void reInit() {
    fileName = null;
    origFileName = null;
    cuName = null;
    cuToInsertionPoint1 = new ArrayList<Token>();
    cuToInsertionPoint2 = new ArrayList<Token>();
    cuFromInsertionPoint2 = new ArrayList<Token>();
    bnfProductions = new ArrayList<NormalProduction>();
    productionTable = new HashMap<String, NormalProduction>();
    lexStateS2I = new HashMap<String, Integer>();
    lexStateI2S = new HashMap<Integer, String>();
    scannerDeclarations = null;
    regExpList = new ArrayList<TokenProduction>();
    tokenCount = 0;
    namedTokensTable = new HashMap<String, RegularExpression>();
    orderedNamedTokens = new ArrayList<RegularExpression>();
    namesOfTokens = new HashMap<Integer, String>();
    regExpsOfTokens = new HashMap<Integer, RegularExpression>();
    simpleTokensTable = new HashMap<String, Map<String, RegularExpression>>();
    maskIndex = 0;
    jj2index = 0;
    maskVals = new ArrayList();
    TokenPrinter.cLine = 0;
    TokenPrinter.cCol = 0;
    eofAction = null;
    eofNextState = null;
  }
}
