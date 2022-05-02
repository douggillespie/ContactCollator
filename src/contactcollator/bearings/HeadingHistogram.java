package contactcollator.bearings;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import PamDetection.AbstractLocalisation;
import PamUtils.PamUtils;
import PamguardMVC.PamDataUnit;
import PamguardMVC.superdet.SuperDetection;
import contactcollator.CollatorDataUnit;
import contactcollator.trigger.CollatorTriggerData;
import pamMaths.PamVector;

/**
 * Make a heading histogram, i.e. a polar histogram for bearing information. 
 * Internal units are all in radians. Some return functions may convert
 * to degrees - which will be obvious from function names.  <p>
 * Internal angles will also (mostly)  be ranged between 0 and 2pi, any angles outside
 * that range will simply be rotated by +/- 2pi. 
 * 
 * @author dg50
 *
 */
public class HeadingHistogram implements Cloneable, Serializable {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Align the top bin centre on zero, otherwise, align it's edge
	 * (Edge alignment might be better for ambiguous bearings)
	 */
	private boolean zeroCentre;
	
	/**
	 * Number of bins in full 2pi circle. 
	 */
	private int nBins; 
	
	/**
	 * Data array. 
	 */
	private double[] data;
	
	/**
	 * Bin edges (array will be data.length+1 long)
	 */
	private double[] binEdges;
	
	/**
	 * Entry count
	 */
	private double nData;

	/**
	 * Width of each bin in Radians.
	 */
	private double binWidth;

	/**
	 * The maximum angle in the histogram. This is needed
	 * for calls to constrainangle. For a non zero centred histrogram 
	 * this will be 2pi-binWidth/2
	 */
	private double maximumAngle; 
	
	/**
	 * Create a heading histrogram with a given number of bearing bins. 
	 * @param nBins number of bearing bins
	 * @param zeroCentre centre the first bearing bin at 0, otherwise it will start at 0. 
	 */
	public HeadingHistogram(int nBins, boolean zeroCentre) {
		this.nBins = nBins;
		this.zeroCentre = zeroCentre;
		data = new double[nBins];
		calculateEdges();
	}

	
	/**
	 * Work out the bin edges. 
	 */
	private void calculateEdges() {
		binEdges = new double[nBins+1];
		binWidth = Math.PI*2./nBins;
		double e1 = 0;
		if (zeroCentre) {
			e1 = -binWidth/2.;
		}
		for (int i = 0; i < nBins+1; i++) {
			binEdges[i] = e1;
			e1 += binWidth;
		}
		maximumAngle = binEdges[nBins];
	}

	/**
	 * Reset the histogram. i.e. set all data to zero.
	 */
	public synchronized void reset() {
		Arrays.fill(data, 0);
		nData = 0;
	}

	/**
	 * Add data from a new data unit. If the data unit is a super detection, then the function
	 * will iterate through all sub detections and add the bearing data from the sub detections. 
	 * @param dataUnit PAMGuard data unit 
	 */
	public synchronized void addData(PamDataUnit dataUnit) {
		if (dataUnit instanceof SuperDetection) {
			SuperDetection superDet = (SuperDetection) dataUnit;
			synchronized (superDet.getSubDetectionSyncronisation()) {
				int n = superDet.getSubDetectionsCount();
				for (int i = 0; i < n; i++) {
					addData(superDet.getSubDetection(i));
				}
			}
			return;
		}
		else if (dataUnit instanceof CollatorDataUnit) {
			CollatorDataUnit collatorUnit = (CollatorDataUnit) dataUnit;
			CollatorTriggerData trigData = collatorUnit.getTriggerData();
			if (trigData != null) {
				List<PamDataUnit> dataList = trigData.getDataList();
				for (PamDataUnit aData : dataList) {
					addData(aData);
				}
			}
		}
		else {
			AbstractLocalisation loc = dataUnit.getLocalisation();
			if (loc == null) {
				return;
			}
			PamVector[] worldVecs = loc.getWorldVectors();
			if (worldVecs == null) {
				return;
			}
			for (int i = 0; i < worldVecs.length; i++) {
				double head = worldVecs[i].getHeading();
				addData(head);
			}
		}
	}
	
