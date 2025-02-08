package contactcollator;

import java.util.ArrayList;
import java.util.ListIterator;

import Localiser.algorithms.locErrors.LocaliserError;
import PamController.PamController;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.RawDataHolder;
import PamguardMVC.RawDataTransforms;
import PamguardMVC.superdet.SuperDetection;
import annotation.DataAnnotation;
import annotation.localise.targetmotion.TMAnnotation;
import clipgenerator.ClipDataUnit;
import contactcollator.bearings.BearingSummary;
import contactcollator.bearings.BearingSummaryLocalisation;
import contactcollator.bearings.HeadingHistogram;
import contactcollator.trigger.CollatorTriggerData;
import group3dlocaliser.Group3DDataUnit;

/**
 * Data output from Contact Collator. The datablock may contain these data units from multiple
 * output streams, each having a different sample rate which may make things a little confusing.
 * <p>Mostly this class is just a wrapper around the ClipDataUnit, so most data fields are accessed
 * through getters in the super class.  
 * @author dg50
 *
 */
public class CollatorDataUnit extends ClipDataUnit implements RawDataHolder,Cloneable {
		
	private CollatorTriggerData triggerData;
	
	private RawDataTransforms rawDataTransforms;

	private BearingSummaryLocalisation bearingSummaryLocalisation;

	private String streamName;
	
	private HeadingHistogram headingHistogram;
	
	private ArrayList<Long> triggerUTCs;
	
	private String speciesID;
	
	private String binaryFileName;
	
	//private TMAnnotation localization3D= null;
	
	private double sourceLat;
	
	private double sourceLon;
	
	private double sourceErrorMagnitude0;
	
	private double sourceErrorMagnitude1;
	
	private long groupd3DLocalizationUID;

	private String groupd3DLocalizationStreamName;
	
	private Group3DDataUnit group3DDataUnit;
	
