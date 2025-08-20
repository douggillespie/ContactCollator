package contactcollator.trigger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import PamguardMVC.PamDataUnit;
import contactcollator.CollatorParamSet;

public class CountingTrigger implements CollatorTrigger {
	
	private CollatorParamSet parameterSet;
	
	private List<PamDataUnit> history;

	private boolean inActiveTriggerState;
	
	public CountingTrigger(CollatorParamSet parameterSet) {
		super();
		this.parameterSet = parameterSet;
		history = new LinkedList<>();
		inActiveTriggerState = false;
	}

	@Override
	public CollatorTriggerData newData(PamDataUnit dataUnit) {
		synchronized (history) {
			history.add(dataUnit);
			//System.out.println("Received new data. Current history length: "+history.size());
			//System.out.println(" 	New data timestamp: "+dataUnit.getTimeMilliseconds());
			if(this.inActiveTriggerState) {
				//System.out.println(" 	Currently in active trigger state.");
				if(!shouldStayInTriggerState(dataUnit)) {
					//System.out.println(" 	Received goahead to step trigger state");
					clearListTo(dataUnit.getTimeMilliseconds());
					this.inActiveTriggerState = false;
				}
			}else {
				//System.out.println(" 	Currently not in active trigger state.");
				clearListTo(dataUnit.getTimeMilliseconds() - (long) (parameterSet.triggerIntervalS*1000.));
			}
			
			
//			System.out.printf("Have %d in history, started with %d and removed %d (history length %3.1fs)\n", history.size(), n1, n1-n2, parameterSet.triggerIntervalS);
		
			if (history.size() < parameterSet.triggerCount) {
				//System.out.println(" 	History size is too small. Current size: "+history.size());
				this.inActiveTriggerState = false;
				return null;
			}
			else {
				/*
				 *  return trigger data, noting that another class is going to be responsible for deciding 
				 *  if it's too soon to save another new data unit (or update?) 
				 */
				this.inActiveTriggerState = true;
				long start = history.get(0).getTimeMilliseconds();
				long end = dataUnit.getEndTimeInMilliseconds();
				long lastPossibleEndTime = start + (long) (parameterSet.triggerIntervalS*1000.);
				List<PamDataUnit> histClone = new ArrayList<>(history);
				//System.out.println(" 	Creating new trigger data with start set to "+start+" and end set to "+end);
				CollatorTriggerData trigData = new CollatorTriggerData(start, end, lastPossibleEndTime, parameterSet.detectionSource, histClone);
				return trigData;
			}
		}
	}
	
	private boolean shouldStayInTriggerState(PamDataUnit dataUnit) {
		long timeFromFirstInTrigger;
		if(this.history.size()==0) {
			return false;
		}
		timeFromFirstInTrigger = dataUnit.getEndTimeInMilliseconds()-history.get(0).getTimeMilliseconds();
		
		if(timeFromFirstInTrigger<(long) (parameterSet.triggerIntervalS*1000.)){
			return true;
		}
		return false;
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
				if (next.getTimeMilliseconds() < minTimeMilliseconds) {
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
