import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.graph.ConnectivityChecker;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

/**
 * @author John May
 */
public class SetDepictionExample {
    
    public static void main(String[] args) throws CDKException, IOException {
        
        final Font font = new Font("Verdana",
                                   Font.PLAIN,
                                   24);
        final DepictionGenerator generator = new DepictionGenerator(Arrays.asList(new BasicSceneGenerator(),
                                                                                  new StandardGenerator(font)));

        IAtomContainer container = new SmilesParser(SilentChemObjectBuilder.getInstance()).parseSmiles("[nH]1cccc1.c1ccccc1.[Na+].[Cl-]");
        
        IAtomContainerSet acSet = ConnectivityChecker.partitionIntoMolecules(container);

        StructureDiagramGenerator sdg = new StructureDiagramGenerator();
        sdg.setUseTemplates(false);
        for (IAtomContainer ac : acSet.atomContainers()) {
            sdg.setMolecule(ac, false);
            sdg.generateCoordinates();
        }
        
        SetDepiction depiction = generator.generate(acSet);
        depiction.setLayout(SetDepiction.Layout.Vertical);
        
        FileWriter fw = new FileWriter("/Users/johnmay/Desktop/fragments.svg");
        fw.write(depiction.toSvg());
        fw.close();
        fw = new FileWriter("/Users/johnmay/Desktop/fragments_x2.svg");
        fw.write(depiction.toSvg(2));
        fw.close();
        fw = new FileWriter("/Users/johnmay/Desktop/fragments_x4.svg");
        fw.write(depiction.toSvg(4));
        fw.close();

        ImageIO.write(depiction.toBufferedImage(2), "png", new File("/Users/johnmay/Desktop/fragments_vert.png"));
        depiction.setLayout(SetDepiction.Layout.Horizontal);
        ImageIO.write(depiction.toBufferedImage(2), "png", new File("/Users/johnmay/Desktop/fragments_horz.png"));
        
        
    }
}
