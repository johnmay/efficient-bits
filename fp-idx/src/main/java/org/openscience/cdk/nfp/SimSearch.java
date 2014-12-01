package org.openscience.cdk.nfp;

import com.google.common.io.CharStreams;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import static org.openscience.cdk.nfp.Similarity.Tanimoto;

/**
 * Search a precomputed binary index for entries.
 *
 * @author John May
 */
public class SimSearch {

    private final static SmilesParser          smipar = new SmilesParser(SilentChemObjectBuilder.getInstance());
    private final static CircularFingerprinter fpr    = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP4);
    private final static int                   len    = 1024;

    public static void main(String[] args) throws IOException, CDKException {

        final String idxPath = args[0];
        final double threshold = Double.parseDouble(args[1]);
        final SimilarityIndex idx = SimilarityIndex.load(new File(idxPath));

        if (args.length < 3) {
            System.err.println("usage ./simsearch <idx> <t> [<file.smi>|<smi> .. <smi>]");
            return;
        }

        File queries = new File(args[2]);
        // test all in a file
        if (queries.exists()) {
            DescriptiveStatistics stats = new DescriptiveStatistics();
            for (String smi : CharStreams.readLines(new FileReader(queries))) {
                long t = runQuery(idx, smi, threshold);
                if (t >= 0)
                    stats.addValue(t);
            }
            System.err.printf("t mean=%.0f, median=%.0f\n", stats.getPercentile(50) / 1e6, stats.getMean() / 1e6);
        }
        // test each smiles argument
        else {
            for (int i = 2; i < args.length; i++) {
                long t = runQuery(idx, args[i], threshold);
            }
        }
    }

    private static long runQuery(SimilarityIndex index, String smi, double threshold) throws CDKException {
        IAtomContainer container = null;

        try {
            container = smipar.parseSmiles(smi);
        } catch (InvalidSmilesException e) {
            System.err.println(e.getMessage());
            return -1;
        }

        BinaryFingerprint query = BinaryFingerprint.valueOf(fpr.getBitFingerprint(container).asBitSet().toLongArray(), len);

        long t0 = System.nanoTime();
        List<Integer> hits = index.findAll(query, threshold, Tanimoto);
        long t1 = System.nanoTime();

        long t = (t1 - t0);

        System.out.printf("%.0f\t%.2f\t%d\t%d\t%d\t%s\n", (t / 1e6), threshold, hits.size(), index.checked(), index.size(), smi);

        return t;
    }
}
