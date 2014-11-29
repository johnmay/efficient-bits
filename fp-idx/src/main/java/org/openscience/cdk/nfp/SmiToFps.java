package org.openscience.cdk.nfp;

import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.BitSet;
import java.util.concurrent.TimeUnit;

/**
 * Calculate the fingerprints for each entry in a SMILES file and output in FPS
 * format.
 *
 * @author John May
 */
public class SmiToFps {

    private static final int TIME_STAMP_INTERVAL = 1250;

    public static void main(String[] args) throws IOException, InvalidSmilesException {

        if (args.length < 1) {
            System.err.println("Usage ./smi2fps {input.smi} [{output.smi}]");
            return;
        }

        final String path = args[0];
        final BufferedWriter bw = args.length > 1
                            ? new BufferedWriter(new FileWriter(args[1])) :
                              new BufferedWriter(new OutputStreamWriter(System.out));

        final BufferedReader rdr    = new BufferedReader(new FileReader(path));
        final SmilesParser   smipar = new SmilesParser(SilentChemObjectBuilder.getInstance());

        // TODO choose fingerprint
        IFingerprinter fpr = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP4);
        final int len = fpr.getSize();

        int cnt = 0;

        long t0 = System.nanoTime();
        String line;
        while ((line = rdr.readLine()) != null) {
            try {

                final IAtomContainer container = smipar.parseSmiles(line);
                final String         id        = suffixedId(line);
                final BitSet         bitSet    = fpr.getBitFingerprint(container).asBitSet();

                StringBuilder sb = new StringBuilder();
                FpsFmt.writeHex(sb, len, bitSet.toLongArray());

                bw.write(sb.toString());
                bw.write("\t");
                bw.write(id);
                bw.newLine();

                if (++cnt % TIME_STAMP_INTERVAL == 0)
                    System.err.print("\r[RUN] processed " + cnt+ " compounds, elapsed time " + elapsedTime(t0, System.nanoTime()));

            } catch (Exception e) {
                System.err.println("[INFO] Skipping " + line + " " + e.getMessage());
            }
        }
        long t1 = System.nanoTime();

        System.err.println("\r[FINISHED] processed " + cnt+ " compounds, elapsed time " + elapsedTime(t0, t1));

        rdr.close();
        bw.close();
    }

    // provides a local id for entries if one is not provided
    private static int idTicker = 0;

    // grab the id/title of a compound from the end of the SMILES input
    // or provide a numeric id
    private static String suffixedId(final String smi) {
        // only check the last but one to avoid
        // substring cases like 'CCO '
        for (int i = 0, len = smi.length() - 1; i < len; i++) {
            if (smi.charAt(i) == ' ' || smi.charAt(i) == '\t')
                return smi.substring(i + 1, smi.length());
        }
        return Integer.toString(++idTicker);
    }

    private static String elapsedTime(long t0, long t1) {
        long dt = System.nanoTime() - t0;
        long min = TimeUnit.NANOSECONDS.toMinutes(dt);
        dt -= TimeUnit.MINUTES.toNanos(min);
        long sec = TimeUnit.NANOSECONDS.toSeconds(dt);
        return String.format("%dm%ds", min, sec);
    }
}
