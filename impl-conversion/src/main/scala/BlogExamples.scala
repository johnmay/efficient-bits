import org.RDKit._
import org.openscience.cdk.exception.InvalidSmilesException
import org.openscience.cdk.fingerprint.{PubchemFingerprinter, CircularFingerprinter}
import org.openscience.cdk.fingerprint.CircularFingerprinter.CLASS_ECFP4
import org.openscience.cdk.inchi.InChIGeneratorFactory
import org.openscience.cdk.interfaces.IAtomContainer
import org.openscience.cdk.silent.{AtomContainer, SilentChemObjectBuilder}
import org.openscience.cdk.smiles.{SmilesGenerator, SmilesParser}
import org.rdkit.RDKit
import uk.ac.cam.ch.wwmm.opsin.NameToStructure

object BlogExamples extends App {

  val bldr = SilentChemObjectBuilder.getInstance
  val sp = new SmilesParser(bldr)
  val igf = InChIGeneratorFactory.getInstance

  RDKit.activate()
  
  println(unismi("caffeine"))
  println(unismi("CN1C=NC2=C1C(=O)N(C)C(=O)N2C"))
  println(unismi("InChI=1S/C8H10N4O2/c1-10-4-9-6-5(10)7(13)12(3)8(14)11(6)2/h4H,1-3H3"))
  println(unismi("1S/C8H10N4O2/c1-10-4-9-6-5(10)7(13)12(3)8(14)11(6)2/h4H,1-3H3"))
  
  val fp1 = new CircularFingerprinter(CLASS_ECFP4).getCountFingerprint("porphyrin")
  
  println(unismi("(R)-butan-2-ol"))
    
  val fp2 = new ExplicitBitVect(512)
  RDKFuncs.getAvalonFP("caffeine", fp2)
  
  val caffeine : RWMol = "caffeine"
  new PubchemFingerprinter(bldr).getBitFingerprint(caffeine)
  
  // String -> CDK,RDkit Structures
  
  implicit def autoParseRDKit(str: String): RWMol = cdk2rdkit(autoParseCDK(str))
  
  implicit def autoParseCDK(str: String): IAtomContainer = {
    if (str.startsWith("InChI=")) { 
      inchipar(str)
    } else if (str.startsWith("1S/")) {
      inchipar("InChI=" + str)
    } else {
      try {
        cdksmipar(str)
      } catch {
        case _: InvalidSmilesException => nompar(str)
      }
    }
  }
  
  // CDK <-> RDKit
  
  implicit def cdk2rdkit(ac : IAtomContainer) : RWMol = rdsmipar(SmilesGenerator.isomeric.create(ac))
  
  implicit def rdkit2cdk(romol : RWMol) : IAtomContainer = cdksmipar(RDKFuncs.getCanonSmiles(romol))

  // String -> Structure
  
  def inchipar(inchi: String) = igf.getInChIToStructure(inchi, bldr).getAtomContainer

  def cdksmipar(smi: String) = sp.parseSmiles(smi)

  def nompar(nom: String) = cdksmipar(NameToStructure.getInstance.parseToSmiles(nom))
  
  def rdsmipar(smi: String) = RWMol.MolFromSmiles(smi)

  // Structure -> String
  
  def cdksmi(ac: IAtomContainer) = SmilesGenerator.isomeric().create(ac)
  
  def rdsmi(romol: ROMol) = RDKFuncs.getCanonSmiles(romol)
  
  def cansmi(ac: IAtomContainer) = SmilesGenerator.unique().create(ac)
  
  // uses InChI to canonicalise the SMILES (O'Boyle N, 2012)
  def unismi(ac: IAtomContainer) = SmilesGenerator.absolute().create(ac)

}
