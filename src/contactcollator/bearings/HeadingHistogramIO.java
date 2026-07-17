package contactcollator.bearings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Write  / read heading histograms from Data io streams. 
 * @author dg50
 *
 */
public class HeadingHistogramIO {
	
	private static final short CURRENT_VERSION = 3;

	/**
	 * Write the heading histogram to a binary output stream
	 * @param dos data output stream
	 * @param headingHist heading histogram
	 * @return number of bytes written
	 * @throws IOException
	 */
	public int writeHeadingHist(DataOutputStream dos, HeadingHistogram headingHist) throws IOException {
		int currentBytes = dos.size();
		if (CURRENT_VERSION >= 3) {
			dos.writeShort(CURRENT_VERSION);
			dos.writeInt(headingHist.getChannelMap());
		}
		int nBins = headingHist.getnBins();
		double[] data = headingHist.getData();
		boolean zeroCent = headingHist.isZeroCentre();
		
		dos.writeShort(CURRENT_VERSION);
		dos.writeShort(nBins);
		dos.writeByte(zeroCent ? 1 : 0);
		for (int i = 0; i < nBins; i++) {
			dos.writeFloat((float) data[i]);
		}
		
		return dos.size()-currentBytes;
	}

	/**
	 * Read a heading histogram from a binary input stream
	 * @param dis data input stream
	 * @return heading histogram
	 * @throws IOException
	 */
	public HeadingHistogram readHeadingHist(DataInputStream dis) throws IOException {
		short version = dis.readShort(); // only needed if the format is changed. Currently only one!
		int channelMap = 0;
		if (version >= 3) {
			channelMap = dis.readInt();
		}
		int nBins = dis.readShort();
		boolean zeroCent = dis.readByte() > 0;
		double[] data = new double[nBins];
		for (int i = 0; i < nBins; i++) {
			data[i] = dis.readFloat();
		}
		HeadingHistogram headingHist = new HeadingHistogram(channelMap, nBins, zeroCent, data);
		return headingHist;
	}
}
