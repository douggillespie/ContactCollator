package contactcollator.trigger;

import java.util.List;

import PamguardMVC.PamDataUnit;

public class CollatorTriggerData {

	private long startTime, endTime;
	private List<PamDataUnit> dataList;
	
	public CollatorTriggerData(long startTime, long endTime, List<PamDataUnit> dataList) {
		super();
		this.startTime = startTime;
		this.endTime = endTime;
		this.dataList = dataList;
	}

	/**
	 * @return the startTime
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @return the endTime
	 */
	public long getEndTime() {
		return endTime;
	}

	/**
	 * @return the dataList
	 */
	public List<PamDataUnit> getDataList() {
		return dataList;
	}
	
}