	//Adding final strings for group3d loc name, because the dataunits are returning null for parentdatablock
	private final String group3dLF = "Group 3D Localiser LF, Group";
	private final String group3dMF = "Group 3D Localiser MF, Group";

	

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
	public CollatorDataUnit(long timeMilliseconds, int channelBitmap, long startSample, float sampleRate, int durationSamples, CollatorTriggerData triggerData, String streamName, double[][] wavData) {
		super(timeMilliseconds, triggerData.getStartTime(), startSample, durationSamples, channelBitmap, null, triggerData.getTriggerName(), wavData, sampleRate);
		this.triggerData = triggerData;
		this.streamName = streamName;
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
	public CollatorDataUnit(long timeMilliseconds, int channelBitmap, long startSample, float sampleRate, int durationSamples, String triggerName, long triggerTime, String streamName, double[][] wavData) {
		super(timeMilliseconds, triggerTime, startSample, durationSamples, channelBitmap, null, triggerName, wavData, sampleRate);
		this.streamName = streamName;
	}

	@Override
	public RawDataTransforms getDataTransforms() {
		if (rawDataTransforms == null) {
			rawDataTransforms = new RawDataTransforms(this, this);
		}
		return rawDataTransforms;
	}
	
	public void setDataTransforms(RawDataTransforms rawDataTransforms) {
		this.rawDataTransforms = rawDataTransforms;
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
	
	public void setTriggerData(CollatorTriggerData triggerData) {
		this.triggerData = triggerData;
	}

	/**
	 * @return the streamName
	 */
	public String getStreamName() {
		return streamName;
	}

	/**
	 * @return the headingHistogram
	 */
	public HeadingHistogram getHeadingHistogram() {
		return headingHistogram;
	}

	public String getSpeciesID() {
		return speciesID;
	}

	public String getBinaryFileName() {
		return binaryFileName;
	}

	public double getSourceLat() {
		return sourceLat;
	}

	public void setSourceLat(double sourceLat) {
		this.sourceLat = sourceLat;
	}

	public double getSourceLon() {
		return sourceLon;
	}

	public void setSourceLon(double sourceLon) {
		this.sourceLon = sourceLon;
	}

	public double getSourceErrorMagnitude0() {
		return sourceErrorMagnitude0;
	}

	public void setSourceErrorMagnitude0(double sourceErrorMagnitude0) {
		this.sourceErrorMagnitude0 = sourceErrorMagnitude0;
	}

	public double getSourceErrorMagnitude1() {
		return sourceErrorMagnitude1;
	}

	public void setSourceErrorMagnitude1(double sourceErrorMagnitude1) {
		this.sourceErrorMagnitude1 = sourceErrorMagnitude1;
	}

	public void setBinaryFileName(String binaryFileName) {
		this.binaryFileName = binaryFileName;
	}

	public void setSpeciesID(String speciesID) {
		this.speciesID = speciesID;
	}
	
	public long getGroupd3DLocalizationUID() {
		return groupd3DLocalizationUID;
	}

	public void setGroupd3DLocalizationUID(long groupd3dLocalizationUID) {
		groupd3DLocalizationUID = groupd3dLocalizationUID;
	}

	public String getGroupd3DLocalizationStreamName() {
		return groupd3DLocalizationStreamName;
	}

	public void setGroupd3DLocalizationStreamName(String groupd3dLocalizationStreamName) {
		groupd3DLocalizationStreamName = groupd3dLocalizationStreamName;
	}

	public void setLocalization3D(Group3DDataUnit localization3D) {
		if(localization3D.getLocalisation()==null) {
			return;
		}
		int nAmbiguity = localization3D.getLocalisation().getAmbiguityCount();
		this.group3DDataUnit = localization3D;
		sourceLat =   localization3D.getLocalisation().getOriginLatLong().getLatitude();
		sourceLon =   localization3D.getLocalisation().getOriginLatLong().getLongitude();
		for(int i=0;i<nAmbiguity;i++) {
			 LocaliserError locError =  localization3D.getLocalisation().getLocError(i);
			 if(i==0) {
				 sourceErrorMagnitude0 = locError.getErrorMagnitude();
			 }else {
				 sourceErrorMagnitude1 = locError.getErrorMagnitude();
			 }
		}
		if(localization3D.getParentDataBlock()!=null) {
			groupd3DLocalizationStreamName = localization3D.getParentDataBlock().getLongDataName();
		}
		groupd3DLocalizationUID = localization3D.getUID();
	
	}

	/**
	 * @param headingHistogram the headingHistogram to set
	 */
	public void setHeadingHistogram(HeadingHistogram headingHistogram) {
		this.headingHistogram = headingHistogram;
	}
	
	public void setTriggerUTCs(ArrayList<Long> trigTimes) {
		this.triggerUTCs = trigTimes;
	}
	
	public CollatorTriggerData findTriggerData() {
		if (triggerData != null && triggerData.getDataList().size()>0) {
			return triggerData;
		}
		
		String trigName = this.triggerName;
		long trigMillis = this.triggerMilliseconds;
//		long startMillis = clipDataUnit.getTimeMilliseconds();
		PamDataBlock<PamDataUnit> dataBlock = findTriggerDataBlock(trigName);
		if (dataBlock == null) {
			return null;
		}
		return triggerData = findTriggerData2(triggerUTCs,dataBlock, 20,0);
	}
	
	private CollatorTriggerData findTriggerData2(ArrayList<Long> trigTimes, PamDataBlock<PamDataUnit> dataBlock, int timeJitter,int attempt) {
		long t1;
		long t2;
		
		ArrayList<PamDataUnit> detectorData = new ArrayList<PamDataUnit>();

		if(trigTimes==null || trigTimes.size()==0) {
			trigTimes = new ArrayList<>();
			trigTimes.add(this.getTriggerMilliseconds());
		}
		
		synchronized (dataBlock.getSynchLock()) {
			
			for(long nextTrigTime:trigTimes) {
				ListIterator<PamDataUnit> iter = dataBlock.getListIterator(PamDataBlock.ITERATOR_END);
				while (iter.hasPrevious()) {
					t1 = nextTrigTime - timeJitter;
					t2 = nextTrigTime + timeJitter;
					PamDataUnit trigUnit = iter.previous();
					long trigTime = trigUnit.getTimeMilliseconds();
					if (trigTime >= t1 && trigTime <= t2 && (trigUnit.getChannelBitmap() & this.getChannelBitmap()) != 0) {
						detectorData.add(trigUnit);
						break;
					}
				}
			}
			
		}
		
		if(detectorData.size()==0) {
			int x=1;
		}

		return new CollatorTriggerData(min(trigTimes),max(trigTimes),dataBlock.getLongDataName(),detectorData);

	}
	
	private String lastFoundName;
	private PamDataBlock<PamDataUnit> lastFoundBlock;
	
	public PamDataBlock<PamDataUnit> findTriggerDataBlock(String dataName){
		if (dataName == null) {
			return null;
		}
		if (dataName.equals(lastFoundName)) {
			return lastFoundBlock;
		}
		PamDataBlock<PamDataUnit> dataBlock = PamController.getInstance().getDetectorDataBlock(dataName);
		if (dataBlock == null) {
			return null;
		}
		lastFoundName = new String(dataName);
		lastFoundBlock = dataBlock;
		return dataBlock;
	}
	
	private long min(ArrayList<Long> arr) {
		if(arr==null||arr.size()==0) {
			return 0;
		}
		long min=arr.get(0);
		for(long el:arr) {
			if(el<min) {
				min=el;
			}
		}
		return min;
	}
	
	private long max(ArrayList<Long> arr) {
		if(arr==null||arr.size()==0) {
			return 0;
		}
		long max=arr.get(0);
		for(long el:arr) {
			if(el>max) {
				max=el;
			}
		}
		return max;
	}
	
	public CollatorDataUnit clone() {
		CollatorDataUnit newUnit = new CollatorDataUnit(this.getTimeMilliseconds(),
				this.getChannelBitmap(),
				this.getStartSample(),
				this.getSampleRate(),
				this.getSampleDuration().intValue(),
				this.getTriggerData(),
				this.getStreamName(),
				this.getWaveData());
		newUnit.setUID(this.getUID());
		newUnit.setDataTransforms(this.getDataTransforms());
		newUnit.setBearingSummary(this.getBearingSummaryLocalisation());
		newUnit.setHeadingHistogram(this.getHeadingHistogram());
		newUnit.setTriggerUTCs(this.triggerUTCs);
		return newUnit;
		
	}

	public Group3DDataUnit getGroup3DDataUnit() {
		return group3DDataUnit;
	}

	public void setGroup3DDataUnit(Group3DDataUnit group3dDataUnit) {
		group3DDataUnit = group3dDataUnit;
	}
	
	@Override
	public void addDataAnnotation(DataAnnotation dataAnnotation) {
		super.addDataAnnotation(dataAnnotation);
		if(this.getTriggerDataUnit()!=null) {
			this.getTriggerDataUnit().addDataAnnotation(dataAnnotation);
		}
		
	}
	
	
	
	@Override
	public void addSuperDetection(SuperDetection superDetection) {
		super.addSuperDetection(superDetection);
		this.findTriggerData();
		if(this.getTriggerDataUnit()!=null) {
			this.getTriggerDataUnit().addSuperDetection(superDetection);
		}
		if(PamController.getInstance().getRunMode()==PamController.RUN_PAMVIEW) {
			//CollatorProcess process = (CollatorProcess) this.getParentDataBlock();
			//process.addSubDetection(this, superDetection);
			CollatorControl cont = (CollatorControl) PamController.getInstance().findControlledUnit("Contact Collator");
			cont.getCollatorProcess().addSubDetection(this, superDetection);
			int x=0;
		}
	}

}
