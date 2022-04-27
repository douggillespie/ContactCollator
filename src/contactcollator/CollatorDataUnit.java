package contactcollator;

import PamguardMVC.RawDataHolder;
import PamguardMVC.RawDataTransforms;
import clipgenerator.ClipDataUnit;
import contactcollator.bearings.BearingSummary;
import contactcollator.bearings.BearingSummaryLocalisation;
import contactcollator.trigger.CollatorTriggerData;

/**
 * Data output from Contact Collator. The datablock may contain these data units from multiple
 * output streams, each having a different sample rate which may make things a little confusing.
 * <p>Mostly this class is just a wrapper around the ClipDataUnit, so most data fields are accessed
 * through getters in the super class.  
 * @author dg50
 *
 */
public class CollatorDataUnit extends ClipDataUnit implements RawDataHolder {
		
	private CollatorTriggerData triggerData;
	
	private RawDataTransforms rawDataTransforms;

	private BearingSummaryLocalisation bearingSummaryLocalisation;

	/**
	 * Real time constructor
	 * @param timeMilliseconds
	 * @param channelBitmap
	 * @param startSample
	 * @param sampleRate
	 * @param durationSamples
	 * @param triggerData
	 * @param wavData
	 */
	public CollatorDataUnit(long timeMilliseconds, int channelBitmap, long startSample, float sampleRate, int durationSamples, CollatorTriggerData triggerData, double[][] wavData) {
		super(timeMilliseconds, triggerData.getStartTime(), startSample, durationSamples, channelBitmap, null, triggerData.getTriggerName(), wavData, sampleRate);
		this.triggerData = triggerData;
	}

	/**
	 * For use reading back from binary files. 
	 * @param timeMilliseconds
	 * @param channelBitmap
	 * @param startSample
	 * @param sampleRate
	 * @param durationSamples
	 * @param triggerName
	 * @param triggerTime
	 * @param wavData
	 */
	public CollatorDataUnit(long timeMilliseconds, int channelBitmap, long startSample, float sampleRate, int durationSamples, String triggerName, long triggerTime, double[][] wavData) {
		super(timeMilliseconds, triggerTime, startSample, durationSamples, channelBitmap, null, triggerName, wavData, sampleRate);
	}

	@Override
	public RawDataTransforms getDataTransforms() {
		if (rawDataTransforms == null) {
			rawDataTransforms = new RawDataTransforms(this, this);
		}
		return rawDataTransforms;
	}

	/**
	 * Set summary bearing information for the contact. 
	 * @param bearingSummaryLocalisation
	 */
	public void setBearingSummary(BearingSummaryLocalisation bearingSummaryLocalisation) {
		this.bearingSummaryLocalisation = bearingSummaryLocalisation;
		this.setLocalisation(bearingSummaryLocalisation);
	}

	/**
	 * Get bearing summary information for the contact in the form of a full localisation 
	 * object (instance of AbstractLocalisation, so can be used throughout PAMGuard). 
	 * @return the bearingSummaryLocalisation
	 */
	public BearingSummaryLocalisation getBearingSummaryLocalisation() {
		return bearingSummaryLocalisation;
	}
	
	/**
	 * Get bearing summary information for the contact
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

	/**
	 * Get the data that were used to trigger the contact. <p>
	 * Contains info on the detector and which data units fed into the trigger. 
	 * @return the triggerData
	 */
	public CollatorTriggerData getTriggerData() {
		return triggerData;
	}

}
