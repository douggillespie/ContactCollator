package contactcollator.localisations;

import Localiser.algorithms.locErrors.LocaliserError;
import PamUtils.LatLong;

/**
 * Summary of 2D localisation data from a contact. 
 * doesn't extendAbstractLocalisation since it won't have a data unit at this point. 
 * will create a new localisation object to wrap this when or if it's used. 
 */
public class LocalisationSummary {

	private long startMillis, endMillis; // start and end times for units contributing to this
	
	private LocalisationType localisationType;
	
	private LatLong latLong;
	
	private LocaliserError localiserError;
	
	/**
	 * Mean bearing to animal as clockwise bearing from north, in radians. 
	 */
	private Double meanBearing;

	private int hydrophoneMap;

	private int nEntries;

	/**
	 * @param startMillis
	 * @param endMillis
	 * @param latLong
	 * @param localiserError
	 * @param nEntries 
	 */
	public LocalisationSummary(long startMillis, long endMillis, LatLong latLong, LocaliserError localiserError, int hydrophoneMap, int nEntries) {
		super();
		this.localisationType = LocalisationType.POSITION;
		this.startMillis = startMillis;
		this.endMillis = endMillis;
		this.latLong = latLong;
		this.localiserError = localiserError;
		this.setHydrophoneMap(hydrophoneMap);
		this.nEntries = nEntries;
	}
	
	/**
	 * 
	 * @param startMillis
	 * @param endMillis
	 * @param meanBearing
	 * @param localiserError
	 * @param hydrophoneMap
	 * @param nEntries
	 */
	public LocalisationSummary(long startMillis, long endMillis, double meanBearing, LocaliserError localiserError, int hydrophoneMap, int nEntries) {
		super();
		this.localisationType = LocalisationType.BEARING;
		this.startMillis = startMillis;
		this.endMillis = endMillis;
		this.meanBearing = meanBearing;
		this.localiserError = localiserError;
		this.setHydrophoneMap(hydrophoneMap);
		this.nEntries = nEntries;
	}

	public LocalisationSummary(long startMillis, long endMillis,int hydrophoneMap, int nEntries) {
		this.localisationType = LocalisationType.NONE;
		this.startMillis = startMillis;
		this.endMillis = endMillis;
		this.setHydrophoneMap(hydrophoneMap);
		this.nEntries = nEntries;
		
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

	/**
	 * @return the startMillis
	 */
	public long getStartMillis() {
		return startMillis;
	}

	/**
	 * @param startMillis the startMillis to set
	 */
	public void setStartMillis(long startMillis) {
		this.startMillis = startMillis;
	}

	/**
	 * @return the endMillis
	 */
	public long getEndMillis() {
		return endMillis;
	}

	/**
	 * @param endMillis the endMillis to set
	 */
	public void setEndMillis(long endMillis) {
		this.endMillis = endMillis;
	}

	/**
	 * @return the nEntries
	 */
	public int getnEntries() {
		return nEntries;
	}

	/**
	 * @param nEntries the nEntries to set
	 */
	public void setnEntries(int nEntries) {
		this.nEntries = nEntries;
	}

	public Double getMeanBearing() {
		return meanBearing;
	}

	public LocalisationType getLocalisationType() {
		return localisationType;
	}
	
}
