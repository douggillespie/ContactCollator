package contactcollator.localisations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import Localiser.algorithms.locErrors.BearingError;
import Localiser.algorithms.locErrors.LocaliserError;
import PamDetection.AbstractLocalisation;
import PamDetection.LocContents;
import PamUtils.LatLong;
import PamUtils.PamUtils;
import PamUtils.sort.IndexSort;
import PamguardMVC.PamDataUnit;
import PamguardMVC.superdet.SuperDetection;
import contactcollator.CollatorStreamProcess;
import contactcollator.trigger.CollatorTriggerData;
import pamMaths.PamVector;
import pamMaths.STD;
import weka.clusterers.EM;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Summarise the localisations using Gaussian Mixture models. 
 * Try basing around the Apache spark ML library. 
 */
public class GMMLocalisationSummariser extends BasicLocalisationSummariser {

	private static final int MAX_MIXTURES = 3;
	private ArrayList<Attribute> coordinateAttributes;
	private STD std = new STD();

	public GMMLocalisationSummariser(CollatorStreamProcess collatorStreamProcess) {
		super(collatorStreamProcess);

		coordinateAttributes = new ArrayList();
		coordinateAttributes.add(new Attribute("x"));
		coordinateAttributes.add(new Attribute("y"));
	}

	@Override
	public ContactLocalisationSummary summariseLocalisations(CollatorTriggerData trigger) {
		// get a set of units that have a latlong.
		// initial code here is similar to in Averager. 
		LocalisationSets localisationSet = getLocalisedUnits(trigger.getDataList());
		ContactLocalisationSummary fullSummary = getLocalisedSummary(trigger, localisationSet.getLocalised());

		ContactLocalisationSummary bearingSummary = getBearingSummary(trigger, localisationSet.getBearinged());
		
		return combineSummaries(fullSummary, bearingSummary, null);
	}
	
	/**
	 * Find an intelligent way of combining localisations that may contain lat longs, bearings only, or nothing. 
	 * For now, just pick what seems best. 
	 * @param latLongs summaries that have lat longs
	 * @param bearings summaries that have only a bearing
	 * @param nothings summaries that have nothing
	 * @return
	 */
	public ContactLocalisationSummary combineSummaries(ContactLocalisationSummary latLongs, ContactLocalisationSummary bearings, ContactLocalisationSummary nothings) {
		if (latLongs != null) {
			return latLongs;
		}
		if (bearings != null) {
			return bearings;
		}
		return nothings;
	}
	
