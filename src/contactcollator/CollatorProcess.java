package contactcollator;

import java.util.ArrayList;

import Acquisition.AcquisitionControl;
import Array.ArrayManager;
import PamController.PamControlledUnit;
import PamController.PamController;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;
import contactcollator.io.CollatorExtendedLogging;
import contactcollator.io.CollatorLogging;
import contactcollator.swing.CollatorOverlayGraphics;


public class CollatorProcess extends PamProcess {

	private CollatorControl collatorControl;
	
	private CollatorDataBlock collatorDataBlock;
	
	private AnnotatedCollationDataBlock annotatedDataBlock;
	
	private ArrayList<CollatorStreamProcess> streamProcesses = new ArrayList<>();
	
	SuperDetDataBlock detGroupDataBlock;
		

	public CollatorProcess(CollatorControl collatorControl) {
		super(collatorControl,null);
		this.collatorControl = collatorControl;
		collatorDataBlock = new CollatorDataBlock(collatorControl.getUnitName(), this, 0);
		collatorDataBlock.setOverlayDraw(new CollatorOverlayGraphics(collatorDataBlock));
		collatorDataBlock.SetLogging(new CollatorExtendedLogging(collatorControl, collatorDataBlock));
		collatorDataBlock.setBinaryDataSource(new CollatorBinaryStorage(collatorControl,collatorDataBlock));
		StandardSymbolManager symbolManager = new ClipSymbolManager(collatorDataBlock, ClipOverlayGraphics.defSymbol, true);
		symbolManager.addSymbolOption(StandardSymbolManager.HAS_LINE_AND_LENGTH);
		collatorDataBlock.setPamSymbolManager(symbolManager);
		addOutputDataBlock(collatorDataBlock);
		annotatedDataBlock = new AnnotatedCollationDataBlock("Annotated_Detections",this,0);
		annotatedDataBlock.setOverlayDraw(new CollatorOverlayGraphics(annotatedDataBlock));
		annotatedDataBlock.SetLogging(new CollatorExtendedLogging(collatorControl, annotatedDataBlock));
		annotatedDataBlock.setJSONDataSource(new CollatorJsonDataSource());
		addOutputDataBlock(annotatedDataBlock);
		
	}
	
	public void addSubDetection(PamDataUnit newUnit,SuperDetection detGroupDu) {
 		DataAnnotation ann = detGroupDu.getDataAnnotation(detGroupDu.getNumDataAnnotations()-1);
 		if(detGroupDu instanceof DetectionGroupDataUnit) {
 			addAnnotation(newUnit,ann);
 		}

	}
	
	public void addAnnotation(PamDataUnit newUnit,DataAnnotation ann) {
		
		System.out.println("Add annotation called");
		
		CollatorDataUnit annotatedCollatorUnit;
 		if(!(newUnit instanceof CollatorDataUnit)) {
 			return;
 		}else {
 			annotatedCollatorUnit = ((CollatorDataUnit) newUnit).clone();
 			//annotatedCollatorUnit = collatorDataBlock.findUnitByUIDandUTC(newUnit.getUID(), newUnit.getTimeMilliseconds());
 		}
 		
 		annotatedCollatorUnit.findTriggerData();
		
 		if(ann.toString().equals("FALSE")) {
 			return;
 		}
 		
		 if(newUnit.getParentDataBlock().getBinaryDataSource()!=null && newUnit.getParentDataBlock().getBinaryDataSource().getBinaryStorageStream()!=null) {
			 annotatedCollatorUnit.setBinaryFileName(newUnit.getParentDataBlock().getBinaryDataSource().getBinaryStorageStream().getMainFileName());
		 }
		 
		 
		 if(annotatedCollatorUnit.getTriggerData().getDataList()!=null
				 &&annotatedCollatorUnit.getTriggerData().getDataList().size()>0
				 &&annotatedCollatorUnit.getTriggerData().getDataList().get(0)!=null) {
			 			 
			 int n =  annotatedCollatorUnit.getTriggerData().getDataList().get(0).getSuperDetectionsCount();
			 
			 for(int i=0;i<n;i++) {
				SuperDetection sup = annotatedCollatorUnit.getTriggerData().getDataList().get(0).getSuperDetection(i);
				if(sup instanceof Group3DDataUnit){
					
					Group3DDataUnit loc3d = (Group3DDataUnit) sup;
										
					annotatedCollatorUnit.setLocalization3D(loc3d);
				}		 
			 }
		 }
		 
		 
		annotatedCollatorUnit.setSpeciesID(ann.toString());
 		annotatedDataBlock.addPamData(annotatedCollatorUnit);
 		
 		//System.out.println("Adding annotation for "+newUnit.toString());
	}
	
