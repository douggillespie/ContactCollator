package contactcollator.localisations;

import Localiser.algorithms.locErrors.EllipticalError;
import Localiser.algorithms.locErrors.LocaliserError;
import PamUtils.LatLong;

/**
 * Summary of 2D localisation data from a contact. 
 * doesn't extendAbstractLocalisation since it won't have a data unit at this point. 
 * will create a new localisation object to wrap this when or if it's used. 
 */
public class LocalisationSummary {

	private LatLong latLong;
	
	private LocaliserError localiserError;

	private int hydrophoneMap;

	/**
	 * @param latLong
	 * @param localiserError
	 */
	public LocalisationSummary(LatLong latLong, LocaliserError localiserError, int hydrophoneMap) {
		super();
		this.latLong = latLong;
		this.localiserError = localiserError;
		this.setHydrophoneMap(hydrophoneMap);
	}

	/**
	 * @return the latLong
	 */
	public LatLong getLatLong() {
		return latLong;
	}

	/**
	 * @param latLong the latLong to set
	 */
	public void setLatLong(LatLong latLong) {
		this.latLong = latLong;
	}

	/**
	 * @return the localiserError
	 */
	public LocaliserError getLocaliserError() {
		return localiserError;
	}

	/**
	 * @param localiserError the localiserError to set
	 */
	public void setLocaliserError(LocaliserError localiserError) {
		this.localiserError = localiserError;
	}

	@Override
	public String toString() {
		return String.format("%s Error %s", latLong, localiserError);
	}

	public int getHydrophoneMap() {
		return hydrophoneMap;
	}

	public void setHydrophoneMap(int hydrophoneMap) {
		this.hydrophoneMap = hydrophoneMap;
	}
	
}
