package contactcollator.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import PamDetection.AbstractLocalisation;
import PamUtils.FrequencyFormat;
import PamUtils.PamCalendar;
import PamUtils.PamUtils;
import PamView.ColourArray;
import PamView.dialog.PamGridBagContraints;
import PamView.panel.PamPanel;
import PamguardMVC.PamDataUnit;
import PamguardMVC.superdet.SuperDetection;
import contactcollator.CollatorDataUnit;
import contactcollator.CollatorParamSet;
import contactcollator.CollatorStreamProcess;
import contactcollator.bearings.HeadingHistogram;
import contactcollator.bearings.HeadingHistogramPaint;
import contactcollator.bearings.HeadingHistogramPanel;
import contactcollator.trigger.CollatorTriggerData;
import pamMaths.HistogramDisplay;
import pamMaths.PamVector;

/**
 * Panel for a stream in a user display. contains the text summary, a bearing histogram and the latest clip spectrogram.
 * @author dg50
 *
 */
public class StreamSummaryPanel {

	private JPanel mainPanel;
	
	private CollatorSetDisplay setDisplay;
	
	private HistogramPanel headingPanel;
	
	private JPanel lastClipPanel;

	private CollatorParamSet collatorParamSet;

	private CollatorDataUnit lastDataUnit;
	
	private Object synchObject = new Object();
	
//	private HeadingHistogram headingHistogram;

	private ColourArray imageArray = ColourArray.createStandardColourArray(256, ColourArray.ColourArrayType.GREY);
	
	private HeadingHistogramPaint headingPainter = new HeadingHistogramPaint();

	private SpecAxisPanel axisPanel;

	private SpecPanel specPanel;

	public int borderSize;
	
	public StreamSummaryPanel(CollatorParamSet collatorParamSet) {
		this.collatorParamSet = collatorParamSet;
		mainPanel = new JPanel();
//		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		mainPanel.setLayout(new BorderLayout());
//		GridBagConstraints c = new PamGridBagContraints();
		mainPanel.setBorder(new TitledBorder(collatorParamSet.setName));
		setDisplay = new CollatorSetDisplay(collatorParamSet, false);
		mainPanel.add(setDisplay.getDialogComponent(),BorderLayout.WEST);
		
		JPanel rightPanel = new JPanel(new BorderLayout());
		mainPanel.add(rightPanel, BorderLayout.EAST);
//		headingHistogram = new HeadingHistogram(12, true);
		
		headingPanel = new HistogramPanel();
//		c.gridx++;
		rightPanel.add(headingPanel,BorderLayout.CENTER);
		
		lastClipPanel = new SpectrogramPanel();
		lastClipPanel.setLayout(new BorderLayout());
		axisPanel = new SpecAxisPanel();
		specPanel = new SpecPanel();
		
		lastClipPanel.add(axisPanel, BorderLayout.CENTER);
		axisPanel.add(specPanel, BorderLayout.CENTER);
//		c.gridx++;
		rightPanel.add(lastClipPanel,BorderLayout.EAST);
	}
	
	public JComponent getComponent() {
		return mainPanel;
	}
	
	public void update(CollatorDataUnit collatorDataUnit) {
		synchronized (synchObject) {
			this.lastDataUnit = collatorDataUnit;
			lastClipPanel.repaint();
//			headingHistogram.addData(collatorDataUnit);
			headingPanel.repaint();
		}
	}

//	private void fillHeadingHistogram(PamDataUnit dataUnit) {
//		if (dataUnit instanceof SuperDetection) {
//			SuperDetection superDet = (SuperDetection) dataUnit;
//			synchronized (superDet.getSubDetectionSyncronisation()) {
//				int n = superDet.getSubDetectionsCount();
//				for (int i = 0; i < n; i++) {
//					fillHeadingHistogram(superDet.getSubDetection(i));
//				}
//			}
//			return;
//		}
//		else if (dataUnit instanceof CollatorDataUnit) {
//			CollatorDataUnit collatorUnit = (CollatorDataUnit) dataUnit;
//			CollatorTriggerData trigData = collatorUnit.getTriggerData();
//			if (trigData != null) {
//				List<PamDataUnit> dataList = trigData.getDataList();
//				for (PamDataUnit aData : dataList) {
//					fillHeadingHistogram(aData);
//				}
//			}
//		}
//		else {
//			AbstractLocalisation loc = dataUnit.getLocalisation();
//			if (loc == null) {
//				return;
//			}
//			PamVector[] worldVecs = loc.getWorldVectors();
//			if (worldVecs == null) {
//				return;
//			}
//			for (int i = 0; i < worldVecs.length; i++) {
//				double head = worldVecs[i].getHeading();
//				headingHistogram.addData(head);
//			}
//		}
//	}

