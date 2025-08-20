package contactcollator.io;

import jsonStorage.JSONObjectData;

public class CollatorJsonData extends JSONObjectData{
	
	double[] wavData;
	float wavFs;
	double centerBearingDegrees;
	double bearingGroupStdDevDegrees;
	String speciedAnnotation;
	String buoyId;
	double lowFrequency;
	double highFrequency;
	public String triggerSource;
	public long startTime;
	public long endTime;
	public int detectionCount;
	public double stdRadians;
	

}
