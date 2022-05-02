package contactcollator.bearings;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import PamView.PamSymbolType;
import PamView.symbol.SymbolData;

/**
 * Functions to paint heading histograms (swing)
 * @author dg50
 *
 */
public class HeadingHistogramPaint {
	
	private SymbolData symbolData;

	/**
	 * Heading histrogram painter using the given symbol for fill, fill colour and line colour
	 * @param symbolData
	 */
	public HeadingHistogramPaint(SymbolData symbolData) {
		super();
		this.symbolData = symbolData;
	}

	/**
	 * Heading histogram with default colours and fill
	 */
	public HeadingHistogramPaint() {
		symbolData = new SymbolData(PamSymbolType.SYMBOL_NONE, 1, 1, true, Color.YELLOW, Color.BLACK);
	}
	
	/**
	 * Paint the histogram within the specified rectangle within the graphics context g. 
	 * @param g Graphics handle
	 * @param rect rectangle within graphics (can be null) 
	 * @param headingHist histogram to draw
	 */
	public void paintHistrogram(Graphics g, Rectangle rect, HeadingHistogram headingHist) {
		Graphics2D g2d = (Graphics2D) g;
		if (rect == null) {
			rect = g.getClipBounds();
		}
		int w = rect.width;
		int h = rect.height;
		int x0 = rect.x + w/2;
		int y0 = rect.y + h/2;
		/*
		 * Paint as a series of triangles using symbol colour and fill. 
		 */
		int[] x = new int[4]; // first and last elements are always x0 and y0;
		int[] y = new int[4];
		x[0] = x[3] = x0;
		y[0] = y[3] = y0;
		FontMetrics fm = g.getFontMetrics();
		if (headingHist == null || headingHist.getnData() == 0) {
			g.setColor(Color.BLACK);
			String str = "no data";
			Rectangle2D sb = fm.getStringBounds(str, g2d);
			g.drawString(str, x0-(int) (sb.getWidth()/2), y0+(int) (sb.getHeight()/2));
			return; // no data to paint. 
		}
		else {
		}
		int nBins = headingHist.getnBins();
		double[] edges = headingHist.getBinEdges(); // length of edges should be 1 more than length of data.
		double[] data = headingHist.getData();
		double max = headingHist.getDataMax();
		double scale = w/2./max;
		for (int i = 0; i < nBins; i++) {
			x[1] = x0 + (int) (Math.sin(edges[i])*data[i]*scale);
			x[2] = x0 + (int) (Math.sin(edges[i+1])*data[i]*scale);
			y[1] = y0 - (int) (Math.cos(edges[i])*data[i]*scale);
			y[2] = y0 - (int) (Math.cos(edges[i+1])*data[i]*scale);
			if (symbolData.fill) {
				g.setColor(symbolData.getFillColor());
				g.fillPolygon(x, y, 4);
			}
			g.setColor(symbolData.getLineColor());
			g.drawPolygon(x, y, 4);
//			g.draw
		}
		g.setColor(Color.BLACK);
		String str = String.format("max %3.1f", max);
		Rectangle2D sb = fm.getStringBounds(str, g2d);
		g.drawString(str, w-(int) (sb.getWidth()), rect.y+(int) (sb.getHeight())); // top right corner
	}

	/**
	 * @return the symbolData
	 */
	public SymbolData getSymbolData() {
		return symbolData;
	}

	/**
	 * @param symbolData the symbolData to set
	 */
	public void setSymbolData(SymbolData symbolData) {
		this.symbolData = symbolData;
	}

}
