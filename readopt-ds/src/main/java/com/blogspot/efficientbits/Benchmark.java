/*
 * Copyright (c) 2017. NextMove Software Ltd.
 */

package com.blogspot.efficientbits;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.graph.GraphUtil;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Benchmark {

    private static OptionParser        optpar     = new OptionParser();
    private static OptionSpec<Integer> stepSpec   = optpar.accepts("step").withRequiredArg().ofType(Integer.class).defaultsTo(100000);
    private static OptionSpec<String>  filterSpec = optpar.accepts("filter").withRequiredArg().ofType(String.class).defaultsTo(".+");
    private static OptionSpec<File>    filesSpec  = optpar.nonOptions().ofType(File.class);


    private static abstract class MeasureAlgorithm {
        long tInit = 0;
        long tRun  = 0;
        long check = 0;
        String desc;

        public MeasureAlgorithm(String desc) {
            this.desc = desc;
        }

        abstract void process(IAtomContainer mol);
    }

    private static MeasureAlgorithm[] algs = new MeasureAlgorithm[]{
        new MeasureAlgorithm("AtomContainer\tDepthFirst\t") {

            private void dfs(IAtomContainer mol, IAtom src, IBond prev) {
                check++;
                src.setFlag(CDKConstants.VISITED, true);
                for (IBond bond : mol.getConnectedBondsList(src)) {
                    if (bond == prev)
                        continue;
                    IAtom dst = bond.getConnectedAtom(src);
                    if (!dst.getFlag(CDKConstants.VISITED))
                        dfs(mol, dst, bond);
                }
            }

            @Override
            void process(IAtomContainer mol) {
                long t0 = System.nanoTime();
                for (IAtom atom : mol.atoms())
                    atom.setFlag(CDKConstants.VISITED, false);
                for (IAtom atom : mol.atoms())
                    if (!atom.getFlag(CDKConstants.VISITED))
                        dfs(mol, atom, null);
                long t1 = System.nanoTime();
                tRun += t1 - t0;
            }
        },
        new MeasureAlgorithm("GraphUtil\tDepthFirst\tVisitFlag") {

            private int dfs(IAtomContainer mol, int[][] adjlist, int src, int prev) {
                check++;
                IAtom atm = mol.getAtom(src);
                atm.setFlag(CDKConstants.VISITED, true);
                for (int dst : adjlist[src]) {
                    if (dst == prev)
                        continue;
                    IAtom nbr = mol.getAtom(dst);
                    if (!nbr.getFlag(CDKConstants.VISITED))
                        dfs(mol, adjlist, dst, src);
                }
                return 0;
            }

            @Override
            void process(IAtomContainer mol) {
                long                    t0      = System.nanoTime();
                int[][]                 adjlist = GraphUtil.toAdjList(mol);
                long                    t1      = System.nanoTime();
                for (IAtom atom : mol.atoms())
                    atom.setFlag(CDKConstants.VISITED, false);
                for (int i = 0; i < mol.getAtomCount(); i++) {
                    IAtom atom = mol.getAtom(i);
                    if (!atom.getFlag(CDKConstants.VISITED))
                        dfs(mol, adjlist, i, i);
                }
                long t2 = System.nanoTime();
                tInit += t1 - t0;
                tRun += t2 - t1;
            }
        },
        new MeasureAlgorithm("GraphUtil+BondMap\tDepthFirst\tVisitFlag") {

            private int dfs(IAtomContainer mol, int[][] adjlist, int src, int prev) {
                check++;
                IAtom atm = mol.getAtom(src);
                atm.setFlag(CDKConstants.VISITED, true);
                for (int dst : adjlist[src]) {
                    if (dst == prev)
                        continue;
                    IAtom nbr = mol.getAtom(dst);
                    if (!nbr.getFlag(CDKConstants.VISITED))
                        dfs(mol, adjlist, dst, src);
                }
                return 0;
            }

            @Override
            void process(IAtomContainer mol) {
                long                    t0      = System.nanoTime();
                GraphUtil.EdgeToBondMap bmap    = GraphUtil.EdgeToBondMap.withSpaceFor(mol);
                int[][]                 adjlist = GraphUtil.toAdjList(mol, bmap);
                long                    t1      = System.nanoTime();
                for (IAtom atom : mol.atoms())
                    atom.setFlag(CDKConstants.VISITED, false);
                for (int i = 0; i < mol.getAtomCount(); i++) {
                    IAtom atom = mol.getAtom(i);
                    if (!atom.getFlag(CDKConstants.VISITED))
                        dfs(mol, adjlist, i, i);
                }
                long t2 = System.nanoTime();
                tInit += t1 - t0;
                tRun += t2 - t1;
            }
        },
        new MeasureAlgorithm("GraphUtil+BondMap\tDepthFirst\tVisitArray") {

            private int dfs(IAtomContainer mol, int[][] adjlist, int src, int prev, boolean[] visit) {
                check++;
                visit[src] = true;
                for (int dst : adjlist[src]) {
                    if (dst == prev)
                        continue;
                    if (!visit[dst])
                        dfs(mol, adjlist, dst, src, visit);
                }
                return 0;
            }

            @Override
            void process(IAtomContainer mol) {
                long                    t0      = System.nanoTime();
                GraphUtil.EdgeToBondMap bmap    = GraphUtil.EdgeToBondMap.withSpaceFor(mol);
                int[][]                 adjlist = GraphUtil.toAdjList(mol, bmap);
                long                    t1      = System.nanoTime();
                boolean[]               visit   = new boolean[mol.getAtomCount()];
                for (int i = 0; i < mol.getAtomCount(); i++) {
                    if (!visit[i]) dfs(mol, adjlist, i, i, visit);
                }
                long t2 = System.nanoTime();
                tInit += t1 - t0;
                tRun += t2 - t1;
            }
        },new MeasureAlgorithm("GraphUtil\tDepthFirst\tVisitArray") {

            private int dfs(IAtomContainer mol, int[][] adjlist, int src, int prev, boolean[] visit) {
                check++;
                visit[src] = true;
                for (int dst : adjlist[src]) {
                    if (dst == prev)
                        continue;
                    if (!visit[dst])
                        dfs(mol, adjlist, dst, src, visit);
                }
                return 0;
            }

            @Override
            void process(IAtomContainer mol) {
                long                    t0      = System.nanoTime();
                int[][]                 adjlist = GraphUtil.toAdjList(mol);
                long                    t1      = System.nanoTime();
                boolean[]               visit   = new boolean[mol.getAtomCount()];
                for (int i = 0; i < mol.getAtomCount(); i++) {
                    if (!visit[i]) dfs(mol, adjlist, i, i, visit);
                }
                long t2 = System.nanoTime();
                tInit += t1 - t0;
                tRun += t2 - t1;
            }
        },
        new MeasureAlgorithm("AtomRef\tDepthFirst\tVisitFlag") {

            private final int dfs(AtomRef src, BondRef prev) {
                check++;
                src.setFlag(CDKConstants.VISITED, true);
                for (BondRef bond : src.getBonds()) {
                    if (bond == prev)
                        continue;
                    AtomRef dst = bond.getConnectedAtom(src);
                    if (!dst.getFlag(CDKConstants.VISITED))
                        dfs(dst, bond);
                }
                return 0;
            }

            @Override
            void process(IAtomContainer mol) {
                long      t0    = System.nanoTime();
                AtomRef[] arefs = AtomRef.getAtomRefs(mol);
                long      t1    = System.nanoTime();
                for (IAtom atom : mol.atoms())
                    atom.setFlag(CDKConstants.VISITED, false);
                for (AtomRef aref : arefs)
                    if (!aref.getFlag(CDKConstants.VISITED))
                        dfs(aref, null);
                long t2 = System.nanoTime();
                tInit += t1 - t0;
                tRun += t2 - t1;
            }
        },
        new MeasureAlgorithm("AtomRef\tDepthFirst\tVisitArray") {

            private int dfs(AtomRef src, BondRef prev, boolean[] visit) {
                check++;
                visit[src.getIndex()] = true;
                for (BondRef bond : src.getBonds()) {
                    if (bond == prev)
                        continue;
                    AtomRef dst = bond.getConnectedAtom(src);
                    if (!visit[dst.getIndex()])
                        dfs(dst, bond, visit);
                }
                return 0;
            }

            @Override
            void process(IAtomContainer mol) {
                long      t0    = System.nanoTime();
                AtomRef[] arefs = AtomRef.getAtomRefs(mol);
                long      t1    = System.nanoTime();
                boolean[] visit = new boolean[mol.getAtomCount()];
                for (AtomRef aref : arefs)
                    if (!visit[aref.getIndex()])
                        dfs(aref, null, visit);
                long t2 = System.nanoTime();
                tInit += t1 - t0;
                tRun += t2 - t1;
            }
        },
        new MeasureAlgorithm("AtomContainer\tRelaxation\t") {

            @Override
            void process(IAtomContainer mol) {
                long  t0   = System.nanoTime();
                int[] prev = new int[mol.getAtomCount()];
                int[] next = new int[mol.getAtomCount()];
                for (int i = 0; i < mol.getAtomCount(); i++) {
                    next[i] = prev[i] = mol.getAtom(i).getAtomicNumber();
                }
                for (int rep = 0; rep < mol.getAtomCount(); rep++) {
                    for (int j = 0; j < mol.getAtomCount(); j++) {
                        IAtom atom = mol.getAtom(j);
                        for (IBond bond : mol.getConnectedBondsList(atom)) {
                            IAtom nbr = bond.getConnectedAtom(atom);
                            next[j] += prev[mol.getAtomNumber(nbr)];
                        }
                    }
                    System.arraycopy(next, 0, prev, 0, next.length);
                }
                long t1 = System.nanoTime();
                tRun += t1 - t0;
                for (int aNext : next) check += aNext;
            }
        },
        new MeasureAlgorithm("AtomContainer\tRelaxation\tImproved") {

            @Override
            void process(IAtomContainer mol) {
                long  t0   = System.nanoTime();
                int[] prev = new int[mol.getAtomCount()];
                int[] next = new int[mol.getAtomCount()];
                for (int i = 0; i < mol.getAtomCount(); i++) {
                    next[i] = prev[i] = mol.getAtom(i).getAtomicNumber();
                }
                for (int rep = 0; rep < mol.getAtomCount(); rep++) {
                    for (IBond bond : mol.bonds()) {
                        IAtom beg    = bond.getAtom(0);
                        IAtom end    = bond.getAtom(1);
                        int   begIdx = mol.getAtomNumber(beg);
                        int   endIdx = mol.getAtomNumber(end);
                        next[begIdx] += prev[endIdx];
                        next[endIdx] += prev[begIdx];
                    }
                    System.arraycopy(next, 0, prev, 0, next.length);
                }
                long t1 = System.nanoTime();
                tRun += t1 - t0;
                for (int aNext : next) check += aNext;
            }
        },
            new MeasureAlgorithm("GraphUtil\tRelaxation\t") {

                @Override
                void process(IAtomContainer mol) {
                    long      t0 = System.nanoTime();
                    int[][]   g = GraphUtil.toAdjList(mol);
                    long      t1      = System.nanoTime();
                    int[]     prev    = new int[mol.getAtomCount()];
                    int[]     next    = new int[mol.getAtomCount()];
                    for (int i = 0; i < mol.getAtomCount(); i++) {
                        next[i] = prev[i] = mol.getAtom(i).getAtomicNumber();
                    }
                    for (int rep = 0; rep < mol.getAtomCount(); rep++) {
                        for (int idx = 0; idx < mol.getAtomCount(); idx++) {
                            for (int dst : g[idx]) {
                                next[idx] += prev[dst];
                            }
                        }
                        System.arraycopy(next, 0, prev, 0, next.length);
                    }
                    long t2 = System.nanoTime();
                    tInit += t1 - t0;
                    tRun += t2 - t1;
                    for (int aNext : next) check += aNext;
                }
            },
        new MeasureAlgorithm("AtomRef\tRelaxation\t") {

            @Override
            void process(IAtomContainer mol) {
                long      t0    = System.nanoTime();
                AtomRef[] arefs = AtomRef.getAtomRefs(mol);
                long      t1    = System.nanoTime();
                int[]     prev  = new int[mol.getAtomCount()];
                int[]     next  = new int[mol.getAtomCount()];
                for (int i = 0; i < mol.getAtomCount(); i++) {
                    next[i] = prev[i] = mol.getAtom(i).getAtomicNumber();
                }
                for (int rep = 0; rep < mol.getAtomCount(); rep++) {
                    for (final AtomRef aref : arefs) {
                        final int idx = aref.getIndex();
                        final List<BondRef> bonds = aref.getBonds();
                        for (int i = 0; i < bonds.size(); i++) {
                            final BondRef bond = bonds.get(i);
                            next[idx] += prev[bond.getConnectedAtom(aref).getIndex()];
                        }
                    }
                    System.arraycopy(next, 0, prev, 0, next.length);
                }
                long t2 = System.nanoTime();
                tInit += t1 - t0;
                tRun += t2 - t1;
                for (int aNext : next) check += aNext;
            }
        },
        new MeasureAlgorithm("AtomRef\tRelaxation\tXor") {

            @Override
            void process(IAtomContainer mol) {
                long      t0    = System.nanoTime();
                AtomRef[] arefs = AtomRef.getAtomRefs(mol);
                long      t1    = System.nanoTime();
                int[]     prev  = new int[mol.getAtomCount()];
                int[]     next  = new int[mol.getAtomCount()];
                for (int i = 0; i < mol.getAtomCount(); i++) {
                    next[i] = prev[i] = mol.getAtom(i).getAtomicNumber();
                }
                for (int rep = 0; rep < mol.getAtomCount(); rep++) {
                    for (final AtomRef aref : arefs) {
                        final int idx = aref.getIndex();
                        final List<BondRef> bonds = aref.getBonds();
                        for (int i = 0; i < bonds.size(); i++) {
                            final BondRef bond = bonds.get(i);
                            next[idx] += prev[bond.getOtherIdx(idx)];
                        }
                    }
                    System.arraycopy(next, 0, prev, 0, next.length);
                }
                long t2 = System.nanoTime();
                tInit += t1 - t0;
                tRun += t2 - t1;
                for (int aNext : next) check += aNext;
            }
        },
        new MeasureAlgorithm("BondRef\tRelaxation\t") {

            @Override
            void process(IAtomContainer mol) {
                long      t0    = System.nanoTime();
                BondRef[] bonds = BondRef.getBondRefs(mol);
                long      t1    = System.nanoTime();
                int[]     prev  = new int[mol.getAtomCount()];
                int[]     next  = new int[mol.getAtomCount()];
                for (int i = 0; i < mol.getAtomCount(); i++) {
                    next[i] = prev[i] = mol.getAtom(i).getAtomicNumber();
                }
                for (int rep = 0; rep < mol.getAtomCount(); rep++) {
                    for (BondRef bond : bonds) {
                        int begIdx = bond.getAtom(0).getIndex();
                        int endIdx = bond.getAtom(1).getIndex();
                        next[begIdx] += prev[endIdx];
                        next[endIdx] += prev[begIdx];
                    }
                    System.arraycopy(next, 0, prev, 0, next.length);
                }
                long t2 = System.nanoTime();
                tInit += t1 - t0;
                tRun += t2 - t1;
                for (int aNext : next) check += aNext;
            }
        },

        new MeasureAlgorithm("GraphUtil+BondMap\tRelaxation\t") {

            @Override
            void process(IAtomContainer mol) {
                long      t0 = System.nanoTime();
                GraphUtil.EdgeToBondMap bondmap = GraphUtil.EdgeToBondMap.withSpaceFor(mol);
                int[][]   g = GraphUtil.toAdjList(mol, bondmap);
                long      t1      = System.nanoTime();
                int[]     prev    = new int[mol.getAtomCount()];
                int[]     next    = new int[mol.getAtomCount()];
                for (int i = 0; i < mol.getAtomCount(); i++) {
                    next[i] = prev[i] = mol.getAtom(i).getAtomicNumber();
                }
                for (int rep = 0; rep < mol.getAtomCount(); rep++) {
                    for (int idx = 0; idx < mol.getAtomCount(); idx++) {
                        for (int dst : g[idx]) {
                            next[idx] += prev[dst];
                        }
                    }
                    System.arraycopy(next, 0, prev, 0, next.length);
                }
                long t2 = System.nanoTime();
                tInit += t1 - t0;
                tRun += t2 - t1;
                for (int aNext : next) check += aNext;
            }
        }
    };


    public static void main(String[] args) {

        long t0 = System.nanoTime();

        OptionSet optset = optpar.parse(args);

        int    step   = optset.valueOf(stepSpec);
        String filter = optset.valueOf(filterSpec);

        int     pos     = 0;
        int     num     = 0;
        Pattern pattern = Pattern.compile(filter);
        while (pos < algs.length) {
            if (pattern.matcher(algs[pos].desc).find()) {
                algs[num++] = algs[pos];
            }
            pos++;
        }
        algs = Arrays.copyOf(algs, num);
        System.err.println("Filter: " + filter);

        System.out.printf("%20s\t%20s\t%-35s\t%15s\t%15s\t%15s\t%15s\t%15s\t%s\n",
                          "DataSet",
                          "DataRange",
                          "DataStruct\tAlgorithm\tTweak",
                          "tElap",
                          "tElapSlower",
                          "tInit",
                          "tRun",
                          "tRunSlower",
                          "CheckSum");

        for (File file : filesSpec.values(optset))
            try (InputStream in = new FileInputStream(args[0]);
                 Reader rdr = new InputStreamReader(in, StandardCharsets.UTF_8);
                 BufferedReader brdr = new BufferedReader(rdr)) {

                SmilesParser smipar = new SmilesParser(SilentChemObjectBuilder.getInstance());
                smipar.kekulise(false); // not needed here

                int count   = 0;
                int linenum = 0;

                String line;
                while ((line = brdr.readLine()) != null) {
                    ++count;
                    ++linenum;

                    if (count == step) {
                        report(file, count, linenum);
                        count = 0;
                    }
                    try {
                        IAtomContainer mol = smipar.parseSmiles(line);
                        for (MeasureAlgorithm alg : algs) {
                            alg.process(mol);
                        }
                    } catch (InvalidSmilesException e) {
                        System.err.println("SMILES ERROR: " + e.getMessage());
                    }
                }

                report(file, count, linenum);
            } catch (IOException e) {

            }

        long t1 = System.nanoTime();
        System.err.printf("Total Elapsed Time: %.2f s\n", (t1 - t0) / 1e9);
    }

    private static void report(File file, int count, int linenum) {
        long bestElap = Long.MAX_VALUE;
        long bestRun  = Long.MAX_VALUE;
        for (MeasureAlgorithm alg : algs) {
            if (alg.tRun < bestRun)
                bestRun = alg.tRun;
            if (alg.tInit + alg.tRun < bestElap)
                bestElap = alg.tInit + alg.tRun;
        }
        for (MeasureAlgorithm alg : algs) {
            System.out.printf("%20s\t%20s\t%-35s\t%15s\t%15s\t%15s\t%15s\t%15s\t%d\n",
                              file.getName(),
                              String.format("%d-%d", linenum - count, linenum),
                              alg.desc,
                              String.format("%.1f", (alg.tInit + alg.tRun) / 1e6),
                              String.format("%.1fx", (alg.tInit + alg.tRun) / (double) bestElap),
                              String.format("%.1f", alg.tInit / 1e6),
                              String.format("%.1f", alg.tRun / 1e6),
                              String.format("%.1fx", alg.tRun / (double) bestRun),
                              alg.check);
            alg.tInit = 0;
            alg.tRun = 0;
        }
    }
}
