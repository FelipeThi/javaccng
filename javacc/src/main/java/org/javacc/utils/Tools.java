package org.javacc.utils;

import org.javacc.Version;
import org.javacc.parser.JavaCCErrors;

import java.io.File;
import java.io.IOException;

public final class Tools {
  /**
   * This prints the banner line when the various tools are invoked.  This
   * takes as argument the tool's full name and its version.
   */
  public static void bannerLine(String fullName, String version) {
    System.out.print("Java Compiler Compiler Version " + Version.versionNumber + " (" + fullName);
    if (!"".equals(version)) {
      System.out.print(" Version " + version);
    }
    System.out.println(")");
  }

  public static void createOutputDir(File path) throws IOException {
    String pathName = path.getCanonicalPath();

    if (!path.exists()) {
      JavaCCErrors.warning("Output directory \"" + pathName + "\" does not exist. Creating the directory.");

      if (!path.mkdirs()) {
        JavaCCErrors.semanticError("Cannot create the output directory \"" + pathName + "\".");
        return;
      }
    }

    if (!path.isDirectory()) {
      JavaCCErrors.semanticError("\"" + pathName + " is not a valid output directory.");
      return;
    }

    if (!path.canWrite()) {
      JavaCCErrors.semanticError("Cannot write to the output output directory \"" + pathName + "\".");
      return;
    }
  }
}
