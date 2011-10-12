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

import org.javacc.Version;
import org.javacc.utils.io.IndentingPrintWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * This package contains data created as a result of parsing and semanticizing
 * a JavaCC input file.  This data is what is used by the back-ends of JavaCC as
 * well as any other back-end of JavaCC related tools such as JJTree.
 */
public final class JavaCCGlobals {
  /** String that identifies the JavaCC generated files. */
  protected static final String toolName = "JavaCC";
  /** The name of the grammar file being processed. */
  public static String fileName;
  /**
   * The name of the original file (before processing by JJTree).
   * Currently this is the same as fileName.
   */
  public static String origFileName;
  /** Set to true if this file has been processed by JJTree. */
  public static boolean jjtreeGenerated;
  /**
   * The list of tools that have participated in generating the
   * input grammar file.
   */
  public static List toolNames;

  /**
   * This prints the banner line when the various tools are invoked.  This
   * takes as argument the tool's full name and its version.
   */
  public static void bannerLine(String fullName, String ver) {
    System.out.print("Java Compiler Compiler Version " + Version.versionNumber + " (" + fullName);
    if (!ver.equals("")) {
      System.out.print(" Version " + ver);
    }
    System.out.println(")");
  }

  /** The name of the parser class (what appears in PARSER_BEGIN and PARSER_END). */
  public static String cuName;
  /**
   * This is a list of tokens that appear after "PARSER_BEGIN(name)" all the
   * way until (but not including) the opening brace "{" of the class "name".
   */
  public static List cuToInsertionPoint1 = new ArrayList();
  /**
   * This is the list of all tokens that appear after the tokens in
   * "cu_to_insertion_point_1" and until (but not including) the closing brace "}"
   * of the class "name".
   */
  public static List cuToInsertionPoint2 = new ArrayList();
  /**
   * This is the list of all tokens that appear after the tokens in
   * "cu_to_insertion_point_2" and until "PARSER_END(name)".
   */
  public static List cuFromInsertionPoint2 = new ArrayList();
  /**
   * A list of all grammar productions - normal and JAVACODE - in the order
   * they appear in the input file.  Each entry here will be a subclass of
   * "NormalProduction".
   */
  public static List bnfProductions = new ArrayList();
  /**
   * A symbol table of all grammar productions - normal and JAVACODE.  The
   * symbol table is indexed by the name of the left hand side non-terminal.
   * Its contents are of type "NormalProduction".
   */
  public static Map productionTable = new HashMap();
  /**
   * A mapping of lexical state strings to their integer internal representation.
   * Integers are stored as java.lang.Integer's.
   */
  public static Map lexStateS2I = new HashMap();
  /**
   * A mapping of the internal integer representations of lexical states to
   * their strings.  Integers are stored as java.lang.Integer's.
   */
  public static Map lexStateI2S = new HashMap();
  /** The declarations to be inserted into the TokenManager class. */
  public static List tokenManagerDeclarations;
  /**
   * The list of all TokenProductions from the input file.  This list includes
   * implicit TokenProductions that are created for uses of regular expressions
   * within BNF productions.
   */
  public static List regExpList = new ArrayList();
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
  public static Map namedTokensTable = new HashMap();
  /**
   * Contains the same entries as "named_tokens_table", but this is an ordered
   * list which is ordered by the order of appearance in the input file.
   */
  public static List orderedNamedTokens = new ArrayList();
  /**
   * A mapping of ordinal values (represented as objects of type "Integer") to
   * the corresponding labels (of type "String").  An entry exists for an ordinal
   * value only if there is a labeled token corresponding to this entry.
   * If there are multiple labels representing the same ordinal value, then
   * only one label is stored.
   */
  public static Map namesOfTokens = new HashMap();
  /**
   * A mapping of ordinal values (represented as objects of type "Integer") to
   * the corresponding RegularExpression's.
   */
  public static Map regExpsOfTokens = new HashMap();
  /**
   * This is a three-level symbol table that contains all simple tokens (those
   * that are defined using a single string (with or without a label).  The index
   * to the first level table is a lexical state which maps to a second level
   * hashtable.  The index to the second level hashtable is the string of the
   * simple token converted to upper case, and this maps to a third level hashtable.
   * This third level hashtable contains the actual string of the simple token
   * and maps it to its RegularExpression.
   */
  public static Map simpleTokensTable = new HashMap();
  /**
   * maskindex, jj2index, maskVals are variables that are shared between
   * ParseEngine and ParseGen.
   */
  protected static int maskIndex = 0;
  protected static int jj2index = 0;
  public static boolean lookaheadNeeded;
  protected static List maskVals = new ArrayList();
  static Action eofAction;
  static String eofNextState;

  // Some general purpose utilities follow.

  /**
   * Returns the identifying string for the file name, given a toolname
   * used to generate it.
   */
  public static String getIdString(String toolName, String fileName) {
    List toolNames = new ArrayList();
    toolNames.add(toolName);
    return getIdString(toolNames, fileName);
  }

