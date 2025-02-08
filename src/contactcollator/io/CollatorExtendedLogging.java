package contactcollator.io;

import java.sql.Types;

import Array.ArrayManager;
import Array.Streamer;
import GPS.GpsData;
import Localiser.algorithms.locErrors.LocaliserError;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import contactcollator.CollatorControl;
import contactcollator.CollatorDataBlock;
import contactcollator.CollatorDataUnit;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLTypes;

public class CollatorExtendedLogging extends CollatorLogging{
	
	
	private PamTableItem 
	 buoyLatitude,
	 buoyLongitude,
	 buoyId,
	 clipBinaryFileName,
	 detectionUID,
	 hasBearing,
	 bearing0,
	 bearingError,
	 speciesId,
	 sourceLatitude,
	 sourceLongitude,
	 sourceLocalizationError0,
	 sourceLocalizationError1,
	 groupd3DLocalizationStreamName,
	 group3DLocalizationUID;

	protected CollatorExtendedLogging(CollatorControl collatorControl, CollatorDataBlock pamDataBlock) {
		super(collatorControl,pamDataBlock);
		
		PamTableDefinition table = new PamTableDefinition(collatorControl.getUnitName());
		
		table.addTableItem(buoyLatitude = new PamTableItem("buoyLatitude", Types.DOUBLE)); 
		table.addTableItem(buoyLongitude = new PamTableItem("buoyLongitude", Types.DOUBLE)); 
		table.addTableItem(buoyId = new PamTableItem("buoyId", Types.CHAR,10)); 
		table.addTableItem(clipBinaryFileName = new PamTableItem("clipBinaryFileName", Types.CHAR,100));
		table.addTableItem(detectionUID = new PamTableItem("detectionUID", Types.BIGINT)); 
		table.addTableItem(hasBearing = new PamTableItem("hasBearing", Types.BOOLEAN)); 
		table.addTableItem(bearing0 = new PamTableItem("bearing0", Types.DOUBLE)); 
		table.addTableItem(bearingError = new PamTableItem("bearingError", Types.DOUBLE)); 
		table.addTableItem(speciesId = new PamTableItem("speciesId", Types.CHAR,10)); 
		table.addTableItem(sourceLatitude = new PamTableItem("sourceLatitude", Types.DOUBLE)); 
		table.addTableItem(sourceLongitude = new PamTableItem("sourceLongitude", Types.DOUBLE)); 
		table.addTableItem(sourceLocalizationError0 = new PamTableItem("sourceLocalizationError0", Types.DOUBLE)); 
		table.addTableItem(sourceLocalizationError1 = new PamTableItem("sourceLocalizationError1", Types.DOUBLE));
		table.addTableItem(group3DLocalizationUID = new PamTableItem("group3DLocalizationUID", Types.BIGINT));
		table.addTableItem(groupd3DLocalizationStreamName = new PamTableItem("groupd3DLocalizationStreamName", Types.CHAR,30)); 

		setTableDefinition(table);


	}

	@Override
	public void setTableData(SQLTypes sqlTypes, PamDataUnit pamDataUnit) {
		super.setTableData(sqlTypes, pamDataUnit);
		CollatorDataUnit newUnit = (CollatorDataUnit) pamDataUnit;
		
		
		sourceLatitude.setValue(newUnit.getSourceLat());
		sourceLongitude.setValue(newUnit.getSourceLon());
			
		sourceLocalizationError0.setValue(newUnit.getSourceErrorMagnitude0());
		sourceLocalizationError1.setValue(newUnit.getSourceErrorMagnitude1());
				 
		group3DLocalizationUID.setValue(newUnit.getGroupd3DLocalizationUID());
		groupd3DLocalizationStreamName.setValue(newUnit.getGroupd3DLocalizationStreamName());
		
		int chIdx = PamUtils.getLowestChannel(newUnit.getChannelBitmap());
		int streamerId = ArrayManager.getArrayManager().getCurrentArray().getStreamerForPhone(chIdx);
		Streamer streamer = ArrayManager.getArrayManager().getCurrentArray().getStreamer(streamerId);
		GpsData streamerGps = streamer.getHydrophoneLocator().getStreamerLatLong(System.currentTimeMillis());
		double lat = streamerGps.getLatitude();
		double lon = streamerGps.getLongitude();
		String id = streamer.getStreamerName();
		buoyLatitude.setValue(lat);
		buoyLongitude.setValue(lon);
		buoyId.setValue(id);
		 if(newUnit.getParentDataBlock().getBinaryDataSource()!=null && newUnit.getParentDataBlock().getBinaryDataSource().getBinaryStorageStream()!=null) {
			 clipBinaryFileName.setValue(newUnit.getParentDataBlock().getBinaryDataSource().getBinaryStorageStream().getMainFileName());
		 }else if(newUnit.getBinaryFileName()!=null){
			 clipBinaryFileName.setValue(newUnit.getBinaryFileName());
		 }
		 if(newUnit.findTriggerData().getDataList().size()>0) {
			 detectionUID.setValue(newUnit.findTriggerData().getDataList().get(0).getUID());
		 }else {
			 detectionUID.setValue(-1);
		 }
		 if(newUnit.getLocalisation()==null) {
			 hasBearing.setValue(false);
		 }else {
			 hasBearing.setValue(true);
			 double bearingReArray = newUnit.getLocalisation().getAngles()[0];
			 double headingRadians = streamer.getHeading()*Math.PI/180;
			 double bearingReN = bearingReArray+headingRadians;
			 bearing0.setValue(bearingReN);
			 if(newUnit.getLocalisation().getAngleErrors()!=null) {
				 bearingError.setValue(newUnit.getLocalisation().getAngleErrors()[0]);
			 }
		 }
		speciesId.setValue(newUnit.getSpeciesID());
	}


}