	/**
	 * Add data to the histogram. Data outside the range of the
	 * histogram will automatically be rotated by +/- 2pi, so an entry 
	 * value of -pi/4 will end up in the same bin as a value of 7pi/4.  
	 * @param radians Heading in radians
	 */
	synchronized public void addData(double radians) {
		data[getBinIndex(radians)] += 1;
		nData ++;
	}
	
	/**
	 * 
	 * Add data to the histogram. Data outside the range of the
	 * histogram will automatically be rotated by +/- 2pi, so an entry 
	 * value of -pi/4 will end up in the same bin as a value of 7pi/4.
	 * @param radians Heading in radians
	 * @param weight weight to add to histogram
	 */
	synchronized public void addData(double radians, double weight) {
		data[getBinIndex(radians)] += weight;
		nData++;
	}
	
	/**
	 * Get the index of the bin within the histogram for a given data entry.
	 * Angles are rotated to be within the range of the histogram (e.g. -pi/2 converts to 3pi/2) 
	 * @param radians heading, clockwise in radians. 
	 * @return bin index indx of the bin. 
	 */
	public int getBinIndex(double radians) {
		radians = PamUtils.constrainedAngleR(radians, maximumAngle);
		int bin = (int) (radians/binWidth);
		bin = Math.max(0, Math.min(bin, nBins-1));
		return bin;
	}
	
	/**
	 * Multiply all data in the histogram by a constant scaling factor.
	 * e.g. can be used to 'decay' data in a histogram over time. 
	 * @param scale scale factor, less than one, zero will be the same as a call to reset().
	 */
	synchronized public void scaleData(double scale) {
		for (int i = 0; i < nBins; i++) {
			data[i] *= scale;
		}
		nData *= scale;
	}

	/**
	 * IS the top bin centred at zero ? Otherwise it's edge is at zero  
	 * @return the zeroCentre
	 */
	public boolean isZeroCentre() {
		return zeroCentre;
	}

	/**
	 * @return the number of histogram bins
	 */
	public int getnBins() {
		return nBins;
	}

	/**
	 * @return the data array, length nBins
	 */
	public double[] getData() {
		return data;
	}

	/**
	 * If the first bin is zero centred, the first bin edge will be < 0. <br>
	 * length of array is nBins+1;
	 * @return the binEdges in radians
	 */
	public double[] getBinEdges() {
		return binEdges;
	}

	/**
	 * The number of data points in the histrogram.
	 * (may be non integer if the histrogram has been scaled) 
	 * @return the nData
	 */
	public double getnData() {
		return nData;
	}

	/**
	 * @return the binWidth in radians
	 */
	public double getBinWidth() {
		return binWidth;
	}

	/**
	 * @return the maximumAngle in radians
	 */
	public double getMaximumAngle() {
		return maximumAngle;
	}

	/**
	 * 
	 * @return The maximum value in the histogram data array
	 */
	public double getDataMax() {
		double max = data[0];
		for (int i = 1; i < nBins; i++) {
			max = Math.max(max, data[i]);
		}
		return max;
	}

	/**
	 * 
	 * @return The minimum value in the histogram data array
	 */
	public double getDataMin() {
		double min = data[0];
		for (int i = 1; i < nBins; i++) {
			min = Math.min(min, data[i]);
		}
		return min;
	}


	@Override
	public HeadingHistogram clone() {
		try {
			HeadingHistogram newOne = (HeadingHistogram) super.clone();
			/*
			 * Perform a hard clone by also cloning the data arrays, otherwise they get shared
			 * between the clone and the original
			 */
			newOne.data = data.clone();
			newOne.binEdges = binEdges.clone();
			return newOne;
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
}
