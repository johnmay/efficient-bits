package org.openscience.cdk.nfp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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

        FileChannel in = new FileInputStream(fpsPath).getChannel();
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
        }
        long t1 = System.nanoTime();
        
        System.err.printf("\rRead %d fingerprints in %.2fs\n", fps.size(), (t1 - t0) / 1e9);
        
        FingerprintSort.index(fps, 1024, new File(fpsPath + ".idx"));
        long t2 = System.nanoTime();

        System.err.printf("\rGenerated index in %.2fs\n", (t2 - t1) / 1e9);
    }

    static void readToEnd(ByteBuffer buffer, StringBuilder sb) {
        while (buffer.hasRemaining()) {
            char c = (char) buffer.get();
            if (c == '\n') return;
            sb.append(c);
        }
    }
}
