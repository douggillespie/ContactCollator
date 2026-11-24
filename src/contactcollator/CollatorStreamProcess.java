package contactcollator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import Localiser.algorithms.locErrors.LocaliserError;
import PamController.PamController;
import PamDetection.AbstractLocalisation;
import PamDetection.LocContents;
import PamDetection.RawDataUnit;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamUtils.PamUtils;
import PamguardMVC.DataUnitBaseData;
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
import contactcollator.localisations.ContactLocalisationSummary;
import contactcollator.localisations.GMMLocalisationSummariser;
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
//			return; why was this in there ? Definitely don't want it for current project where we want CC to work on multi buoy output
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
		/*
		 * ncollatorTrigger will return something non null whenever the count is greater than 
		 * the minimum, then keep returning every time new data arrive, so the same trigger
		 * will come in here manymany times from it becoming big enough, to it being expired / reset
		 * and a new trigger being started. 
		 */
		boolean immediate = true;
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
				 *  Except that this is buggy as all hell, so the second one just creates a second
				 *  data unit. 
				 *  Need to think of a better logic to remove this from queue if a new one comes in 
				 *  within a couple of seconds. For now, leave it. 
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
	 * the system has triggered and decided it want's to make an output data unit<br>
	 * This will get called for every new data unit once the size of the trigger data has
	 * become sufficiently large. 
	 * @param trigger trigger information
	 * @param dataUnit last data unit that went into the trigger. 
	 */
	private void newDetectionTrigger(CollatorTriggerData trigger, PamDataUnit dataUnit) {
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
			
			/**
			 * Consider what to do here if there are multiple localisations within the summary. 
			 */
			ContactLocalisationSummary locSummary = get2DLocalisationSummary(trigger);
			if (locSummary != null) {
				newDataUnit.setLocalisationSummary(locSummary);
//				System.out.printf("Locations summaries for %s: %s\n", dataUnit.toString(), locSummary.toString());
//				newDataUnit.setLocalisation(new SummaryLocalisation(newDataUnit, locSummary, locSummary.getHydrophoneMap()));
			}
			
			// this is the old method from when there was only one group. Probably too obsolete now to keep
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
			//				System.out.printf("Request clip data for channelMap  %d-%d at %s\n", trigChannelMap, firstTrigChan, PamCalendar.formatTime(clipStartMillis, true));
			int firstTrigChan = PamUtils.getLowestChannel(newDataUnit.getChannelBitmap());
			long durSamples = newDataUnit.getSampleDuration();
			ClipRequest newRequest = new ClipRequest(newDataUnit.getStartSample(),(int) durSamples,newDataUnit, firstTrigChan);

			synchronized(clipRequestSynch) {
				// replace in queue - with multiple clusters, the same request can arrive many times, 
				// which isn't great. This n
				//					Iterator<ClipRequest> clipIt = clipRequestQueue.iterator();
				//					while (clipIt.hasNext()) {
				//						CollatorTriggerData td1 = clipIt.next().unfinishedDataUnit.getTriggerData();
				//						CollatorTriggerData td2 = newRequest.unfinishedDataUnit.getTriggerData();
				//						if (clipIt.next().unfinishedDataUnit.getTriggerData() == newRequest.unfinishedDataUnit.getTriggerData()) {
				//							clipIt.remove();
				//						}
				//					}
				if (PamController.getInstance().getRunMode() == PamController.RUN_NETWORKRECEIVER) {
					processClipRequest(newRequest);
				}
				else {
					clipRequestQueue.add(newRequest);
				}
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
	 * Get a summary of all localisations that have at least a latlong. 
	 * @param trigger
	 * @return 
	 */
	private ContactLocalisationSummary get2DLocalisationSummary(CollatorTriggerData trigger) {
//			LocalisationSummariser summariser = new LocalisationAverager(this);
//		System.out.printf("Get localisation summary for %d calls\n", trigger.getDataList().size());
		LocalisationSummariser summariser = new GMMLocalisationSummariser(this);
		
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
		 * Logic was pretty messed up if the trigger length was a lot longer than the output
		 * clip (which is should be), and what was happening was that it would take a section of sound 
		 * in the middle of the trigger time. For DO whistles, that worked OK since it would usually find something, 
		 * but for sparse sounds, like NARW, it tended to pick a time between the sounds, which was 
		 * pretty useless. So logic is changes to pick the best sound in the list of possible trigger datas, 
		 * and centre the clip around that time. 
		 */
		PamDataUnit best = getBestDataUnit(trigger.getDataList());
		long triggerEndTime = trigger.getEndTime();
		long triggerStartTime = trigger.getStartTime();
		long triggerDurationMillis = triggerEndTime-triggerStartTime;
		int triggerDurationSamples = (int) (fs*triggerDurationMillis/1000);
		long triggerStartSample = best.getStartSample();
		/**
		 * trigChannelMap is the channel map of the best (loudest) call that gets used as the examplar template. 
		 */
		int trigChannelMap = best.getChannelBitmap();
//		long triggerEndSample = triggerStartSample+triggerDurationSamples;
		//long wavEnd = cloneCopy.get(cloneCopy.size()-1).getEndTimeInMilliseconds();
		//long wavStart = cloneCopy.get(0).getTimeMilliseconds();
		//long wavLength = wavEnd-wavStart;
		// start by centering the call around the best data unit, then adjust so that it fits
		// within the limits of the trigger data
		long clipStart = best.getTimeMilliseconds();
		long clipEnd = best.getEndTimeInMilliseconds();
		// max length of the clip is the min of trig duration or total max length
		long maxMillis = Math.min((long) (parameterSet.outputClipLengthS * 1000), triggerDurationMillis);
		// if the length of the clip is 
		if (clipEnd-clipStart > maxMillis) {
			long rem = maxMillis - (clipEnd-clipStart);
			clipStart += rem/2;
			clipEnd -= rem/2;
		}
		// mnimum length is this, stretch out if needed. 
		long minMillis = (long) (parameterSet.minClipLengthS * 1000);
		if (clipEnd-clipStart < minMillis) {
			long add = clipEnd-clipStart - minMillis;
			clipStart -= add/2;
			clipEnd += add/2;
		}

		long clipStartMillis = clipStart;
		long clipStartSample = triggerStartSample + (long)( (clipStart - best.getTimeMilliseconds()) * fs/1000.);
		int clipDurationSamples = (int) ((clipEnd-clipStart)*fs/1000.);
		
//		
//		
//		long bufferSamples = (long) (parameterSet.minClipLengthS*fs)-(triggerDurationSamples);
//		if(bufferSamples<0) {
//			bufferSamples = (long) (parameterSet.outputClipLengthS*fs)-(triggerDurationSamples);
//		}
//		long prePost = (long) bufferSamples/2;
//		
//		long clipStartSample = triggerStartSample-prePost;
//		long clipEndSample = triggerEndSample+prePost;
//		
//		int clipDurationSamples = (int) (clipEndSample-clipStartSample);
//		
//		long clipStartMillis = triggerStartTime-(long)(1000*prePost/fs);
		
//		System.out.printf("Create clip chan %d %s to %s, starting at %s\n", trigChannelMap, PamCalendar.formatTime(triggerStartTime, true), 
//				PamCalendar.formatTime(triggerEndTime, true), PamCalendar.formatTime(clipStartMillis, true));
		
		int firstTrigChan = PamUtils.getLowestChannel(trigChannelMap); // why was this only giving one channel to the clip unit ? 
		firstTrigChan = 1<<firstTrigChan;
//		int trigChannels = trigger.getChannelBitmap();
		CollatorDataUnit newData = new CollatorDataUnit(clipStartMillis, firstTrigChan, clipStartSample, parameterSet.outputSampleRate, clipDurationSamples, trigger, parameterSet.setName, null);
		
		
		return newData;
	}
	
	/**
	 * Get the loudest, longest, or latest. 
	 * @param dataList
	 * @return
	 */
	private PamDataUnit getBestDataUnit(List<PamDataUnit> dataList) {
		if (dataList == null || dataList.size() == 0) {
			return null;
		}
		// try to find the loudest or longest data unit. 
		List<PamDataUnit> toSort = new ArrayList<PamDataUnit>(dataList);
		toSort.sort(new FindBest(0));
		PamDataUnit best = toSort.get(toSort.size()-1);
		return best;
	}
	
	private class FindBest implements Comparator<PamDataUnit> {
		
		
		private long earliest = 0;
		

		/**
		 * @param earliest earliest time for any data unit so that we select the best
		 * one after a certain time. 
		 */
		public FindBest(long earliest) {
			super();
			this.earliest = earliest;
		}


		@Override
		public int compare(PamDataUnit o1, PamDataUnit o2) {
			if (o1.getTimeMilliseconds() < earliest) {
				return 1;
			}
			// try to select on amplitude
			Double a1 = o1.getAmplitudeDB();
			Double a2 = o2.getAmplitudeDB();
			if (a1 != null && a2 != null) {
				return (int) Math.signum(a2-a1);
			}
			// try to select on duration
			Double d1 = o1.getDurationInMilliseconds();
			Double d2 = o2.getDurationInMilliseconds();
			if (d1 != null && d2 != null) {
				return (int) Math.signum(d2-d1);
			}
			
			// give up and select on time. 
			return (int) (o2.getTimeMilliseconds()-o1.getTimeMilliseconds());
		}
		
	}

	public int processClipRequest(ClipRequest nextClipRequest) {
		
		double[][] wavData = null;
		CollatorDataUnit unfinishedUnit = nextClipRequest.unfinishedDataUnit;
		
//		int firstChanIdx = PamUtils.getLowestChannel(nextClipRequest.unfinishedDataUnit.getChannelBitmap());
		int firstChanIdx = nextClipRequest.firstChannel;
		int firstChanMap = PamUtils.makeChannelMap(new int[]{firstChanIdx});
//		System.out.printf("Request wav data for channel map %d\n", firstChanMap);
		firstChanMap = 1;
		int errorType = 0;
		
		try {
			wavData = rawDataBlock.getSamples(nextClipRequest.clipStartSample, nextClipRequest.clipDurationSamples, firstChanMap);
		}
		catch (RawDataUnavailableException e) {
			errorType = e.getDataCause();
		}
		
		if (errorType == 0 && wavData != null) {
			unfinishedUnit.setRawData(wavData);
			unfinishedUnit.setSampleDuration((long) wavData[0].length);
		}
		
		/**
		 * Synch adding data with collatorControl, but NOT the datablock since that will really 
		 * mess up some other threading stuff by blocking the datablock users for too long
		 * leading to a lock. 
		 */
		collatorBlock.addPamData(unfinishedUnit);
//		unfinishedUnit.setParentDataBlock(collatorBlock);

		/*
		 * Bit messy, but by the time we get here, there may be multiple localisations in the data unit. 
		 * in which case we need to generate multiple output data units. Later, we may revise this and 
		 * change to a single massive json summary of all localisation data in the unit ? 
		 * 
		 * Going to revise this and send out one more complex summary with multiple localisations 
		 * at least to the locations data block. Other blocks may prefer multiple lines still to 
		 * fit into the simpler database format ? 
		 */
//		collatorControl.getCollatorProcess().getCollatofLocationDataBlock().addPamData(unfinishedUnit);
//		CollatorDataUnit[] splitUnits = splitLocalisations(unfinishedUnit);
//		synchronized (collatorControl) {
////			System.out.printf("Add %d collator split units\n", splitUnits.length);
//			for (int i = 0; i < splitUnits.length; i++) {
//				collatorBlock.addPamData(splitUnits[i]);
//				if (splitUnits[i].getLocalisationSummary() != null) {
//					collatorControl.getCollatorProcess().getCollatofLocationDataBlock().addPamData(splitUnits[i]);
//				}
//			}
//		}
		return errorType;
	}
	
	/**
	 * Split up an output unit into multiple units if the clustering algorithm has suggested multiple sources. 
	 * @param collatorData
	 * @return
	 */
	private CollatorDataUnit[] splitLocalisations(CollatorDataUnit collatorData) {
		ContactLocalisationSummary locSummary = collatorData.getLocalisationSummary();
		if (locSummary == null) {
			CollatorDataUnit[] outs = {collatorData};
			return outs;
		}
		// otherwise split the unit up. 
		ArrayList<LocalisationSummary> summaries = locSummary.getSummaries();
		CollatorDataUnit[] outs = new CollatorDataUnit[summaries.size()];
		DataUnitBaseData baseData = collatorData.getBasicData();
		for (int i = 0; i < outs.length; i++) {
			outs[i] = collatorData.clone();
			LocalisationSummary locSum = summaries.get(i);
			if (locSum == null) {
				continue; // should never happen, perhaps let it just throw exception ? 
			}
			outs[i].setChannelBitmap(locSum.getHydrophoneMap());
			LatLong latLong = locSum.getLatLong();
			if (latLong != null) {
				outs[i].setSourceLat(latLong.getLatitude());
				outs[i].setSourceLon(latLong.getLongitude());
			}
			LocaliserError err = locSum.getLocaliserError();
			if (err != null) {
				String j = err.getJsonErrorString();
				outs[i].setJsonLocalisationError(j);
//				double[] mags = err.
			}
			ArrayList<LocalisationSummary> newList = new ArrayList<LocalisationSummary>();
			newList.add(locSum);
			ContactLocalisationSummary newSummary = new ContactLocalisationSummary(locSummary.getMethod(), newList);
			outs[i].setLocalisationSummary(newSummary);
			outs[i].setLocalisation(new SummaryLocalisation(outs[i], locSum, locSum.getHydrophoneMap()));
		}
		return outs;
	}

	public void processRequestList() {
		if (PamController.getInstance().getRunMode() != PamController.RUN_NORMAL &&
				PamController.getInstance().getRunMode() != PamController.RUN_MIXEDMODE &&
				PamController.getInstance().getRunMode() != PamController.RUN_NETWORKRECEIVER) {
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
						li.remove();
						break;
					case RawDataUnavailableException.DATA_ALREADY_DISCARDED:
					case RawDataUnavailableException.INVALID_CHANNEL_LIST:
//						System.out.println("Clip ?error : " + clipErr);
						li.remove();
					case RawDataUnavailableException.DATA_NOT_ARRIVED:
						continue; // hopefully, will get this next time !
					default:
						li.remove();
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
