package contactcollator.localisations;

import Localiser.algorithms.locErrors.EllipticalError;
import PamUtils.LatLong;
import contactcollator.trigger.CollatorTriggerData;

// check Ellipse stats
public class EllipseTest {

	public static void main(String[] args) {
		
		LatLong lCent = new LatLong(20, 20);
		double[] angs = {Math.PI/180*5, 0, 0};
		double[] errMag = {30, 5, 0};
		EllipticalError eErr = new EllipticalError(angs, errMag);
		System.out.println(eErr.toString());
	
//		Now simulate a lot  of points around that, and see if we can reconstruct it. 
		GMMLocalisationSummariser gls = new GMMLocalisationSummariser(null);
		int n = 300;
		LatLong[] news = new LatLong[n];
		// get the x and y positions out so we can easily plot. 
		double[] x = new double[n];
		double[] y = new double[n];
		for (int i = 0; i < n; i++) {
			news[i] = gls.shiftElliptical(lCent, eErr);
			double[] xy = lCent.distanceTo(news[i]);
			x[i] = xy[0];
			y[i] = xy[1];
		}
		EllipticalError newEllipse = gls.getErrEllipse(news, lCent);
		System.out.println(newEllipse.toString());
	}

}
