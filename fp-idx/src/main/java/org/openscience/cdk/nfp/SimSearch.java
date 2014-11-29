package org.openscience.cdk.nfp;

import com.google.common.io.CharStreams;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.openscience.cdk.nfp.Similarity.Tanimoto;

/**
 * Search a precomputed binary index for entries.
 * @author John May
 */
public class SimSearch {

    private final static SmilesParser          smipar = new SmilesParser(SilentChemObjectBuilder.getInstance());
    private final static CircularFingerprinter fpr    = new CircularFingerprinter();
    private final static int                   len    = 1024;

    public static void main(String[] args) throws IOException, CDKException {

        final String idxPath = args[0];
        final SimilarityIndex idx = SimilarityIndex.load(new File(idxPath));

        if (args.length < 2) {
            System.err.println("usage ./simsearch <idx> [<file.smi>|<smi> .. <smi>]");
            return;
        }

        File queries = new File(args[1]);
        // test all in a file
        if (queries.exists()) {
            for (String smi : CharStreams.readLines(new FileReader(queries))) {
                runQuery(idx, smi);
            }
        }
        // test each smiles argument
        else {
            for (int i = 1; i < args.length; i++) {
                runQuery(idx, args[i]);
            }
        }
    }

    private static void runQuery(SimilarityIndex index, String smi) throws CDKException {
        IAtomContainer container = null;

        try {
            container = smipar.parseSmiles(smi);
        } catch (InvalidSmilesException e) {
            System.err.println(e.getMessage());
            return;
        }

        BinaryFingerprint query = BinaryFingerprint.valueOf(fpr.getBitFingerprint(container).asBitSet().toLongArray(), len);

        long t0 = System.nanoTime();
        index.find(query, 50, 0.9, Tanimoto);
        long t1 = System.nanoTime();

        System.out.printf("%s %.2f ms\n", smi, (t1 - t0) / 1e6);
    }
}
