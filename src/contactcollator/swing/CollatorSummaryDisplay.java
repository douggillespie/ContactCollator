package contactcollator.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import PamView.dialog.PamGridBagContraints;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserverAdapter;
import contactcollator.CollatorControl;
import contactcollator.CollatorDataUnit;
import contactcollator.CollatorParamSet;
import contactcollator.CollatorParams;
import contactcollator.CollatorProcess;
import contactcollator.ConfigObserver;
import userDisplay.UserDisplayComponent;

public class CollatorSummaryDisplay implements UserDisplayComponent, ConfigObserver {

	private CollatorControl collatorControl;
	
	private CollatorProcess collatorProcess;
	
	private CollatorSummaryProvider summaryProvider;
	
	private JPanel outerPanel;
	
	private JPanel scrolledPanel;

	private String uniqueName;
	
	private CollatorObserver collatorObserver;
	
	private HashMap<String, StreamSummaryPanel> summaryPanels = new HashMap<>();
	
	public CollatorSummaryDisplay(CollatorControl collatorControl, CollatorSummaryProvider summaryProvider) {
		super();
		this.collatorControl = collatorControl;
		this.summaryProvider = summaryProvider;
		this.collatorProcess = collatorControl.getCollatorProcess();
		outerPanel = new JPanel(new BorderLayout());
		scrolledPanel = new JPanel();
//		scrolledPanel.setLayout(new BoxLayout(scrolledPanel, BoxLayout.Y_AXIS));
		scrolledPanel.setLayout(new GridBagLayout());
		JScrollPane scrollPane = new JScrollPane(scrolledPanel);
		outerPanel.add(BorderLayout.CENTER, scrollPane);
		createStreamComponents();
		collatorObserver = new CollatorObserver();
		collatorProcess.getCollatorDataBlock().addObserver(collatorObserver);
		collatorControl.addConfigObserver(this);
	}

	@Override
	public Component getComponent() {
		return outerPanel;
	}
	
	/**
	 * Create the components for each stream inside the scrolled pane.
	 */
	private void createStreamComponents() {
		/*
		 * Will probably need to do quite a bit of cleaning up of old panes as well 
		 * as creation of new ones. 
		 */
		summaryPanels.clear();
		scrolledPanel.removeAll();
		CollatorParams params = collatorControl.getCollatorParams();
		GridBagConstraints c = new PamGridBagContraints();
		for (CollatorParamSet paramSet : params.parameterSets) {
			StreamSummaryPanel ssp = new StreamSummaryPanel(paramSet);
			scrolledPanel.add(ssp.getComponent(), c);
			summaryPanels.put(paramSet.setName, ssp);
			c.gridy++;
		}
	}

	@Override
	public void openComponent() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void closeComponent() {
		collatorProcess.getCollatorDataBlock().deleteObserver(collatorObserver);
	}

	@Override
	public void notifyModelChanged(int changeType) {
//		if (changeType == PamController.)
	}

	@Override
	public String getUniqueName() {
		return uniqueName;
	}
	
	private StreamSummaryPanel findStreamPanel(String streamName) {
		return summaryPanels.get(streamName);
	}

	@Override
	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;		
	}

	@Override
	public String getFrameTitle() {
		return collatorControl.getUnitName() + " summary data";
	}

	@Override
	public void configUpdate() {
		createStreamComponents();
	}
	
	private void newData(CollatorDataUnit collatorDataUnit) {
		StreamSummaryPanel summaryPanel = findStreamPanel(collatorDataUnit.getStreamName());
		if (summaryPanel == null) {
			System.out.println("Unable to find summary stream panel for Collator data from " + collatorDataUnit.getStreamName());
			return;
		}
		summaryPanel.update(collatorDataUnit);
	}

	private class CollatorObserver extends PamObserverAdapter {

		@Override
		public String getObserverName() {
			return uniqueName;
		}

		@Override
		public void addData(PamObservable observable, PamDataUnit pamDataUnit) {
			newData((CollatorDataUnit) pamDataUnit);
		}

		@Override
		public void updateData(PamObservable observable, PamDataUnit pamDataUnit) {
			newData((CollatorDataUnit) pamDataUnit);
		}
		
	}

}
