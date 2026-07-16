package contactcollator.localisations;

import java.util.Iterator;
import java.util.Set;

import Jama.Matrix;
import Localiser.algorithms.locErrors.EllipticalError;
import Localiser.algorithms.locErrors.LocaliserError;
import PamDetection.AbstractLocalisation;
import PamDetection.LocContents;
import PamUtils.LatLong;
import PamguardMVC.PamDataUnit;
import contactcollator.CollatorStreamProcess;
import contactcollator.trigger.CollatorTriggerData;
import pamMaths.PamMatrix;
import pamMaths.STD;

public class LocalisationAverager extends BasicLocalisationSummariser {

	public LocalisationAverager(CollatorStreamProcess collatorStreamProcess) {
		super(collatorStreamProcess);
		// TODO Auto-generated constructor stub
	}

	@Override
	public LocalisationSummary summariseLocalisations(CollatorTriggerData trigger) {
		// get a set of units that have a latlong. 
		Set<PamDataUnit> locUnits = getLocalisedUnits(trigger.getDataList());
		if (locUnits.size() == 0) {
			return null;
		}
		// double check that everything does have the latlong we need !
		int nPoint = 0;
		Iterator<PamDataUnit> it = locUnits.iterator();
		int hydrophones = 0;
		while (it.hasNext()) {
			PamDataUnit unit = it.next();
			AbstractLocalisation loc = unit.getLocalisation();
			if (loc == null || !loc.hasLocContent(LocContents.HAS_LATLONG)) {
				it.remove();
			}
			hydrophones |= unit.getHydrophoneBitmap();
			nPoint++;
		}
		if (locUnits.size() == 0) {
			return null;
		}
		if (locUnits.size() == 1) {
			// single unit, so just return its localisatoin data. 
			AbstractLocalisation loc = locUnits.iterator().next().getLocalisation();
			return new LocalisationSummary(loc.getLatLong(0), loc.getLocError(0), hydrophones);
		}
		int iPoint = 0;
		double[] xPoints = new double[nPoint];
		double[] yPoints = new double[nPoint];
		LatLong[] latLongs = new LatLong[nPoint];
		 it = locUnits.iterator();
		while (it.hasNext()) {
			PamDataUnit unit = it.next();
			AbstractLocalisation loc = unit.getLocalisation();
			LatLong latLong = loc.getLatLong(0);
			latLongs[iPoint] = latLong; 
			xPoints[iPoint] = latLong.getLongitude();
			yPoints[iPoint] = latLong.getLatitude();
			iPoint++;
		}
		STD std = new STD();
		double meanX = std.getMean(xPoints);
		double meanY = std.getMean(yPoints);
		LatLong meanLatLong = new LatLong(meanY, meanX);
		/*
		 * Am now going to rotate about this to maximise the 
		 * statndard deviation of all points, which is then the major axis
		 * of an error ellipse, the minor axis will be the std of the perpendicular. 
		 */
		// translate the points so they are about the mean. 
		for (int i = 0; i < nPoint; i++) {
			xPoints[i] = meanLatLong.distanceToMetresX(latLongs[i]);
			yPoints[i] = meanLatLong.distanceToMetresY(latLongs[i]);
//			pointMatrix[0][i] = xPoints[i]-meanX;
//			pointMatrix[1][i] = yPoints[i]-meanY;
//			xPoints[i] -= meanX;
//			yPoints[i] -= meanY;
		}
		double[][] pointMatrix = {xPoints, yPoints};
		Matrix points = new Matrix(pointMatrix);
//		new Matrix
		double aStep = 10;
		double bestAngle = 0;
		double bestError = Double.MAX_VALUE;
		double bestXSpan = 0, bestYSpan = 0;
		for (double a = 0; a < 180; a+= aStep) {
			double ar = a * Math.PI / 180;
			double sa = Math.sin(ar);
			double ca = Math.cos(ar);
			double[][] m = {{ca, -sa}, {sa, ca}};
			Matrix rotMatrix = new Matrix(m);
			Matrix rotated = rotMatrix.times(points);
			double[] xr = rotated.getArray()[0];
			double[] yr = rotated.getArray()[1];
			double xSpan = std.getSTD(xr);
			double ySpan = std.getSTD(yr);
			double span = xSpan*ySpan;
//			System.out.printf("Angle %3.0f xSpan %3.1fm ySpan %3.1fm, a=%3.1fm^2\n", a, xSpan, ySpan, xSpan*ySpan);
			if (span < bestError) {
				bestError = span;
				bestAngle = a;
				bestXSpan = xSpan;
				bestYSpan = ySpan;
			}
		}
		bestAngle *= Math.PI/180;
		double[] errAngles = {bestAngle, 0, 0.};
		double[] bestErr = {bestXSpan, bestYSpan, 0.};
		EllipticalError err = new EllipticalError(errAngles, bestErr);
		LocalisationSummary summary = new LocalisationSummary(meanLatLong, err, hydrophones);
//		System.out.println("Localisation summary " + summary);
		return summary;
		
//		// do some sort of weighted average of all the localisations. then try to assess the standard deviation of 
//		// all those points, and work out the orientation of the resulting error ellipse. 
//		double x = 0;
//		double y = 0;
//		double nx = 0;
//		double ny = 0;
//		double varx = 0;
//		double vary = 0;
//				
//		for (PamDataUnit dataUnit : locUnits) {
//			AbstractLocalisation loc = dataUnit.getLocalisation();
////			if (loc == null) {
////				continue;
////			}
//			LatLong latLong = loc.getLatLong(0);
//			if (latLong == null) {
//				continue;
//			}
//			LocaliserError locErr = loc.getLocError(0);
//			x += latLong.getLongitude();
//			y += latLong.getLatitude();
//			nx++;
//			ny++;
//		}
//		x /= nx;
//		y /= ny;
//		for (PamDataUnit dataUnit : locUnits) {
//			AbstractLocalisation loc = dataUnit.getLocalisation();
////			if (loc == null) {
////				continue;
////			}
//			LatLong latLong = loc.getLatLong(0);
//			if (latLong == null) {
//				continue;
//			}
//			varx += Math.pow(latLong.getLongitude()-x, 2);
//			vary += Math.pow(latLong.getLatitude()-y, 2);
//		}
//		varx /= nx;
//		vary /= ny;
		
		
		
//		return null;
	}

}
