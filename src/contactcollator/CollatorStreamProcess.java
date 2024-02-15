package contactcollator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import PamController.PamController;
import PamDetection.RawDataUnit;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;
import PamguardMVC.dataSelector.DataSelector;
import contactcollator.bearings.BearingSummariser;
import contactcollator.bearings.BearingSummary;
import contactcollator.bearings.BearingSummaryLocalisation;
import contactcollator.bearings.HeadingHistogram;
import contactcollator.trigger.CollatorRateFilter;
import contactcollator.trigger.CollatorTrigger;
import contactcollator.trigger.CollatorTriggerData;
import contactcollator.trigger.CountingTrigger;
import decimator.DecimatorParams;
import decimator.DecimatorProcessW;
import decimator.DecimatorWorker;

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
	private CollatorRateFilter collatorRateFilter;
	private DecimatorWorker decimator;
	private BearingSummariser bearingSummariser;
	
	private HeadingHistogram headingHistogram;

	public CollatorStreamProcess(CollatorControl collatorControl, CollatorDataBlock collatorBlock, CollatorParamSet parameterSet) {
		super(collatorControl, null);
		this.collatorControl = collatorControl;
		this.collatorBlock = collatorBlock;
		this.parameterSet = parameterSet;
		rawDataObserver = new RawDataObserver();
		bearingSummariser = new BearingSummariser();
		/*
		 *  make an internal copy of the data, it will not use much memory and means we don't have to
		 *  lock the main raw data block for too long while preparing output 
		 */		
		rawDataCopy = new PamRawDataBlock("internal copy", this, 0, sampleRate);
		collatorRateFilter = new CollatorRateFilter();
		
		headingHistogram = new HeadingHistogram(24, true);
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
		/**
		 * Fill the heading histrogram whether we're using this or not. 
		 */
		headingHistogram.addData(dataUnit);
		
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
		int option = collatorRateFilter.judgeTriggerData(parameterSet, trigger);
		if (option == CollatorRateFilter.TRIGGER_SENDDATA) {
			// sort out all the data we'll be wanting and send or update output. Probably need to decimate it, etc. 
			// can we block the datablock here ? Or do I need to clone the clone ? 
			ArrayList<RawDataUnit> cloneCopy = null;
			synchronized (rawDataCopy.getSynchLock()) {
				cloneCopy = rawDataCopy.getDataCopy();
			}
			/* 
			 * that's safe ! Freed copy, can now take time decimating those data, etc. though note that this may 
			 * still block the trigger data thread if the next stage takes more than a second or two, to may need 
			 * to make an entirely new thread to handle these final bits of the processing?? 
			 */
			CollatorDataUnit newDataUnit = createOutputData(trigger, cloneCopy, 0x1);
			if (newDataUnit == null) {
				return;
			}
			BearingSummary bearingSummary = getBearingSummary(trigger);
			if (bearingSummary != null) {
				newDataUnit.setBearingSummary(new BearingSummaryLocalisation(newDataUnit, bearingSummary));
			}
			if (headingHistogram != null) {
				newDataUnit.setHeadingHistogram(headingHistogram.clone());
				headingHistogram.reset();
			}
			
			/**
			 * Synch adding data with collatorControl, but NOT the datablock since that will really 
			 * mess up some other threading stuff by blocking the datablock users for too long
			 * leading to a lock. 
			 */
			synchronized (collatorControl) {
				collatorBlock.addPamData(newDataUnit);
			}
			collatorTrigger.reset();
//			wav = rawDataCopy.
		}
		
	}
	
	/**
	 * Get a summary of bearing information that might be in the trigger. 
	 * @param trigger
	 */
	private BearingSummary getBearingSummary(CollatorTriggerData trigger) {
		/*
		 * Note that if data in the trigger are super dets, then we should pull the
		 * bearing information from all of the sub detections. 
		 * Here we loop through the data units that made up the trigger (may only be one). 
		 * Within bearing summariser it will loop through sub detections of these data units. 
		 */
		bearingSummariser.reset();
		List<PamDataUnit> dataList = trigger.getDataList();
		for (PamDataUnit dataUnit : dataList) {
			bearingSummariser.addData(dataUnit);
		}
		return bearingSummariser.getSummary();
	}

	/**
	 * Get waveform data from the store at full sample rate. 
	 * @param trigger
	 * @param cloneCopy 
	 * @param channelMap
	 */
	private CollatorDataUnit createOutputData(CollatorTriggerData trigger, ArrayList<RawDataUnit> cloneCopy, int channelMap) {
		int nChan = PamUtils.getNumChannels(channelMap);
		double[][] wavData = new double[nChan][];
		float fs = parameterSet.outputSampleRate;

		/**
		 * Stretch the start and end times slightly if they fit within the max length
		 */
		if (cloneCopy.size() == 0) {
			return null;
		}
		long selectEnd = trigger.getEndTime();
		long selectStart = trigger.getStartTime();
		long wavEnd = cloneCopy.get(cloneCopy.size()-1).getEndTimeInMilliseconds();
		long wavStart = cloneCopy.get(0).getTimeMilliseconds();
		if (wavEnd > selectEnd) {
			selectEnd += (selectEnd-selectStart)/5;
			selectEnd = Math.min(selectEnd, wavEnd);
		}
		if (wavStart < selectStart) {
			selectStart = selectStart - (long) (selectEnd-selectStart)/5;
		}
		selectStart = Math.max(selectStart, selectEnd-(long)(parameterSet.outputClipLengthS*1000));
//		selectStart = wavStart;
//		selectEnd = wavEnd;
		// allocate more data than we need, then trim it down later. 
		int allocatedSamples = (int) ((selectEnd-selectStart) * fs / 1000.);
		if (allocatedSamples <= 0) {
			return null;
		}
//		System.out.printf("Extract %d ms from %dms available for output to %d samples\n", selectEnd-selectStart, wavEnd-wavStart, allocatedSamples);
		for (int i = 0; i < nChan; i++) {
			wavData[i] = new double[allocatedSamples]; 
		}
		int[] channelSamples = new int[nChan];
		RawDataUnit decimatedData = null;
		double[] raw = null;
		long clipStartMillis = -1;
		long clipStartSample = -1;
//		if (decimator != null) {
//			decimator.
//		}
		for (RawDataUnit rawUnit : cloneCopy) {
			if (rawUnit.getEndTimeInMilliseconds() < selectStart) {
				continue;
			}
			if (rawUnit.getTimeMilliseconds() > selectEnd) {
				break;
			}
			int rawChan = PamUtils.getSingleChannel(rawUnit.getChannelBitmap());
			int iChan = PamUtils.getChannelPos(rawChan, channelMap);
			if (iChan < 0) {
				continue;
			}

			if (decimator != null) {
				decimatedData = decimator.process(rawUnit);
				if (decimatedData != null) {
					raw = decimatedData.getRawData();
				}
			}
			else {
				raw = rawUnit.getRawData();
			}
			if (clipStartMillis < 0) {
				clipStartMillis = rawUnit.getTimeMilliseconds();
				clipStartSample = rawUnit.getStartSample();
			}
			if (raw == null) {
				continue; // this will look messy, but should stop it crashing. 
			}
			int newLength = channelSamples[iChan] + raw.length;
			if (newLength > wavData[iChan].length) {
				wavData[iChan] = Arrays.copyOf(wavData[iChan], newLength);
			}
			System.arraycopy(raw, 0, wavData[iChan], channelSamples[iChan], raw.length);
			channelSamples[iChan] += raw.length;
		}
		// now check the final lengths of the arrays. Very likely one or other of these will get called. 
		for (int i = 0; i < nChan; i++) {
			if (channelSamples[i] > allocatedSamples) {
				// keep the end, skip the start
				int skipSamples = channelSamples[i]-allocatedSamples;
				clipStartMillis += skipSamples * 1000 / fs;
				wavData[i] = Arrays.copyOfRange(wavData[i], channelSamples[i]-allocatedSamples, channelSamples[i]);
			}
			else if (channelSamples[i] < allocatedSamples) {
				// keep the start
				wavData[i] = Arrays.copyOf(wavData[i], channelSamples[i]);
			}
		}
		
		CollatorDataUnit newData = new CollatorDataUnit(clipStartMillis, channelMap, clipStartSample, parameterSet.outputSampleRate, wavData[0].length, trigger, parameterSet.setName, wavData);
				
		return newData;
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
			return 0;//(long) (parameterSet.outputClipLengthS*1000.) + 3000;
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
			rawDataCopy.setNaturalLifetimeMillis((int) ((parameterSet.outputClipLengthS*1.1) * 1000)+20000);
			if (parameterSet.outputSampleRate == rawDataBlock.getSampleRate()) {
				decimator = null;
			}
			else {
				DecimatorParams dp = new DecimatorParams(parameterSet.outputSampleRate);
				decimator = new DecimatorWorker(dp, rawDataBlock.getChannelMap(), rawDataBlock.getSampleRate(), parameterSet.outputSampleRate);
			}
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
		collatorTrigger.reset();
		
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

	/**
	 * @return the parameterSet
	 */
	public CollatorParamSet getParameterSet() {
		return parameterSet;
	}

}
