package org.openscience.cdk.nfp;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.openscience.cdk.nfp.Similarity.Tanimoto;

/**
 * @author John May
 */
public class IdxSim {
    public static void main(String[] args) throws IOException, CDKException {

        final String idxPath = args[0];
        final SimilarityIndex idx = SimilarityIndex.load(new File(idxPath));


        for (int i = 1; i < args.length; i++) {
            final String smi = args[i];

            IAtomContainer container = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles(smi);
            int len = 1024;
            CircularFingerprinter fpr = new CircularFingerprinter();

            BinaryFingerprint query = BinaryFingerprint.valueOf(fpr.getBitFingerprint(container).asBitSet().toLongArray(), len);

            long t0 = System.nanoTime();
            idx.find(query, 2, 0.8, Tanimoto);
            long t1 = System.nanoTime();

            System.out.printf("%s %.2f ms\n", smi, (t1 - t0) / 1e6);
        }


    }
}
