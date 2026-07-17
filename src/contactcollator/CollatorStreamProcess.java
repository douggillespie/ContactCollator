package contactcollator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import PamController.PamController;
import PamDetection.AbstractLocalisation;
import PamDetection.LocContents;
import PamDetection.RawDataUnit;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;
import PamguardMVC.RawDataUnavailableException;
import PamguardMVC.dataSelector.DataSelector;
import PamguardMVC.superdet.SuperDetection;
import clickDetector.ClickDetection;
import clipgenerator.ClipDataUnit;
import clipgenerator.ClipDisplayDataBlock;
import clipgenerator.ClipProcess.ClipRequest;
import clipgenerator.clipDisplay.ClipDisplayDecorations;
import clipgenerator.clipDisplay.ClipDisplayParent;
import clipgenerator.clipDisplay.ClipDisplayUnit;
import contactcollator.bearings.BearingSummariser;
import contactcollator.bearings.BearingSummary;
import contactcollator.bearings.BearingSummaryLocalisation;
import contactcollator.bearings.HeadingHistogram;
import contactcollator.localisations.LocalisationAverager;
import contactcollator.localisations.LocalisationSummariser;
import contactcollator.localisations.LocalisationSummary;
import contactcollator.localisations.SummaryLocalisation;
import contactcollator.trigger.CollatorRateFilter;
import contactcollator.trigger.CollatorTrigger;
import contactcollator.trigger.CollatorTriggerData;
import contactcollator.trigger.CountingTrigger;
import decimator.DecimatorParams;
import decimator.DecimatorProcessW;
import decimator.DecimatorWorker;

public class CollatorStreamProcess extends PamProcess implements ClipDisplayParent{

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
	private List<ClipRequest> clipRequestQueue;
	private Object clipRequestSynch = new Object();

	/**
	 * Now need a hash map of HeadingHistograms since we may be dealing with multiple
	 * hydrophone clusters in different locations. 
	 */
//	private HeadingHistogram headingHistogram;
	HashMap<Integer, HeadingHistogram> headingHistograms = new HashMap<Integer, HeadingHistogram>();
	private static final int NHEADINGHISTBINS = 24;

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
		rawDataCopy = new PamRawDataBlock("internal copy", this, 0, this.getSampleRate());
		collatorRateFilter = new CollatorRateFilter();
		
//		headingHistogram = new HeadingHistogram(24, true);
		
