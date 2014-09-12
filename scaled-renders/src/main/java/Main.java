import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.geometry.cip.CIPTool;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.isomorphism.Mappings;
import org.openscience.cdk.isomorphism.Pattern;
import org.openscience.cdk.renderer.SymbolVisibility;
import org.openscience.cdk.renderer.color.CDK2DAtomColors;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.standard.SelectionVisibility;
import org.openscience.cdk.renderer.generators.standard.StandardGenerator;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.smarts.SmartsPattern;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;

import static org.openscience.cdk.renderer.generators.standard.StandardGenerator.ANNOTATION_LABEL;
import static org.openscience.cdk.renderer.generators.standard.StandardGenerator.HIGHLIGHT_COLOR;

/**
 * The main class.
 * @author John May
 */
final class Main {

    public static void main(String[] args) throws ParseException {

        if (args.length == 0) {
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("cdk-render -name <name> -pdf <file>",
                                    options());
            return;
        }

        final CommandLine cmdln = parse(args);
        final IAtomContainer container = loadInput(cmdln);

        // configure font
        final Font font = new Font(cmdln.getOptionValue("ff", "Verdana"),
                                   Font.PLAIN,
                                   Integer.parseInt(cmdln.getOptionValue("fs", "18")));

        final DepictionGenerator generator = new DepictionGenerator(Arrays.asList(new BasicSceneGenerator(),
                                                                                  new StandardGenerator(font)));


        // configure rendering options
        {
            if (cmdln.hasOption("kekule"))
                generator.getRendererModel2D().set(StandardGenerator.Visibility.class,
                                                   SymbolVisibility.all());
            if (cmdln.hasOption("color-atoms"))
                generator.getRendererModel2D().set(StandardGenerator.AtomColor.class,
                                                   new CDK2DAtomColors());
            generator.getRendererModel2D().set(StandardGenerator.StrokeRatio.class,
                                               Double.parseDouble(cmdln.getOptionValue("sts", "1")));
        }
        
        // highlight smarts pattern?
        highlightSmartsPattern(container, Color.RED, cmdln.getOptionValue("sma", "[!*]"));

        // show annotations?
        if (cmdln.hasOption("atom-numbers") && cmdln.hasOption("cip-labels"))
            System.err.println("Currently only atom numbers OR CIP descriptors can be displayed, not both.");
        
        if (cmdln.hasOption("atom-numbers")) {
            for (int i = 0; i < container.getAtomCount(); i++) {
                container.getAtom(i).setProperty(ANNOTATION_LABEL,
                                                 Integer.toString(1 + i));
            }
        } else if (cmdln.hasOption("cip-labels")) {
            CIPTool.label(container);
            for (IAtom atom : container.atoms())
                atom.setProperty(ANNOTATION_LABEL, cipLabel(atom));
            for (IBond bond : container.bonds())
                bond.setProperty(ANNOTATION_LABEL, cipLabel(bond));            
        }
        
        // do the depiction generation
        final Depiction depiction = generator.generate(container);

        // store depiction in various formats
        if (cmdln.hasOption("pdf")) {
            File f = getOutputFile(cmdln, "pdf");
            write(f, depiction.toPdf());
            System.out.println("Generated... " + f);
        }
        if (cmdln.hasOption("svg")) {
            File f = getOutputFile(cmdln, "svg");
            write(f, depiction.toSvg());
            System.out.println("Generated... " + f);
        }
        if (cmdln.hasOption("png")) {
            try {
                File f = getOutputFile(cmdln, "png");
                ImageIO.write(depiction.toBufferedImage(), "png", f);
                System.out.println("Generated... " + f);
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }

    }

    private static void highlightSmartsPattern(IAtomContainer container, Color highlightColor, String smarts) {
        try {
            Pattern ptrn = SmartsPattern.create(smarts,
                                                SilentChemObjectBuilder.getInstance());
            Mappings mappings = ptrn.matchAll(container);

            int[] p = mappings.first();
            for (int x : p) {
                container.getAtom(x).setProperty(HIGHLIGHT_COLOR,
                                                 highlightColor);
            }
            for (IBond bond : container.bonds()) {
                if (bond.getAtom(0).getProperty(HIGHLIGHT_COLOR) != null && bond.getAtom(1).getProperty(HIGHLIGHT_COLOR) != null)
                    bond.setProperty(HIGHLIGHT_COLOR,
                                     highlightColor);
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }


    private static CommandLine parse(String[] args) throws ParseException {
        CommandLineParser cmdpar = new BasicParser();
        return cmdpar.parse(options(), args);

    }

    static File getOutputFile(CommandLine cmdln, String fmt) {
        String defaultName = "cdk-depiction-" + UUID.randomUUID();
        return ensureExtension(new File(cmdln.getOptionValue(fmt, defaultName)), fmt);
    }

    static File ensureExtension(File f, String type) {
        if (f.getName().endsWith(type))
            return f;
        return new File(f.getPath() + '.' + type);
    }

    static void write(File f, String str) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(f);
            fw.write(str);
        } catch (IOException e) {
            System.err.println("Could not write output");
        } finally {
            try {
                if (fw != null) fw.close();
            } catch (IOException e) {
                // ignored
            }
        }
    }
    
    static String cipLabel(IChemObject object) {
        String descriptor = object.getProperty(CDKConstants.CIP_DESCRIPTOR);
        if (descriptor == null || descriptor.equals("NONE"))
            return null;
        return StandardGenerator.ITALIC_DISPLAY_PREFIX + descriptor;
    }

    private static Options options() {
        Options opts = new Options();
        opts.addOption(new Option("smi", true, "Load molecule from SMILES"));
        opts.addOption(new Option("inchi", true, "Load molecule from IUPAC International Chemical Identifier"));
        opts.addOption(new Option("name", true, "Load molecule from chemical name (OPSIN)"));
        opts.addOption(new Option("mol", true, "Load molecule from molfile (V2000)"));
        
        // export formats
        opts.addOption(OptionBuilder.withArgName("file")
                                    .withDescription("Generate SVG output")
                                    .hasOptionalArg()
                                    .create("svg"));
        opts.addOption(OptionBuilder.withArgName("file")
                                    .withDescription("Generate PDF output")
                                    .hasOptionalArg()
                                    .create("pdf"));
        opts.addOption(OptionBuilder.withArgName("file")
                                    .withDescription("Generate PNG output")
                                    .hasOptionalArg()
                                    .create("png"));
        // rendering options      
        opts.addOption(new Option("atmnums", "atom-numbers", false, "Show atom numbes"));
        opts.addOption(new Option("cip", "cip-labels", false, "Show Cahn-Ingold-Prelog descriptors"));
        opts.addOption(new Option("col", "color-atoms", false, "Color hetro atoms"));
        opts.addOption(new Option("k", "kekule", false, "Render kekule output"));
        opts.addOption(new Option("sma", "smarts", true, "Highlight the specified SMARTS pattern (first hit)"));
        opts.addOption(new Option("sts", "stroke-scale", true, "the stroke scaling"));
        opts.addOption(new Option("ff", "font-family", true, "the font family"));
        opts.addOption(new Option("fs", "font-size", true, "the font size"));
        return opts;
    }

    private static IAtomContainer loadInput(CommandLine cmdln) {
        IAtomContainer container = null;
        if (cmdln.hasOption("smi")) {
            container = MolInput.fromSmiles(cmdln.getOptionValue("smi"));
        }
        if (cmdln.hasOption("name")) {
            if (container != null)
                throw new IllegalArgumentException("multiple inputs specified - please specify only one");
            container = MolInput.fromName(cmdln.getOptionValue("name"));
        }
        if (cmdln.hasOption("inchi")) {
            if (container != null)
                throw new IllegalArgumentException("multiple inputs specified - please specify only one");
            container = MolInput.fromInChI(cmdln.getOptionValue("inchi"));
        }
        if (cmdln.hasOption("mol")) {
            if (container != null)
                throw new IllegalArgumentException("multiple inputs specified - please specify only one");
            container = MolInput.fromMolfile(cmdln.getOptionValue("mol"));
        }
        if (container == null)
            throw new IllegalArgumentException("No input specified");
        return container;
    }

    private static double getScale(CommandLine cmdln) {
        if (!cmdln.hasOption("scale"))
            return 1;
        String scaleStr = cmdln.getOptionValue("scale");
        if (scaleStr.indexOf('x') >= 0)
            scaleStr = scaleStr.replaceAll("x", "");
        return Double.parseDouble(scaleStr);
    }
}
