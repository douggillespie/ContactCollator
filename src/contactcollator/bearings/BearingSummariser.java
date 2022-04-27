package contactcollator.bearings;

import java.util.ArrayList;

import PamDetection.AbstractLocalisation;
import PamUtils.PamUtils;
import PamguardMVC.PamDataUnit;
import PamguardMVC.superdet.SuperDetection;
import pamMaths.PamVector;
import pamMaths.STD;

/**
 * functions to generate a bearing summary.
 * @author dg50
 *
 */
public class BearingSummariser {

	private int n;
	private PamVector totalVector;
	private ArrayList<PamVector> allVectors = new ArrayList();
	private int ambiguityCount;
	private STD std = new STD();
	private int hydrophoneMap;
		
	public BearingSummariser() {
		super();
		reset();
	}
	public void reset() {
		totalVector = new PamVector();
		n = 0;
		allVectors.clear();
		ambiguityCount = 0;
		hydrophoneMap = 0;
	}

	public void addData(PamDataUnit pamDataUnit) {
		// if it's got sub detections, use them instead. 
		if (pamDataUnit instanceof SuperDetection) {
			SuperDetection superDet = (SuperDetection) pamDataUnit;
			synchronized (superDet.getSubDetectionSyncronisation()) {
				int n = superDet.getSubDetectionsCount();
				for (int i = 0; i < n; i++) {
					PamDataUnit subDet = superDet.getSubDetection(i);
					useData(subDet);
				}
			}
		}
		else {
			useData(pamDataUnit);
		}
	}

	private void useData(PamDataUnit pamDataUnit) {
		AbstractLocalisation loc = pamDataUnit.getLocalisation();
		if (loc == null) {
			return;
		}
		PamVector[] vec = loc.getWorldVectors();
		if (vec.length == 0) {
			return;
		}
		if (vec.length > 1) {
			ambiguityCount++;
		}
		n++;
		allVectors.add(vec[0]);
		totalVector = totalVector.add(vec[0]);
		hydrophoneMap |= pamDataUnit.getHydrophoneBitmap();
	}
	
	public BearingSummary getSummary() {
		if (n == 0) {
			return null;
		}
		totalVector.normalise();
		double meanHead = totalVector.getHeading();
		/*
		 *  now get the standard deviation and possibly the confidence
		 *  intervals of the bearings. 
		 */
		double[] angDiffs = new double[n];
		for (int i = 0; i < n; i++) {
			angDiffs[i] = PamUtils.constrainedAngleR(allVectors.get(i).getHeading()-meanHead);
		}
		double stdAngle = std.getSTD(angDiffs);
		return new BearingSummary(n, totalVector, stdAngle, hydrophoneMap, ambiguityCount>0);
	}
	
}
