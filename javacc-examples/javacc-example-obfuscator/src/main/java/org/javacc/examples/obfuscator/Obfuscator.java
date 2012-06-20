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

package org.javacc.examples.obfuscator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Writer;

public class Obfuscator extends Globals {
  // These data structures implement a stack that is used to recursively
  // walk the input directory structure looking for Java files.
  static String[][] dirStack = new String[100][];
  static int[] dirStackIndex = new int[100];
  static int dirStackSize;
  static File[] dirFile = new File[100];

  static {
    dirFile[0] = inpDir;
    dirStackSize = 1;
    dirStack[dirStackSize] = dirFile[dirStackSize - 1].list();
    dirStackIndex[dirStackSize] = 0;
  }

  // Returns true if this is a Java file.
  static boolean javaFile(String name) {
    return name.length() >= 6 && name.substring(name.length() - 5).equals(".java");
  }

  // The iterator.  This uses the above data structures to walk the input
  // directory tree.  Every time it finds a Java file or when it cannot find
  // any more Java file, it returns to the caller.
  static void nextJavaFile() {
    while (true) {
      if (dirStackIndex[dirStackSize] == dirStack[dirStackSize].length) {
        dirStackSize--;
        if (dirStackSize == 0) {
          return;
        }
        else {
          dirStackIndex[dirStackSize]++;
        }
      }
      else {
        dirFile[dirStackSize] = new File(dirFile[dirStackSize - 1],
            dirStack[dirStackSize][dirStackIndex[dirStackSize]]);
        if (dirStack[dirStackSize][dirStackIndex[dirStackSize]].equals("SCCS")) {
          dirStackIndex[dirStackSize]++;
        }
        else if (dirFile[dirStackSize].isDirectory()) {
          dirStackSize++;
          dirStack[dirStackSize] = dirFile[dirStackSize - 1].list();
          dirStackIndex[dirStackSize] = 0;
        }
        else if (javaFile(dirStack[dirStackSize][dirStackIndex[dirStackSize]])) {
          dirStackIndex[dirStackSize]++;
          return;
        }
        else {
          dirStackIndex[dirStackSize]++;
        }
      }
    }
  }

  // The main Obfuscator routine.  It calls the iterator for each Java file to
  // work on.  It then creates the output file, then parses the input Java file
  // to determine whether or not it has a main program and also to collect the
  // tokens that make up this file.  It then calls printOutputFile that takes
  // first token and walks the next field chain printing tokens as it goes along.
  // Finally a main program is created if necessary.
  static public void start() throws IOException {
    while (true) {
      nextJavaFile();
      if (dirStackSize == 0) {
        break;
      }
      createOutputFile();
      File file = dirFile[dirStackSize];
      System.out.println("Obfuscating " + file.getPath());
      System.out.println("       into " + outFile.getPath());
      JavaParser parser;
      Token first;
      try {
        parser = new JavaParser(
            new JavaScanner(
                new CharStream.Escaping(
                    new CharStream.ForReader(
                        new InputStreamReader(
                            new FileInputStream(file))))));
        first = parser.CompilationUnit(dirStack[dirStackSize][dirStackIndex[dirStackSize] - 1]);
      }
      catch (ParseException ex) {
        System.out.println("Parse error in file " + file.getPath());
        throw new Error();
      }
      catch (IOException ex) {
        System.out.println("Could not open file " + file.getPath());
        throw new Error();
      }
      printOutputFile(first);
      if (mainExists) {
        createMainClass();
      }
    }
  }

  static File outFile;
  static PrintWriter out;

  static void createOutputFile() {
    outFile = outDir;
    for (int i = 1; i < dirStackSize; i++) {
      outFile = new File(outFile, map(dirStack[i][dirStackIndex[i]]));
      if (outFile.exists()) {
        if (!outFile.isDirectory()) {
          System.out.println("Unexpected error!");
          throw new Error();
        }
      }
      else {
        if (!outFile.mkdir()) {
          System.out.println("Could not create directory " + outFile.getPath());
          throw new Error();
        }
      }
    }
    String origFileName = dirStack[dirStackSize][dirStackIndex[dirStackSize] - 1];
    String newFileName = map(origFileName.substring(0, origFileName.length() - 5)) + ".java";
    outFile = new File(outFile, newFileName);
    try {
      out = new PrintWriter(new FileWriter(outFile));
    }
    catch (IOException e) {
      System.out.println("Could not create file " + outFile.getPath());
      throw new Error();
    }
  }