	private class HistogramPanel extends HeadingHistogramPanel {

		public HistogramPanel() {
			super(60, 60);
		}

		@Override
		public void paintHistogram(Graphics g) {
			
			headingPainter.paintHistrogram(g, null, getHeadingHistogram());
		}
		
	}
	
	HeadingHistogram getHeadingHistogram() {
		if (lastDataUnit == null) {
			return null;
		}
		else {
			return lastDataUnit.getHeadingHistogram();
		}
	}
	
	private class SpectrogramPanel extends HeadingHistogramPanel {

		public SpectrogramPanel() {
			super(120, 80);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void paintHistogram(Graphics g) {
			
		}
	}
	
	private class SpecAxisPanel extends PamPanel {
		
		private int fontHeight;
		private int fontAscent;
		private FontMetrics fm;
		public SpecAxisPanel() {
			super(new BorderLayout());
			Font f = getFont();
			f = new Font(f.getName(), f.getStyle(), f.getSize()-2);
			setFont(f);
			fm = getFontMetrics(getFont());
			fontHeight = fm.getAscent(); // the numbers used don't have any decent !
			borderSize = fontHeight + 1;
			fontAscent = fm.getAscent();
			setBorder(new EmptyBorder(borderSize, borderSize, borderSize, 2));
		}
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (lastDataUnit == null) {
				return;
			}
			String tStr = PamCalendar.formatTime(lastDataUnit.getTimeMilliseconds(), true);
			g.drawString(tStr, borderSize, fontAscent);
			float fs = lastDataUnit.getSourceSampleRate(); // was clipDisplayPanel.getSampleRate()
			String lenString = String.format("%3.2fs", (float)lastDataUnit.getSampleDuration() / fs);
			Rectangle2D strSize = fm.getStringBounds(lenString, g);
			g.drawString(lenString, (int) (getWidth()-strSize.getWidth()), getHeight()-fm.getDescent());

			int iChan = PamUtils.getLowestChannel(lastDataUnit.getChannelBitmap());
			String chString = String.format("Ch%d", iChan);
			g.drawString(chString, fm.charWidth('c')/2, getHeight()-fm.getDescent());

			double f = lastDataUnit.getDisplaySampleRate()/2.;
			String fStr = FrequencyFormat.formatFrequency(f, true);
			strSize = fm.getStringBounds(fStr, g);
			int x = fontAscent;
			int y = (int) (strSize.getWidth() + borderSize);
			Graphics2D g2d = (Graphics2D) g;
			g2d.translate(x, y);
			g2d.rotate(-Math.PI/2.);
			g2d.drawString(fStr, 0,0);
			g2d.rotate(+Math.PI/2.);
			g2d.translate(-x, -y);
		}
		
	}
	
	private class SpecPanel extends PamPanel {

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Rectangle bounds = this.getBounds();
//			g.drawLine(0, 0, bounds.width, bounds.height);
			CollatorDataUnit dataUnit = null;
			synchronized (synchObject) {
				dataUnit = lastDataUnit;
			}
			if (dataUnit == null) {
				String str = "no data";
				FontMetrics fm = g.getFontMetrics();
				g.drawString(str, (bounds.width-fm.stringWidth(str))/2, (bounds.height+fm.getHeight())/2);
			}
			else {
				BufferedImage image = dataUnit.getClipImage(0, 512, 256, 30, 80, imageArray.getColours());
				if (image != null) {
					g.drawImage(image, 0, 0, bounds.width, bounds.height, 0, 0, image.getWidth(), image.getHeight(), null);
				}
			}
		}
		
	}
}
