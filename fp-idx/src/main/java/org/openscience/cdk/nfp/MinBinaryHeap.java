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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

final class MinBinaryHeap {

    int[]    key;
    double[] val;
    int      size;

    MinBinaryHeap(int size) {
        this.key = new int[size + 1];
        this.val = new double[size + 1];
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
        if (size < key.length) {
            insert(x, c);
        }
        else if (c > val[1]) {
            key[1] = x;
            val[1] = c;
            heapify(1);
        }
    }

    void insert(int x, double c) {
        size = size + 1;
        int i = size - 1;

        while (i > 1 && val[parent(i)] > c) {
            key[i] = key[parent(i)];
            val[i] = val[parent(i)];
            i = parent(i);
        }
        key[i] = x;
        val[i] = c;
    }

    void exch(int i, int j) {
        int tmpId = key[i];
        double tmpSim = val[i];

        key[i] = key[j];
        val[i] = val[j];

        key[j] = tmpId;
        val[j] = tmpSim;
    }

    double min() {
        assert size > 1;
        return val[1];
    }

    int deleteMin() {
        int min = key[1];
        key[1] = key[--size];
        heapify(1);
        return min;
    }

    void heapify(int i) {
        int l = left(i);
        int r = right(i);
        int lo = -1;

        if (l < size && val[l] < val[i])
            lo = l;
        else
            lo = i;

        if (r < size && val[r] < val[lo])
            lo = r;

        if (lo == i)
            return;

        exch(i, lo);
        heapify(lo);
    }

    Iterable<Map.Entry<Integer,Double>> pairs() {
        ArrayList<Map.Entry<Integer,Double>> entries = new ArrayList<>(key.length-1);
        for (int i = 1; i < size; i++)
            entries.add(new AbstractMap.SimpleImmutableEntry<>(key[i], val[i]));
        // FIXME can do ordering better with binary heap
        Collections.sort(entries, new Comparator<Map.Entry<Integer, Double>>() {
            @Override
            public int compare(Map.Entry<Integer, Double> a, Map.Entry<Integer, Double> b)
            {
                return Double.compare(b.getValue(), a.getValue());
            }
        });
        return entries;
    }

    Iterable<Integer> keys() {
        ArrayList<Integer> keys = new ArrayList<Integer>(key.length-1);
        for (int i = 1; i < size; i++)
            keys.add(key[i]);
        return keys;
    }

    @Override public String toString() {
        return Arrays.toString(key) + "\n" + Arrays.toString(val);
    }
}