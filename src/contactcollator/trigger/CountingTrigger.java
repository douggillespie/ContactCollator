package contactcollator.trigger;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import PamguardMVC.PamDataUnit;
import contactcollator.CollatorParamSet;

public class CountingTrigger implements CollatorTrigger {
	
	private CollatorParamSet parameterSet;
	
	private List<PamDataUnit> history;

	public CountingTrigger(CollatorParamSet parameterSet) {
		super();
		this.parameterSet = parameterSet;
		history = new LinkedList<>();
	}

	@Override
	public CollatorTriggerData newData(PamDataUnit dataUnit) {
		clearListTo((long) (dataUnit.getTimeMilliseconds() - parameterSet.triggerIntervalS*1000));
		synchronized (history) {
			history.add(dataUnit);
		}
		if (history.size() < parameterSet.triggerCount) {
			return null;
		}
		else {
			/*
			 *  return trigger data, noting that another class is going to be responsible for deciding 
			 *  if it's too soon to save another new data unit (or update?) 
			 */
			long start = history.get(0).getTimeMilliseconds();
			long end = dataUnit.getEndTimeInMilliseconds();
			List<PamDataUnit> histClone = new LinkedList<>(history);
			CollatorTriggerData trigData = new CollatorTriggerData(start, end, parameterSet.detectionSource, histClone);
			return trigData;
		}
	}

	/**
	 * Clear the data history up to the given time. 
	 * @param minTimeMilliseconds  min time in milliseconds. Any earlier data will be rmeoved from the list. 
	 */
	private void clearListTo(long minTimeMilliseconds) {
		synchronized (history) {
			ListIterator<PamDataUnit> it = history.listIterator();
			while (it.hasNext()) {
				PamDataUnit next = it.next();
				if (next.getTimeMilliseconds() >= minTimeMilliseconds) {
					break;
				}
				else {
					it.remove();
				}
			}
			
		}
		
	}

	@Override
	public void reset() {
		synchronized (history) {
			history.clear();
		}		
	}

}
