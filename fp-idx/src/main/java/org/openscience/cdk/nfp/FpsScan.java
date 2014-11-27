package org.openscience.cdk.nfp;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Scan
 *
 * @author John May
 */
public class FpsScan {

    private static final char SEPARATOR = '\t';

    public static void main(String[] args) throws IOException, CDKException {

        final String fpsPath = args[0];
        final String smi = args[1];
        final double lim = Double.parseDouble(args[2]);

        final int len = 1024;
        final long[] words = new long[len / 64];


        // generate query fp
        CircularFingerprinter fpr = new CircularFingerprinter();
        IAtomContainer container = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smi);
        BinaryFingerprint qFp = BinaryFingerprint.valueOf(fpr.getBitFingerprint(container).asBitSet().toLongArray(), len);

        int cnt = 0;
        BufferedReader br = new BufferedReader(new FileReader(fpsPath));
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
        String line = null;
        long t0 = System.nanoTime();

        while ((line = br.readLine()) != null) {

            try {
                FpsFmt.readHex(line, len, words);
            } catch (Exception e) {
                e.printStackTrace();
            }

            BinaryFingerprint dFp = BinaryFingerprint.valueOf(words, len);

            double t = qFp.similarity(dFp, Similarity.Tanimoto);
            boolean include = t >= lim;

            if (include) {
                bw.write(line);
                bw.append(SEPARATOR);
                bw.write(String.format("%.2f", t));
                bw.newLine();
            }

            if (++cnt % 1000 == 0) {
                System.err.printf("\rscanned %d %.2fs ", cnt, (System.nanoTime() - t0) / 1e9);
            }
        }
        long t1 = System.nanoTime();
        System.err.printf("\rscanned %d %.2fs ", cnt, (t1 - t0) / 1e9);

        br.close();
        bw.close();
    }


}
