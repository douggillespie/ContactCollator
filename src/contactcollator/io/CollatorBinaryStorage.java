package contactcollator.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import Localiser.algorithms.locErrors.LocaliserError;
import Localiser.algorithms.locErrors.json.LocaliserErrorFactory;
import PamUtils.LatLong;
import PamguardMVC.DataUnitBaseData;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.RawDataUtils;
import binaryFileStorage.BinaryDataSource;
import binaryFileStorage.BinaryHeader;
import binaryFileStorage.BinaryObjectData;
import binaryFileStorage.ModuleFooter;
import binaryFileStorage.ModuleHeader;
import contactcollator.CollatorControl;
import contactcollator.CollatorDataUnit;
import contactcollator.bearings.BearingSummary;
import contactcollator.bearings.BearingSummaryLocalisation;
import contactcollator.bearings.HeadingHistogram;
import contactcollator.bearings.HeadingHistogramIO;
import contactcollator.localisations.ContactLocalisationSummary;
import contactcollator.localisations.LocalisationSummary;
import contactcollator.trigger.CollatorTriggerData;
import pamMaths.PamVector;

public class CollatorBinaryStorage extends BinaryDataSource {

	private CollatorControl collatorControl;
	
	/**
	 * Increase to 3 when adding multiple heading histograms. 
	 */
	private static final int CURRENTVERSION = 4;
	
	public static final int CLIP_COLLATOR = 12000;
	
	private LocaliserErrorFactory localiserErrorFactory = new LocaliserErrorFactory();
	
	private DataOutputStream dos;
	
	private ByteArrayOutputStream bos;
	
	private RawDataUtils rawDataUtils = new RawDataUtils();
	
	private HeadingHistogramIO headingHistogramIO;

	public CollatorBinaryStorage(CollatorControl collatorControl, PamDataBlock parentDataBlock) {
		super(parentDataBlock);
		this.collatorControl = collatorControl;
		headingHistogramIO = new HeadingHistogramIO();
	}

	@Override
	public String getStreamName() {
		return collatorControl.getUnitName();
	}

	@Override
	public int getStreamVersion() {
		return CURRENTVERSION;
	}

	@Override
	public int getModuleVersion() {
		return 165;
	}

