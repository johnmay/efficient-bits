import org.openscience.cdk.geometry.GeometryUtil;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.elements.ElementGroup;
import org.openscience.cdk.renderer.elements.IRenderingElement;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates a scaled depiction for a structure instance.
 *
 * @author John May
 */
final class DepictionGenerator {

    List<IGenerator<IAtomContainer>> generators;
    RendererModel                    parameters;

    DepictionGenerator(List<IGenerator<IAtomContainer>> generators) {
        this.generators = generators;
        this.parameters = new RendererModel();
        for (IGenerator<IAtomContainer> generator : generators)
            parameters.registerParameters(generator);
    }

    RendererModel getRendererModel2D() {
        return parameters;
    }

    Depiction generate(IAtomContainer container) {
        setScale(container);

        ElementGroup primitives = getDiagram(container);

        return new Depiction(primitives, container, parameters);
    }

    private ElementGroup getDiagram(IAtomContainer container) {
        ElementGroup primitives = new ElementGroup();
        for (IGenerator<IAtomContainer> generator : generators)
            primitives.add(generator.generate(container, parameters));
        return primitives;
    }

    SetDepiction generate(IAtomContainerSet containerSet) {
        setScale(containerSet);

        List<IRenderingElement> diagrams = new ArrayList<IRenderingElement>();
        List<IAtomContainer> containers = new ArrayList<IAtomContainer>();
        for (IAtomContainer container : containerSet.atomContainers()) {
            diagrams.add(getDiagram(container));
            containers.add(container);
        }

        return new SetDepiction(diagrams, containers, parameters);
    }

    private void setScale(IAtomContainer container) {
        if (container.getBondCount() > 0) {
            setScaleForBondLength(GeometryUtil.getBondLengthMedian(container));
        }
        else {
            setScaleForBondLength(1.5);
        }
    }

    private void setScale(IAtomContainerSet set) {
        List<Double> lengths = new ArrayList<Double>();
        for (IAtomContainer container : set.atomContainers()) {
            for (IBond bond : container.bonds()) {
                lengths.add(GeometryUtil.getLength2D(bond));
            }
        }

        if (lengths.size() == 0) {
            setScaleForBondLength(1.5);
        }
        else {
            // median
            setScaleForBondLength(lengths.get(lengths.size() / 2));
        }
    }


    private void setScaleForBondLength(double bondLength) {
        setScale(parameters.get(BasicSceneGenerator.BondLength.class) / bondLength);
    }

    private void setScale(double scale) {
        parameters.set(BasicSceneGenerator.Scale.class, scale);
    }
}
