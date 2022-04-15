package contactcollator;

import PamguardMVC.DataUnitBaseData;
import PamguardMVC.PamDataUnit;
import PamguardMVC.RawDataHolder;
import PamguardMVC.RawDataTransforms;

public class CollatorDataUnit extends PamDataUnit<PamDataUnit, PamDataUnit> implements RawDataHolder {
	
	private double[][] waveData;
	
	private float sampleRate;
	
	private String detectorSource;
	
	private long detectionUID; // may want to replace this with a list. 

	public CollatorDataUnit(DataUnitBaseData basicData) {
		super(basicData);
	}

	public CollatorDataUnit(long timeMilliseconds, int channelBitmap, long startSample, long durationSamples) {
		super(timeMilliseconds, channelBitmap, startSample, durationSamples);
	}

	@Override
	public double[][] getWaveData() {
		return waveData;
	}

	@Override
	public RawDataTransforms getDataTransforms() {
		// TODO Auto-generated method stub
		return null;
	}

}
