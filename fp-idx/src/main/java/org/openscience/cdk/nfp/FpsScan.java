package org.openscience.cdk.nfp;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Linear scan of an FPS file.
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
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
        long t0 = System.nanoTime();

        FileChannel in = new FileInputStream(fpsPath).getChannel();
        ByteBuffer buffer = in.map(FileChannel.MapMode.READ_ONLY, 0, new File(fpsPath).length());

        StringBuilder idStrBldr = new StringBuilder();
        
        while (buffer.hasRemaining()) {
            
            // reset
            idStrBldr.setLength(0);
            
            FpsFmt.readHex(buffer, len, words); // hex bit set
            buffer.get(); // tab
            readToEnd(buffer, idStrBldr); // id
            
            BinaryFingerprint dFp = BinaryFingerprint.valueOf(words, len);
            double t = qFp.similarity(dFp, Similarity.Tanimoto);
            boolean display = t >= lim;

            if (display) {
                bw.write(idStrBldr.toString());
                bw.write(SEPARATOR);
                bw.write(String.format("%.2f", t));
                bw.newLine();
            }
        }
        long t1 = System.nanoTime();
        in.close();
        bw.close();
        System.err.printf("\rscanned in %.2fs \n", (t1 - t0) / 1e9);
    }

    static void readToEnd(ByteBuffer buffer, StringBuilder sb) {
        while (buffer.hasRemaining()) {
            char c = (char) buffer.get();
            if (c == '\n') return;
            sb.append(c);
        }
    }


}