  static void printOutputFile(Token first) throws IOException {
    Token t = first;
    for (int i = 0; i < t.getColumn(); i++) {
      out.print(" ");
    }
    while (true) {
      String image = t.getImage();

      if (t.getKind() == JavaConstants.IDENTIFIER) {
        t.setImage(map(image));
      }

      escape(image, out);

      if (t.next == null) {
        out.println();
        break;
      }

      int line = t.getLine();
      int column = t.getColumn();
      for (int n = 0; n < image.length(); n++) {
        if (image.charAt(n) == '\n') {
          line++;
          column = 0;
        }
        else {
          column++;
        }
      }
      int nextLine = t.next.getLine();
      int nextColumn = t.next.getColumn();
      if (line != nextLine) {
        for (int i = line; i < nextLine; i++) {
          out.println();
        }
        for (int i = 0; i < nextColumn; i++) {
          out.print(" ");
        }
      }
      else {
        for (int i = column; i < nextColumn; i++) {
          out.print(" ");
        }
      }
      t = t.next;
    }
    out.close();
  }

  static void escape(String str, Writer out) throws IOException {
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (c < 0x20 || c > 0x7e) {
        String s = Integer.toString(c, 16);
        out.write("\\u");
        for (int n = 0; n < 4 - s.length(); n++) {
          out.write('0');
        }
        out.write(s);
      }
      else {
        out.write(c);
      }
    }
  }

  // This creates a main program if there was one in the original file.  This
  // main program has the same name and resides in the same package as the original
  // file and it simply calls the obfuscated main program.  This allows scripts
  // to continue to work.
  static void createMainClass() {
    boolean mustCreate = false;
    File mFile = outDir;
    for (int i = 1; i < dirStackSize; i++) {
      mFile = new File(mFile, dirStack[i][dirStackIndex[i]]);
      mustCreate = mustCreate || !map(dirStack[i][dirStackIndex[i]]).equals(dirStack[i][dirStackIndex[i]]);
      if (mFile.exists()) {
        if (!mFile.isDirectory()) {
          System.out.println("Error: Created file " + mFile.getPath() + ", but need to create a main program with the same path prefix.  Please remove identifiers from the path prefix from your <useidsfile> and run again.");
          throw new Error();
        }
      }
    }
    String origFileName = dirStack[dirStackSize][dirStackIndex[dirStackSize] - 1];
    String newFileName = map(origFileName.substring(0, origFileName.length() - 5)) + ".java";
    if (!mustCreate && origFileName.equals(newFileName)) {
      return; // this main program has not been obfuscated.
    }
    if (!mFile.exists() && !mFile.mkdirs()) {
      System.out.println("Could not create " + mFile.getPath());
      throw new Error();
    }
    mFile = new File(mFile, origFileName);
    try {
      out = new PrintWriter(new FileWriter(mFile));
    }
    catch (IOException ex) {
      System.out.println("Could not create " + mFile.getPath());
      throw new Error();
    }
    System.out.print("Generating main program ");
    String pname = "";
    if (dirStackSize > 1) {
      for (int i = 1; i < dirStackSize; i++) {
        pname += "." + dirStack[i][dirStackIndex[i]];
      }
      out.println("package " + pname.substring(1) + ";");
      System.out.print(pname.substring(1) + ".");
      out.println();
    }
    System.out.println(origFileName.substring(0, origFileName.length() - 5));
    out.println("public class " + origFileName.substring(0, origFileName.length() - 5) + " {");
    out.println();
    out.println("  public static void main(String[] args) {");
    pname = "";
    for (int i = 1; i < dirStackSize; i++) {
      pname += map(dirStack[i][dirStackIndex[i]]) + ".";
    }
    out.println("    " + pname + map(origFileName.substring(0, origFileName.length() - 5)) + ".main(args);");
    out.println("  }");
    out.println();
    out.println("}");
    out.close();
  }
}
