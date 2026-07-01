package contactcollator.swing;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import PamController.PamController;
import PamUtils.FrequencyFormat;
import PamView.component.PamSettingsIconButton;
import PamView.dialog.PamDialogPanel;
import PamView.dialog.PamGridBagContraints;
import PamView.dialog.PamTextDisplay;
import PamView.dialog.ScrollingPamLabel;
import PamView.dialog.SourcePanel;
import PamguardMVC.PamDataBlock;
import contactcollator.CollatorParamSet;

/**
 *  simpler display panel for a collator set which can be used in the main dialog, alongside
 *  a button which will open a more interesting dialog to change the settings. 
 * @author dg50
 *
 */
public class CollatorSetDisplay implements PamDialogPanel {

	private JPanel mainPanel;
	
	private CollatorParamSet collatorParamSet;
	
	private DisplayField name;//, speciesCode;
	
	private TitledBorder titledBorder;
	
	private PamSettingsIconButton filterButton;
	
	private DisplayField triggerCount, triggerSeconds;
	
	private DisplayField sourceSampleRate, outputSampleRate, outputSeconds;
	
	private DisplayField detectionSource, rawDataSource, dataOutput;
	
//	private static String longEmptyString = "                              "; //30
//	private static String shortEmptyString = "      "; // 6

	public CollatorSetDisplay(CollatorParamSet collatorParamSet, boolean showBorder) {
		super();
		this.collatorParamSet = collatorParamSet;
		mainPanel = new JPanel(new GridBagLayout());
		titledBorder = new TitledBorder("");
		if (showBorder) {
			mainPanel.setBorder(titledBorder);
		}
		GridBagConstraints c = new PamGridBagContraints();
		

		mainPanel.add(new JLabel("Name: ", JLabel.RIGHT), c);
		c.gridx++;
		mainPanel.add(name = new DisplayField(30), c);
//		c.gridx++;
//		mainPanel.add(new JLabel("  Code: ", JLabel.RIGHT), c);
//		c.gridx++;
//		mainPanel.add(speciesCode = new DisplayField(3), c);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		mainPanel.add(new JLabel("  Trigger: ", JLabel.RIGHT), c);
		c.gridx++;
		c.gridwidth = 3;
		mainPanel.add(detectionSource = new DisplayField(30), c);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		mainPanel.add(new JLabel("  Raw Source: ", JLabel.RIGHT), c);
		c.gridx++;
		c.gridwidth = 3;
		mainPanel.add(rawDataSource = new DisplayField(30), c);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		mainPanel.add(new JLabel(" Data output: ", JLabel.RIGHT), c);
		c.gridx++;
		c.gridwidth = 3;
		mainPanel.add(dataOutput = new DisplayField(30), c);
		
//		SwingUtilities.invokeLater(new Runnable() {
//			
//			@Override
//			public void run() {
				setParams();
				
//			}
//		});
	}
	
	private class DisplayField extends JLabel {

		public DisplayField(int maxChars) {
			super();
		}
		
	}
	
	@Override
	public JComponent getDialogComponent() {
		return mainPanel;
	}

	public void setParams(CollatorParamSet paramSet) {
		this.collatorParamSet = paramSet;
		setParams();
	}
	@Override
	public void setParams() {
		if (collatorParamSet == null) {
			collatorParamSet = new CollatorParamSet();
			return;
		}
		// TODO Auto-generated method stub
//		titledBorder.setTitle(collatorParamSet.setName);
//		mainPanel.setBorder(new TitledBorder(collatorParamSet.setName));
		name.setText(collatorParamSet.setName);
//		speciesCode.setText(collatorParamSet.speciesCode);
		detectionSource.setText(collatorParamSet.detectionSource);
		PamDataBlock sourceData = PamController.getInstance().getDataBlockByLongName(collatorParamSet.rawDataSource);
		if (sourceData == null) {
			rawDataSource.setText("Unknown: " + collatorParamSet.rawDataSource);
		}
		else {
			String name = sourceData.getLongDataName();
			if (collatorParamSet.outputSampleRate != sourceData.getSampleRate()) {
				name += String.format(", resampled at %s", FrequencyFormat.formatFrequency(collatorParamSet.outputSampleRate, true));
			}
			rawDataSource.setText(name);
		}
//		data output info
		String str = String.format("%3.1fs clip, Minimum output interval %3.1fs", collatorParamSet.outputClipLengthS, collatorParamSet.minimumUpdateIntervalS);
		dataOutput.setText(str);
		
		Component rp = mainPanel.getTopLevelAncestor();
		if (rp instanceof JDialog) {
			((JDialog) rp).pack();
		}
	}

	@Override
	public boolean getParams() {
		// TODO Auto-generated method stub
		return false;
	}

}
