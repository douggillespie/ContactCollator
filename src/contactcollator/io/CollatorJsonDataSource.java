package contactcollator.io;

import Array.ArrayManager;
import Array.Streamer;
import PamguardMVC.PamDataUnit;
import contactcollator.CollatorDataUnit;
import jsonStorage.JSONObjectDataSource;

public class CollatorJsonDataSource extends JSONObjectDataSource<CollatorJsonData>{
	
	public CollatorJsonDataSource() {
		super();
		objectData = new CollatorJsonData();
	}

	@Override
	protected void addClassSpecificFields(PamDataUnit pamDataUnit) {
		CollatorDataUnit newUnit = (CollatorDataUnit) pamDataUnit;
		objectData.buoyId = getpbId(newUnit.getChannelBitmap());
		objectData.wavData = newUnit.getWaveData()[0];
		objectData.wavFs = newUnit.getSourceSampleRate();
		objectData.centerBearingDegrees = newUnit.getBearingSummaryLocalisation().getRealWorldVectors()[0].getHeading();
		objectData.lowFrequency = newUnit.getFrequency()[0];
		objectData.highFrequency = newUnit.getFrequency()[1];
		
	}
	
	private String getpbId(int channelBitmap) {
		int channelIdx = PamUtils.PamUtils.getLowestChannel(channelBitmap);
		int stremerIdx = ArrayManager.getArrayManager().getCurrentArray().getStreamerForPhone(channelIdx);
		Streamer streamer = ArrayManager.getArrayManager().getCurrentArray().getStreamer(stremerIdx);
		String streamerName = streamer.getStreamerName();
		String pbId = "pb"+streamerName.substring(streamerName.length()-3);
		return pbId;
	}

	@Override
	protected void setObjectType(PamDataUnit pamDataUnit) {
		objectData.identifier = -1;
	}

	@Override
	protected CollatorJsonData initializeObjectData() {
		// TODO Auto-generated method stub
		return  new CollatorJsonData();
	}

}
