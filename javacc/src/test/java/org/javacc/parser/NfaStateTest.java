package org.javacc.parser;

import org.javacc.JavaCCTestCase;
import org.javacc.utils.io.IndentingPrintWriter;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.Assert.*;

/**
 * A sea anchor, to ensure that code is not inadvertently broken.
 *
 * @author timp
 * @since 16 Mar 2007
 */
public class NfaStateTest extends JavaCCTestCase {
  private final String path = getJJInputDirectory() + "JavaCC.jj";
  private final JavaCCState state = new JavaCCState();
  private final ScannerGen scannerGen = new ScannerGen(state);

  @Before
  public void setUp() throws Exception {
    Options.init();
    Main.reInitAll();
  }

  protected void setupState() throws Exception {
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(
            new FileInputStream(path), Options.getGrammarEncoding()));
    JavaCCParser parser = new JavaCCParser(
        new JavaCCScanner(
            new JavaCharStream(reader)));
    parser.setState(state);
    parser.start();
    state.fileName = path;
    Semanticize semanticize = new Semanticize(state);
    semanticize.start();
    scannerGen.start();
  }

  /** Test method for {@link NfaState#hasTransitions()}. */
  public void testHasTransitions() {
  }

  /** Test method for {@link NfaStates#computeClosures()}. */
  public void testComputeClosures() {
  }

  /** Test method for {@link NfaStates#canStartNfaUsingAscii(ScannerGen, char)}. */
  public void testCanStartNfaUsingAscii() {
  }

  /** Test method for {@link NfaState#getFirstValidPos(String, int, int)}. */
  public void testGetFirstValidPos() {
  }

  /** Test method for {@link NfaStates#moveFromSet(char, List, List)}. */
  public void testMoveFrom() {
  }

  /** Test method for {@link NfaStates#moveFromSet(char, List, List)}. */
  public void testMoveFromSet() {
  }

  /**
   * Test method for {@link NfaState#
   * moveFromSetForRegEx(char, org.javacc.parser.NfaState[],
   * org.javacc.parser.NfaState[], int)}.
   */
  public void testMoveFromSetForRegEx() {
  }

  /**
   * Test method for {@link NfaState#
   * GenerateInitMoves(IndentingPrintWriter)}.
   */
  public void testGenerateInitMoves() {
  }

  /**
   * Test method for {@link NfaState#
   * DumpStateSets(IndentingPrintWriter)}.
   */
  @Test
  public void testDumpStateSets() {
    StringWriter output = new StringWriter();
    IndentingPrintWriter contentWriter = new IndentingPrintWriter(output);
    ScannerGen scannerGen = new ScannerGen(state);
    scannerGen.nfaStates.dumpStateSets(contentWriter);
    assertEquals("private static final int[] jjNextStates = {};\n",
        output.toString().replaceAll("\r", ""));
  }

  /**
   * Test method for {@link NfaState#
   * DumpStateSets(IndentingPrintWriter)}.
   */
  @Test
  public void testDumpStateSetsInitialised() throws Exception {
    StringWriter output = new StringWriter();
    IndentingPrintWriter contentWriter = new IndentingPrintWriter(output);
    setupState();
    scannerGen.nfaStates.dumpStateSets(contentWriter);
    assertEquals("private static final int[] jjNextStates = {34, 35, 12, 38, 39, 42, 43, 23, 24,\n" +
        "  26, 14, 16, 49, 51, 6, 52, 59, 8, 9, 12, 23, 24, 28, 26, 34, 35, 12, 44, 45,\n" +
        "  12, 53, 54, 60, 61, 62, 10, 11, 17, 18, 20, 25, 27, 29, 36, 37, 40, 41, 46,\n" +
        "  47, 55, 56, 57, 58, 63, 64};\n",
        output.toString().replaceAll("\r", ""));
  }

  /**
   * Test method for {@link NfaState#
   * dumpCharAndRangeMoves(IndentingPrintWriter)}.
   */
  @Test
  public void testDumpCharAndRangeMoves() throws Exception {
    StringWriter output = new StringWriter();
    IndentingPrintWriter contentWriter = new IndentingPrintWriter(output);
    scannerGen.nfaStates.dumpCharAndRangeMoves(scannerGen, contentWriter);
    assertEquals("int i2 = (jjChar & 0xff) >> 6;\n" +
        "long l2 = 1L << (jjChar & 63);\n" +
        "do {\n" +
        "  switch(jjStateSet[--i]) {\n" +
        "    default: break;\n" +
        "  }\n" +
        "} while(i != startsAt);\n",
        output.toString().replaceAll("\r", ""));
  }

  /**
   * Test method for {@link NfaState#
   * dumpCharAndRangeMoves(IndentingPrintWriter)}.
   */
  @Test
  public void testDumpCharAndRangeMovesInitialised() throws Exception {
    StringWriter output = new StringWriter();
    IndentingPrintWriter contentWriter = new IndentingPrintWriter(output);
    setupState();
    scannerGen.nfaStates.dumpCharAndRangeMoves(scannerGen, contentWriter);
    assertEquals("int hiByte = (int)(jjChar >> 8);\n" +
        "int i1 = hiByte >> 6;\n" +
        "long l1 = 1L << (hiByte & 63);\n" +
        "int i2 = (jjChar & 0xff) >> 6;\n" +
        "long l2 = 1L << (jjChar & 63);\n" +
        "do {\n" +
        "  switch(jjStateSet[--i]) {\n" +
        "    default: break;\n" +
        "  }\n" +
        "} while(i != startsAt);\n",
        output.toString().replaceAll("\r", ""));
  }

  /**
   * Test method for {@link NfaState#
   * dumpNonAsciiMoveMethods(IndentingPrintWriter)}.
   */
  @Test
  public void testDumpNonAsciiMoveMethods() {
    StringWriter output = new StringWriter();
    IndentingPrintWriter contentWriter = new IndentingPrintWriter(output);
    scannerGen.nfaStates.dumpNonAsciiMoveMethods(contentWriter);
    assertEquals("", output.toString());
  }

  /**
   * Test method for {@link NfaState#
   * dumpNonAsciiMoveMethods(IndentingPrintWriter)}.
   */
  @Test
  public void testDumpNonAsciiMoveMethodsInitialised() throws Exception {
    StringWriter output = new StringWriter();
    IndentingPrintWriter contentWriter = new IndentingPrintWriter(output);
    setupState();
    scannerGen.nfaStates.dumpNonAsciiMoveMethods(contentWriter);
    assertEquals("private static boolean jjCanMove_0(int hiByte, int i1, int i2, long l1, long l2) {\n" +
        "  switch(hiByte) {\n" +
        "    case 0:\n" +
        "      return ((jjbitVec2[i2] & l2) != 0L);\n" +
        "    default:\n" +
        "      if ((jjbitVec0[i1] & l1) != 0L) { return true; }\n" +
        "      return false;\n" +
        "  }\n" +
        "}\n" +
        "private static boolean jjCanMove_1(int hiByte, int i1, int i2, long l1, long l2) {\n" +
        "  switch(hiByte) {\n" +
        "    case 0:\n" +
        "      return ((jjbitVec4[i2] & l2) != 0L);\n" +
        "    case 2:\n" +
        "      return ((jjbitVec5[i2] & l2) != 0L);\n" +
        "    case 3:\n" +
        "      return ((jjbitVec6[i2] & l2) != 0L);\n" +
        "    case 4:\n" +
        "      return ((jjbitVec7[i2] & l2) != 0L);\n" +
        "    case 5:\n" +
        "      return ((jjbitVec8[i2] & l2) != 0L);\n" +
        "    case 6:\n" +
        "      return ((jjbitVec9[i2] & l2) != 0L);\n" +
        "    case 7:\n" +
        "      return ((jjbitVec10[i2] & l2) != 0L);\n" +
        "    case 9:\n" +
        "      return ((jjbitVec11[i2] & l2) != 0L);\n" +
        "    case 10:\n" +
        "      return ((jjbitVec12[i2] & l2) != 0L);\n" +
        "    case 11:\n" +
        "      return ((jjbitVec13[i2] & l2) != 0L);\n" +
        "    case 12:\n" +
        "      return ((jjbitVec14[i2] & l2) != 0L);\n" +
        "    case 13:\n" +
        "      return ((jjbitVec15[i2] & l2) != 0L);\n" +
        "    case 14:\n" +
        "      return ((jjbitVec16[i2] & l2) != 0L);\n" +
        "    case 15:\n" +
        "      return ((jjbitVec17[i2] & l2) != 0L);\n" +
        "    case 16:\n" +
        "      return ((jjbitVec18[i2] & l2) != 0L);\n" +
        "    case 17:\n" +
        "      return ((jjbitVec19[i2] & l2) != 0L);\n" +
        "    case 18:\n" +
        "      return ((jjbitVec20[i2] & l2) != 0L);\n" +
        "    case 19:\n" +
        "      return ((jjbitVec21[i2] & l2) != 0L);\n" +
        "    case 20:\n" +
        "      return ((jjbitVec0[i2] & l2) != 0L);\n" +
        "    case 22:\n" +
        "      return ((jjbitVec22[i2] & l2) != 0L);\n" +
        "    case 23:\n" +
        "      return ((jjbitVec23[i2] & l2) != 0L);\n" +
        "    case 24:\n" +
        "      return ((jjbitVec24[i2] & l2) != 0L);\n" +
        "    case 30:\n" +
        "      return ((jjbitVec25[i2] & l2) != 0L);\n" +
        "    case 31:\n" +
        "      return ((jjbitVec26[i2] & l2) != 0L);\n" +
        "    case 32:\n" +
        "      return ((jjbitVec27[i2] & l2) != 0L);\n" +
        "    case 33:\n" +
        "      return ((jjbitVec28[i2] & l2) != 0L);\n" +
        "    case 48:\n" +
        "      return ((jjbitVec29[i2] & l2) != 0L);\n" +
        "    case 49:\n" +
        "      return ((jjbitVec30[i2] & l2) != 0L);\n" +
        "    case 77:\n" +
        "      return ((jjbitVec31[i2] & l2) != 0L);\n" +
        "    case 159:\n" +
        "      return ((jjbitVec32[i2] & l2) != 0L);\n" +
        "    case 164:\n" +
        "      return ((jjbitVec33[i2] & l2) != 0L);\n" +
        "    case 215:\n" +
        "      return ((jjbitVec34[i2] & l2) != 0L);\n" +
        "    case 250:\n" +
        "      return ((jjbitVec35[i2] & l2) != 0L);\n" +
        "    case 251:\n" +
        "      return ((jjbitVec36[i2] & l2) != 0L);\n" +
        "    case 253:\n" +
        "      return ((jjbitVec37[i2] & l2) != 0L);\n" +
        "    case 254:\n" +
        "      return ((jjbitVec38[i2] & l2) != 0L);\n" +
        "    case 255:\n" +
        "      return ((jjbitVec39[i2] & l2) != 0L);\n" +
        "    default:\n" +
        "      if ((jjbitVec3[i1] & l1) != 0L) { return true; }\n" +
        "      return false;\n" +
        "  }\n" +
        "}\n" +
        "private static boolean jjCanMove_2(int hiByte, int i1, int i2, long l1, long l2) {\n" +
        "  switch(hiByte) {\n" +
        "    case 0:\n" +
        "      return ((jjbitVec40[i2] & l2) != 0L);\n" +
        "    case 2:\n" +
        "      return ((jjbitVec5[i2] & l2) != 0L);\n" +
        "    case 3:\n" +
        "      return ((jjbitVec41[i2] & l2) != 0L);\n" +
        "    case 4:\n" +
        "      return ((jjbitVec42[i2] & l2) != 0L);\n" +
        "    case 5:\n" +
        "      return ((jjbitVec43[i2] & l2) != 0L);\n" +
        "    case 6:\n" +
        "      return ((jjbitVec44[i2] & l2) != 0L);\n" +
        "    case 7:\n" +
        "      return ((jjbitVec45[i2] & l2) != 0L);\n" +
        "    case 9:\n" +
        "      return ((jjbitVec46[i2] & l2) != 0L);\n" +
        "    case 10:\n" +
        "      return ((jjbitVec47[i2] & l2) != 0L);\n" +
        "    case 11:\n" +
        "      return ((jjbitVec48[i2] & l2) != 0L);\n" +
        "    case 12:\n" +
        "      return ((jjbitVec49[i2] & l2) != 0L);\n" +
        "    case 13:\n" +
        "      return ((jjbitVec50[i2] & l2) != 0L);\n" +
        "    case 14:\n" +
        "      return ((jjbitVec51[i2] & l2) != 0L);\n" +
        "    case 15:\n" +
        "      return ((jjbitVec52[i2] & l2) != 0L);\n" +
        "    case 16:\n" +
        "      return ((jjbitVec53[i2] & l2) != 0L);\n" +
        "    case 17:\n" +
        "      return ((jjbitVec19[i2] & l2) != 0L);\n" +
        "    case 18:\n" +
        "      return ((jjbitVec20[i2] & l2) != 0L);\n" +
        "    case 19:\n" +
        "      return ((jjbitVec54[i2] & l2) != 0L);\n" +
        "    case 20:\n" +
        "      return ((jjbitVec0[i2] & l2) != 0L);\n" +
        "    case 22:\n" +
        "      return ((jjbitVec22[i2] & l2) != 0L);\n" +
        "    case 23:\n" +
        "      return ((jjbitVec55[i2] & l2) != 0L);\n" +
        "    case 24:\n" +
        "      return ((jjbitVec56[i2] & l2) != 0L);\n" +
        "    case 30:\n" +
        "      return ((jjbitVec25[i2] & l2) != 0L);\n" +
        "    case 31:\n" +
        "      return ((jjbitVec26[i2] & l2) != 0L);\n" +
        "    case 32:\n" +
        "      return ((jjbitVec57[i2] & l2) != 0L);\n" +
        "    case 33:\n" +
        "      return ((jjbitVec28[i2] & l2) != 0L);\n" +
        "    case 48:\n" +
        "      return ((jjbitVec58[i2] & l2) != 0L);\n" +
        "    case 49:\n" +
        "      return ((jjbitVec30[i2] & l2) != 0L);\n" +
        "    case 77:\n" +
        "      return ((jjbitVec31[i2] & l2) != 0L);\n" +
        "    case 159:\n" +
        "      return ((jjbitVec32[i2] & l2) != 0L);\n" +
        "    case 164:\n" +
        "      return ((jjbitVec33[i2] & l2) != 0L);\n" +
        "    case 215:\n" +
        "      return ((jjbitVec34[i2] & l2) != 0L);\n" +
        "    case 250:\n" +
        "      return ((jjbitVec35[i2] & l2) != 0L);\n" +
        "    case 251:\n" +
        "      return ((jjbitVec59[i2] & l2) != 0L);\n" +
        "    case 253:\n" +
        "      return ((jjbitVec37[i2] & l2) != 0L);\n" +
        "    case 254:\n" +
        "      return ((jjbitVec60[i2] & l2) != 0L);\n" +
        "    case 255:\n" +
        "      return ((jjbitVec61[i2] & l2) != 0L);\n" +
        "    default:\n" +
        "      if ((jjbitVec3[i1] & l1) != 0L) { return true; }\n" +
        "      return false;\n" +
        "  }\n" +
        "}\n", output.toString().replaceAll("\r", ""));
  }

  /** Test method for {@link NfaStates#dumpMoveNfa(ScannerGen, IndentingPrintWriter)}. */
  @Test
  public void testDumpMoveNfa() throws Exception {
    StringWriter output = new StringWriter();
    IndentingPrintWriter contentWriter = new IndentingPrintWriter(output);
    try {
      scannerGen.nfaStates.dumpMoveNfa(scannerGen, contentWriter);
      fail("Should have bombed");
    }
    catch (ArrayIndexOutOfBoundsException e) {
      e = null;
    }
    assertEquals("", output.toString());
    /*
     assertEquals("static private final void jjCheckNAdd(int state)\n" +
        "{\n" +
        "   if (jjRounds[state] != jjRound)\n" +
        "   {\n" +
        "      jjStateSet[jjNewStateCount++] = state;\n" +
        "      jjRounds[state] = jjRound;\n" +
        "   }\n" +
        "}\n" +
        "static private final void jjAddStates(int start, int end)\n" +
        "{\n" +
        "   do {\n" +
        "      jjStateSet[jjNewStateCount++] = jjNextStates[start];\n" +
        "   } while (start++ != end);\n" +
        "}\n" +
        "static private final void jjCheckNAddTwoStates(int state1, int state2)\n" +
        "{\n" +
        "   jjCheckNAdd(state1);\n" +
        "   jjCheckNAdd(state2);\n" +
        "}\n" +
        "static private final void jjCheckNAddStates(int start, int end)\n" +
        "{\n" +
        "   do {\n" +
        "      jjCheckNAdd(jjnextStates[start]);\n" +
        "   } while (start++ != end);\n" +
        "}\n" +
        "static private final void jjCheckNAddStates(int start)\n" +
        "{\n" +
        "   jjCheckNAdd(jjnextStates[start]);\n" +
        "   jjCheckNAdd(jjnextStates[start + 1]);\n" +
        "}\n" +
        "", output.toString());
        */
  }

  /** Test method for {@link NfaStates#dumpMoveNfa(ScannerGen, IndentingPrintWriter)} )}. */
  @Test
  public void testDumpMoveNfaInitialised() throws Exception {
    StringWriter output = new StringWriter();
    IndentingPrintWriter contentWriter = new IndentingPrintWriter(output);
    setupState();
    scannerGen.nfaStates.dumpMoveNfa(scannerGen, contentWriter);
    assertEquals("private int jjMoveNfa_3(int startState, int pos) throws java.io.IOException {\n" +
        "  return pos;\n" +
        "}\n", output.toString().replaceAll("\r", ""));
  }

  /**
   * Test method for {@link NfaState#
   * dumpStatesForState(IndentingPrintWriter)}.
   */
  @Test
  public void testDumpStatesForState() throws Exception {
    StringWriter output = new StringWriter();
    IndentingPrintWriter contentWriter = new IndentingPrintWriter(output);
    scannerGen.nfaStates.dumpStatesForState(contentWriter);
    assertEquals("protected static final int[][][] statesForState = null;\n",
        output.toString().replaceAll("\r", ""));
  }

  /**
   * Test method for {@link NfaState#
   * dumpStatesForState(IndentingPrintWriter)}.
   */
  @Test
  public void testDumpStatesForStateInitialised() throws Exception {
    StringWriter output = new StringWriter();
    IndentingPrintWriter contentWriter = new IndentingPrintWriter(output);
    setupState();
    scannerGen.nfaStates.dumpStatesForState(contentWriter);
    assertEquals("protected static final int[][][] statesForState = {\n" +
        " {\n" +
        "   { 0 },\n" +
        "   { 1 },\n" +
        "   { 2 },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 5 },\n" +
        "   { 6 },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 8 },\n" +
        "   { 9 },\n" +
        "   { 10 },\n" +
        "   { 11 },\n" +
        "   { 12 },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 14 },\n" +
        "   { 15 },\n" +
        "   { 16 },\n" +
        "   { 17 },\n" +
        "   { 18 },\n" +
        "   { 19 },\n" +
        "   { 20 },\n" +
        "   { 21 },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 23 },\n" +
        "   { 24 },\n" +
        "   { 25 },\n" +
        "   { 26 },\n" +
        "   { 27 },\n" +
        "   { 28 },\n" +
        "   { 29 },\n" +
        "   { 30 },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 32 },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 34 },\n" +
        "   { 35 },\n" +
        "   { 36 },\n" +
        "   { 37 },\n" +
        "   { 38 },\n" +
        "   { 39 },\n" +
        "   { 40 },\n" +
        "   { 41 },\n" +
        "   { 42 },\n" +
        "   { 43 },\n" +
        "   { 44 },\n" +
        "   { 45 },\n" +
        "   { 46 },\n" +
        "   { 47 },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 49 },\n" +
        "   { 50 },\n" +
        "   { 51 },\n" +
        "   { 52 },\n" +
        "   { 53 },\n" +
        "   { 54 },\n" +
        "   { 55 },\n" +
        "   { 56 },\n" +
        "   { 57 },\n" +
        "   { 58 },\n" +
        "   { 59 },\n" +
        "   { 60 },\n" +
        "   { 61 },\n" +
        "   { 62 },\n" +
        "   { 63 },\n" +
        "   { 64 },\n" +
        " },\n" +
        " null,\n" +
        " {\n" +
        "   { 0, 2, },\n" +
        "   { 1 },\n" +
        "   { 0, 2, },\n" +
        " },\n" +
        " null,\n" +
        " null,\n" +
        "\n" +
        "};\n",
        output.toString().replaceAll("\r", ""));
  }

  /** Test method for {@link NfaStates#dumpStatesForKind(IndentingPrintWriter)}. */
  @Test
  public void testDumpStatesForKind() {
    StringWriter output = new StringWriter();
    IndentingPrintWriter contentWriter = new IndentingPrintWriter(output);
    scannerGen.nfaStates.dumpStatesForKind(contentWriter);
    assertEquals("protected static final int[][][] statesForState = null;\n" +
        "protected static final int[][] kindForState = null;\n",
        output.toString().replaceAll("\r", ""));
  }

  /** Test method for {@link NfaStates#dumpStatesForKind(IndentingPrintWriter)}. */
  @Test
  public void testDumpStatesForKindInitialised() throws Exception {
    StringWriter output = new StringWriter();
    IndentingPrintWriter contentWriter = new IndentingPrintWriter(output);
    setupState();
    scannerGen.nfaStates.dumpStatesForKind(contentWriter);
    assertEquals("protected static final int[][][] statesForState = {\n" +
        " {\n" +
        "   { 0 },\n" +
        "   { 1 },\n" +
        "   { 2 },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 5 },\n" +
        "   { 6 },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 8 },\n" +
        "   { 9 },\n" +
        "   { 10 },\n" +
        "   { 11 },\n" +
        "   { 12 },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 14 },\n" +
        "   { 15 },\n" +
        "   { 16 },\n" +
        "   { 17 },\n" +
        "   { 18 },\n" +
        "   { 19 },\n" +
        "   { 20 },\n" +
        "   { 21 },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 23 },\n" +
        "   { 24 },\n" +
        "   { 25 },\n" +
        "   { 26 },\n" +
        "   { 27 },\n" +
        "   { 28 },\n" +
        "   { 29 },\n" +
        "   { 30 },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 32 },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 34 },\n" +
        "   { 35 },\n" +
        "   { 36 },\n" +
        "   { 37 },\n" +
        "   { 38 },\n" +
        "   { 39 },\n" +
        "   { 40 },\n" +
        "   { 41 },\n" +
        "   { 42 },\n" +
        "   { 43 },\n" +
        "   { 44 },\n" +
        "   { 45 },\n" +
        "   { 46 },\n" +
        "   { 47 },\n" +
        "   { 3, 4, 7, 13, 22, 31, 33, 48, },\n" +
        "   { 49 },\n" +
        "   { 50 },\n" +
        "   { 51 },\n" +
        "   { 52 },\n" +
        "   { 53 },\n" +
        "   { 54 },\n" +
        "   { 55 },\n" +
        "   { 56 },\n" +
        "   { 57 },\n" +
        "   { 58 },\n" +
        "   { 59 },\n" +
        "   { 60 },\n" +
        "   { 61 },\n" +
        "   { 62 },\n" +
        "   { 63 },\n" +
        "   { 64 },\n" +
        " },\n" +
        " null,\n" +
        " {\n" +
        "   { 0, 2, },\n" +
        "   { 1 },\n" +
        "   { 0, 2, },\n" +
        " },\n" +
        " null,\n" +
        " null,\n" +
        "\n" +
        "};\n" +
        "protected static final int[][] kindForState = {\n" +
        "{ 20, 20, 20, 20, 80, 80, 80, 84, 84, 84, 84, 84, 84, 89,\n" +
        "  89, 89, 89, 89, 89, 89, 89, 89, 90, 90, 90, 90, 90, 90, 90,\n" +
        "  90, 90, 140, 140, 84, 84, 84, 84, 84, 84, 84, 84, 84, 84, 84,\n" +
        "  84, 84, 84, 84, 84, 80, 80, 80, 84, 84, 84, 84, 84, 84, 84,\n" +
        "  84, 84, 84, 84, 84, 84},\n" +
        "null\n" +
        ",\n" +
        "{ 23, 23, 23},\n" +
        "null\n,\n" +
        "null\n\n" +
        "};\n",
        output.toString().replaceAll("\r", ""));
  }
}
