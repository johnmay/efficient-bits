/*
 * =====================================
 *  Copyright (c) 2020 NextMove Software
 * =====================================
 */


package org.openscience.cdk.fputil;

import org.openscience.cdk.fputil.CdkFp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class Main {
  public static void main(String[] args) {
    byte[] fp = new byte[1024 / 8];
    int progress = 0;
    long   t0 = System.nanoTime();
    try (BufferedReader brdr = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), StandardCharsets.UTF_8))) {
      String line;
      while ((line = brdr.readLine()) != null) {
        if (!CdkFp.encode(fp, line, CdkFp.ECFP4, 1024)) {
          System.err.println("Error: " + line);
        }
        if (++progress % 1000 == 0)
          System.err.printf("\r%d...", progress);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    long t1 = System.nanoTime();
    System.err.println(TimeUnit.NANOSECONDS.toMillis(t1 - t0));
  }
}
