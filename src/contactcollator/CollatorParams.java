package contactcollator;

import java.io.Serializable;
import java.util.ArrayList;

import PamModel.parametermanager.ManagedParameters;
import PamModel.parametermanager.PamParameterSet;
import PamModel.parametermanager.PamParameterSet.ParameterSetType;

public class CollatorParams implements ManagedParameters, Cloneable, Serializable {

	public static final long serialVersionUID = 1L;
	
	public ArrayList<CollatorParamSet> parameterSets = new ArrayList();
	
	public int getNParameterSets() {
		return parameterSets.size();
	}
	
	public void addParameterSet(CollatorParamSet paramSet) {
		parameterSets.add(paramSet);
	}

	@Override
	public PamParameterSet getParameterSet() {
		return PamParameterSet.autoGenerate(this, ParameterSetType.DETECTOR);
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
