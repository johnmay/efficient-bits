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
import java.util.Arrays;
import java.util.List;

/**
 * @author John May
 */
public final class FingerprintSort {

    static List<BinaryFingerprint> sort(List<BinaryFingerprint> src, int length) {        
        if (src.size() < 2)
            return src;
        int[] count = new int[length + 1];
        BinaryFingerprint[] dest = new BinaryFingerprint[src.size()];
        for (BinaryFingerprint fp : src)
            count[fp.cardinality() + 1]++;
        for (int i = 1; i < count.length; i++)
            count[i] += count[i - 1];
        for (BinaryFingerprint fp : src)
            dest[count[fp.cardinality()]++] = fp;
        return Arrays.asList(dest);
    }

    static int[] index(List<BinaryFingerprint> src, int length, File f) throws IOException {
        
        
        FileChannel channel = new RandomAccessFile(f, "rw").getChannel();

        int[] count        = new int[length + 1];
        int   step         = (length / 8);

        ByteBuffer  buffer  = channel.map(FileChannel.MapMode.READ_WRITE,
                                          0,
                                          4 + (4 * count.length) + (step * src.size()));
        
        
        for (BinaryFingerprint fp : src)
            count[fp.cardinality() + 1]++;
        for (int i = 1; i < count.length; i++)
            count[i] += count[i - 1];
        
        buffer.putInt(count.length);
        buffer.asIntBuffer().put(count);
        int offset = buffer.position() + (count.length * 4);
        
        // order stores where each fingerprint appears
        int[] ordering = new int[src.size()];
        int n = 0;
        
        for (BinaryFingerprint fp : src) {
            int idx = count[fp.cardinality()]++;
            put(buffer,
                offset + (step * idx), fp);
            ordering[idx] = n++; 
        }
        
        return ordering;
    }

    static void put(ByteBuffer buffer, int pos, BinaryFingerprint fp) {
        buffer.position(pos);
        fp.write(buffer);
    }
}
