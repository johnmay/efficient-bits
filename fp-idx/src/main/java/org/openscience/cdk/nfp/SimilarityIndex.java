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
import java.util.ArrayList;
import java.util.List;

/**
 * @author John May
 */
public final class SimilarityIndex {

    private final int[]      counts;
    private final ByteBuffer buffer;
    private final int        offset, step;
    private final FileChannel channel;
    private final int length = 1024;

    // total size of the index (number of the entries)
    private final int nEntries;

    // search stats
    private int nChecked = 0;

    private SimilarityIndex(int[] counts, FileChannel channel, ByteBuffer buffer) {
        this.counts = counts;
        this.buffer = buffer;
        this.offset = buffer.position() + (counts.length * 4);
        this.step = (counts.length - 1) / 8;
        this.channel = channel;

        this.nEntries = counts[counts.length - 1];
    }

    List<Integer> top(BinaryFingerprint query, int k, Measure measure) {

        int queryCardinality = query.cardinality();

        int[] ordering = new int[counts.length + 2];
        int n = 0;
        int max = counts.length - 1;
        ordering[n++] = queryCardinality;

        int jHi = queryCardinality + 1;
        int jLo = queryCardinality - 1;
        while (true) {
            if (jHi < max)
                ordering[n++] = jHi++;
            if (jLo > 0)
                ordering[n++] = jLo--;
            if (jLo == 0 && jHi == max)
                break;
        }

        MinBinaryHeap heap = new MinBinaryHeap(k);

        BinaryFingerprint fp = new BinaryFingerprint(length);

        for (int i = 0; i < n; i++) {
            final int bin = ordering[i];

            if (bin < 0 || bin >= counts.length)
                continue;

            if (k <= heap.size && heap.min() > measure.bound(queryCardinality, bin))
                break;

            for (int st = counts[bin], end = counts[bin + 1]; st < end; st++) {
                buffer.position(offset + st * step);
                fp.readBytes(buffer, length);
                heap.add(st, fp.similarity(query, Similarity.Tanimoto));
            }
        }

        return heap.keys();
    }

    /**
     * Select all fingerprints in the index that are similar (at a specified threshold)
     * to a query fingerprint.
     *
     * @param query     query fingerprint
     * @param threshold the threshold (e.g. 0.8)
     * @param measure   similarity measure
     * @return FP indexes that match
     */
    List<Integer> findAll(BinaryFingerprint query, double threshold, Measure measure) {

        List<Integer> xs = new ArrayList<Integer>();

        int queryCardinality = query.cardinality();

        int[] ordering = new int[counts.length + 2];
        int n = 0;
        ordering[n++] = queryCardinality;

        int jHi = queryCardinality + 1;
        int jLo = queryCardinality - 1;
        while (true) {
            if (measure.bound(queryCardinality, jHi) < threshold)
                break;
            ordering[n++] = jHi++;
            if (measure.bound(queryCardinality, jHi) < threshold)
                break;
            ordering[n++] = jLo--;
        }

        BinaryFingerprint localFp = new BinaryFingerprint(length);

        nChecked = 0;

        // for each bin (by popcount)
        for (int i = 0; i < n; i++) {
            final int bin = ordering[i];

            if (bin < 0 || bin >= counts.length)
                continue;

            nChecked += counts[bin + 1] - counts[bin];

            // for each fingerprint in bin
            for (int st = counts[bin], end = counts[bin + 1]; st < end; st++) {
                buffer.position(offset + st * step);
                localFp.readBytes(buffer, length);
                if (localFp.similarity(query, Similarity.Tanimoto) >= threshold) {
                    xs.add(st);
                }
            }
        }

        return xs;
    }

    int checked() {
        return nChecked;
    }

    int size() {
        return nEntries;
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
