package contactcollator.trigger;

import PamguardMVC.PamDataUnit;

public interface CollatorTrigger {

	/**
	 * Add new data to the trigger. If it's not time to trigger output, then 
	 * null is returned, otherwise an object with necessary information. 
	 * @param dataUnit new data unit from detector
	 * @return null or trigger data
	 */
	public CollatorTriggerData newData(PamDataUnit dataUnit);
	
	/**
	 * Reset (e.g. after a PAMGuard restart)
	 */
	public void reset();
	
}
