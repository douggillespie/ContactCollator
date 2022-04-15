package contactcollator.trigger;

import java.util.LinkedList;

import PamguardMVC.PamDataUnit;
import contactcollator.CollatorParamSet;

public class CountingTrigger implements CollatorTrigger {
	
	private CollatorParamSet parameterSet;
	
	private LinkedList<PamDataUnit> history;

	public CountingTrigger(CollatorParamSet parameterSet) {
		super();
		this.parameterSet = parameterSet;
		history = new LinkedList<>();
	}

	@Override
	public CollatorTriggerData newData(PamDataUnit dataUnit) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

}
