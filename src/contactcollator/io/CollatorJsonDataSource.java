package contactcollator.io;

import Array.ArrayManager;
import Array.Streamer;
import PamguardMVC.PamDataUnit;
import contactcollator.CollatorDataUnit;
import contactcollator.trigger.CollatorTriggerData;
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
		if(newUnit.getBearingSummaryLocalisation()!=null) {
			objectData.centerBearingDegrees = newUnit.getBearingSummaryLocalisation().getRealWorldVectors()[0].getHeading();
			if(newUnit.getBearingSummaryLocalisation().getBearingSummary()!=null) {
				objectData.stdRadians = newUnit.getBearingSummaryLocalisation().getBearingSummary().getStdHeading();
			}
		}
		objectData.lowFrequency = newUnit.getFrequency()[0];
		objectData.highFrequency = newUnit.getFrequency()[1];
		objectData.triggerSource = newUnit.triggerName;
		CollatorTriggerData triggerData = newUnit.findTriggerData();
		if(triggerData!=null && triggerData.getDataList().size()>0) {
			objectData.startTime = triggerData.getStartTime();
			objectData.endTime = triggerData.getEndTime();
			objectData.detectionCount = triggerData.getDataList().size();
			//newUnit.getHeadingHistogram().getData();
		}
		if(newUnit.getSpeciesID()!=null) {
			objectData.speciesAnnotation = newUnit.getSpeciesID();
		}else {
			objectData.speciesAnnotation = "not annotated";
		}
		
		//newUnit.
		
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
		return new CollatorJsonData();
	}

}
