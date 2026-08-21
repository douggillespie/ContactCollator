package contactcollator.localisations;

import Localiser.algorithms.locErrors.BearingError;
import Localiser.algorithms.locErrors.LocaliserError;
import PamDetection.AbstractLocalisation;
import PamDetection.LocContents;
import PamUtils.LatLong;
import PamguardMVC.PamDataUnit;
import pamMaths.PamVector;

public class SummaryLocalisation extends AbstractLocalisation {

	private LocalisationSummary localisationSummary;

	/**
	 * @return the localisationSummary
	 */
	public LocalisationSummary getLocalisationSummary() {
		return localisationSummary;
	}

	public SummaryLocalisation(PamDataUnit pamDataUnit, LocalisationSummary localisationSummary, int referenceHydrophone) {
		super(pamDataUnit, 0, referenceHydrophone);
		this.localisationSummary = localisationSummary;
		if (localisationSummary.getLatLong() != null) {
			addLocContents(LocContents.HAS_LATLONG);
		}
		if (localisationSummary.getMeanBearing() != null) {
			addLocContents(LocContents.HAS_BEARING);
		}
	}

	@Override
	public LatLong getLatLong(int iSide) {
		return localisationSummary.getLatLong();
	}

	@Override
	public LocaliserError getLocError(int side) {
		return localisationSummary.getLocaliserError();
	}

	@Override
	public int getAmbiguityCount() {
		return 1;
	}

	@Override
	public PamVector[] getRealWorldVectors() {
		if (localisationSummary == null || localisationSummary.getMeanBearing() == null) {
			return null;
		}
		double mb = localisationSummary.getMeanBearing();
		PamVector[] vec = {new PamVector(Math.sin(mb), Math.cos(mb), 0)};
		return vec;
	}

	@Override
	public double[] getAngleErrors() {
		LocaliserError err = localisationSummary.getLocaliserError();
		if (err instanceof BearingError) {
			BearingError be = (BearingError) err;
			double[] val = {be.getErrorMagnitude()};
			return val;
		}
		else {
			return null;
		}
	}
	
	
	
	

}
