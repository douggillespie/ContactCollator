package contactcollator.localisations;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import PamDetection.AbstractLocalisation;
import PamDetection.LocContents;
import PamguardMVC.PamDataUnit;
import PamguardMVC.superdet.SuperDetection;
import contactcollator.CollatorStreamProcess;

public abstract class BasicLocalisationSummariser implements LocalisationSummariser {

	protected CollatorStreamProcess collatorStreamProcess;

	/**
	 * @param collatorStreamProcess
	 */
	public BasicLocalisationSummariser(CollatorStreamProcess collatorStreamProcess) {
		super();
		this.collatorStreamProcess = collatorStreamProcess;
	}

	/**
	 * @return the collatorStreamProcess
	 */
	public CollatorStreamProcess getCollatorStreamProcess() {
		return collatorStreamProcess;
	}
	
	/**
	 * Get a sorted set of localised units, note that these may 
	 * be super detections of the original list of units that went in. 
	 * @param dataList
	 * @return
	 */
	public Set<PamDataUnit> getLocalisedUnits(List<PamDataUnit> dataList) {
		int nUnits = dataList.size();
		int nLocs = 0;
		Set<PamDataUnit> localisedUnits = new TreeSet();
		for (PamDataUnit dataUnit : dataList) {
			PamDataUnit loc = findLatLongLocalisation(dataUnit);
			if (loc != null) {
				nLocs++;
				localisedUnits.add(loc);
			}
		}
		return localisedUnits;
	}
	/**
	 * Perform an iterative search through a detection and all it's super detections in hope of finding
	 * a latlong localisation. 
	 * <br> But return the data unit with the localisation so that we can know the time of 
	 * it too ! This may well end up being one of the superdetections, e.g. from crossed bearing localiser. 
	 * @param dataUnit
	 * @return
	 */
	public PamDataUnit findLatLongLocalisation(PamDataUnit dataUnit) {
		PamDataUnit du = dataUnit;
		AbstractLocalisation loc = du.getLocalisation();
		if (loc != null && loc.hasLocContent(LocContents.HAS_LATLONG)) {
//			System.out.printf("Found latlong %s in %s in in %s\n", loc.getLatLong(0), 
//					loc.getClass().getName(), du.getClass().getName());
			return du;
		}
		int nSuperDets = du.getSuperDetectionsCount();
		for (int i = 0; i < nSuperDets; i++) {
			SuperDetection superDet = du.getSuperDetection(i);
			PamDataUnit superloc = findLatLongLocalisation(superDet);
			if (superloc != null) {
				return superloc;
			}
		}
		return null;
	}
}
