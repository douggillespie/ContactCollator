package contactcollator.localisations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import Jama.Matrix;
import Localiser.algorithms.locErrors.EllipticalError;
import Localiser.algorithms.locErrors.LocaliserError;
import PamDetection.AbstractLocalisation;
import PamDetection.LocContents;
import PamUtils.LatLong;
import PamguardMVC.PamDataUnit;
import PamguardMVC.superdet.SuperDetection;
import contactcollator.CollatorStreamProcess;
import pamMaths.PamVector;
import pamMaths.STD;

public abstract class BasicLocalisationSummariser implements LocalisationSummariser {

	protected CollatorStreamProcess collatorStreamProcess;
	
	private STD std = new STD();
	
	private Random random = new Random();

	/**
	 * @param collatorStreamProcess
	 */
	public BasicLocalisationSummariser(CollatorStreamProcess collatorStreamProcess) {
		super();
		this.collatorStreamProcess = collatorStreamProcess;
	}

	/**
	 * @return the collatorStreamProcess
	 */
	public CollatorStreamProcess getCollatorStreamProcess() {
		return collatorStreamProcess;
	}
	
	/**
	 * Get a list of localised units, note that these may 
	 * be super detections of the original list of units that went in. 
	 * @param dataList
	 * @return
	 */
	public ArrayList<PamDataUnit> getLocalisedUnits(List<PamDataUnit> dataList) {
		int nUnits = dataList.size();
		int nLocs = 0;
		ArrayList<PamDataUnit> localisedUnits = new ArrayList();
		for (PamDataUnit dataUnit : dataList) {
			PamDataUnit loc = findLatLongLocalisation(dataUnit);
			if (loc != null) {
				nLocs++;
				localisedUnits.add(loc);
			}
		}
//		localisedUnits.so
		return localisedUnits;
	}
	/**
	 * Perform an iterative search through a detection and all it's super detections in hope of finding
	 * a latlong localisation. 
	 * <br> But return the data unit with the localisation so that we can know the time of 
	 * it too ! This may well end up being one of the superdetections, e.g. from crossed bearing localiser. 
	 * @param dataUnit
	 * @return
	 */
	public PamDataUnit findLatLongLocalisation(PamDataUnit dataUnit) {
		PamDataUnit du = dataUnit;
		AbstractLocalisation loc = du.getLocalisation();
		if (loc != null && loc.hasLocContent(LocContents.HAS_LATLONG)) {
//			System.out.printf("Found latlong %s in %s in in %s\n", loc.getLatLong(0), 
//					loc.getClass().getName(), du.getClass().getName());
			return du;
		}
		int nSuperDets = du.getSuperDetectionsCount();
		for (int i = 0; i < nSuperDets; i++) {
			SuperDetection superDet = du.getSuperDetection(i);
			PamDataUnit superloc = findLatLongLocalisation(superDet);
			if (superloc != null) {
				return superloc;
			}
		}
		return null;
	}
	/**
	 * Get lat longs for a cluster
	 * @param latLongs all lat longs
	 * @param clusterInds indices of the ones we want
	 * @return
	 */
	protected LatLong[] pickLatLongs(LatLong[] latLongs, int[] clusterInds) {
		LatLong[] lls = new LatLong[clusterInds.length];
		for (int i = 0; i < clusterInds.length; i++) {
			lls[i] = latLongs[clusterInds[i]];
		}
		return lls;
	}
	
	/**
	 * Get a subset of localiser errors from a list. 
	 * @param allErrors all errors
	 * @param clusterInds indices of the ones we want. 
	 * @return
	 */
	protected LocaliserError[] pickLocaliserErrors(LocaliserError[] allErrors, int[] clusterInds) {
		LocaliserError[] errs = new LocaliserError[clusterInds.length];
		for (int i = 0; i < clusterInds.length; i++) {
			errs[i] = allErrors[clusterInds[i]];
		}
		return errs;
	}
	
	/**
	 * Get an average location and it's uncertainty for a set of lat longs. 
	 * @param latLongs array of lat longs
	 * @param errors array of errors - must be null or same length as latLongs
	 * @param nRandom number of random points to add, based on error, for each latLong
	 * @return summary localisation and error. 
	 */
	protected LocalisationSummary getAverageLocalisation(long startMillis, long endMillis, LatLong[] latLongs, LocaliserError[] errors, int hydrophoneMap, int nRandom) {
		if (errors == null) {
			nRandom = 1;
		}
		if (errors == null || nRandom == 1) {
			return getAverageLocalisation(startMillis, endMillis, latLongs, hydrophoneMap);
		}
		if (latLongs.length == 1) {
			return new LocalisationSummary(startMillis, endMillis, latLongs[0], errors == null ? null : errors[0], hydrophoneMap, 1);
		}
		/*
		 *  otherwise we need to generate a load of points and average them to get a better overall distribution
		 *  of possibilities.  
		 */
		LatLong mean = LatLong.getMean(latLongs);
		int nTot = nRandom * latLongs.length;
		LatLong[] randoms = new LatLong[nTot];
		for (int i = 0, k = 0; i < latLongs.length; i++) {
			randoms[k++] = latLongs[i]; // always take the default central position.  
			for (int j = 1; j < nRandom; j++,k++) {
				randoms[k] = shiftRandom(latLongs[i], errors[i]);
			}
		}
		EllipticalError err = getErrEllipse(randoms, mean);
		
		return new LocalisationSummary(startMillis, endMillis, mean, err, hydrophoneMap, latLongs.length);
	}

