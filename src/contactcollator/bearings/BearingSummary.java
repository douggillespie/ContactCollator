package contactcollator.bearings;

import pamMaths.PamVector;

public class BearingSummary{

	private int nPoints;
	private PamVector worldVector;
	private double meanHeading;
	private double stdHeading;
	private int hydrophoneMap;
	private boolean ambiguity;

	public BearingSummary(int nPoints, PamVector meanVector, double meanHeading, double stdHeading, int hydrophoneMap, boolean ambiguity) {
		this.nPoints = nPoints;
		this.worldVector = meanVector;
		this.meanHeading = meanHeading;
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
	 * @return the meanHeading
	 */
	public double getMeanHeading() {
		return meanHeading;
	}

	/**
	 * @return the stdHeading
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
