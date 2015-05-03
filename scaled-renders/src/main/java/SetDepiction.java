import de.erichseifert.vectorgraphics2d.PDFGraphics2D;
import de.erichseifert.vectorgraphics2d.SVGGraphics2D;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.renderer.elements.Bounds;
import org.openscience.cdk.renderer.elements.IRenderingElement;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;
import org.openscience.cdk.renderer.visitor.IDrawVisitor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * @author John May
 */
public class SetDepiction extends AbstractDepiction {

    public static final double ACS_1996_BOND_LENGTH_MM = 5.08;
    public static final double ACS_1996_MARGIN_MM      = 0.56;

    private final List<IAtomContainer>    containers;
    private final List<IRenderingElement> diagrams;
    private final List<Rectangle2D>       bounds;

    private double width, height;

    private Layout layout = Layout.Horizontal;

    enum Layout {
        Vertical,
        Horizontal,
        Grid
    }

    SetDepiction(List<IRenderingElement> diagrams, List<IAtomContainer> containers, RendererModel params) {

        this.diagrams = diagrams;
        this.containers = containers;
        this.bounds = new ArrayList<Rectangle2D>();
        for (int i = 0; i < diagrams.size(); i++)
            bounds.add(getBounds(diagrams.get(i), containers.get(i)));
        this.parameters = params;
        this.scale = params.get(BasicSceneGenerator.Scale.class);
        this.margin = params.get(BasicSceneGenerator.Margin.class);
        setLayout(Layout.Horizontal);
    }

