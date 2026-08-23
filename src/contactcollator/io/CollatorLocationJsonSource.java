package contactcollator.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

import Localiser.algorithms.locErrors.LocaliserError;
import PamUtils.LatLong;
import PamUtils.PamCalendar;
import PamguardMVC.PamDataUnit;
import contactcollator.CollatorDataUnit;
import contactcollator.localisations.ContactLocalisationSummary;
import contactcollator.localisations.LocalisationSummary;
import jsonStorage.JSONObjectDataSource;

public class CollatorLocationJsonSource extends JSONObjectDataSource<CollatorLocationJasonData> {

	public CollatorLocationJsonSource() {
		super(false);
	}

	@Override
	protected CollatorLocationJasonData initializeObjectData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void addClassSpecificFields(PamDataUnit pamDataUnit) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void setObjectType(PamDataUnit pamDataUnit) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getPackedObject(PamDataUnit dataUnit) {
		/*
		 * I think I have to ignore the standard method and just go totally bespoke on this ?
		 * Or is that reinventing a wheel ?  
		 */
		CollatorDataUnit cdu = (CollatorDataUnit) dataUnit;
		LocalisationSummary locSummary = null;
		ContactLocalisationSummary clocSummary = cdu.getLocalisationSummary();
		if (clocSummary != null && clocSummary.getSummaries().size() > 0) {
			locSummary = clocSummary.getSummaries().get(0);
		}
		// there is only one loc summary per data unit, so can get on and json it. 

		JsonFactory jf = new JsonFactory();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		String errString = null;
		String ERRREPLACE = "aofjoifujeg";
		try {
			JsonGenerator jg = jf.createGenerator(os);
			jg.writeStartObject();
			jg.writeStringField("Timestamp", PamCalendar.formatDBDateTime(cdu.getTimeMilliseconds(), true));
			jg.writeStringField("Endtime", PamCalendar.formatDBDateTime(cdu.getEndTimeInMilliseconds(), true));
			jg.writeNumberField("UID", cdu.getUID());
			jg.writeNumberField("Channels", cdu.getChannelBitmap());
			jg.writeStringField("Type", cdu.getStreamName());
			if (locSummary != null) {
				if (locSummary.getMeanBearing() != null) {
					jg.writeNumberField("Bearing", locSummary.getMeanBearing());
				}
				LatLong ll = locSummary.getLatLong();
				if (ll != null) {
					jg.writeNumberField("Latitude", ll.getLatitude());
					jg.writeNumberField("Longitude", ll.getLongitude());
				}
				LocaliserError locErr = locSummary.getLocaliserError();
				if (locErr != null) {
					errString = locErr.getJsonErrorString();
					// bit of a bodge, but it will do for now. 
					jg.writeStringField("Error", ERRREPLACE);
//					jg.writeFieldName("Error");
//					os.write(errStr.getBytes());
				}
			}
			
//			jg.writeEndArray();
			jg.writeEndObject();
			jg.close();
			
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		String jsonString = os.toString();
		if (errString != null) {
			// finish bodging in the error string
			jsonString = jsonString.replace("\""+ERRREPLACE+"\"", errString);
		}
		return jsonString;
	}

}
