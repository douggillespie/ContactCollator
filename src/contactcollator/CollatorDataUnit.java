package contactcollator;

import PamguardMVC.DataUnitBaseData;
import PamguardMVC.PamDataUnit;
import PamguardMVC.RawDataHolder;
import PamguardMVC.RawDataTransforms;
import clipgenerator.ClipDataUnit;
import contactcollator.bearings.BearingSummary;
import contactcollator.bearings.BearingSummaryLocalisation;
import contactcollator.trigger.CollatorTriggerData;

public class CollatorDataUnit extends ClipDataUnit implements RawDataHolder {
		
	private float sampleRate;
	
	private String detectorSource;
	
	private long detectionUID; // may want to replace this with a list. 

	private CollatorTriggerData triggerData;
	
	private RawDataTransforms rawDataTransforms;

	private BearingSummaryLocalisation bearingSummaryLocalisation;

//	public CollatorDataUnit(DataUnitBaseData basicData) {
//		super(basicData);
//	}

	/*
	 * 
	public ClipDataUnit(long timeMilliseconds, long triggerMilliseconds,
			long startSample, int durationSamples, int channelMap, String fileName,
			String triggerName,	double[][] rawData, float sourceSampleRate) {
	 */
	public CollatorDataUnit(long timeMilliseconds, int channelBitmap, long startSample, float sampleRate, int durationSamples, CollatorTriggerData triggerData, double[][] wavData) {
		super(timeMilliseconds, triggerData.getStartTime(), startSample, durationSamples, channelBitmap, null, triggerData.getTriggerName(), wavData, sampleRate);
		this.triggerData = triggerData;
	}

	@Override
	public RawDataTransforms getDataTransforms() {
		if (rawDataTransforms == null) {
			rawDataTransforms = new RawDataTransforms(this, this);
		}
		return rawDataTransforms;
	}

	public void setBearingSummary(BearingSummaryLocalisation bearingSummaryLocalisation) {
		this.bearingSummaryLocalisation = bearingSummaryLocalisation;
		this.setLocalisation(bearingSummaryLocalisation);
	}

	/**
	 * @return the bearingSummaryLocalisation
	 */
	public BearingSummaryLocalisation getBearingSummaryLocalisation() {
		return bearingSummaryLocalisation;
	}
	
	/**
	 * @return the bearingSummary
	 */
	public BearingSummary getBearingSummary() {
		if (bearingSummaryLocalisation == null) {
			return null;
		}
		else {
			return bearingSummaryLocalisation.getBearingSummary();
		}
	}

}