  /**
   * Returns the identifying string for the file name, given a set of tool
   * names that are used to generate it.
   */
  public static String getIdString(List toolNames, String fileName) {
    int i;
    String toolNamePrefix = "Generated By:";

    for (i = 0; i < toolNames.size() - 1; i++) { toolNamePrefix += (String) toolNames.get(i) + "&"; }
    toolNamePrefix += (String) toolNames.get(i) + ":";

    if (toolNamePrefix.length() > 200) {
      System.out.println("Tool names too long.");
      throw new Error();
    }

    return toolNamePrefix + " Do not edit this line. " + addUnicodeEscapes(fileName);
  }

  /**
   * Returns true if tool name passed is one of the tool names returned
   * by getToolNames(fileName).
   */
  public static boolean isGeneratedBy(String toolName, String fileName) {
    List v = getToolNames(fileName);

    for (int i = 0; i < v.size(); i++) { if (toolName.equals(v.get(i))) { return true; } }

    return false;
  }

  private static List makeToolNameList(String str) {
    List retVal = new ArrayList();

    int limit1 = str.indexOf('\n');
    if (limit1 == -1) { limit1 = 1000; }
    int limit2 = str.indexOf('\r');
    if (limit2 == -1) { limit2 = 1000; }
    int limit = (limit1 < limit2) ? limit1 : limit2;

    String tmp;
    if (limit == 1000) {
      tmp = str;
    }
    else {
      tmp = str.substring(0, limit);
    }

    if (tmp.indexOf(':') == -1) { return retVal; }

    tmp = tmp.substring(tmp.indexOf(':') + 1);

    if (tmp.indexOf(':') == -1) { return retVal; }

    tmp = tmp.substring(0, tmp.indexOf(':'));

    int i = 0, j = 0;

    while (j < tmp.length() && (i = tmp.indexOf('&', j)) != -1) {
      retVal.add(tmp.substring(j, i));
      j = i + 1;
    }

    if (j < tmp.length()) { retVal.add(tmp.substring(j)); }

    return retVal;
  }

  /**
   * Returns a List of names of the tools that have been used to generate
   * the given file.
   */
  public static List getToolNames(String fileName) {
    char[] buf = new char[256];
    FileReader stream = null;
    int read, total = 0;

    try {
      stream = new FileReader(fileName);

      for (; ; ) {
        if ((read = stream.read(buf, total, buf.length - total)) != -1) {
          if ((total += read) == buf.length) { break; }
        }
        else { break; }
      }

      return makeToolNameList(new String(buf, 0, total));
    }
    catch (FileNotFoundException e1) {}
    catch (IOException e2) {
      if (total > 0) { return makeToolNameList(new String(buf, 0, total)); }
    }
    finally {
      if (stream != null) {
        try { stream.close(); }
        catch (Exception e3) { }
      }
    }

    return new ArrayList();
  }

  public static void createOutputDir(File outputDir) {
    if (!outputDir.exists()) {
      JavaCCErrors.warning("Output directory \"" + outputDir + "\" does not exist. Creating the directory.");

      if (!outputDir.mkdirs()) {
        JavaCCErrors.semanticError("Cannot create the output directory : " + outputDir);
        return;
      }
    }

    if (!outputDir.isDirectory()) {
      JavaCCErrors.semanticError("\"" + outputDir + " is not a valid output directory.");
      return;
    }

    if (!outputDir.canWrite()) {
      JavaCCErrors.semanticError("Cannot write to the output output directory : \"" + outputDir + "\"");
      return;
    }
  }

  public static String add_escapes(String str) {
    String retval = "";
    char ch;
    for (int i = 0; i < str.length(); i++) {
      ch = str.charAt(i);
      if (ch == '\b') {
        retval += "\\b";
      }
      else if (ch == '\t') {
        retval += "\\t";
      }
      else if (ch == '\n') {
        retval += "\\n";
      }
      else if (ch == '\f') {
        retval += "\\f";
      }
      else if (ch == '\r') {
        retval += "\\r";
      }
      else if (ch == '\"') {
        retval += "\\\"";
      }
      else if (ch == '\'') {
        retval += "\\\'";
      }
      else if (ch == '\\') {
        retval += "\\\\";
      }
      else if (ch < 0x20 || ch > 0x7e) {
        String s = "0000" + Integer.toString(ch, 16);
        retval += "\\u" + s.substring(s.length() - 4, s.length());
      }
      else {
        retval += ch;
      }
    }
    return retval;
  }

  public static String addUnicodeEscapes(String str) {
    String retval = "";
    char ch;
    for (int i = 0; i < str.length(); i++) {
      ch = str.charAt(i);
      if (ch < 0x20 || ch > 0x7e || ch == '\\') {
        String s = "0000" + Integer.toString(ch, 16);
        retval += "\\u" + s.substring(s.length() - 4, s.length());
      }
      else {
        retval += ch;
      }
    }
    return retval;
  }

  public static int cline, ccol;