		clipRequestQueue = new LinkedList<ClipRequest>();
		
	}

	@Override
	public void pamStart() {
		rawDataObserver.pause=false;

	}

	@Override
	public void pamStop() {
		rawDataObserver.pause=true;

	}
	
	@Override
	public void newData(PamObservable o, PamDataUnit dataUnit) {
		// see if we actually want it using the data selector
		if(PamController.getInstance().getRunMode()==PamController.RUN_NETWORKRECEIVER) {
			return;
		}
		if (wantDetectionData(dataUnit)) {
			useDetectionData(dataUnit);
		}else {
//			System.out.println("Decided to not use the new detection data.");
		}
		
	}
	
	

	@Override
	public void updateData(PamObservable o, PamDataUnit dataUnit) {
		// see if we actually want it using the data selector
		if(PamController.getInstance().getRunMode()==PamController.RUN_NETWORKRECEIVER) {
			return;
		}
		if (wantDetectionData(dataUnit)) {
			// try to find a 2 or 3d localisation either in this unit or a super detection. 
//			findLatLongLocalisation(dataUnit); // the localisations come in too late
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
	 * Get a heading histogram for the channels. Create if required. 
	 * @param channelMap
	 * @return
	 */
	private HeadingHistogram getHeadingHistogram(int channelMap) {
		synchronized (headingHistograms) {
			HeadingHistogram hh = headingHistograms.get(channelMap);
			if (hh == null) {
				hh = new HeadingHistogram(channelMap, NHEADINGHISTBINS, true);
				headingHistograms.put(channelMap, hh);
			}
			return hh;
		}
	}

	/**
	 * Now use the data, potentially even creating output
	 * @param dataUnit incoming detection data unit. 
	 */
	private void useDetectionData(PamDataUnit dataUnit) {
		/**
		 * Fill the heading histrogram whether we're using this or not. 
		 */
		HeadingHistogram headingHistogram = getHeadingHistogram(dataUnit.getChannelBitmap());
		headingHistogram.addData(dataUnit);
		
		CollatorTriggerData trigger = collatorTrigger.newData(dataUnit);
		boolean immediate = false;
		if (trigger != null) {
			// we want to do something
			if (immediate) {
				newDetectionTrigger(trigger, dataUnit);
			}
			else {
				/*
				 *  wait two seconds for localisations to come in, then execute.
				 *  when using the crossed bearing localiser, this causes all units
				 *  to have a localisation, rather than the last one not having a localisation. 
				 *  Will need to think a bit harder about how to set this delay ! 
				 */
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						newDetectionTrigger(trigger, dataUnit);
					}
				});
				t.start();
			}
			
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
			int firstChannel = PamUtils.getLowestChannel(trigger.getDataList().get(0).getChannelBitmap());
			int[] channelList = new int[] {firstChannel};
			int bitmap = PamUtils.makeChannelMap(channelList);
			CollatorDataUnit newDataUnit = createOutputData(trigger, cloneCopy, bitmap);
			if (newDataUnit == null) {
				return;
			}
			AbstractLocalisation bestLocalisation = null;
			LocalisationSummary locSummary = get2DLocalisationSummary(trigger);
			if (locSummary != null) {
				newDataUnit.setLocalisationSummary(locSummary);
				newDataUnit.setLocalisation(new SummaryLocalisation(newDataUnit, locSummary, locSummary.getHydrophoneMap()));
			}
			
			BearingSummary bearingSummary = getBearingSummary(trigger);
			if (bearingSummary != null) {
				newDataUnit.setBearingSummary(new BearingSummaryLocalisation(newDataUnit, bearingSummary));
			}

			/**
			 * Copy all histograms (there may be 0 - many)
			 */
			HashMap<Integer, HeadingHistogram> mapCopy = null;
			synchronized (headingHistograms) {
				mapCopy = new HashMap<Integer, HeadingHistogram>(headingHistograms);
				headingHistograms.clear();
			}
//			if (headingHistogram != null) {
				newDataUnit.setHeadingHistograms(mapCopy);
//				headingHistogram.reset();
//			}
			
			
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
	 * Get a summary of all localisations that have at least a latlong. 
	 * @param trigger
	 * @return 
	 */
	private LocalisationSummary get2DLocalisationSummary(CollatorTriggerData trigger) {
			LocalisationSummariser summariser = new LocalisationAverager(this);
			return summariser.summariseLocalisations(trigger);
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
		long triggerEndTime = trigger.getEndTime();
		long triggerStartTime = trigger.getStartTime();
		long triggerDurationMillis = triggerEndTime-triggerStartTime;
		int triggerDurationSamples = (int) (fs*triggerDurationMillis/1000);
		long triggerStartSample = trigger.getDataList().get(0).getStartSample();
		int trigChannelMap = trigger.getDataList().get(0).getChannelBitmap();
		long triggerEndSample = triggerStartSample+triggerDurationSamples;
		//long wavEnd = cloneCopy.get(cloneCopy.size()-1).getEndTimeInMilliseconds();
		//long wavStart = cloneCopy.get(0).getTimeMilliseconds();
		//long wavLength = wavEnd-wavStart;
		long bufferSamples = (long) (parameterSet.minClipLengthS*fs)-(triggerDurationSamples);
		if(bufferSamples<0) {
			bufferSamples = (long) (parameterSet.outputClipLengthS*fs)-(triggerDurationSamples);
		}
		long prePost = (long) bufferSamples/2;
		
		long clipStartSample = triggerStartSample-prePost;
		long clipEndSample = triggerEndSample+prePost;
		
		int clipDurationSamples = (int) (clipEndSample-clipStartSample);
		
		long clipStartMillis = triggerStartTime-(long)(1000*prePost/fs);
		
		int firstTrigChan = PamUtils.getLowestChannel(trigChannelMap); 
		firstTrigChan = 1<<firstTrigChan;
		CollatorDataUnit newData = new CollatorDataUnit(clipStartMillis, firstTrigChan, clipStartSample, parameterSet.outputSampleRate, 0, trigger, parameterSet.setName, null);
				
//		System.out.printf("Request clip data for channelMap  %d-%d\n", trigChannelMap, firstTrigChan);
		ClipRequest newRequest = new ClipRequest(clipStartSample,clipDurationSamples,newData, firstTrigChan);
		
		synchronized(clipRequestSynch) {
			clipRequestQueue.add(newRequest);
		}
		
		return newData;
	}
	
	public int processClipRequest(ClipRequest nextClipRequest) {
		double[][] wavData;
		
		CollatorDataUnit unfinishedUnit = nextClipRequest.unfinishedDataUnit;
		
//		int firstChanIdx = PamUtils.getLowestChannel(nextClipRequest.unfinishedDataUnit.getChannelBitmap());
		int firstChanIdx = nextClipRequest.firstChannel;
		int firstChanMap = PamUtils.makeChannelMap(new int[]{firstChanIdx});
//		System.out.printf("Request wav data for channel map %d\n", firstChanMap);
		
		try {
			wavData = rawDataBlock.getSamples(nextClipRequest.clipStartSample, nextClipRequest.clipDurationSamples, firstChanMap);
		}
		catch (RawDataUnavailableException e) {
			return e.getDataCause();
		}
		
		unfinishedUnit.setRawData(wavData);
		unfinishedUnit.setSampleDuration((long) wavData[0].length);
		
		/**
		 * Synch adding data with collatorControl, but NOT the datablock since that will really 
		 * mess up some other threading stuff by blocking the datablock users for too long
		 * leading to a lock. 
		 */
		unfinishedUnit.setParentDataBlock(collatorBlock);
		synchronized (collatorControl) {
			collatorBlock.addPamData(unfinishedUnit);
		}
		return 0;
	}
	
	public void processRequestList() {
		if (PamController.getInstance().getRunMode() != PamController.RUN_NORMAL &&
				PamController.getInstance().getRunMode() != PamController.RUN_MIXEDMODE) {
			return;
		}
		if (clipRequestQueue.size() == 0) {
			return;
		}
		synchronized(clipRequestSynch) {
			ClipRequest clipRequest;
			ListIterator<ClipRequest> li = clipRequestQueue.listIterator();
			int clipErr;
			while (li.hasNext()) {
				clipRequest = li.next();
				clipErr = processClipRequest(clipRequest);
				switch (clipErr) {
					case 0: // no error - clip should have been created. 
					case RawDataUnavailableException.DATA_ALREADY_DISCARDED:
					case RawDataUnavailableException.INVALID_CHANNEL_LIST:
						//					System.out.println("Clip error : " + clipErr);
						li.remove();
					case RawDataUnavailableException.DATA_NOT_ARRIVED:
						continue; // hopefully, will get this next time !
				}
			}
		}
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
	
	public class ClipRequest{
		public int firstChannel;
		long clipStartSample;
		int clipDurationSamples;
		
		CollatorDataUnit unfinishedDataUnit;
		
		public ClipRequest(long clipStartSample,int clipDurationSamples,CollatorDataUnit unfinishedDataUnit, int firstChannel){
			if(clipStartSample<0) {
				clipStartSample=0;
			}
			this.clipStartSample = clipStartSample;
			this.clipDurationSamples = clipDurationSamples;
			this.unfinishedDataUnit = unfinishedDataUnit;
			this.firstChannel = firstChannel;
		}
	}
	
	private class RawDataObserver implements PamObserver {
		
		boolean pause = false;

		@Override
		public long getRequiredDataHistory(PamObservable observable, Object arg) {
			long minH = (long) parameterSet.outputClipLengthS;
			minH += Math.max(3000, 192000/(long)getSampleRate());

			return minH;
		}

		@Override
		public void addData(PamObservable observable, PamDataUnit pamDataUnit) {
			/**
			 * Make a copy of the data (not the raw data) into a local array. won't need much memory, so OK. 
			 */
			if(pause) {
				return;
			}
			RawDataUnit in = (RawDataUnit) pamDataUnit;
			RawDataUnit copy = new RawDataUnit(in.getTimeMilliseconds(), in.getChannelBitmap(), in.getStartSample(), in.getSampleDuration());
			copy.setRawData(in.getRawData());
			synchronized (rawDataCopy.getSynchLock()) {
				rawDataCopy.addPamData(copy);
			}
			processRequestList();
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
			if (parameterSet.makeWaveClip) {
				rawDataBlock.addObserver(rawDataObserver, false);
			}
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
		if (rawDataCopy != null) {
			rawDataCopy.deleteObserver(rawDataObserver);
			rawDataCopy = null;
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

	@Override
	public ClipDisplayDataBlock getClipDataBlock() {
		// TODO Auto-generated method stub
		return collatorBlock;
	}

	@Override
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return this.getSetName()+" Clips";
	}

	@Override
	public ClipDisplayDecorations getClipDecorations(ClipDisplayUnit clipDisplayUnit) {
		// TODO Auto-generated method stub
		return new ClipDisplayDecorations(clipDisplayUnit);
	}

	@Override
	public void displaySettingChange() {
		// TODO Auto-generated method stub
		
	}

}
