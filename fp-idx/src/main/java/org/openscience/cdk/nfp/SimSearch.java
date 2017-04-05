package org.openscience.cdk.nfp;

import com.google.common.io.CharStreams;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.commons.math3.analysis.function.Tan;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
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

  private static final OptionSpec<Double>  thresholdSpec;
  private static final OptionSpec<Integer> countSpec;
  private static final OptionSpec<File>    idxSpec;
  private static final OptionSpec<String>  inputSpec;

  private static final OptionParser optpar = new OptionParser();

  static {
    thresholdSpec = optpar.accepts("min", "Minimum similarity")
                          .withRequiredArg()
                          .ofType(Double.class)
                          .defaultsTo(0d);
    countSpec = optpar.accepts("k", "Count")
                      .withRequiredArg()
                      .ofType(Integer.class)
                      .defaultsTo(10);
    idxSpec = optpar.accepts("idx", "Index")
                    .withRequiredArg()
                    .ofType(File.class)
                    .required();
    inputSpec = optpar.nonOptions()
                      .ofType(String.class);
  }

  private static BinaryFingerprint getFingerprint(String smi)
  {
    try {
      IAtomContainer mol = smipar.parseSmiles(smi);
      return BinaryFingerprint.valueOf(fpr.getBitFingerprint(mol).asBitSet().toLongArray(), len);
    } catch (InvalidSmilesException e) {
      return null;
    } catch (CDKException e) {
      return null;
    }
  }

  public static void main(String[] args) throws IOException, CDKException
  {

    final OptionSet optset;
    try {
      optset = optpar.parse(args);
    } catch (OptionException e) {
      System.err.println(e.getMessage());
      return;
    }

    boolean thresholdSpecified = optset.has(thresholdSpec);
    boolean countSpecified     = optset.has(countSpec);
    if (thresholdSpecified && countSpecified) {
      System.err.println("Specify one of -k or -min");
      return;
    } else if (!thresholdSpecified && !countSpecified) {
      countSpecified = true;
    }

    final File    fidx = optset.valueOf(idxSpec);
    final Double  min  = optset.valueOf(thresholdSpec);
    final Integer k    = optset.valueOf(countSpec);

    final SimilarityIndex idx = SimilarityIndex.load(fidx);

    try (final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(System.out))) {



      int queryIdx = 0;
      for (String input : inputSpec.values(optset)) {
        if (isFile(input)) {
          try (FileInputStream fin = new FileInputStream(input);
               InputStreamReader rdr = new InputStreamReader(fin, StandardCharsets.UTF_8);
               BufferedReader brdr = new BufferedReader(rdr)) {


            String line;
            while ((line = brdr.readLine()) != null) {

              final String q = line;
              ResultPairEmitter emitter = new ResultPairEmitter() {
                @Override
                public void emit(int id, double score)
                {
                  try {
                    out.write(q);
                    out.write(' ');
                    out.write(Double.toString(score));
                    out.write(' ');
                    out.write(Integer.toString(id));
                    out.write('\n');
                  } catch (IOException e) {
                    // ignored
                  }
                }
              };

              BinaryFingerprint   fp = getFingerprint(line);
              if (countSpecified) {
                idx.top(fp, k, Similarity.Tanimoto, emitter);
              } else {
                idx.findAll(fp, min, Similarity.Tanimoto, emitter);
              }
              queryIdx++;
              out.flush();
            }
          }
        } else {
          BinaryFingerprint   fp = getFingerprint(input);

          final String q = input;
          ResultPairEmitter emitter = new ResultPairEmitter() {
            @Override
            public void emit(int id, double score)
            {
              try {
                out.write(q);
                out.write(' ');
                out.write(Integer.toString(id));
                out.write(' ');
                out.write(Double.toString(score));
                out.write('\n');
              } catch (IOException e) {
                // ignored
              }
            }
          };

          if (countSpecified) {
            idx.top(fp, k, Similarity.Tanimoto, emitter);
          } else {
            idx.findAll(fp, min, Similarity.Tanimoto, emitter);
          }

          queryIdx++;
          out.flush();
        }
      }
    }
  }

  private static boolean isFile(String x)
  {
    return new File(x).exists();
  }
}
