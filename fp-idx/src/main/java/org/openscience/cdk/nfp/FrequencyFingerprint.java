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

import java.util.Map;
import java.util.TreeMap;

/**
 * @author John May
 */
public final class FrequencyFingerprint extends Fingerprint {

    final Map<Integer, Counter> freqs;

    public FrequencyFingerprint() {
        this.freqs = new TreeMap<Integer, Counter>();
    }

    int add(int x) {
        Counter cnt = freqs.get(x);
        if (cnt != null) {
            cnt.value++;
        }
        else {
            freqs.put(x, new Counter(1));
        }
        return x;
    }

    void remove(int x) {
        Counter cnt = freqs.get(x);
        if (cnt == null) return;
        if (cnt.value == 1) freqs.remove(x);
        else cnt.value--;
    }

    void clear(int x) {
        freqs.remove(x);
    }

    boolean get(int x) {
        return freqs.containsKey(x);
    }

    int freq(int x) {
        Counter cnt = freqs.get(x);
        return cnt != null ? cnt.value : 0;
    }

    double similarity(FrequencyFingerprint that, Similarity similarity) {
        int a = 0;
        int b = 0;
        int both = 0;
        int neither = 0;

        Map.Entry<Integer, Counter>[] aEnts = this.freqs.entrySet().toArray(new Map.Entry[this.freqs.size()]);
        Map.Entry<Integer, Counter>[] bEnts = that.freqs.entrySet().toArray(new Map.Entry[that.freqs.size()]);

        int i = 0, j = 0;
        while (i < aEnts.length && j < bEnts.length) {
            
            int aKey = aEnts[i].getKey();
            int bKey = bEnts[j].getKey();

            if (aKey < bKey) {
                a += aEnts[i++].getValue().value;
            }
            else if (aKey > bKey) {
                b += bEnts[j++].getValue().value;
            }
            else {
                int ap = aEnts[i++].getValue().value; 
                int bp = bEnts[j++].getValue().value; 
                if (ap == bp) {
                    both++;
                } else if (ap > bp) {
                    a += ap - bp;
                    both += bp;
                } else if (bp > ap) {
                    b += bp - ap;
                    both += ap;
                }
            }
        }
        
        while (i < aEnts.length) {
            a += aEnts[i++].getValue().value;   
        }
        while (j < bEnts.length) {
            b += bEnts[j++].getValue().value;
        }

        System.out.println(a + " " + b + " " + both);

        return similarity.compute(a, b, both, neither);
    }

    @Override public String toString() {
        return freqs.toString();
    }

    private static final class Counter {
        private int value;

        private Counter(int value) {
            this.value = value;
        }

        @Override public String toString() {
            return Integer.toString(value);
        }
    }
}
