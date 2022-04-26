package contactcollator.bearings;

import PamDetection.AbstractLocalisation;
import PamDetection.LocContents;
import PamguardMVC.PamDataUnit;
import pamMaths.PamVector;

public class BearingSummaryLocalisation extends AbstractLocalisation {

	private BearingSummary bearingSummary;

	public BearingSummaryLocalisation(PamDataUnit pamDataUnit, BearingSummary bearingSummary) {
		super(pamDataUnit, LocContents.HAS_BEARING, bearingSummary.getHydrophoneMap());
		this.bearingSummary = bearingSummary;
	}

	/**
	 * @return the bearingSummary
	 */
	public BearingSummary getBearingSummary() {
		return bearingSummary;
	}

	@Override
	public PamVector[] getWorldVectors() {
		PamVector[] vecs = {bearingSummary.getWorldVector()};
		return vecs;
	}

	@Override
	public double[] getAngles() {
		double[] angs = {bearingSummary.getMeanHeading()};
		return angs;
		
	}

	@Override
	public double getBearingReference() {
		return super.getBearingReference();
	}

	@Override
	public double getBearingError(int iSide) {
		return bearingSummary.getStdHeading();
	}

	@Override
	public int getAmbiguityCount() {
		return bearingSummary.isAmbiguity() ? 2 : 1;
	}

}
