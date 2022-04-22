package contactcollator;

import java.util.ArrayList;

import Acquisition.AcquisitionControl;
import Array.ArrayManager;
import PamController.PamControlledUnit;
import PamController.PamController;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class CollatorProcess extends PamProcess {

	private CollatorControl collatorControl;
	
	private CollatorDataBlock collatorDataBlock;
	
	private ArrayList<CollatorStreamProcess> streamProcesses = new ArrayList<>();

	public CollatorProcess(CollatorControl collatorControl) {
		super(collatorControl, null);
		this.collatorControl = collatorControl;
		collatorDataBlock = new CollatorDataBlock(collatorControl.getUnitName(), this, 0);
	}

	@Override
	public void pamStart() {
		
	}

	@Override
	public void pamStop() {
		
	}

	@Override
	public void setupProcess() {
		super.setupProcess();
		/*
		 *  could have anything in it, so set the channel map to the acquisition map.
		 *  However, this may not be subscribed to a process, so go to the array ?
		 */
		int nPhones = ArrayManager.getArrayManager().getCurrentArray().getHydrophoneCount();
		int hydrophoneMap = PamUtils.makeChannelMap(nPhones);
		collatorDataBlock.setChannelMap(hydrophoneMap);
		// sample rate is tricky, since it's going to be variable, so can't really use that !
		
		organiseStreamProcesses();
		
		// set the parent process of the main output datablock (i.e. the parent of this) to be the first 
		// acquisition or a few things don't work properly. 
		AcquisitionControl daq = (AcquisitionControl) PamController.getInstance().findControlledUnit(AcquisitionControl.unitType);
		if (daq != null) {
			setParentDataBlock(daq.getRawDataBlock());
		}
	}

	@Override
	public void prepareProcess() {
		super.prepareProcess();
	}

	/**
	 * Set up a process for every set. try to avoid adding or removing any unnecessarily. 
	 */
	private void organiseStreamProcesses() {
		ArrayList<CollatorParamSet> sets = collatorControl.getCollatorParams().parameterSets;
		// go through the sets and add any which aren't there. 
		for (CollatorParamSet aSet : sets) {
			if (findStreamProcess(aSet.setName) == null) {
				addStreamProcess(aSet);
			}
		}
		// go through the processes and remove any which shouldn't be there, or update their parameters.
		for (CollatorStreamProcess csp : streamProcesses) {
			CollatorParamSet paramSet = collatorControl.findParameterSet(csp.getSetName());
			if (paramSet == null) {
				removeStreamProcess(csp);
			}
			else {
				csp.setParameters(paramSet);
			}
		}
	}
	
	private void addStreamProcess(CollatorParamSet aSet) {
		CollatorStreamProcess newProcess = new CollatorStreamProcess(collatorControl, collatorDataBlock, aSet);
		streamProcesses.add(newProcess);
		collatorControl.addPamProcess(newProcess);
		newProcess.setupProcess();
	}

	private void removeStreamProcess(CollatorStreamProcess streamProcess) {
		streamProcess.destroyProcess();
		collatorControl.removePamProcess(streamProcess);
		streamProcesses.remove(streamProcess);
	}

	/**
	 * find a stream process. These are identified by name (I can't think of another way)
	 * @param streamName name from a parameter set
	 * @return sub process or null if not found.
	 */
	public CollatorStreamProcess findStreamProcess(String streamName) {
		try {
			for (CollatorStreamProcess sp : streamProcesses) {
				if (sp.getSetName().equals(streamName)) {
					return sp;
				}
			}
		}
		catch(NullPointerException e) {} 
		return null;
	}

	/**
	 * @return the collatorDataBlock
	 */
	public CollatorDataBlock getCollatorDataBlock() {
		return collatorDataBlock;
	}



}
