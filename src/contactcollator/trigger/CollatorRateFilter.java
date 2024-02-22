package contactcollator.trigger;

import contactcollator.CollatorParamSet;

/**
 * Class to limit the rate that these things can go through at. 
 * Still need to think about how these are going to work - do we send updates if data are 
 * from the same sequence, or nothing ?
 * Guessing that if enough time has passes, we send an update if there are common data units with 
 * the previous one and if there isn't an overlap, then it's a new data.  
 * @author dg50
 *
 */
public class CollatorRateFilter {

	public static final int TRIGGER_DONTSEND = 0;
	public static final int TRIGGER_SENDDATA = 1;
	public static final int TRIGGER_SENDUPDATE = 2;
	
	private CollatorTriggerData lastSent = null;
	private CollatorTriggerData lastUpdate = null;
	
	
	
	public int judgeTriggerData(CollatorParamSet paramSet, CollatorTriggerData triggerData) {
		// keep it really simple for now. If it overlaps, update, otherwise send new.
		if (triggerData == null) {
			return TRIGGER_DONTSEND;
		}
		if (lastSent != null) {
			if (triggerData.getStartTime() > lastSent.getEndTime() && 
					triggerData.getStartTime() > lastSent.getStartTime() + 
					paramSet.minimumUpdateIntervalS*1000.) {
				lastSent = lastUpdate = triggerData;
				return TRIGGER_SENDDATA;
			}
		}
		if (lastUpdate != null) {
			if (triggerData.getStartTime() <= lastUpdate.getEndTime()) {
				lastUpdate = triggerData;
				return TRIGGER_SENDUPDATE;
			}
		}
		
		lastSent = lastUpdate = triggerData;
		return TRIGGER_SENDDATA;
	}
}
