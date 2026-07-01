package contactcollator;

import java.io.Serializable;
import java.util.ArrayList;

import PamModel.parametermanager.ManagedParameters;
import PamModel.parametermanager.PamParameterSet;
import detectiongrouplocaliser.DetectionGroupDataBlock;

public class CollatorParams implements ManagedParameters, Cloneable, Serializable {

	public static final long serialVersionUID = 1L;
	
	public ArrayList<CollatorParamSet> parameterSets = new ArrayList();
	
	public boolean listenForAnnotations=false;
	
	public String detectionGroupSource = null;
	
	public ArrayList<String> superDetectionSourceList = new ArrayList<String>();
	
	public int getNParameterSets() {
		return parameterSets.size();
	}
	
	public void addParameterSet(CollatorParamSet paramSet) {
		parameterSets.add(paramSet);
	}

	@Override
	public PamParameterSet getParameterSet() {
		return PamParameterSet.autoGenerate(this);
	}

	@Override
	protected CollatorParams clone() {
		try {
			return (CollatorParams) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
