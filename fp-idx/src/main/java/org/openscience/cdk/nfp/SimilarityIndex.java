/*
 * Copyright (c) 2014 European Bioinformatics Institute (EMBL-EBI)
 *                    John May <jwmay@users.sf.net>
 *   
 * Contact: cdk-devel@lists.sourceforge.net
 *   
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version. All we ask is that proper credit is given
 * for our work, which includes - but is not limited to - adding the above 
 * copyright notice to the beginning of your source code files, and to any
 * copyright notice that you may distribute with programs based on this work.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 U
 */

package org.openscience.cdk.nfp;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author John May
 */
public final class SimilarityIndex {

    private final int[]      counts;
    private final ByteBuffer buffer;
    private final int        offset, step;
    private final FileChannel channel;

    private SimilarityIndex(int[] counts, FileChannel channel, ByteBuffer buffer) {
        this.counts = counts;
        this.buffer = buffer;
        this.offset = buffer.position() + (counts.length * 4);
        this.step = (counts.length - 1) / 8;
        this.channel = channel;
    }

    void find(BinaryFingerprint query, int k, double lim) {

        int queryCardinality = query.cardinality();

        int[] ordering = new int[counts.length + 2];
        int n = 0;
        ordering[n++] = queryCardinality;

        int jHi = queryCardinality + 1;
        int jLo = queryCardinality - 1;
        while (true) {
            if (bound(queryCardinality, jHi) < lim)
                break;
            ordering[n++] = jHi++;
            if (bound(queryCardinality, jHi) < lim)
                break;
            ordering[n++] = jLo--;
        }

        // byte[] bytes = new byte[1024 / 8];

        MinBinaryHeap heap = new MinBinaryHeap(k);

        for (int i = 0; i < n; i++) {
            int bin = ordering[i];
            if (k <= heap.size && heap.min() > bound(queryCardinality, bin))
                break;
            for (int st = counts[bin]; st < counts[bin + 1]; st++) {
                buffer.position(offset + st * step);
                //buffer.get(bytes);
                BinaryFingerprint fp = BinaryFingerprint.fromBytes(buffer, 1024);
                double c = fp.similarity(query, Similarity.Tanimoto);
                heap.add(st, c);
            }
        }

        System.out.println(heap);

    }

    static double bound(int q, int t) {
        // tanimoto only
        return q < t ? q / (double) t
                     : t / (double) q;
    }

    void close() throws IOException {
        channel.close();
    }

    static SimilarityIndex load(File f) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        FileChannel channel = raf.getChannel();
        ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, f.length());
        int s = buffer.getInt();
        int[] counts = new int[s];
        buffer.asIntBuffer().get(counts);
        return new SimilarityIndex(counts, channel, buffer);
    }


}
