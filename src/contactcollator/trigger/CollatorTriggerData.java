package contactcollator.trigger;

import java.util.List;

import PamguardMVC.PamDataUnit;

public class CollatorTriggerData {

	private long startTime, endTime;
	private List<PamDataUnit> dataList;
	private String triggerName;
	
	public CollatorTriggerData(long startTime, long endTime, String triggerName, List<PamDataUnit> dataList) {
		super();
		this.startTime = startTime;
		this.endTime = endTime;
		this.triggerName = triggerName;
		this.dataList = dataList;
	}

	/**
	 * @return the startTime of the trigger event
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @return the endTime of the trigger event
	 */
	public long getEndTime() {
		return endTime;
	}

	/**
	 * @return the dataList : list of all data included in the trigger. 
	 */
	public List<PamDataUnit> getDataList() {
		return dataList;
	}

	/**
	 * @return the triggerName
	 */
	public String getTriggerName() {
		return triggerName;
	}
	
}
