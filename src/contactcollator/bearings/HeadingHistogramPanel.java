package contactcollator.bearings;

import java.awt.Dimension;
import java.awt.Graphics;

import PamView.panel.PamPanel;

public abstract class HeadingHistogramPanel extends PamPanel {

	private Dimension minDimension;
	
	public HeadingHistogramPanel(int minWidth, int minHeight) {
		this(new Dimension(minWidth, minHeight));
	}
	
	
	public HeadingHistogramPanel(Dimension minDimension) {
		super();
		this.minDimension = minDimension;
	}

	@Override
	public Dimension getMinimumSize() {
		return checkMinSize(super.getMinimumSize());
	}

	@Override
	public Dimension getPreferredSize() {
		return checkMinSize(super.getPreferredSize());
	}
	
	/*
	 * return minimum size. 
	 */
	private Dimension checkMinSize(Dimension d) {
		Dimension check = new Dimension(Math.max(d.width, minDimension.width), Math.max(d.height, minDimension.height));
		return check;
	}


	/**
	 * @return the minDimension
	 */
	public Dimension getMinDimension() {
		return minDimension;
	}


	/**
	 * @param minDimension the minDimension to set
	 */
	public void setMinDimension(Dimension minDimension) {
		this.minDimension = minDimension;
	}

	/**
	 * Set the minimum width
	 * @param minWidth
	 */
	public void setMinWidth(int minWidth) {
		minDimension.width = minWidth;
	}
	
	/**
	 * Set the minimum height. 
	 * @param minHeight
	 */
	public void setMinHeight(int minHeight) {
		minDimension.height = minHeight;
	}


	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		paintHistogram(g);
	}

	public abstract void paintHistogram(Graphics g);
	

}
