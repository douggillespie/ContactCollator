package contactcollator.localisations;

import java.util.ArrayList;

import PamguardMVC.PamDataUnit;

/**
 * Something to return from the getLocalisedUnits function in BasicLocalisationSummariser
 * that groups all units in a contact into three groups, those that have a 2D or 3D localisation
 * those that just have bearing, and those with nothing. 
 */
public class LocalisationSets {

	private ArrayList<PamDataUnit> localised;
	
	private ArrayList<PamDataUnit> bearinged;
	
	private ArrayList<PamDataUnit> nothing;

	/**
	 * @param localised
	 * @param bearinged
	 * @param nothing
	 */
	public LocalisationSets(ArrayList<PamDataUnit> localised, ArrayList<PamDataUnit> bearinged,
			ArrayList<PamDataUnit> nothing) {
		super();
		this.localised = localised;
		this.bearinged = bearinged;
		this.nothing = nothing;
	}
	
//	/**
//	 * Remove duplicates since a lot of the entries with a crossed bearing will be in twice. 
//	 * @param dataList
//	 * @return
//	 */
//	public ArrayList<PamDataUnit> removeDuplicates(ArrayList<PamDataUnit> dataList) {
//		
//	}

	/**
	 * @return the data units that have a 2D or 3D localisation with a LatLong
	 */
	public ArrayList<PamDataUnit> getLocalised() {
		return localised;
	}

	/**
	 * @return the data units that only have a bearing localisation, not a lat long
	 */
	public ArrayList<PamDataUnit> getBearinged() {
		return bearinged;
	}

	/**
	 * @return the data units that don't have any localisation information
	 */
	public ArrayList<PamDataUnit> getNothing() {
		return nothing;
	}
}