	/**
	 * Get a summary of localisations that only have a bearing. Use a clustering algorithm to 
	 * generate multiple bearings if it seems sensible. 
	 * @param trigger
	 * @param bearinged list of units that have a localisation that can return a bearing vector. 
	 * @return
	 */
	private ContactLocalisationSummary getBearingSummary(CollatorTriggerData trigger,
			ArrayList<PamDataUnit> bearinged) {
		if (bearinged == null || bearinged.size() == 0) {
			return null;
		}
		/*
		 *  use Weka clustering algorithm. do x and y separately, also consider that the data
		 *  may come from multiple hydrophone clusters, so will also need to do each of those clusters 
		 *  separately.   
		 */
		ArrayList<SuperDetection> groups = groupByHydrophones(bearinged);
		if (groups == null) {
			return null;
		}
		
		ArrayList<LocalisationSummary> summaryList = new ArrayList<LocalisationSummary>();
		
		for (int iGroup = 0; iGroup < groups.size(); iGroup++) {
			SuperDetection hydGroup = groups.get(iGroup);
			ArrayList<PamDataUnit> units = hydGroup.getSubDetections();
			int n = units.size();
			// prepare clustering
			Instances	data = new Instances("Position data", coordinateAttributes, n);
			for (int i = 0; i < n; i++) {
				PamDataUnit aUnit = units.get(i);
				AbstractLocalisation loc = aUnit.getLocalisation();
				if (loc == null) {
					continue; // should never happen since we selected units with localisation
				}
				PamVector[] vec = loc.getRealWorldVectors();
				if (vec == null) {
					continue; // should never happen since we selected units with localisation
				}
				double[] xy = {vec[0].getElement(0), vec[0].getElement(1)};
				data.add(new DenseInstance(1, xy)); // weight is 1 on everything. Probably a unit vector, so generating a circle. 
			}

			EM gmmClusterer = new EM();
			int nClus = 0;
			int[] clusterCount = null;
			int[] clusterInds = null;
			try {
//				gmmClusterer.setNumClusters(3); 
				gmmClusterer.setMaxIterations(100); // Set maximum iterations for EM convergence
				// start with a 5 degree min standard deviation
				gmmClusterer.setMinStdDev(Math.toRadians(2.5));

				// Train the model
//				System.out.printf("Training Gaussian Mixture Model...");
				gmmClusterer.buildClusterer(data);
				nClus = gmmClusterer.numberOfClusters();
//				System.out.printf("Hydrophone group 0x%X has %d bearing clusters from %d units\n", hydGroup.getChannelBitmap(), nClus, n);
				clusterInds = new int[data.size()];
				clusterCount = new int[nClus];
				for (int i = 0; i < data.size(); i++) {
					clusterInds[i] = gmmClusterer.clusterInstance(data.get(i));
					clusterCount[clusterInds[i]] ++; // get a count of every cluster. 
				}
			}
			catch (Exception e) {
				System.out.println(e.toString());
				continue;
			}
			// sort the clusters by size
			int[] clusterOrder = IndexSort.sortArray(clusterCount);
			// extract the ind's and do similar rejection of outliers.
			int nUsed = 0;
			for (int c = nClus-1; c >= 0; c--) {
				int[] inds = getClusterInds(clusterInds, clusterOrder[c]);
				// get the mean bearing and the standard deviation around that. 
				// to get the mean, need to use xy, but for SD, can use bearings about the mean
				double x = 0, y = 0;
				for (int i = 0; i < inds.length; i++) {
					double[] xy = data.get(inds[i]).toDoubleArray();
					x += xy[0];
					y += xy[1];
				}
				double meanAngle = Math.atan2(y, x);
				double[] dA = new double[inds.length];
				for (int i = 0; i < inds.length; i++) {
					double[] xy = data.get(inds[i]).toDoubleArray();
					double a = Math.atan2(xy[1], xy[0]);
					dA[0] = PamUtils.constrainedAngleR(a - meanAngle, Math.PI); // keep entries between -pi and pi
				}
				double angleSTD = std.getSTD(dA);
				// I believe we now have an angle and a standard deviation. 
				// change to clockwise angle from north
				meanAngle = Math.PI / 2 - meanAngle;
				long startMillis = units.get(inds[0]).getTimeMilliseconds();
				long endMillis = units.get(inds[inds.length-1]).getEndTimeInMilliseconds();
				LocalisationSummary locSummary = new LocalisationSummary(startMillis, endMillis, meanAngle, new BearingError(meanAngle, angleSTD), hydGroup.getChannelBitmap(), inds.length);
				summaryList.add(locSummary);
//				System.out.printf("Add bearing summary from cluster 0x%x at %3.1f degres\n", hydGroup.getChannelBitmap(), Math.toDegrees(meanAngle));
				
				nUsed += inds.length;
				if ((double) nUsed / (double) data.numInstances() > 0.8) {
					break; // ignore 20% outliers. 
				}
			}
			
		}
				
		return new ContactLocalisationSummary("Bearing Clusters", summaryList);
	}


