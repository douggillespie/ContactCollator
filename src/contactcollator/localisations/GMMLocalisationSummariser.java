package contactcollator.localisations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import Localiser.algorithms.locErrors.LocaliserError;
import PamDetection.AbstractLocalisation;
import PamDetection.LocContents;
import PamUtils.LatLong;
import PamUtils.sort.IndexSort;
import PamguardMVC.PamDataUnit;
import contactcollator.CollatorStreamProcess;
import contactcollator.trigger.CollatorTriggerData;
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

	public GMMLocalisationSummariser(CollatorStreamProcess collatorStreamProcess) {
		super(collatorStreamProcess);

		coordinateAttributes = new ArrayList();
		coordinateAttributes.add(new Attribute("Lon"));
		coordinateAttributes.add(new Attribute("Lat"));
	}

	@Override
	public ContactLocalisationSummary summariseLocalisations(CollatorTriggerData trigger) {
		// get a set of units that have a latlong.
		// initial code here is similar to in Averager. 
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
			// single unit, so just return its localisation data. 
			AbstractLocalisation loc = locUnits.iterator().next().getLocalisation();
			return new ContactLocalisationSummary("GMM Single Cluster",  new LocalisationSummary(loc.getLatLong(0), loc.getLocError(0), hydrophones, 1));
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
		meanLatLong = new LatLong(0,0);
		
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
			gmmClusterer.setNumClusters(3); 
			gmmClusterer.setMaxIterations(100); // Set maximum iterations for EM convergence
//			gmmClusterer.setMinStdDev(5);

			// 4. Train the model
			System.out.printf("Training Gaussian Mixture Model...");
			gmmClusterer.buildClusterer(data);
			System.out.printf("  returned %d clusters from %d entries\n", gmmClusterer.numberOfClusters(), data.size());
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
				System.out.printf("GMM Entry %2d assigned %d is at %4.0f,%4.0f from origin\n", i ,clusterID[i], xy[0], xy[1]);
//				System.out.printf("%d, ", clusterID[i]);
				double[] softAssignments = gmmClusterer.distributionForInstance(instance);
				clusterProb[i] = softAssignments[clusterID[i]];
			} catch (Exception e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
			}
		}
		int[] clusterOrder = IndexSort.sortArray(clusterCount);
		System.out.printf("\n");
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
			LatLong[] groupLL = pickLatLongs(lls, inds);
			LocaliserError[] groupErrors = pickLocaliserErrors(localiserErrors, inds);
			int nRandom = Math.max(30/groupLL.length, 5);
			System.out.printf("Cluster %d %d/%d has %d localisations, gen %d random points for each\n", i, clusterOrder[i], nClus, inds.length, nRandom);
			LocalisationSummary summary = getAverageLocalisation(groupLL, groupErrors, hydrophones, nRandom);
			summaryList.add(summary);
			nUsed += inds.length;
			if ((double) nUsed / (double) data.numInstances() > 0.8) {
				break;
			}
		}
		return new ContactLocalisationSummary("GMM Localiser", summaryList);
		

//		// 5. Output model capabilities and distribution details
//		System.out.println(gmmClusterer.toString());
//		try {
//			// 6. Predict cluster memberships / profiles for each data point
//			System.out.println("\n--- Instance cluster assignments ---");
//			for (int i = 0; i < data.numInstances(); i++) {
//				Instance instance = data.instance(i);
//
//				// Get the most likely cluster ID (0-indexed)
//				int clusterID = gmmClusterer.clusterInstance(instance);
//
//				// Get the soft probability distribution across all clusters
//				double[] softAssignments = gmmClusterer.distributionForInstance(instance);
//
//				System.out.printf("Instance %d -> Assigned Cluster: %d | Probability: %.2f%%%n", 
//						i, clusterID, (softAssignments[clusterID] * 100));
//			}
//		}
//		catch (Exception e) {
//			System.out.println(e.toString());
//		}
		/*
		 *  now see how many clusters there are, but ignore clusters that have very few entries (needs a
		 *  parameter) and make an output of the mean of all the points in each of the other clusters. 
		 */
		


		//		int iPoint = 0;
		//		double[] xPoints = new double[nPoint];
		//		double[] yPoints = new double[nPoint];
		//		double[] weight = new double[nPoint];
		//		LatLong[] latLongs = new LatLong[nPoint];
		//		 it = locUnits.iterator();
		//		while (it.hasNext()) {
		//			PamDataUnit unit = it.next();
		//			AbstractLocalisation loc = unit.getLocalisation();
		//			LatLong latLong = loc.getLatLong(0);
		//			latLongs[iPoint] = latLong; 
		//			xPoints[iPoint] = latLong.getLongitude();
		//			yPoints[iPoint] = latLong.getLatitude();
		//			// weight according to the error. 
		//			LocaliserError locErr = loc.getLocError(0);
		//			weight[iPoint] = 1;
		//			if (locErr != null) {
		//				double errMag = locErr.getErrorMagnitude();
		//				if (errMag > 0) {
		//					weight[iPoint] = 1/errMag;
		//				}
		//			}
		//			iPoint++;
		//		}
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
