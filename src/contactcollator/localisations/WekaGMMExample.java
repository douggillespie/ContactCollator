package contactcollator.localisations;

import java.util.Random;

import weka.clusterers.EM;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.datagenerators.DataGenerator;

public class WekaGMMExample {

	public static void main(String[] args) {
		try {
			// 1. Load the dataset (using Weka's built-in iris dataset as an example)
			// Replace with your own path to a .arff or .csv file
//			String dataPath = "C:\\PAMGuardTest\\GMM\\iris.arff"; 
//			DataSource source = new DataSource(dataPath);
//			Instances data = source.getDataSet();
			
			int nCol = 2;
			FastVector<Attribute> wAttrs = new FastVector(nCol+1);
			for (int i = 0; i < nCol; i++) {
				wAttrs.addElement(new Attribute(String.format("Dim%d", i)));
			}  
			int nCluster = 3;
			int nPerCluster = 10;
			int totEntries = nCluster*nPerCluster;
			double[] xM = {0, 5, 8};
			double[] yM = {1, -3, 6};
			Random r = new Random();

			Instances	data = new Instances("Training data", wAttrs, totEntries);
			
			for (int iC = 0; iC < xM.length; iC++) {
				for (int iE = 0; iE < nPerCluster; iE++) {
					double x = xM[iC] + r.nextGaussian()*2;
					double y = yM[iC] + r.nextGaussian()*2;
					double[] rowData = {x,y};
					Instance inst = new DenseInstance(1, rowData);
					data.add(inst);
				}
			}
			

			
			// create a set of 2D data for testing. 
//			DataGenerator dataGen = new Data

			// Optional: If your dataset has a class/label attribute, remove it for unsupervised clustering
			if (data.classIndex() == -1 && data.attribute("class") != null) {
				data.setClass(data.attribute("class"));
			}
			// If class is set, we can tell Weka to ignore it during clustering
			// or simply remove the attribute programmatically using a Weka filter.

			// 2. Initialise the EM (Gaussian Mixture Model) clusterer
			EM gmmClusterer = new EM();

			// 3. Set parameters for the GMM
			// -1 automatically determines the number of clusters using cross-validation
			// Or specify a fixed integer (e.g., gmmClusterer.setNumClusters(3);)
			gmmClusterer.setNumClusters(3); 
//			gmmClusterer.set
			gmmClusterer.setMaxIterations(100); // Set maximum iterations for EM convergence

			// 4. Train the model
			System.out.println("Training Gaussian Mixture Model...");
			gmmClusterer.buildClusterer(data);

			// 5. Output model capabilities and distribution details
			System.out.println(gmmClusterer.toString());

			// 6. Predict cluster memberships / profiles for each data point
			System.out.println("\n--- Instance cluster assignments ---");
			for (int i = 0; i < data.numInstances(); i++) {
				Instance instance = data.instance(i);

				// Get the most likely cluster ID (0-indexed)
				int clusterID = gmmClusterer.clusterInstance(instance);

				// Get the soft probability distribution across all clusters
				double[] softAssignments = gmmClusterer.distributionForInstance(instance);

				System.out.printf("Instance %d -> Assigned Cluster: %d | Probability: %.2f%%%n", 
						i, clusterID, (softAssignments[clusterID] * 100));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
