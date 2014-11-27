package org.openscience.cdk.nfp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * @author John May
 */
public class FpsToIdx {

    public static void main(String[] args) throws IOException {

        final String fpsPath = args[0];
        final int len = 1024;
        final long[] words = new long[len / 64];

        final List<BinaryFingerprint> fps = new ArrayList<BinaryFingerprint>();
        
        int cnt = 0;
        BufferedReader br = new BufferedReader(new FileReader(fpsPath));
        String line = null;
        long t0 = System.nanoTime();
        while ((line = br.readLine()) != null) {

            try {
                FpsFmt.readHex(line, len, words);
            } catch (Exception e) {
                e.printStackTrace();
            }

            BinaryFingerprint bfp = BinaryFingerprint.valueOf(words, len);

            fps.add(bfp);
            
            if (++cnt % 1000 == 0) {
                System.err.printf("\rread %d %.2fs", cnt, (System.nanoTime() - t0) / 1e9);
            }
        }
        long t1 = System.nanoTime();
        System.err.printf("\rread %d %.2fs\n", cnt, (t1 - t0) / 1e9);
        
        FingerprintSort.index(fps, 1024, new File(fpsPath + ".idx"));
        long t2 = System.nanoTime();

        System.err.printf("\rwrite idx %.2fs\n", (t2 - t1) / 1e9);
    }
}
