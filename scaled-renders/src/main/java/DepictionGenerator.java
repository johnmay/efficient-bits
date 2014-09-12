import org.openscience.cdk.geometry.GeometryUtil;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.elements.ElementGroup;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;

import java.util.List;

/**
 * Generates a scaled depiction for a structure instance.
 * @author John May
 */
final class DepictionGenerator {

    List<IGenerator<IAtomContainer>> generators;
    RendererModel parameters;

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
        
        ElementGroup primitives = new ElementGroup();
        for (IGenerator<IAtomContainer> generator : generators)
            primitives.add(generator.generate(container, parameters));
        
        return new Depiction(primitives, container, parameters);
    }

    private void setScale(IAtomContainer container) {
        if (container.getBondCount() > 0) {
            setScaleForBondLength(GeometryUtil.getBondLengthMedian(container));
        }
        else {
            setScaleForBondLength(1.5);
        }
    }

    private void setScaleForBondLength(double bondLength) {
        setScale(parameters.get(BasicSceneGenerator.BondLength.class) / bondLength);
    }

    private void setScale(double scale) {
        parameters.set(BasicSceneGenerator.Scale.class, scale);
    }
}