	/**
	 * Shift a lat long by a random ammount according to the error ellipse. 
	 * @param latLong
	 * @param localiserError
	 * @return
	 */
	private LatLong shiftRandom(LatLong latLong, LocaliserError localiserError) {
		if (localiserError instanceof EllipticalError) {
			return shiftElliptical(latLong, (EllipticalError) localiserError);
		}
		// else use a generic shift along the main axis of the error. 
//		double a[] = localiserError.geta
		PamVector errorDir = localiserError.getErrorDirection();
		if (errorDir == null) {
			return latLong;
		}
		double errorMag = localiserError.getErrorMagnitude() * random.nextGaussian();
		PamVector shift = errorDir.times(errorMag);

		return latLong.addDistanceMeters(shift);
	}
	
	/**
	 * Shift lat long by a random ammount around elliptical error. 
	 * @param latLong
	 * @param ellipticalError
	 * @return
	 */
	public LatLong shiftElliptical(LatLong latLong, EllipticalError ellipticalError) {
		double[] angles = ellipticalError.getAngles();
		double[] mags = ellipticalError.getEllipseDim();
		double[] rMags = new double[mags.length];
		if (angles == null || mags == null) {
			return latLong;
		}
		for (int i = 0; i < mags.length; i++) {
			rMags[i] = mags[i] * random.nextGaussian();
		}
		double ang = Math.PI/2-angles[0];
		double x = Math.cos(ang)*rMags[0];
		double y = Math.sin(ang)*rMags[0];
		x += Math.sin(ang) * rMags[1];
		y += Math.cos(ang) * rMags[1];
		return latLong.addDistanceMeters(x, y);
	}

	/**
	 * Simple average location, and best estimate of error. 
	 * @param latLongs
	 * @param hydrophoneMap
	 * @return
	 */
	private LocalisationSummary getAverageLocalisation(long startMillis, long endMillis, LatLong[] latLongs, int hydrophoneMap) {
		LatLong ave = LatLong.getMean(latLongs);
		EllipticalError err = getErrEllipse(latLongs, ave);
		return new LocalisationSummary(startMillis, endMillis, ave, err, hydrophoneMap, latLongs.length);
	}
	
	/**
	 * Get a reasonable elliptical error around a set of lat longs. 
	 * @param latLongs all latlongs
	 * @param meanLatLong should be mean of above. Can be null, in which case it's recalculated. 
	 * @return
	 */
	protected EllipticalError getErrEllipse(LatLong[] latLongs, LatLong meanLatLong) {
		/**
		 * Could possibly iterate this function a bit to get finer resolution, e.g. start with 10 deg steps, then go down to 1.
		 */
		if (meanLatLong == null) {
			meanLatLong = LatLong.getMean(latLongs);
		}
		/*
		 * Rotate about the mean to maximise the 
		 * standard deviation of all points, which is then the major axis
		 * of an error ellipse, the minor axis will be the std of the perpendicular. 
		 */
		// translate the points so they are about the mean. 
		int nPoint = latLongs.length;
		double[] xPoints = new double[nPoint];
		double[] yPoints = new double[nPoint];
		for (int i = 0; i < nPoint; i++) {
			xPoints[i] = meanLatLong.distanceToMetresX(latLongs[i]);
			yPoints[i] = meanLatLong.distanceToMetresY(latLongs[i]);
		}
		double[][] pointMatrix = {xPoints, yPoints};
		Matrix points = new Matrix(pointMatrix);
		// do final assessment in two stages to get down to one degree accuracy. 
		EllipticalError err1 = getErrEllipse(points, 0, 180, 10);
		double a = Math.toDegrees(err1.getAngles()[0]);
		EllipticalError err2 = getErrEllipse(points, a-5, a+5, 1);
		return err2;
	}
	
	/**
	 * Get the elliptical error from the matrix of points by rotating them across
	 * a range of angles with given step size. Can be called iteratively to get fine scale answer
	 * @param points Matrix of points, converted to xy frame
	 * @param startAngle start angle for search
	 * @param endAngle end angle for search
	 * @param aStep step size for search
	 * @return Largest possible elliptical error. 
	 */
	protected EllipticalError getErrEllipse(Matrix points, double startAngle, double endAngle, double aStep) {
//		new Matrix
		double bestAngle = 0;
		double bestError = Double.MAX_VALUE;
		double bestXSpan = 0, bestYSpan = 0;
		for (double a = startAngle; a <= endAngle-aStep; a+= aStep) {
			double ar = a * Math.PI / 180;
			double sa = Math.sin(ar);
			double ca = Math.cos(ar);
			double[][] m = {{ca, -sa}, {sa, ca}};
			Matrix rotMatrix = new Matrix(m);
			Matrix rotated = rotMatrix.times(points);
			double[] xr = rotated.getArray()[1];
			double[] yr = rotated.getArray()[0];
			double xSpan = std.getSTD(xr);
			double ySpan = std.getSTD(yr);
			double span = ySpan;
//			System.out.printf("Angle %3.0f xSpan %3.1fm ySpan %3.1fm, a=%3.1fm^2\n", a, xSpan, ySpan, xSpan*ySpan);
			if (span < bestError) {
				bestError = span;
				bestAngle = a;
				bestXSpan = xSpan;
				bestYSpan = ySpan;
			}
		}
		bestAngle *= Math.PI/180; // convert back to radians.
//		bestAngle = Math.PI-bestAngle;
		double[] errAngles = {bestAngle, 0, 0.};
		double[] bestErr = {bestXSpan, bestYSpan, 0.};
		EllipticalError err = new EllipticalError(errAngles, bestErr);
		return err;
	}
}
