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
 * @author John May
 */
abstract class AbstractDepiction {

    public static final double ACS_1996_BOND_LENGTH_MM = 5.08;
    public static final double ACS_1996_MARGIN_MM      = 0.56;

    protected RendererModel parameters;
    protected double scale, margin;

    protected abstract void draw(AWTDrawVisitor visitor, Rectangle2D viewBounds);
    
    /**
     * Low-level draw method used by other rendering methods.
     *
     * @param visitor the draw visitor
     * @param viewBounds    the view bounds - the diagram will be centered in the bounds
     */
    void draw(IDrawVisitor visitor, Rectangle2D viewBounds, Rectangle2D drawBounds, IRenderingElement drawing) {

        double zoomToFit = Math.min(viewBounds.getWidth() / (drawBounds.getWidth() * scale),
                                    viewBounds.getHeight() / (drawBounds.getHeight() * scale));

        // do blow up depiction only shrink
        if (zoomToFit > 1)
            zoomToFit = 1;

        AffineTransform transform = new AffineTransform();
        transform.translate(viewBounds.getCenterX(), viewBounds.getCenterY());
        transform.scale(scale, -scale);
        transform.scale(zoomToFit, zoomToFit);
        transform.translate(-drawBounds.getCenterX(), -drawBounds.getCenterY());

        // not always needed
        AWTFontManager fontManager = new AWTFontManager();
        fontManager.setFontForZoom(zoomToFit);

        visitor.setRendererModel(parameters);
        visitor.setFontManager(fontManager);
        visitor.setTransform(transform);

        // setup up transform
        visitor.visit(drawing);
    }
    
    abstract double width();

    abstract double height();

    /**
     * Generate a raster image at default scale.
     *
     * @return The generated image.
     */
    BufferedImage toBufferedImage() {
        return toBufferedImage(1);
    }

    /**
     * Generate a scaled raster image.
     *
     * @param scale resize the image by this amount
     * @return The generated image.
     */
    BufferedImage toBufferedImage(double scale) {
        return toBufferedImage(width() * scale, height() * scale);
    }

    BufferedImage toBufferedImage(double width, double height) {
        BufferedImage img = new BufferedImage((int) (margin + width + margin),
                                              (int) (margin + height + margin),
                                              BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g2 = img.createGraphics();
        g2.setBackground(parameters.get(BasicSceneGenerator.BackgroundColor.class));
        g2.clearRect(0, 0, img.getWidth(), img.getHeight());
        draw(new AWTDrawVisitor(g2),
             new Rectangle2D.Double(margin, margin, width, height));
        g2.dispose();
        return img;
    }

    /**
     * Convert the depiction to PDF. The depiction is scaled to match the recommended bond length of
     * the ACS 1996 style.
     *
     * @return content of the PDF
     * @see #toPdf(double, double)
     */
    String toPdf() {
        return toPdf(1d);
    }

    /**
     * Convert the depiction to PDF.
     *
     * @param scale resize the image by this amount
     * @return content of the PDF
     * @see #toPdf(double, double)
     */
    String toPdf(double scale) {
        return toPdf(scale * ACS_1996_BOND_LENGTH_MM,
                     scale * ACS_1996_MARGIN_MM);
    }

    /**
     * Convert the depiction to PDF. The bond lengths are rescaled to be an appropriate length in
     * 'mm'.
     *
     * @param bondLengthInMM the length of a bond (in mm)
     * @param marginInMM     the width of a margin (in mm)
     * @return content of the PDF
     */
    String toPdf(double bondLengthInMM, double marginInMM) {
        final double rescale = rescaleForBondLength(bondLengthInMM);
        return toPdf(width() * rescale, height() * rescale, marginInMM);
    }

    String toPdf(double width, double height, double margin) {
        Graphics2D g2 = new PDFGraphics2D(0, 0,
                                          margin + width + margin,
                                          margin + height + margin);
        draw(AWTDrawVisitor.forVectorGraphics(g2),
             new Rectangle2D.Double(margin, margin, width, height));
        g2.dispose();
        return g2.toString();
    }

    /**
     * Convert the depiction to SVG. The depiction is scaled to match the recommended bond length of
     * the ACS 1996 style.
     *
     * @return content of the SVG
     * @see #toPdf(double, double)
     */
    String toSvg() {
        return toSvg(1d);
    }

    /**
     * Convert the depiction to SVG.
     *
     * @param scale resize the image by this amount
     * @return content of the SVG
     * @see #toPdf(double, double)
     */
    String toSvg(double scale) {
        return toSvg(scale * ACS_1996_BOND_LENGTH_MM,
                     scale * ACS_1996_MARGIN_MM);
    }

    /**
     * Convert the depiction to SVG. The bond lengths are rescaled to be an appropriate length in
     * 'mm'.
     *
     * @param bondLengthInMM the length of a bond (in mm)
     * @param marginInMM     the width of a margin (in mm)
     * @return content of the SVG
     */
    String toSvg(double bondLengthInMM, double marginInMM) {
        final double rescale = rescaleForBondLength(bondLengthInMM);
        return toSvg(width() * rescale, height() * rescale, marginInMM);
    }

    String toSvg(double width, double height, double margin) {
        Graphics2D g2 = new SVGGraphics2D(0, 0,
                                          margin + width + margin,
                                          margin + height + margin);
        draw(AWTDrawVisitor.forVectorGraphics(g2),
             new Rectangle2D.Double(margin, margin, width, height));
        g2.dispose();
        return g2.toString();
    }

    private double rescaleForBondLength(double length) {
        return length / parameters.get(BasicSceneGenerator.BondLength.class);
    }

    // helper method traverses the nested rendering elements and finds all 'Bounds' instances
    static void unpackBounds(IRenderingElement element, List<Bounds> listOfBounds) {
        if (element instanceof ElementGroup)
            for (IRenderingElement child : (ElementGroup) element)
                unpackBounds(child, listOfBounds);
        else if (element instanceof Bounds)
            listOfBounds.add((Bounds) element);
    }

    static Rectangle2D getBounds(IRenderingElement primitives, IAtomContainer container) {
        // does the IRenderingElement contain a bounds hint?
        List<Bounds> listOfBounds = new ArrayList<Bounds>();
        unpackBounds(primitives, listOfBounds);

        double[] xyLimits = new double[4];

        if (listOfBounds.isEmpty()) {
            xyLimits = GeometryUtil.getMinMax(container);
        }
        else {
            xyLimits[0] = xyLimits[1] = Double.MAX_VALUE;
            xyLimits[2] = xyLimits[3] = -Double.MAX_VALUE;
            for (Bounds bounds : listOfBounds) {
                xyLimits[0] = Math.min(xyLimits[0], bounds.minX);
                xyLimits[1] = Math.min(xyLimits[1], bounds.minY);
                xyLimits[2] = Math.max(xyLimits[2], bounds.maxX);
                xyLimits[3] = Math.max(xyLimits[3], bounds.maxY);
            }
        }

        return new Rectangle2D.Double(xyLimits[0],
                                      xyLimits[1],
                                      xyLimits[2] - xyLimits[0],
                                      xyLimits[3] - xyLimits[1]);
    }


}
