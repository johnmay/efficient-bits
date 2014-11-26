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

/**
 * @author John May
 */
public class SmiToFps {

    public static void main(String[] args) throws IOException, InvalidSmilesException {

        String path = args[0];
        BufferedWriter bw = args.length > 1 
                            ? new BufferedWriter(new FileWriter(args[1])) :
                            new BufferedWriter(new OutputStreamWriter(System.out));

        BufferedReader br = new BufferedReader(new FileReader(path));
        SmilesParser smipar = new SmilesParser(SilentChemObjectBuilder.getInstance());
        String line = null;

        int len = 1024; // internal to the ECFP4
        IFingerprinter fpr = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP4);

        int cnt = 0;
        long t0 = System.nanoTime();


        while ((line = br.readLine()) != null) {
            try {
                
                final String id = line.substring(line.indexOf(' '));
                final IAtomContainer container = smipar.parseSmiles(line);
                final BitSet bitSet = fpr.getBitFingerprint(container).asBitSet();

                StringBuilder sb = new StringBuilder();
                FpsFmt.writeHex(sb, len, bitSet.toLongArray());

                bw.write(sb.toString());
                bw.write("\t");
                bw.write(id);
                bw.newLine();

                if (++cnt % 5000 == 0) {
                    System.err.printf("\r%d compounds %6.2f s", cnt, (System.nanoTime() - t0) / 1e9);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        br.close();
    }
}