	public void addGroup3DLoc(Group3DDataUnit superDu,ConnectedRegionDataUnit crdu) {
		CollatorDataUnit collatorUnit = collatorDataBlock.findClipFromCRDU(crdu);
		if(collatorUnit == null) {
			System.out.println("Can't find collator unit for crdu "+crdu.getUID());
			return;
		}
		collatorUnit.setLocalization3D(superDu);
		collatorDataBlock.updatePamData(collatorUnit, System.currentTimeMillis());
		CollatorDisplayProvider displayProvider = this.collatorControl.getStreamDisplayProvider(collatorUnit.getStreamName());
		if(displayProvider!=null) {
			displayProvider.displayPanel.paintEdge(collatorUnit);
		}
		
	}

	
	@Override
	public void updateData(PamObservable observable, PamDataUnit pamDataUnit) {
		if(!(pamDataUnit instanceof SuperDetection)) {
			return;
		}
		SuperDetection du = (SuperDetection) pamDataUnit;
		PamDataUnit u;
		ArrayList<SubdetectionInfo<PamDataUnit>> lastAddedSubDetections = du.getAndResetLastAddedSubDetections();
		if(lastAddedSubDetections!=null && 
			lastAddedSubDetections.size()>0 && 
			(lastAddedSubDetections.get(0).getSubDetection() instanceof ClickDetection)) {
			addClickDetectionEvent(lastAddedSubDetections,du);
		}
		for(SubdetectionInfo<PamDataUnit> nextInfo : lastAddedSubDetections) {
			if(annotatedDataBlock.findUnitByUIDandUTC(nextInfo.getSubDetection().getUID(), nextInfo.getSubDetection().getTimeMilliseconds())!=null) {
				continue;
			}
			if(du instanceof Group3DDataUnit && nextInfo.getSubDetection() instanceof ConnectedRegionDataUnit) {
				addGroup3DLoc((Group3DDataUnit)du,(ConnectedRegionDataUnit) nextInfo.getSubDetection());
			}
			addSubDetection(nextInfo.getSubDetection(),du);
		}
		
	}
	
	@Override
	public void newData(PamObservable observable, PamDataUnit pamDataUnit) {
		if(!(pamDataUnit instanceof SuperDetection)) {
			return;
		}
		updateData(observable,pamDataUnit);
	}
	
	@Override
	public void pamStart() {
		
	}

	@Override
	public void pamStop() {
		
	}

	@Override
	public void setupProcess() {
		super.setupProcess();
		
		
		/*
		 *  could have anything in it, so set the channel map to the acquisition map.
		 *  However, this may not be subscribed to a process, so go to the array ?
		 */
		int nPhones = ArrayManager.getArrayManager().getCurrentArray().getHydrophoneCount();
		int hydrophoneMap = PamUtils.makeChannelMap(nPhones);
		collatorDataBlock.setChannelMap(hydrophoneMap);
		// sample rate is tricky, since it's going to be variable, so can't really use that !
		
		if(collatorControl.getCollatorParams().listenForAnnotations) {
			
			annotatedDataBlock.setChannelMap(hydrophoneMap);

			if(collatorControl.getCollatorParams().superDetectionSourceList!=null) {
				for(String nextBlockName:collatorControl.getCollatorParams().superDetectionSourceList) {
					PamDataBlock block = PamController.getInstance().getDataBlockByLongName(nextBlockName);
					if(block!=null && block instanceof SuperDetDataBlock) {
						detGroupDataBlock =  (SuperDetDataBlock) block;
						detGroupDataBlock.addObserver(this);
					}
				}
			}
			
		}
		
		organiseStreamProcesses();
		
		// set the parent process of the main output datablock (i.e. the parent of this) to be the first 
		// acquisition or a few things don't work properly. 
		AcquisitionControl daq = (AcquisitionControl) PamController.getInstance().findControlledUnit(AcquisitionControl.unitType);
		if (daq != null) {
			setParentDataBlock(daq.getRawDataBlock());
		}
		
	
	}

	@Override
	public void prepareProcess() {
		super.prepareProcess();
	}

	/**
	 * Set up a process for every set. try to avoid adding or removing any unnecessarily. 
	 */
	private synchronized void organiseStreamProcesses() {
		ArrayList<CollatorParamSet> sets = collatorControl.getCollatorParams().parameterSets;
		// go through the sets and add any which aren't there. 
		for (CollatorParamSet aSet : sets) {
			if (findStreamProcess(aSet.setName) == null) {
				addStreamProcess(aSet);
			}
		}
		// go through the processes and remove any which shouldn't be there, or update their parameters.
		for (CollatorStreamProcess csp : streamProcesses) {
			CollatorParamSet paramSet = collatorControl.findParameterSet(csp.getSetName());
			if (paramSet == null) {
				removeStreamProcess(csp);
			}
			else {
				csp.setParameters(paramSet);
			}
		}
	}
	
	private void addStreamProcess(CollatorParamSet aSet) {
		CollatorStreamProcess newProcess = new CollatorStreamProcess(collatorControl, collatorDataBlock, aSet);
		streamProcesses.add(newProcess);
		collatorControl.addPamProcess(newProcess);
		newProcess.setupProcess();
	}

	private void removeStreamProcess(CollatorStreamProcess streamProcess) {
		streamProcess.destroyProcess();
		collatorControl.removePamProcess(streamProcess);
		streamProcesses.remove(streamProcess);
	}

	/**
	 * find a stream process. These are identified by name (I can't think of another way)
	 * @param streamName name from a parameter set
	 * @return sub process or null if not found.
	 */
	public CollatorStreamProcess findStreamProcess(String streamName) {
		try {
			for (CollatorStreamProcess sp : streamProcesses) {
				if (sp.getSetName().equals(streamName)) {
					return sp;
				}
			}
		}
		catch(NullPointerException e) {} 
		return null;
	}
	
	public ArrayList<CollatorStreamProcess> getStreamProcesses(){
		return streamProcesses;
	}

	/**
	 * @return the collatorDataBlock
	 */
	public CollatorDataBlock getCollatorDataBlock() {
		return collatorDataBlock;
	}
	
	public void saveWAV(long fs, double[][] wav, long UID) {
		AudioFormat af = new Wav16AudioFormat(fs, wav.length);
		//SelectFolder select = new SelectFolder("Select output folder", 30, true);
		//select.
		//WavFileWriter wavFile = new WavFileWriter("C:\\SystemTesting\\Nextimus\\WavtestFolder\\"+String.valueOf(UID)+".wav", af);
		//wavFile.write(wav);
		//wavFile.close();
	}

	
	

}
