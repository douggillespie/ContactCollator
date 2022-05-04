package contactcollator.bearings;

import pamMaths.PamVector;

public class BearingSummary{

	private int nPoints;
	private PamVector worldVector;
	private double stdHeading;
	private int hydrophoneMap;
	private boolean ambiguity;

	public BearingSummary(int nPoints, PamVector meanVector, double stdHeading, int hydrophoneMap, boolean ambiguity) {
		this.nPoints = nPoints;
		this.worldVector = meanVector;
		this.stdHeading = stdHeading;
		this.hydrophoneMap = hydrophoneMap;
		this.ambiguity = ambiguity;
	}

	/**
	 * @return the nPoints
	 */
	public int getnPoints() {
		return nPoints;
	}

	/**
	 * Vector in the reference frame of the hydrophone array. 
	 * @return the worldVector
	 */
	public PamVector getWorldVector() {
		return worldVector;
	}

	/**
	 * @return the meanHeading in radians clockwise from ahead. 
	 */
	public double getMeanHeading() {
		return worldVector.getHeading();
	}

	/**
	 * @return the Standard Deviation of the headings in radians. 
	 */
	public double getStdHeading() {
		return stdHeading;
	}

	/**
	 * @return the hydrophoneMap
	 */
	public int getHydrophoneMap() {
		return hydrophoneMap;
	}

	/**
	 * @return the ambiguity
	 */
	public boolean isAmbiguity() {
		return ambiguity;
	}

}
