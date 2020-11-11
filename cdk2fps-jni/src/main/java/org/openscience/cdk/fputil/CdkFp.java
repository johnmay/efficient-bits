/*
 * =====================================
 *  Copyright (c) 2020 NextMove Software
 * =====================================
 */

package org.openscience.cdk.fputil;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.fingerprint.ExtendedFingerprinter;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.IFingerprinter;
import org.openscience.cdk.fingerprint.LingoFingerprinter;
import org.openscience.cdk.fingerprint.MACCSFingerprinter;
import org.openscience.cdk.fingerprint.PubchemFingerprinter;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import java.util.Arrays;
import java.util.BitSet;

/**
 * Simplified API for calculating CDK fingerprints.
 */
public class CdkFp {

  private static final IChemObjectBuilder bldr   = SilentChemObjectBuilder.getInstance();
  private static final SmilesParser       smipar = new SmilesParser(bldr);

  public static final int VARI_MASK = 0x0000_ffff;
  public static final int TYPE_MASK = 0x000f_0000;

  public static final int ECFP     = 0x0001_0000;
  public static final int FCFP     = 0x0002_0000;
  public static final int PATH     = 0x0003_0000;
  public static final int EXTPATH  = 0x0004_0000;
  public static final int PUBCHEM  = 0x0005_0000;
  public static final int MACCS166 = 0x0006_0000;
  public static final int LINGOS   = 0x0007_0000;

  public static final int ECFP0 = ECFP;
  public static final int ECFP2 = ECFP + 1;
  public static final int ECFP4 = ECFP + 2;
  public static final int ECFP6 = ECFP + 3;
  public static final int FCFP0 = FCFP;
  public static final int FCFP2 = FCFP + 1;
  public static final int FCFP4 = FCFP + 2;
  public static final int FCFP6 = FCFP + 3;
  public static final int PATH5 = PATH + 5;
  public static final int PATH6 = PATH + 6;
  public static final int PATH7 = PATH + 7;

  public static boolean encode(byte[] fp, String smi, int flav, int len) {
    IAtomContainer mol;
    try {
      mol = smipar.parseSmiles(smi);
    } catch (InvalidSmilesException e) {
      // Error: Bad SMILES (e.getMessage() to get why)
      return false;
    }

    /* JWM: We could set the aromaticity here but some fingerprints will redo,
       APIs need some cleanup */

    IFingerprinter fpr;
    switch (flav & TYPE_MASK) {
      case ECFP:
        fpr = new CircularFingerprinter((flav & VARI_MASK) + 1, len);
        break;
      case FCFP:
        fpr = new CircularFingerprinter((flav & VARI_MASK) + 5, len);
        break;
      case PATH:
        fpr = new Fingerprinter(len, (flav & VARI_MASK));
        break;
      case EXTPATH:
        fpr = new ExtendedFingerprinter(len, (flav & VARI_MASK));
        break;
      case PUBCHEM:
        fpr = new PubchemFingerprinter(bldr);
        if (fpr.getSize() > len) {
          // Error: result is not large enough (881 required)
          return false;
        }
        break;
      case MACCS166:
        fpr = new MACCSFingerprinter();
        if (fpr.getSize() > len) {
          // Error: result is not large enough (166 required)
          return false;
        }
        break;
      case LINGOS:
        fpr = new LingoFingerprinter(len);
        break;
      default:
        // Error: unsupported FP type
        return false;
    }

    BitSet bitset = null;
    try {
      bitset = fpr.getBitFingerprint(mol).asBitSet();
    } catch (CDKException e) {
      // something went wrong
      return false;
    }
    Arrays.fill(fp, (byte) 0);
    for (int bit = bitset.nextSetBit(0);
        bit >= 0;
        bit = bitset.nextSetBit(bit + 1))
      fp[bit >> 3] |= 1L << (bit & 0x3);

    return true;
  }
}
