package contactcollator.localisations;

//import org.apache.spark.ml.clustering.GaussianMixture;
//import org.apache.spark.ml.clustering.GaussianMixtureModel;
//import org.apache.spark.ml.linalg.Vectors;
//import org.apache.spark.sql.Dataset;
//import org.apache.spark.sql.Row;
//import org.apache.spark.sql.RowFactory;
//import org.apache.spark.sql.SparkSession;
//import org.apache.spark.sql.types.*;

import java.util.Arrays;
import java.util.List;

public class GMMTest {
//	public static void main(String[] args) {
//        // 1. Initialize SparkSession
//        SparkSession spark = SparkSession.builder()
//                .appName("JavaGMM2DExample").master("local[*]").getOrCreate();
//
//        // 2. Prepare 2D data: Groups around (1,1) and (5,5)
//        List<Row> data = Arrays.asList(
//                RowFactory.create(Vectors.dense(1.0, 1.1)),
//                RowFactory.create(Vectors.dense(5.0, 5.1))
//        );
//
//        // Define schema
//        StructType schema = new StructType(new StructField[]{
//                new StructField("features", new org.apache.spark.ml.linalg.VectorUDT(), false, Metadata.empty())
//        });
//
//        Dataset<Row> dataset = spark.createDataFrame(data, schema);
//
//        // 3. Train GMM
//        GaussianMixtureModel model = new GaussianMixture()
//                .setK(2).setFeaturesCol("features").fit(dataset);
//
//        // 4. Output parameters
//        for (int i = 0; i < model.getK(); i++) {
//        	String  mwc = model.getWeightCol();
////        	model.get
////            System.out.printf("Cluster %d: Weight=%.2f, Mean=%s\n", 
////                i, model.getWeightCol()[i], model.gaussians()[i].mean());
//        }
//
//        spark.stop();
//    }
}
