package contactcollator;

import PamController.PamController;
import PamDetection.RawDataUnit;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;
import PamguardMVC.dataSelector.DataSelector;
import contactcollator.trigger.CollatorTrigger;
import contactcollator.trigger.CollatorTriggerData;
import contactcollator.trigger.CountingTrigger;

public class CollatorStreamProcess extends PamProcess {

	private CollatorControl collatorControl;
	private CollatorDataBlock collatorBlock;
	private CollatorParamSet parameterSet;
	private RawDataObserver rawDataObserver;
	private PamRawDataBlock rawDataBlock;
	private PamRawDataBlock rawDataCopy;
	private PamDataBlock detectorDatablock;
	private DataSelector detectionDataSelector;
	private CollatorTrigger collatorTrigger;

	public CollatorStreamProcess(CollatorControl collatorControl, CollatorDataBlock collatorBlock, CollatorParamSet parameterSet) {
		super(collatorControl, null);
		this.collatorControl = collatorControl;
		this.collatorBlock = collatorBlock;
		this.parameterSet = parameterSet;
		rawDataObserver = new RawDataObserver();
		/*
		 *  make an internal copy of the data, it will not use much memory and means we don't have to
		 *  lock the main raw data block for too long while preparing output 
		 */		
		rawDataCopy = new PamRawDataBlock("internal copy", this, 0, sampleRate);
	}

	@Override
	public void pamStart() {
		// TODO Auto-generated method stub

	}

	@Override
	public void pamStop() {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void newData(PamObservable o, PamDataUnit dataUnit) {
		// see if we actually want it using the data selector
		if (wantDetectionData(dataUnit)) {
			useDetectionData(dataUnit);
		}
	}
	
	/**
	 * Use the data selector built into the detection datablock to see if we want the incoming data. 
	 * @param dataUnit
	 * @return true if it scores OK
	 */
	private boolean wantDetectionData(PamDataUnit dataUnit) {
		if (detectionDataSelector != null) {
			double score = detectionDataSelector.scoreData(dataUnit);
			return score > 0;
		}
		return true;
	}

	/**
	 * Now use the data, potentially even creating output
	 * @param dataUnit incoming detection data unit. 
	 */
	private void useDetectionData(PamDataUnit dataUnit) {
		CollatorTriggerData trigger = collatorTrigger.newData(dataUnit);
		if (trigger != null) {
			// we want to do something
			newDetectionTrigger(trigger, dataUnit);
		}
	}

	/**
	 * the system has triggered and decided it want's to make an output data unit
	 * @param trigger trigger information
	 * @param dataUnit last data unit that went into the trigger. 
	 */
	private void newDetectionTrigger(CollatorTriggerData trigger, PamDataUnit dataUnit) {
		// TODO Auto-generated method stub
		
	}

	public String getSetName() {
		return parameterSet.setName;
	}

	/**
	 * Set parameters and if necessary update the process settings (datablocks, etc) 
	 * @param paramSet
	 */
	public void setParameters(CollatorParamSet paramSet) {
		this.parameterSet = paramSet;
		collatorTrigger = new CountingTrigger(parameterSet);
	}
	
	private class RawDataObserver implements PamObserver {

		@Override
		public long getRequiredDataHistory(PamObservable observable, Object arg) {
			return 0;
		}

		@Override
		public void addData(PamObservable observable, PamDataUnit pamDataUnit) {
			/**
			 * Make a copy of the data (not the raw data) into a local array. won't need much memory, so OK. 
			 */
			RawDataUnit in = (RawDataUnit) pamDataUnit;
			RawDataUnit copy = new RawDataUnit(in.getTimeMilliseconds(), in.getChannelBitmap(), in.getStartSample(), in.getSampleDuration());
			copy.setRawData(in.getRawData());
			rawDataCopy.addPamData(copy);			
		}

		@Override
		public void updateData(PamObservable observable, PamDataUnit pamDataUnit) {
		}

		@Override
		public void removeObservable(PamObservable observable) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setSampleRate(float sampleRate, boolean notify) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void noteNewSettings() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getObserverName() {
			return collatorControl.getUnitName() + " " + parameterSet.setName;
		}

		@Override
		public void masterClockUpdate(long milliSeconds, long sampleNumber) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public PamObserver getObserverObject() {
			return this;
		}

		@Override
		public void receiveSourceNotification(int type, Object object) {
			
		}
		
	}

	@Override
	public void setupProcess() {
		
		detectorDatablock = PamController.getInstance().getDataBlockByLongName(parameterSet.detectionSource);
		setParentDataBlock(detectorDatablock);
		
		rawDataBlock = PamController.getInstance().getRawDataBlock(parameterSet.rawDataSource);
		if (rawDataBlock != null) {
			rawDataBlock.addObserver(rawDataObserver, false);
			rawDataCopy.setChannelMap(rawDataBlock.getChannelMap());
			rawDataCopy.setSampleRate(rawDataBlock.getSampleRate(), false);
			rawDataCopy.setNaturalLifetimeMillis((int) ((parameterSet.outputClipLengthS*1.1) * 1000));
		}
		
		super.setupProcess();
	}

	@Override
	public void prepareProcess() {
		rawDataCopy.clearAll();
		
		if (detectorDatablock == null) {
			return;
		}
		detectionDataSelector = detectorDatablock.getDataSelector(collatorControl.getDataSelectorName(getSetName()), false);
		
		collatorTrigger = new CountingTrigger(parameterSet);
		
		super.prepareProcess();
	}

	@Override
	public void destroyProcess() {
		/*
		 * super class should remove this observer from data block 
		 */
		super.destroyProcess();
		/*
		 * Stop the raw data monitor looking at it too ... 
		 */
		if (rawDataBlock != null) {
			rawDataBlock.deleteObserver(rawDataObserver);
			rawDataBlock = null;
		}
		
	}

	@Override
	public String getProcessName() {
		return parameterSet.setName;
	}

}
