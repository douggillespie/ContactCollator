package contactcollator.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

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
import pamMaths.PamVector;

public class CollatorBinaryStorage extends BinaryDataSource {

	private CollatorControl collatorControl;
	
	private static final int CURRENTVERSION = 1;
	
	private DataOutputStream dos;
	
	private ByteArrayOutputStream bos;
	
	RawDataUtils rawDataUtils = new RawDataUtils();

	public CollatorBinaryStorage(CollatorControl collatorControl, PamDataBlock parentDataBlock) {
		super(parentDataBlock);
		this.collatorControl = collatorControl;
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
		return 0;
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
			PamDataUnit trigUnit = collatorDataUnit.getTriggerDataUnit();
			dos.writeLong(trigUnit == null ? 0 : trigUnit.getUID());
			dos.writeFloat(collatorDataUnit.getSourceSampleRate());
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
				int n = bearingSummary.getnPoints();
				int phones = bearingSummary.getHydrophoneMap();
				double stdHead = bearingSummary.getStdHeading();
				int ambiguity = bearingSummary.isAmbiguity() ? 1 : 0;
				dos.writeShort(n);
				dos.writeInt(phones);
				dos.writeByte(ambiguity);
				dos.writeFloat((float) head);
				dos.writeFloat((float) slant);
				dos.writeFloat((float) stdHead);
			}
			
			
			
			rawDataUtils.writeWaveClipInt8(dos, collatorDataUnit.getRawData());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public PamDataUnit sinkData(BinaryObjectData binaryObjectData, BinaryHeader bh, int moduleVersion) {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(binaryObjectData.getData()));
		BearingSummary bearingSummary = null;
		CollatorDataUnit cdu = null;
		try {
			long trigMillis = dis.readLong();
			String trigName = dis.readUTF();
			String streamName = dis.readUTF();
			long trigUID = dis.readLong();
			float fs = dis.readFloat();
			boolean hasBearing = dis.readByte() > 0;
			DataUnitBaseData baseData = binaryObjectData.getDataUnitBaseData();
			if (hasBearing) {
				int n = dis.readShort();
				int phones = dis.readInt();
				boolean ambiguity = dis.readByte() == 0 ? false : true;
				double head = dis.readFloat();
				double slant = dis.readFloat();
				double headSTD = dis.readFloat();
				PamVector vec = PamVector.fromHeadAndSlant(head, slant);
				bearingSummary = new BearingSummary(n, vec, headSTD, phones, ambiguity);
			}
			double[][] rawData = rawDataUtils.readWavClipInt8(dis);

			cdu = new CollatorDataUnit(binaryObjectData.getTimeMilliseconds(), baseData.getChannelBitmap(), baseData.getStartSample(), fs, rawData[0].length, trigName, trigMillis, streamName, rawData);
			cdu.setBearingSummary(new BearingSummaryLocalisation(cdu, bearingSummary));
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
