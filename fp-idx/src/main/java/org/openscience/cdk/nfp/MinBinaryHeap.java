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

import java.util.Arrays;

final class MinBinaryHeap {

    int[]    ids;
    double[] sims;
    int      size;

    MinBinaryHeap(int size) {
        this.ids = new int[size + 1];
        this.sims = new double[size + 1];
        this.size = 1;
    }

    static int parent(int i) {
        return i / 2;
    }

    static int left(int i) {
        return 2 * i;
    }

    static int right(int i) {
        return 2 * i + 1;
    }

    void add(int x, double c) {
        if (size < ids.length) {
            insert(x, c);
        }
        else if (c > sims[1]) {
            ids[1] = x;
            sims[1] = c;
            heapify(1);
        }
    }

    void insert(int x, double c) {
        size = size + 1;
        int i = size - 1;

        while (i > 1 && sims[parent(i)] > c) {
            ids[i] = ids[parent(i)];
            sims[i] = sims[parent(i)];
            i = parent(i);
        }
        ids[i] = x;
        sims[i] = c;
    }

    void exch(int i, int j) {
        int tmpId = ids[i];
        double tmpSim = sims[i];

        ids[i] = ids[j];
        sims[i] = sims[j];

        ids[j] = tmpId;
        sims[j] = tmpSim;
    }

    double min() {
        assert size > 1;
        return sims[1];
    }

    int deleteMin() {
        int min = ids[1];
        ids[1] = ids[--size];
        heapify(1);
        return min;
    }

    void heapify(int i) {
        int l = left(i);
        int r = right(i);
        int lo = -1;

        if (l < size && sims[l] < sims[i])
            lo = l;
        else
            lo = i;

        if (r < size && sims[r] < sims[lo])
            lo = r;

        if (lo == i)
            return;

        exch(i, lo);
        heapify(lo);
    }


    @Override public String toString() {
        return Arrays.toString(ids) + "\n" + Arrays.toString(sims);
    }

//    public static void main(String[] args) {
//        for (int i = 1; i < 20; i++) {
//            System.out.println(MinBinaryHeap.left(i) + "-" + MinBinaryHeap.right(i));
//        }
//    }
}