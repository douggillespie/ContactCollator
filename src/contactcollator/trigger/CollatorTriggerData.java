package contactcollator.trigger;

import java.util.ArrayList;
import java.util.List;

import PamUtils.PamCalendar;
import PamguardMVC.PamDataUnit;

public class CollatorTriggerData implements Cloneable {

	private long startTime, endTime, lastPossibleEndTime;
	private List<PamDataUnit> dataList;
	private String triggerName;
	private CollatorTriggerData newData;
	
	public CollatorTriggerData(long startTime, long endTime, long lastPossibleEndTime, String triggerName, List<PamDataUnit> dataList) {
		super();
		this.startTime = startTime;
		this.endTime = endTime;
		this.triggerName = triggerName;
		this.dataList = dataList;
		this.setLastPossibleEndTime(lastPossibleEndTime);
	}
	
	public CollatorTriggerData(long startTime, long endTime, String triggerName, List<PamDataUnit> dataList) {
		super();
		this.startTime = startTime;
		this.endTime = endTime;
		this.triggerName = triggerName;
		this.dataList = dataList;
		this.setLastPossibleEndTime(endTime);
	}
	
	/**
	 * Get bitmap of all data in list. 
	 * @return
	 */
	public int getChannelBitmap() {
		if (dataList == null) {
			return 0;
		}
		int b = 0;
		for (PamDataUnit du : dataList) {
			b |= du.getChannelBitmap();
		}
		return b;
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

	public long getLastPossibleEndTime() {
		return lastPossibleEndTime;
	}

	public void setLastPossibleEndTime(long lastPossibleEndTime) {
		this.lastPossibleEndTime = lastPossibleEndTime;
	}

	@Override
	public String toString() {
		return String.format("Trigger %s %s - %s with %d datas", triggerName, 
				PamCalendar.formatDBDateTime(startTime), PamCalendar.formatTime(endTime), dataList.size());
	}

	@Override
	public CollatorTriggerData clone() {
		try {
			List<PamDataUnit> aDataList = dataList;
			if (aDataList != null) {
				aDataList = new ArrayList<PamDataUnit>(dataList);
			}
			newData = (CollatorTriggerData) super.clone();
			newData.dataList = aDataList;
			
			return newData;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
