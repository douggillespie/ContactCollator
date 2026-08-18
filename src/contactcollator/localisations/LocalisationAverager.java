package contactcollator.localisations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import Jama.Matrix;
import Localiser.algorithms.locErrors.EllipticalError;
import Localiser.algorithms.locErrors.LocaliserError;
import PamDetection.AbstractLocalisation;
import PamDetection.LocContents;
import PamUtils.LatLong;
import PamguardMVC.PamDataUnit;
import contactcollator.CollatorStreamProcess;
import contactcollator.trigger.CollatorTriggerData;
import pamMaths.PamMatrix;
import pamMaths.STD;

public class LocalisationAverager extends BasicLocalisationSummariser {

	public LocalisationAverager(CollatorStreamProcess collatorStreamProcess) {
		super(collatorStreamProcess);
		// TODO Auto-generated constructor stub
	}

	@Override
	public ContactLocalisationSummary summariseLocalisations(CollatorTriggerData trigger) {
		// get a set of units that have a latlong. 
		ArrayList<PamDataUnit> locUnits = getLocalisedUnits(trigger.getDataList());
		if (locUnits.size() == 0) {
			return null;
		}
		// double check that everything does have the latlong we need !
		int nPoint = 0;
		Iterator<PamDataUnit> it = locUnits.iterator();
		int hydrophones = 0;
		ArrayList<AbstractLocalisation> allLocs = new ArrayList();
		/**
		 * Make two identical lists now, of the units, and their localisations. 
		 */
		while (it.hasNext()) {
			PamDataUnit unit = it.next();
			AbstractLocalisation loc = unit.getLocalisation();
			if (loc == null || !loc.hasLocContent(LocContents.HAS_LATLONG)) {
				it.remove();
			}
			allLocs.add(loc);
			hydrophones |= unit.getHydrophoneBitmap();
			nPoint++;
		}
		if (locUnits.size() == 0) {
			return null;
		}
		if (locUnits.size() == 1) {
			// single unit, so just return its localisatoin data. 
			AbstractLocalisation loc = locUnits.iterator().next().getLocalisation();
			return new ContactLocalisationSummary("location Averager", 
					new LocalisationSummary(trigger.getStartTime(), trigger.getEndTime(), loc.getLatLong(0), loc.getLocError(0), hydrophones, 1));
		}
		LatLong[] lls = new LatLong[allLocs.size()];
		LocaliserError[] errs = new LocaliserError[allLocs.size()];
		int i = 0;
		for (AbstractLocalisation loc : allLocs) {
			lls[i] = loc.getLatLong(0);
			errs[i] = loc.getLocError(0);
			i++;
		}
		LocalisationSummary locSum = getAverageLocalisation(trigger.getStartTime(), trigger.getEndTime(), lls, errs, hydrophones, 10);
		return new ContactLocalisationSummary("Location Average", locSum);
		
	}

}
