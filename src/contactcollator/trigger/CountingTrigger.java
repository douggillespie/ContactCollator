package contactcollator.trigger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
		int n1 = history.size();
		clearListTo(dataUnit.getTimeMilliseconds() - (long) (parameterSet.triggerIntervalS*1000.));
		int n2 = history.size();
		synchronized (history) {
//			history.add(dataUnit);
			insertNew(history, dataUnit);
			//			System.out.printf("Have %d in history, started with %d and removed %d (history length %3.1fs)\n", history.size(), n1, n1-n2, parameterSet.triggerIntervalS);
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
			List<PamDataUnit> histClone = new ArrayList<>(history);
			CollatorTriggerData trigData = new CollatorTriggerData(start, end, parameterSet.detectionSource, histClone);
			return trigData;
		}
	}

	/**
	 * Insert a new data unit into the list, but put it in the right place since
	 * the units may not be arriving in order. 
	 * @param history
	 * @param newOne
	 * @return position in the list. 
	 */ 
	private void insertNew(List<PamDataUnit> history, PamDataUnit newOne) {
		synchronized (history) {
			if (history.size() == 0) {
				history.add(newOne);
				return;
			}
			history.add(newOne);
			Collections.sort(history);
//			ListIterator<PamDataUnit> it = history.listIterator(history.size()-1);
//			int nextInd = 0;
//			while (it.hasPrevious()) {
//				if (it.previous().getTimeMilliseconds() <= newOne.getTimeMilliseconds()) {
//					nextInd = it.nextIndex();
//				}
//			}
//			
//			Iterator<PamDataUnit> li = history.iterator();
//			while (li.hasNext()) {
//				if (li.next().getTimeMilliseconds() > newOne.getTimeMilliseconds()) {
//					li.i
//				}
//			}

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
