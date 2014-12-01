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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

/**
 * @author John May
 */
public final class BinaryFingerprint extends Fingerprint {

    public static int WORD_SIZE    = 64;
    public static int ADDRESS_SIZE = Long.numberOfTrailingZeros(WORD_SIZE);

    final long[] words;
    final int    length;

    public BinaryFingerprint(int n) {
        if (!powerOfTwo(n))
            throw new IllegalArgumentException();
        this.length = n;
        this.words = new long[n / WORD_SIZE];
    }

    private BinaryFingerprint(int n, long[] words) {
        if (!powerOfTwo(n))
            throw new IllegalArgumentException();
        this.length = n;
        this.words = words;
    }

    static boolean powerOfTwo(int n) {
        return ((n != 0) && (n & (n - 1)) == 0);
    }

    int hash(int x) {
        return x & length - 1;
    }

    int add(int x) {
        int h = hash(x);
        words[h >> ADDRESS_SIZE] |= 1L << h;
        return h;
    }

    void clear(int x) {
        words[x >> ADDRESS_SIZE] &= ~(1L << x);
    }

    boolean get(int x) {
        return (words[x >> ADDRESS_SIZE] & 1L << x) != 0;
    }

    int xorshift(int x) {
        x ^= (x << 13);
        x ^= (x >> 17);
        return x ^ (x << 5);
    }

    int freq(int x) {
        return get(x) ? 1 : 0;
    }

    String toHex() {
        StringBuilder sb = new StringBuilder();
        for (long word : words) {
            for (int j = 0; j < 8; j++) {
                String hex = Long.toHexString(word & 0xff);
                if (hex.length() < 2) sb.append('0');
                sb.append(hex);
                word >>>= 8;
            }
        }
        return sb.toString();
    }

    byte[] toByteArray() {
        byte[] bs = new byte[words.length * 8];
        int i = 0;
        for (long word : words) {
            for (int j = 0; j < 8; j++) {
                bs[i++] = (byte) (word & 0xff);
                word >>>= 8;
            }
        }
        return bs;
    }


    static BinaryFingerprint fromBytes(ByteBuffer buffer, int length) {
        BinaryFingerprint fp = new BinaryFingerprint(length);
        fp.readBytes(buffer, length);
        return fp;
    }

    void readBytes(ByteBuffer buffer, int length) {
        int n = 0;
        int nWords = length / 64;
        while (nWords-- > 0) {
            words[n++] = buffer.getLong();
        }
    }

    void write(ByteBuffer buffer) {
        for (long word : words)
            buffer.putLong(word);
    }

    static BinaryFingerprint valueOf(long[] words, int len) {
        return new BinaryFingerprint(len, Arrays.copyOf(words, len / 64));
    }

    static BinaryFingerprint fromBytes(byte[] bytes) {
        BinaryFingerprint fp = new BinaryFingerprint(bytes.length * 8);
        int n = 0;
        for (int i = 0; i < bytes.length; i += 8) {
            fp.words[n++] = ((((long) bytes[i + 7] & 0xff) << 56) |
                    (((long) bytes[i + 6] & 0xff) << 48) |
                    (((long) bytes[i + 5] & 0xff) << 40) |
                    (((long) bytes[i + 4] & 0xff) << 32) |
                    (((long) bytes[i + 3] & 0xff) << 24) |
                    (((long) bytes[i + 2] & 0xff) << 16) |
                    (((long) bytes[i + 1] & 0xff) << 8) |
                    (((long) bytes[i] & 0xff)));
        }
        return fp;
    }

    BinaryFingerprint fold(int n) {
        BinaryFingerprint fp = new BinaryFingerprint(n);
        for (int i = 0; i < length; i++) {
            if (get(i)) fp.add(i);
        }
        return fp;
    }

    static BinaryFingerprint fromHex(String str) {
        char[] cs = str.toCharArray();
        for (int i = 0; i < cs.length; i += 2) {
            System.out.println('a' - '0');
        }
        return null;
    }

    BinaryFingerprint xor(BinaryFingerprint fp) {
        long[] xor = new long[words.length];
        for (int i = 0; i < xor.length; i++)
            xor[i] = words[i] ^ fp.words[i];
        return new BinaryFingerprint(length, xor);
    }

    int cardinality() {
        int sum = 0;
        for (long word : words)
            sum += Long.bitCount(word);
        return sum;
    }

    boolean contains(BinaryFingerprint other) {
        assert length == other.length;
        for (int i = 0; i < words.length; i++)
            if (this.words[i] != (this.words[i] & other.words[i]))
                return false;
        return true;
    }

    boolean intersects(BinaryFingerprint other) {
        assert length == other.length;
        for (int i = 0; i < words.length; i++)
            if ((this.words[i] & other.words[i]) != 0)
                return true;
        return false;
    }

    double similarity(BinaryFingerprint that, Measure measure) {
        assert length == that.length;

        // count the number of set bits in a, b and both
        int a = 0, b = 0, both = 0;
        for (int i = 0; i < words.length; i++) {
            a += Long.bitCount(this.words[i]);
            b += Long.bitCount(that.words[i]);
            both += Long.bitCount(this.words[i] & that.words[i]);
        }

        // dervive the number of bits only in a, b and neither
        final int onlyA = a - both;
        final int onlyB = b - both;
        final int neither = length - (both + onlyA + onlyB);

        return measure.compute(onlyA, onlyB, both, neither);
    }

    BitSet toBitset() {
        final BitSet bs = new BitSet(length);
        for (int i = 0; i < words.length; i++) {
            long word = words[i];
            for (int j = 0; j < WORD_SIZE; j++) {
                if ((word & 0x1L) != 0)
                    bs.set((i * WORD_SIZE) + j);
                word >>>= 1;
            }
        }
        return bs;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('<');
        for (int i = 0; i < length; i++) {
            if (get(i)) {
                if (sb.length() > 1)
                    sb.append(", ");
                sb.append(i);
            }
        }
        sb.append('>');
        return sb.toString();
    }
}
