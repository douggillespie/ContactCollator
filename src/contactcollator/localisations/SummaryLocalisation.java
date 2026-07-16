package contactcollator.localisations;

import Localiser.algorithms.locErrors.LocaliserError;
import PamDetection.AbstractLocalisation;
import PamDetection.LocContents;
import PamUtils.LatLong;
import PamguardMVC.PamDataUnit;

public class SummaryLocalisation extends AbstractLocalisation {

	private LocalisationSummary localisationSummary;

	/**
	 * @return the localisationSummary
	 */
	public LocalisationSummary getLocalisationSummary() {
		return localisationSummary;
	}

	public SummaryLocalisation(PamDataUnit pamDataUnit, LocalisationSummary localisationSummary, int referenceHydrophone) {
		super(pamDataUnit, LocContents.HAS_LATLONG, referenceHydrophone);
		this.localisationSummary = localisationSummary;
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
	
	

}
