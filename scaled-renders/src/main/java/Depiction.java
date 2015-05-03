import de.erichseifert.vectorgraphics2d.PDFGraphics2D;
import de.erichseifert.vectorgraphics2d.SVGGraphics2D;
import org.openscience.cdk.geometry.GeometryUtil;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.elements.Bounds;
import org.openscience.cdk.renderer.elements.ElementGroup;
import org.openscience.cdk.renderer.elements.IRenderingElement;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.renderer.visitor.IDrawVisitor;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Holds on to rendering primitives and diagram bounds allow an structure diagram to be generated to
 * several output formats.
 *
 * @author John May
 */
final class Depiction extends AbstractDepiction {


    private final IAtomContainer    container;
    private final IRenderingElement primitives;
    private final Rectangle2D diagramBounds;

    Depiction(IRenderingElement primitives, IAtomContainer container, RendererModel params) {
        this.primitives = primitives;
        this.container = container;
        this.parameters = params;
        this.scale = params.get(BasicSceneGenerator.Scale.class);
        this.margin = params.get(BasicSceneGenerator.Margin.class);
        this.diagramBounds = getBounds(primitives, container);
    }

    
    /**
     * Access the width of the diagram.
     *
     * @return diagram width
     */
    double width() {
        return diagramBounds.getWidth() * scale;
    }

    /**
     * Access the height of the diagram.
     *
     * @return diagram width
     */
    double height() {
        return diagramBounds.getHeight() * scale;
    }

    @Override protected void draw(AWTDrawVisitor visitor, Rectangle2D viewBounds) {
        this.draw(visitor, viewBounds, diagramBounds, primitives);
    }
}