    void setLayout(final Layout layout) {
        this.layout = layout;
        switch (layout) {
            case Horizontal:
                width = 0;
                height = 0;
                for (Rectangle2D bound : bounds) {
                    width += bound.getWidth();
                    height = Math.max(height, bound.getHeight());
                }
                break;
            case Vertical:
                width = 0;
                height = 0;
                for (Rectangle2D bound : bounds) {
                    height += bound.getHeight();
                    width = Math.max(width, bound.getWidth());
                }
                break;
            case Grid:
                // more complicated, possibly need parameters
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override double width() {
        return width * scale;
    }

    @Override double height() {
        return height * scale;
    }

    @Override protected void draw(AWTDrawVisitor visitor, Rectangle2D viewBounds) {
        throw new UnsupportedOperationException();
    }

    @Override BufferedImage toBufferedImage(double width, double height) {

        double x = margin;
        double y = margin;

        double xZoom = width / width();
        double yZoom = height / height();

        double zoom = Math.min(xZoom, yZoom);
        
        BufferedImage img; 
        Graphics2D g2;

        switch (layout) {
            case Horizontal:
                img = new BufferedImage((int) (margin + width + (2 * margin * (bounds.size() - 1))),
                                        (int) (margin + height + margin),
                                        BufferedImage.TYPE_3BYTE_BGR);
                g2 = img.createGraphics();
                g2.setBackground(parameters.get(BasicSceneGenerator.BackgroundColor.class));
                g2.clearRect(0, 0, img.getWidth(), img.getHeight());
                for (int i = 0; i < bounds.size(); i++) {
                    double w = zoom * bounds.get(i).getWidth() * scale;
                    double h = zoom * bounds.get(i).getHeight() * scale;
                    y = margin + ((height - h) / 2);
                    draw(AWTDrawVisitor.forVectorGraphics(g2),
                         new Rectangle2D.Double(x, y, w, h),
                         bounds.get(i),
                         diagrams.get(i));
                    x += (2 * margin) + w;
                }
                break;
            case Vertical:
                img = new BufferedImage((int) (margin + width + margin),
                                        (int) (margin + height + (2 * margin * (bounds.size() - 1))),
                                        BufferedImage.TYPE_3BYTE_BGR);
                g2 = img.createGraphics();
                g2.setBackground(parameters.get(BasicSceneGenerator.BackgroundColor.class));
                g2.clearRect(0, 0, img.getWidth(), img.getHeight());
                for (int i = 0; i < bounds.size(); i++) {
                    double w = zoom * bounds.get(i).getWidth() * scale;
                    double h = zoom * bounds.get(i).getHeight() * scale;
                    x = margin + ((width - w) / 2);
                    draw(AWTDrawVisitor.forVectorGraphics(g2),
                         new Rectangle2D.Double(x, y, w, h),
                         bounds.get(i),
                         diagrams.get(i));
                    y += (2 * margin) + h;
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }

        g2.dispose();
        return img;
    }

    @Override String toPdf(double width, double height, double margin) {
        double x = margin;
        double y = margin;

        double xZoom = width / width();
        double yZoom = height / height();

        double zoom = Math.min(xZoom, yZoom);

        Graphics2D g2;

        switch (layout) {
            case Horizontal:
                g2 = new PDFGraphics2D(0, 0,
                                       margin + width + (2 * margin * (bounds.size() - 1)),
                                       margin + height + margin);
                for (int i = 0; i < bounds.size(); i++) {
                    double w = zoom * bounds.get(i).getWidth() * scale;
                    double h = zoom * bounds.get(i).getHeight() * scale;
                    y = margin + ((height - h) / 2);
                    draw(AWTDrawVisitor.forVectorGraphics(g2),
                         new Rectangle2D.Double(x, y, w, h),
                         bounds.get(i),
                         diagrams.get(i));
                    x += (2 * margin) + w;
                }
                break;
            case Vertical:
                g2 = new PDFGraphics2D(0, 0,
                                       margin + width + margin,
                                       margin + height + (2 * margin * (bounds.size() - 1)));
                for (int i = 0; i < bounds.size(); i++) {
                    double w = zoom * bounds.get(i).getWidth() * scale;
                    double h = zoom * bounds.get(i).getHeight() * scale;
                    x = margin + ((width - w) / 2);
                    draw(AWTDrawVisitor.forVectorGraphics(g2),
                         new Rectangle2D.Double(x, y, w, h),
                         bounds.get(i),
                         diagrams.get(i));
                    y += (2 * margin) + h;
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }

        g2.dispose();
        return g2.toString();
    }
    
    
    @Override String toSvg(double width, double height, double margin) {
        double x = margin;
        double y = margin;

        double xZoom = width / width();
        double yZoom = height / height();

        double zoom = Math.min(xZoom, yZoom);

        Graphics2D g2;

        switch (layout) {
            case Horizontal:
                g2 = new SVGGraphics2D(0, 0,
                                       margin + width + (2 * margin * (bounds.size() - 1)),
                                       margin + height + margin);
                for (int i = 0; i < bounds.size(); i++) {
                    double w = zoom * bounds.get(i).getWidth() * scale;
                    double h = zoom * bounds.get(i).getHeight() * scale;
                    y = margin + ((height - h) / 2);
                    draw(AWTDrawVisitor.forVectorGraphics(g2),
                         new Rectangle2D.Double(x, y, w, h),
                         bounds.get(i),
                         diagrams.get(i));
                    x += (2 * margin) + w;
                }
                break;
            case Vertical:
                g2 = new SVGGraphics2D(0, 0,
                                       margin + width + margin,
                                       margin + height + (2 * margin * (bounds.size() - 1)));
                for (int i = 0; i < bounds.size(); i++) {
                    double w = zoom * bounds.get(i).getWidth() * scale;
                    double h = zoom * bounds.get(i).getHeight() * scale;
                    x = margin + ((width - w) / 2);
                    draw(AWTDrawVisitor.forVectorGraphics(g2),
                         new Rectangle2D.Double(x, y, w, h),
                         bounds.get(i),
                         diagrams.get(i));
                    y += (2 * margin) + h;
                }
                break;
            default:
                throw new UnsupportedOperationException();
        }

        g2.dispose();
        return g2.toString();
    }
}
