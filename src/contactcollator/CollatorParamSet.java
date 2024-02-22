package contactcollator;

import java.io.Serializable;

import PamModel.parametermanager.ManagedParameters;
import PamModel.parametermanager.PamParameterSet;
import PamModel.parametermanager.PamParameterSet.ParameterSetType;

public class CollatorParamSet  implements ManagedParameters, Cloneable, Serializable {

	public static final long serialVersionUID = 1L;
	
	/**
	 * Name, to display in dialogs, use for data selectors, etc. 
	 */
	public String setName = "unknown";
	
	/**
	 * Species code to pass on with output data
	 */
//	public String speciesCode;
	
	/**
	 * Detector source (any detector)
	 */
	public String detectionSource;
	
	/**
	 * Make an output clip waveform. 
	 */
	public boolean makeWaveClip;
	
	/**
	 * Raw data to make clip from
	 */
	public String rawDataSource;
	
	/**
	 * Length of output clips in seconds. 
	 */
	public float outputClipLengthS = 2;
	
	/**
	 * clip sample rate (will be decimated if outputFS < inputFS)
	 */
	public int outputSampleRate;
	
	/**
	 * min number of detections to trigger an output. 
	 */
	public int triggerCount = 1;
	
	/**
	 * Interval for trigger count in seconds. 
	 */
	public float triggerIntervalS = 10;
	
	/**
	 * Minimum update interval in seconds for data saving, etc. 
	 */
	public float minimumUpdateIntervalS = 10;

	@Override
	public PamParameterSet getParameterSet() {
		return PamParameterSet.autoGenerate(this, ParameterSetType.DETECTOR);
	}

	@Override
	protected CollatorParamSet clone() {
		try {
			return (CollatorParamSet) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
