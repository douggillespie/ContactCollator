package contactcollator.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamDetection.PamDetection;
import PamDetection.RawDataUnit;
import PamView.component.PamSettingsIconButton;
import PamView.dialog.PamDialog;
import PamView.dialog.PamDialogPanel;
import PamView.dialog.PamGridBagContraints;
import PamView.dialog.SourcePanel;
import PamguardMVC.PamDataBlock;
import PamguardMVC.dataSelector.DataSelector;
import PamguardMVC.dataSelector.DataSelectorCreator;
import PamguardMVC.dataSelector.NullDataSelectorCreator;
import contactcollator.CollatorControl;
import contactcollator.CollatorParamSet;

public class CollatorSetPanel implements PamDialogPanel {

	private JPanel mainPanel;
	
	private CollatorParamSet collatorParamSet;
	
	private JTextField name, speciesCode;
	
	private PamSettingsIconButton filterButton;
	
	private JTextField triggerCount, triggerSeconds;
	
	private JTextField sourceSampleRate, outputSampleRate, outputSeconds;
	
	private JTextField minupdateInterval;
	
	private SourcePanel detectionSourcePanel, rawDataSourcePanel;

	private CollatorControl collatorControl;

	private CollatorDialog collatorDialog;
	
	public CollatorSetPanel(CollatorControl collatorControl, CollatorDialog collatorDialog, CollatorParamSet collatorParamSet) {
		super();
		this.collatorControl = collatorControl;
		this.collatorDialog = collatorDialog;
		this.collatorParamSet = collatorParamSet;
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(new TitledBorder(""));
		GridBagLayout gridBag = new GridBagLayout();
		JPanel namePanel = new JPanel(gridBag);
		JPanel sourcePanel = new JPanel(gridBag);
		JPanel outPanel = new JPanel(new GridBagLayout());
		mainPanel.add(namePanel);
		mainPanel.add(sourcePanel);
		mainPanel.add(outPanel);
		namePanel.setBorder(new TitledBorder("Name"));
		sourcePanel.setBorder(new TitledBorder("Detection Source"));
		outPanel.setBorder(new TitledBorder("Output Raw Data"));
		
	
		
		// stuff for the name panel
		GridBagConstraints c = new PamGridBagContraints();
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridy++;
		namePanel.add(new JLabel("Name: ", JLabel.RIGHT), c);
		c.gridx++;
		namePanel.add(name = new JTextField(15), c);
		c.gridx++;
		namePanel.add(new JLabel("  Code: ", JLabel.RIGHT), c);
		c.gridx++;
		namePanel.add(speciesCode = new JTextField(3), c);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		
		// stuff for the detection source panel
		c = new PamGridBagContraints();
		c.gridwidth = 4;
		detectionSourcePanel = new SourcePanel(collatorDialog, PamDetection.class, false, true);
		sourcePanel.add(detectionSourcePanel.getPanel(), c);
		detectionSourcePanel.addSelectionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				detectionSourceChange();
			}

		});		
		c.gridx+=c.gridwidth;
		c.gridwidth = 1;
		filterButton = new PamSettingsIconButton();
		sourcePanel.add(filterButton, c);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		sourcePanel.add(new JLabel(" Trigger Count ", JLabel.RIGHT), c);
		c.gridx++;
		sourcePanel.add(triggerCount = new JTextField(4), c);
		c.gridx++;
		sourcePanel.add(new JLabel(", Interval ", JLabel.RIGHT), c);
		c.gridx++;
		sourcePanel.add(triggerSeconds = new JTextField(4), c);
		c.gridx++;
		sourcePanel.add(new JLabel(" s ", JLabel.LEFT), c);
		
		
		filterButton.setToolTipText("Data selection");
		filterButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				filterButtonPressed();
			}
		});

		// stuff for the output panel
		c = new PamGridBagContraints();
		c.gridwidth = 5;
		rawDataSourcePanel = new SourcePanel(collatorDialog, RawDataUnit.class, false, true);
		outPanel.add(rawDataSourcePanel.getPanel(), c);
		JButton defaultButton = new JButton("Default");
		c.gridx += c.gridwidth;
		outPanel.add(defaultButton, c);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 1;
		outPanel.add(new JLabel(" FS = ", JLabel.RIGHT), c);
		c.gridx++;
		outPanel.add(sourceSampleRate = new JTextField(5), c);
		sourceSampleRate.setEditable(false);
		c.gridx++;
		outPanel.add(new JLabel(" Hz,", JLabel.LEFT), c);
		c.gridx++;
		outPanel.add(new JLabel(" Decimate to ", JLabel.RIGHT), c);
		c.gridx++;
		outPanel.add(outputSampleRate = new JTextField(5), c);
		c.gridx++;
		outPanel.add(new JLabel(" Hz ", JLabel.LEFT), c);
		c.gridx = 0;
		c.gridy++;
		outPanel.add(new JLabel(" Clip length ", JLabel.RIGHT), c);
		c.gridx++;
		outPanel.add(outputSeconds = new JTextField(5), c);
		c.gridx++;
		outPanel.add(new JLabel(" s,", JLabel.LEFT), c);
		c.gridx++;
		outPanel.add(new JLabel(" Min send interval ", JLabel.RIGHT), c);
		c.gridx++;
		outPanel.add(minupdateInterval = new JTextField(5), c);
		c.gridx++;
		outPanel.add(new JLabel(" s", JLabel.LEFT), c);
		
		
		
		rawDataSourcePanel.addSelectionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				outputSourceChanged();
			}
		});
		defaultButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				defaultOutputButton();
			}
		});
		
		name.setToolTipText("Enter a user friendly name for this output data set");
		speciesCode.setToolTipText("Enter a shorter code which will be saved with data");
		detectionSourcePanel.getPanel().setToolTipText("Trigger data source");
		triggerCount.setToolTipText("Number of detections required to trigger output");
		triggerSeconds.setToolTipText("Trigger count period in seconds");
		defaultButton.setToolTipText("Use the raw data source feeding the trigger data detector");
		sourceSampleRate.setToolTipText("Sample rate of source data");
		outputSampleRate.setToolTipText("Enter a lower sample rate if you want data decimated prior to output");
		outputSeconds.setToolTipText("Clip length in seconds (enter 0 for no output clip data)");
		minupdateInterval.setToolTipText("Minimum updated interval for data output");
	}


	@Override
	public JComponent getDialogComponent() {
		return mainPanel;
	}

	protected void filterButtonPressed() {
		if (! haveDataSelector()) {
			return;
		}
		PamDataBlock detectionSource = detectionSourcePanel.getSource();
		if (detectionSource == null) {
			return;
		}
		String selName = name.getText();
		if (selName == null || selName.length() == 0) {
			PamDialog.showWarning(collatorDialog, "You must set a name for this stream before using the data selector",
					"Warning");
			return;
		}
		selName = collatorControl.getDataSelectorName(selName);
		DataSelector dataSelector = detectionSource.getDataSelector(selName, false);
		if (dataSelector == null) {
			return;
		}
		dataSelector.showSelectDialog(collatorControl.getGuiFrame());
	}
	
	private void detectionSourceChange() {
//		PamDataBlock detectionSource = detectionSourcePanel.getSource();
//		if (detectionSource == null) {
//			return;
//		}
//		DataSelector dataSel = detectionSource.getDataSelector(null, getParams());
//		dataSel.getDialogButton(null)
		filterButton.setEnabled(haveDataSelector());
	}
	
	protected void outputSourceChanged() {
		PamDataBlock out = rawDataSourcePanel.getSource();
		if (out == null) {
			sourceSampleRate.setText(null);
		}
		else {
			sourceSampleRate.setText(String.format("%5.0f", out.getSampleRate()));
			String txt = outputSampleRate.getText();
			try {
				float val = Float.valueOf(txt);
			}
			catch (Exception e) {
				outputSampleRate.setText(sourceSampleRate.getText()); // set the same if nothing is there. 
			}
		}
	}


	protected void defaultOutputButton() {
		PamDataBlock dataBlock = detectionSourcePanel.getSource();
		if (dataBlock == null) {
			return;
		}
		dataBlock = dataBlock.getFirstRawSourceDataBlock();
		if (dataBlock == null) {
			return;
		}
//		while (dataBlock != null) {
//			if (RawDataUnit.class.isAssignableFrom(dataBlock.getUnitClass())) {
				rawDataSourcePanel.setSource(dataBlock);
				outputSampleRate.setText(String.format("%5.0f", dataBlock.getSampleRate()));
//				break;
//			}
//		}
	}


	private boolean haveDataSelector() {
		PamDataBlock detectionSource = detectionSourcePanel.getSource();
		if (detectionSource == null) {
			return false;
		}
		DataSelectorCreator selCreator = detectionSource.getDataSelectCreator();
		if (selCreator == null) {
			return false;
		}
		else {
			return selCreator.getClass() != NullDataSelectorCreator.class;
		}
	}

	@Override
	public void setParams() {
		if (collatorParamSet == null) {
			collatorParamSet = new CollatorParamSet();
		}
		name.setText(collatorParamSet.setName);
		speciesCode.setText(collatorParamSet.speciesCode);
		detectionSourcePanel.setSource(collatorParamSet.detectionSource);
		rawDataSourcePanel.setSource(collatorParamSet.rawDataSource);
		triggerCount.setText(String.format("%d", collatorParamSet.triggerCount));
		triggerSeconds.setText(String.format("%3.1f", collatorParamSet.triggerIntervalS));
		if (collatorParamSet.outputSampleRate > 0) {
			outputSampleRate.setText(String.format("%d", collatorParamSet.outputSampleRate));
		}
		outputSeconds.setText(String.format("%3.1f", collatorParamSet.outputClipLengthS));
		minupdateInterval.setText(String.format("%3.1f", collatorParamSet.minimumUpdateIntervalS));
		outputSourceChanged();
	}

	@Override
	public boolean getParams() {
		if (collatorParamSet == null) {
			collatorParamSet = new CollatorParamSet();
		}
		collatorParamSet.setName = name.getText();
		if (collatorParamSet.setName == null || collatorParamSet.setName.length() == 0) {
			return collatorDialog.showWarning("You must enter a name for the set");
		}
		collatorParamSet.speciesCode = speciesCode.getText();
		if (collatorParamSet.speciesCode == null || collatorParamSet.speciesCode.length() == 0) {
			return collatorDialog.showWarning("You must enter a species code for the set");
		}
		collatorParamSet.detectionSource = detectionSourcePanel.getSourceName();
		if (collatorParamSet.detectionSource == null) {
			return collatorDialog.showWarning("You must enter a detection source");
		}
		try {
			collatorParamSet.triggerCount = Integer.valueOf(triggerCount.getText()); // don't care if this is zero.
			collatorParamSet.triggerIntervalS = Float.valueOf(triggerSeconds.getText());
			if (collatorParamSet.triggerCount > 1 && collatorParamSet.triggerIntervalS == 0) {
				return collatorDialog.showWarning("You must enter a trigger interval greater than zero for a count greater than 1");
			}
		}
		catch (NumberFormatException e) {
			return collatorDialog.showWarning("Invalid or empty number field for trigger data");
		}
		collatorParamSet.rawDataSource = rawDataSourcePanel.getSourceName();
		if (collatorParamSet.rawDataSource == null) {
			return collatorDialog.showWarning("You must enter a raw data source");
		}
		
		float rawFS = 0;
		try {
			collatorParamSet.outputSampleRate = Integer.valueOf(outputSampleRate.getText());
			rawFS = Integer.valueOf(sourceSampleRate.getText());
		}
		catch (NumberFormatException e) {
			return collatorDialog.showWarning("Invalid or empty number field for output sample rate");
		}
		if (collatorParamSet.outputSampleRate > rawFS) {
			return collatorDialog.showWarning("The final output sample rate should not be greater than the raw data sample rate");
		}
		try {
			collatorParamSet.outputClipLengthS = Float.valueOf(outputSeconds.getText());
		}
		catch (NumberFormatException e) {
			return collatorDialog.showWarning("Invalid or empty clip length");
		}
		try {
			collatorParamSet.minimumUpdateIntervalS = Float.valueOf(minupdateInterval.getText());
		}
		catch (NumberFormatException e) {
			return collatorDialog.showWarning("Invalid or empty minimum update interval");
		}
		
		return true;
	}

	/**
	 * @return the collatorParamSet
	 */
	public CollatorParamSet getCollatorParamSet() {
		return collatorParamSet;
	}

}