	/**
	 * Get a summary of localisations that have a LatLong using a 2D clustering algorithm. 
	 * @param trigger
	 * @param locUnits all units should have a localisation with a latlong
	 * @return cluster summary
	 */
	private ContactLocalisationSummary getLocalisedSummary(CollatorTriggerData trigger, ArrayList<PamDataUnit> locUnits) {
		if (locUnits == null || locUnits.size() == 0) {
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
			// single unit, so just return its localisation data. 
			AbstractLocalisation loc = locUnits.iterator().next().getLocalisation();
			return new ContactLocalisationSummary("GMM Single Cluster",  
					new LocalisationSummary(trigger.getStartTime(), trigger.getEndTime(), loc.getLatLong(0), loc.getLocError(0), hydrophones, 1));
		}

		// can put everything straight into Weka data formats. 
		// first need to get the mean lat long so that we can work in metres. 
		LatLong[] lls = new LatLong[locUnits.size()];
		LocaliserError[] localiserErrors = new LocaliserError[locUnits.size()];
		int iP = 0;
		it = locUnits.iterator();
		while (it.hasNext()) {
			PamDataUnit unit = it.next();
			AbstractLocalisation loc = unit.getLocalisation();
			LatLong latLong = loc.getLatLong(0);
			lls[iP] = latLong;
			localiserErrors[iP] = loc.getLocError(0);
			iP++;
		}
		LatLong meanLatLong = LatLong.getMean(lls);
//		meanLatLong = new LatLong(0,0);
		
		Instances	data = new Instances("Position data", coordinateAttributes, locUnits.size());
		it = locUnits.iterator();
		while (it.hasNext()) {
			PamDataUnit unit = it.next();
			AbstractLocalisation loc = unit.getLocalisation();
			LatLong latLong = loc.getLatLong(0);
			double[] xy = meanLatLong.distanceTo(latLong);
			if (xy.length > 2) {
				xy = Arrays.copyOf(xy, 2); // get rid of height
			}
			//			// weight according to the error. 
			LocaliserError locErr = loc.getLocError(0);
			double weight = 1;
//			if (locErr != null) {
//				double errMag = locErr.getErrorMagnitude();
//				if (errMag > 0) {
//					weight = 1/errMag;
//				}
//			}
			data.add(new DenseInstance(weight, xy));
		}

		// 2. Initialise the EM (Gaussian Mixture Model) clusterer
		EM gmmClusterer = new EM();
		try {
//			gmmClusterer.setNumClusters(3); 
			gmmClusterer.setMaxIterations(100); // Set maximum iterations for EM convergence
			gmmClusterer.setMinStdDev(50);

			// 4. Train the model
//			System.out.printf("Training Gaussian Mixture Model...");
			gmmClusterer.buildClusterer(data);
//			System.out.printf("  returned %d clusters from %d entries\n", gmmClusterer.numberOfClusters(), data.size());
		}
		catch (Exception e) {
			System.out.println(e.toString());
		}
		int nClus = 0;
		try {
			nClus = gmmClusterer.numberOfClusters();
		} catch (Exception e) {
			e.printStackTrace();
		}
		int[] clusterCount = new int[nClus]; // count of data in each cluster. 
		// and get the cluster id for each point:
		int[] clusterID = new int[data.numInstances()];
		double[] clusterProb = new double[data.numInstances()];
		for (int i = 0; i < data.numInstances(); i++) {
			Instance instance = data.instance(i);
//			instance.
			double[] xy = instance.toDoubleArray();
			// Get the most likely cluster ID (0-indexed)
			try {
				clusterID[i] = gmmClusterer.clusterInstance(instance);
				clusterCount[clusterID[i]]++;
//				System.out.printf("GMM Entry %2d assigned %d is at %4.0f,%4.0f from origin\n", i ,clusterID[i], xy[0], xy[1]);
//				System.out.printf("%d, ", clusterID[i]);
				double[] softAssignments = gmmClusterer.distributionForInstance(instance);
				clusterProb[i] = softAssignments[clusterID[i]];
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		int[] clusterOrder = IndexSort.sortArray(clusterCount);
//		System.out.printf("\n");
		/*
		 *  now go through clusters in reverse order, so we output the one with most entries first, and 
		 *  don't do any that have < 10% of total number.
		 */
		ArrayList<LocalisationSummary> summaryList = new ArrayList<LocalisationSummary>();
		int nUsed = 0; 
		for (int i = nClus-1; i>= 0; i--) {
			// pick out the lat longs that have that cluster id
//			LatLong[] latLongs = pickLatLongs(lls, clusterID, i);
			int[] inds = getClusterInds(clusterID, clusterOrder[i]);
			if (inds.length < 2) {
//				System.out.println("Why is there a zero length group?");
				continue;
			}
			// get the start and end times for those units. 
			PamDataUnit locUnit = locUnits.get(inds[0]);
			long startMillis = locUnit.getTimeMilliseconds();
			long endMillis = locUnit.getEndTimeInMilliseconds();
			for (int u = 1; u < inds.length; u++) {
				locUnit = locUnits.get(u);
				startMillis = Math.min(startMillis, locUnit.getTimeMilliseconds());
				endMillis = Math.max(endMillis, locUnit.getEndTimeInMilliseconds());
			}
			
			LatLong[] groupLL = pickLatLongs(lls, inds);
			LocaliserError[] groupErrors = pickLocaliserErrors(localiserErrors, inds);
			int nRandom = Math.max(30/groupLL.length, 5);
//			System.out.printf("Cluster %d %d/%d has %d localisations, gen %d random points for each\n", i, clusterOrder[i], nClus, inds.length, nRandom);
			LocalisationSummary summary = getAverageLocalisation( startMillis, endMillis, groupLL, groupErrors, hydrophones, nRandom);
			summaryList.add(summary);
			nUsed += inds.length;
			if ((double) nUsed / (double) data.numInstances() > 0.8) {
				break;
			}
		}
		
		return new ContactLocalisationSummary("GMM Localiser", summaryList);
	}

	/**
	 * Get a list of cluster indexes for a given cluster. 
	 * @param clusterIDs cluster id's from GMM
	 * @param id index to search for
	 * @return list of indices corresponding to id.
	 */
	private int[] getClusterInds(int[] clusterIDs, int id) {
		int[] inds = new int[clusterIDs.length];
		int n = 0;
		for (int i = 0; i < clusterIDs.length; i++) {
			if (clusterIDs[i] == id) {
				inds[n++] = i;
			}
		}
		if (n < clusterIDs.length) {
			inds = Arrays.copyOf(inds, n);
		}
		return inds;
	}
	



	
}