  protected static void printTokenSetup(Token t) {
    Token tt = t;
    while (tt.specialToken != null) { tt = tt.specialToken; }
    cline = tt.getBeginLine();
    ccol = tt.getBeginColumn();
  }

  protected static void printTokenOnly(Token t, IndentingPrintWriter ostr) {
    for (; cline < t.getBeginLine(); cline++) {
      ostr.println("");
      ccol = 1;
    }
    for (; ccol < t.getBeginColumn(); ccol++) {
      ostr.print(" ");
    }
    if (t.getKind() == JavaCCParserConstants.STRING_LITERAL ||
        t.getKind() == JavaCCParserConstants.CHARACTER_LITERAL) { ostr.print(addUnicodeEscapes(t.getImage())); }
    else { ostr.print(t.getImage()); }
    cline = t.getEndLine();
    ccol = t.getEndColumn() + 1;
    char last = t.getImage().charAt(t.getImage().length() - 1);
    if (last == '\n' || last == '\r') {
      cline++;
      ccol = 1;
    }
  }

  protected static void printToken(Token t, IndentingPrintWriter ostr) {
    Token tt = t.specialToken;
    if (tt != null) {
      while (tt.specialToken != null) { tt = tt.specialToken; }
      while (tt != null) {
        printTokenOnly(tt, ostr);
        tt = tt.next;
      }
    }
    printTokenOnly(t, ostr);
  }

  protected static void printTokenList(List list, IndentingPrintWriter ostr) {
    Token t = null;
    for (Iterator it = list.iterator(); it.hasNext(); ) {
      t = (Token) it.next();
      printToken(t, ostr);
    }

    if (t != null) { printTrailingComments(t, ostr); }
  }

  protected static void printLeadingComments(Token t, IndentingPrintWriter ostr) {
    if (t.specialToken == null) { return; }
    Token tt = t.specialToken;
    while (tt.specialToken != null) { tt = tt.specialToken; }
    while (tt != null) {
      printTokenOnly(tt, ostr);
      tt = tt.next;
    }
    if (ccol != 1 && cline != t.getBeginLine()) {
      ostr.println("");
      cline++;
      ccol = 1;
    }
  }

  protected static void printTrailingComments(Token t, IndentingPrintWriter ostr) {
    if (t.next == null) { return; }
    printLeadingComments(t.next);
  }

  public static String printTokenOnly(Token t) {
    String retval = "";
    for (; cline < t.getBeginLine(); cline++) {
      retval += "\n";
      ccol = 1;
    }
    for (; ccol < t.getBeginColumn(); ccol++) {
      retval += " ";
    }
    if (t.getKind() == JavaCCParserConstants.STRING_LITERAL ||
        t.getKind() == JavaCCParserConstants.CHARACTER_LITERAL) { retval += addUnicodeEscapes(t.getImage()); }
    else { retval += t.getImage(); }
    cline = t.getEndLine();
    ccol = t.getEndColumn() + 1;
    char last = t.getImage().charAt(t.getImage().length() - 1);
    if (last == '\n' || last == '\r') {
      cline++;
      ccol = 1;
    }
    return retval;
  }

  protected static String printToken(Token t) {
    String retval = "";
    Token tt = t.specialToken;
    if (tt != null) {
      while (tt.specialToken != null) { tt = tt.specialToken; }
      while (tt != null) {
        retval += printTokenOnly(tt);
        tt = tt.next;
      }
    }
    retval += printTokenOnly(t);
    return retval;
  }

  protected static String printLeadingComments(Token t) {
    String retval = "";
    if (t.specialToken == null) { return retval; }
    Token tt = t.specialToken;
    while (tt.specialToken != null) { tt = tt.specialToken; }
    while (tt != null) {
      retval += printTokenOnly(tt);
      tt = tt.next;
    }
    if (ccol != 1 && cline != t.getBeginLine()) {
      retval += "\n";
      cline++;
      ccol = 1;
    }
    return retval;
  }

  protected static String printTrailingComments(Token t) {
    if (t.next == null) { return ""; }
    return printLeadingComments(t.next);
  }

  @Deprecated
  public static void reInit() {
    fileName = null;
    origFileName = null;
    jjtreeGenerated = false;
    toolNames = null;
    cuName = null;
    cuToInsertionPoint1 = new ArrayList();
    cuToInsertionPoint2 = new ArrayList();
    cuFromInsertionPoint2 = new ArrayList();
    bnfProductions = new ArrayList();
    productionTable = new HashMap();
    lexStateS2I = new HashMap();
    lexStateI2S = new HashMap();
    tokenManagerDeclarations = null;
    regExpList = new ArrayList();
    tokenCount = 0;
    namedTokensTable = new HashMap();
    orderedNamedTokens = new ArrayList();
    namesOfTokens = new HashMap();
    regExpsOfTokens = new HashMap();
    simpleTokensTable = new HashMap();
    maskIndex = 0;
    jj2index = 0;
    maskVals = new ArrayList();
    cline = 0;
    ccol = 0;
    eofAction = null;
    eofNextState = null;
  }
}
