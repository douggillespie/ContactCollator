package contactcollator;

import java.awt.image.BufferedImage;
import java.util.List;

import PamView.ColourArray;
import PamView.PamColors;
import PamguardMVC.PamDataUnit;
import PamguardMVC.RawDataTransforms;
import clipgenerator.ClipSpectrogram;
import contactcollator.bearings.BearingSummary;
import contactcollator.trigger.CollatorTriggerData;
import pamMaths.PamVector;

/**
 * Sample file that's not actually used for anything, but lists some of the main functions that
 * can be called on a CollatorDataUnit to extract data for whatever purpose is of interest. 
 * @author dg50
 *
 */
public class UsageExample {

	public void UsageExample(CollatorDataUnit collatorDataUnit) {
		
		/**
		 * Start time in milliseconds. 
		 */
		long timeMillis = collatorDataUnit.getTimeMilliseconds();
		
		/**
		 * End time of the contact.
		 */
		long endMillis = collatorDataUnit.getEndTimeInMilliseconds();
		
		/**
		 * Get an object with information about what triggered the contact. 
		 */
		CollatorTriggerData triggerData = collatorDataUnit.getTriggerData();
		
		/**
		 * List of all data that went into triggering the contact. 
		 */
		List<PamDataUnit> triggerList = triggerData.getDataList();
		
		/**
		 * The name of the data that triggered the contact
		 */
		String triggerName = triggerData.getTriggerName();

		/**
		 * the name of the collator stream (as set in the configuration, different to the trigger name)
		 */
		String streamName = collatorDataUnit.getStreamName();
		
		/**
		 * Get an object containing summary information about bearings to detections forming the contact. 
		 * May be null if there was no bearing information in the detections forming the contact. 
		 */
		BearingSummary bearingSummary = collatorDataUnit.getBearingSummary();
		
		/**
		 * The mean heading relative to the array, clockwise from ahead.
		 */
		double meanHead = bearingSummary.getMeanHeading();
		
		/**
		 * the standard deviation of the bearings forming the contact in radians 
		 */
		double bearingSTD = bearingSummary.getStdHeading();
		
		/**
		 * A unit vector in the reference frame of the array. Note that getMeanHeading is equal to
		 * meanHead = pi/2-atan2(worldVec[1],worldVec[0]). The vector also contains slant angle 
		 * information (i.e. in third vector component, which may be non zero) where slantAngle = asin(worldVec[2]);
		 */
		PamVector vector = bearingSummary.getWorldVector();
		
		/**
		 * Double array of raw data clip of the time of the contact. Currently 
		 * a single channel so a 1xn array. 
		 */
		double[][] rawData = collatorDataUnit.getRawData();
		
		/**
		 * The sample rate for the raw data. May not be the same as the input to the detector since the 
		 * contact collator may be configured to downsample raw data for som eoutput. 
		 */
		float sampleRate = collatorDataUnit.getSourceSampleRate();
		
		/**
		 * An object which can provide the acoustic data in various transformed formats:
		 */
		RawDataTransforms rawDataTransforms = collatorDataUnit.getDataTransforms();
		
		/**
		 * Get the raw data in int16 format (smaller quicker to store / transfer than double[] data)
		 */
		short[] wavData = rawDataTransforms.getShortWaveData(0);
		
		/**
		 * Get spectrogram data for the clip
		 */
		ClipSpectrogram spectrogram = rawDataTransforms.getSpectrogram(512, 256);
		
		/**
		 * Get the actual data for that spectrogram object. 
		 */
		double[][] channelSpec = spectrogram.getSpectrogram(0);
		
		/**
		 * Get a spectrogram image using the specified FFT parameters, amplitude range and colour scheme. 
		 * (In reality, you'd reuse the colour array, not create every time). 
		 */
		BufferedImage swingImage = collatorDataUnit.getClipImage(0, 512, 256, 30, 110, ColourArray.createHotArray(256).getColours());
	}
	
	

}
