package org.openscience.cdk.nfp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Convert an FPS file to a binary index.
 *
 * @author John May
 */
public class FpsToIdx {

    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.err.println("usage: ./mkidx {input.fps} [{output.idx}]");
            return;
        }

        final String fpsPath = args[0];
        final String idxPath = args.length < 2 ? fpsPath + ".idx"
                                               : args[1];

        final int len = 1024; // circular fp len

        final long[] words = new long[len / 64];

        // todo avoid storing all fp in mem
        final List<BinaryFingerprint> fps = new ArrayList<BinaryFingerprint>();
        final List<String> ids = new ArrayList<String>();

        FileChannel in = new FileInputStream(fpsPath).getChannel();
        // map the whole file for reading
        ByteBuffer buffer = in.map(FileChannel.MapMode.READ_ONLY, 0, new File(fpsPath).length());

        StringBuilder idStrBldr = new StringBuilder();

        long t0 = System.nanoTime();
        while (buffer.hasRemaining()) {
            // reset
            idStrBldr.setLength(0);

            FpsFmt.readHex(buffer, len, words); // hex bit set
            buffer.get(); // tab
            readToEnd(buffer, idStrBldr); // id
            
            fps.add(BinaryFingerprint.valueOf(words, len));
            ids.add(idStrBldr.toString());
        }
        long t1 = System.nanoTime();
        
        System.err.printf("\rRead %d fingerprints in %.2fs\n", fps.size(), (t1 - t0) / 1e9);
        
        int[] indexOrder = FingerprintSort.index(fps, 1024, new File(idxPath));
        long t2 = System.nanoTime();

        System.err.printf("\rGenerated index in %.2fs\n", (t2 - t1) / 1e9);

        BufferedWriter bw = new BufferedWriter(new FileWriter(idxPath + ".id"));
        for (int i = 0; i < indexOrder.length; i++) {
            bw.write(Integer.toString(i));
            bw.write('\t');
            bw.write(ids.get(indexOrder[i]));
            bw.newLine();
        }                        
        bw.close();
        
        long t3 = System.nanoTime();
        System.err.printf("\rWrote id file in %.2fs\n", (t3- t2) / 1e9);
    }

    static void readToEnd(ByteBuffer buffer, StringBuilder sb) {
        while (buffer.hasRemaining()) {
            char c = (char) buffer.get();
            if (c == '\n') return;
            sb.append(c);
        }
    }
}