	@Override
	public byte[] getModuleHeaderData() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public ModuleHeader sinkModuleHeader(BinaryObjectData binaryObjectData, BinaryHeader bh) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModuleFooter sinkModuleFooter(BinaryObjectData binaryObjectData, BinaryHeader bh,
			ModuleHeader moduleHeader) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BinaryObjectData getPackedData(PamDataUnit pamDataUnit) {
		if (dos == null || bos == null) {
			dos = new DataOutputStream(bos = new ByteArrayOutputStream());
		}
		else {
			bos.reset();
		}
		CollatorDataUnit collatorDataUnit = (CollatorDataUnit) pamDataUnit;

		try {
			dos.writeLong(collatorDataUnit.triggerMilliseconds);
			dos.writeUTF(collatorDataUnit.triggerName);
			dos.writeUTF(collatorDataUnit.getStreamName());
//			PamDataUnit trigUnit = collatorDataUnit.getTriggerDataUnit(); %only in clip, not used in Contacts. make list instead
//			dos.writeLong(trigUnit == null ? 0 : trigUnit.getUID());
			dos.writeFloat(collatorDataUnit.getSourceSampleRate());
			CollatorTriggerData triggerData = collatorDataUnit.getTriggerData();
			List<PamDataUnit> triggerList;
			if(triggerData!=null) {
				triggerList = triggerData.getDataList(); // does this need synchronising? OK if we don't use an iterator, so what if it grows!
			}else {
				triggerList = null;
			}
			int nTrig = triggerList != null ? triggerList.size() : 0;
			dos.writeShort(nTrig);			
			for (int i = 0; i < nTrig; i++) {
				dos.writeLong(triggerList.get(i).getUID());
				dos.writeLong(triggerList.get(i).getTimeMilliseconds());
			}
			
			/*
			 * And do heading information, since that's reasonably essential.
			 */			
			BearingSummary bearingSummary = collatorDataUnit.getBearingSummary();
			if (bearingSummary == null || bearingSummary.getWorldVector() == null) {
				dos.writeByte(0);
			}
			else {
				dos.writeByte(1);
				PamVector vec = bearingSummary.getWorldVector(); 
				double head = vec.getHeading();
				double slant = vec.getPitch();
				int nBear = bearingSummary.getnPoints();
				int phones = bearingSummary.getHydrophoneMap();
				double stdHead = bearingSummary.getStdHeading();
				int ambiguity = bearingSummary.isAmbiguity() ? 1 : 0;
				dos.writeShort(nBear);
				dos.writeInt(phones);
				dos.writeByte(ambiguity);
				dos.writeFloat((float) head);
				dos.writeFloat((float) slant);
				dos.writeFloat((float) stdHead);
			}
			
//			% and the bearing histograms, however many there are. 
			HashMap<Integer, HeadingHistogram> headingHists = collatorDataUnit.getHeadingHistograms();
			if (headingHists == null || headingHists.size() == 0) {
				dos.writeByte(0);
			}
			else {
				dos.writeByte(headingHists.size());
				Collection<HeadingHistogram> hists = headingHists.values();
				for (HeadingHistogram aHist : hists) {
					headingHistogramIO.writeHeadingHist(dos, aHist);
				}
			}
			// write out the localisation information:
			ContactLocalisationSummary locSummary = collatorDataUnit.getLocalisationSummary();
			if (locSummary == null || locSummary.getSummaries() == null || locSummary.getSummaries().size() == 0) {
				dos.writeShort(0);
			}
			else {
				ArrayList<LocalisationSummary> summaries = locSummary.getSummaries();
				dos.writeShort(summaries.size()); // number of summaries. 
				for (int i = 0; i < summaries.size(); i++) {
					LocalisationSummary aSummary = summaries.get(i);
					Double head = aSummary.getMeanBearing();
					LatLong ll = aSummary.getLatLong();
					dos.writeInt(aSummary.getHydrophoneMap());
					if (head != null) {
						dos.writeShort(1); // flag to say bearing
						dos.writeDouble(head);
					}
					else {
						dos.writeShort(2); // flag to say latlong
						dos.writeDouble(ll.getLatitude());
						dos.writeDouble(ll.getLongitude());
					}
					dos.writeUTF(aSummary.getLocaliserError().getJsonErrorString());
				}
			}
			
			
			rawDataUtils.writeWaveClipInt8(dos, collatorDataUnit.getRawData());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		BinaryObjectData pbo = new BinaryObjectData(CLIP_COLLATOR, bos.toByteArray());
		
		return pbo;
	}

	@Override
	public PamDataUnit sinkData(BinaryObjectData binaryObjectData, BinaryHeader bh, int moduleVersion) {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(binaryObjectData.getData()));
		BearingSummary bearingSummary = null;
		HeadingHistogram headingHist = null;
		CollatorDataUnit cdu = null;
		ArrayList<LocalisationSummary> localisationSummaries = null;
		try {
			long trigMillis = dis.readLong();
			String trigName = dis.readUTF();
			String streamName = dis.readUTF();
			//long trigUID = dis.readLong();
			float fs = dis.readFloat();
			
			int nTrig = dis.readShort();
			long[] triggerUIDs = new long[nTrig];
			ArrayList<Long> triggerUTCs = new ArrayList<Long>();
			for(int i=0;i<nTrig;i++) {
				triggerUIDs[i]=dis.readLong();
				triggerUTCs.add(dis.readLong());
			}
			
			boolean hasBearing = dis.readByte() > 0;
			DataUnitBaseData baseData = binaryObjectData.getDataUnitBaseData();
			if (hasBearing) {
				int n = dis.readShort();
				int phones = dis.readInt();
				boolean ambiguity = dis.readByte() == 0 ? false : true;
				double head = dis.readFloat();
				double slant = dis.readFloat();
				double headSTD = dis.readFloat();
				PamVector vec = PamVector.fromHeadAndSlant(head*180.0/Math.PI, slant*180.0/Math.PI);
				bearingSummary = new BearingSummary(n, vec, headSTD, phones, ambiguity);
			}
			int nHeadingHists = 1;
			if (moduleVersion >= 3) {
				nHeadingHists = dis.readByte();
			}
			HashMap<Integer, HeadingHistogram> headingHists = null;
			if (nHeadingHists > 0) {
				headingHists = new HashMap<Integer, HeadingHistogram>();
			}
			for (int i = 0; i < nHeadingHists; i++) {
				headingHist = headingHistogramIO.readHeadingHist(dis);
				headingHists.put(headingHist.getChannelMap(), headingHist);
			}
			
			if (moduleVersion >= 4) {
				// get the localisation data
				int nLocalisation = dis.readShort();
				LocalisationSummary locSum;
				localisationSummaries = new ArrayList<>();
				if (nLocalisation > 0) {
					for (int i = 0; i < nLocalisation; i++) {
						int channels = dis.readInt();
						int type = dis.readShort();
						double bear = 0;
						String errStr;
						LatLong ll = null;
						if (type == 1) {
							bear = dis.readDouble();
						}
						else {
							double lat = dis.readDouble();
							double lon = dis.readDouble();
							ll = new LatLong(lat, lon);
						}
						errStr = dis.readUTF();
						LocaliserError locErr = localiserErrorFactory.getErrorFromJsonString(errStr);
						if (type == 1) {
							locSum = new LocalisationSummary(baseData.getTimeMilliseconds(), baseData.getEndTime(), bear, locErr, channels, nTrig);
						}
						else {
							locSum = new LocalisationSummary(baseData.getTimeMilliseconds(), baseData.getEndTime(), ll, locErr, channels, nTrig);
						}
						localisationSummaries.add(locSum);
					}
					

				}
			}
			
			double[][] rawData = rawDataUtils.readWavClipInt8(dis);
			int dataLength = 0;
			if (rawData != null && rawData.length > 0) {
				dataLength = rawData[0].length;
			}
			cdu = new CollatorDataUnit(binaryObjectData.getTimeMilliseconds(), baseData.getChannelBitmap(), baseData.getStartSample(), fs, dataLength, trigName, trigMillis, streamName, rawData);
			if (localisationSummaries != null) {
				cdu.setLocalisationSummary(new ContactLocalisationSummary("Binary read", localisationSummaries));
			}
			//cdu.findTriggerDataUnit();
			cdu.setTriggerUTCs(triggerUTCs);
			
			if(bearingSummary!=null) {
				cdu.setBearingSummary(new BearingSummaryLocalisation(cdu, bearingSummary));
			}
			cdu.setHeadingHistograms(headingHists);
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return cdu;
	}
	
	@Override
	public void newFileOpened(File outputFile) {
		// TODO Auto-generated method stub
		
	}


}
