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
 * Linear scan of an FPS file filtering out all those that
 * match the query smiles (provided) above a given threshold.
 *
 * @author John May
 */
public class FpsScan {

    private static final char   SEPARATOR         = '\t';
    private static final double DEFAULT_THRESHOLD = 0.8;

    public static void main(String[] args) throws IOException, CDKException {

        if (args.length < 2) {
            System.err.println("usage: ./fpsscan {input.fps} 'SMILES' ['t']");
            return;
        }

        final String fpsPath = args[0];
        final String smi = args[1];
        final double lim = args.length > 2 ? Double.parseDouble(args[2]) : DEFAULT_THRESHOLD;

        final int len = 1024;
        final long[] words = new long[len / 64];

        // generate query fp (TODO make choosable)
        CircularFingerprinter fpr = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP4);
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
        System.err.printf("\rScanned in %.2fs \n", (t1 - t0) / 1e9);
    }

    static void readToEnd(ByteBuffer buffer, StringBuilder sb) {
        while (buffer.hasRemaining()) {
            char c = (char) buffer.get();
            if (c == '\n') return;
            sb.append(c);
        }
    }


}
