package contactcollator.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import PamView.dialog.GenericSwingDialog;
import PamView.dialog.PamDialogPanel;
import PamView.dialog.PamGridBagContraints;
import PamView.panel.PamPanel;
import contactcollator.CollatorControl;
import contactcollator.CollatorParamSet;
import contactcollator.CollatorParams;

public class CollatorDialogPanel implements PamDialogPanel {

	private JPanel outerPanel; // may later want a scroll pane between outer and main. Not yet though. 

	private JPanel mainPanel;

	private JPanel buttonPanel;

	private CollatorControl collatorControl;

	private CollatorDialog collatorDialog;

	public CollatorDialogPanel(CollatorControl collatorControl, CollatorDialog collatorDialog) {
		super();
		this.collatorControl = collatorControl;
		this.collatorDialog = collatorDialog;
		this.outerPanel = new JPanel(new BorderLayout());
		this.mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.setBorder(new TitledBorder("Detection streams"));
		outerPanel.add(BorderLayout.CENTER, mainPanel);
		buttonPanel = new JPanel(new FlowLayout());
		JButton addButton = new JButton("Add stream");
		buttonPanel.add(addButton);
		outerPanel.add(BorderLayout.SOUTH, buttonPanel);
		addButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addStreamSet(null, true);
			}
		});

	}

	@Override
	public JComponent getDialogComponent() {
		return outerPanel;
	}

	@Override
	public void setParams() {
		//		Component rp = mainPanel.getTopLevelAncestor();
		//		if (rp instanceof JDialog) {
		//			((JDialog) rp).pack();
		//		}
		CollatorParams collatorParams = collatorControl.getCollatorParams();
		ArrayList<CollatorParamSet> paramSets = collatorParams.parameterSets;
		if (paramSets == null) {
			return;
		}
		for (CollatorParamSet paramSet : paramSets) {
			addStreamSet(paramSet, false);
		}
		packLater();
	}
	
	private void packLater() {
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				if (collatorDialog != null) {
					collatorDialog.pack();
				}
			}
		});
	}

	@Override
	public boolean getParams() {
		Component[] sets = mainPanel.getComponents();
		CollatorParams newParams = new CollatorParams();
		for (int i = 0; i < sets.length; i++) {
			if (sets[i] instanceof StreamSet == false) {
				continue;
			}
			StreamSet aSet = (StreamSet) sets[i];
			String err = collatorControl.checkParamSet(aSet.paramSet);
			if (err != null) {
				return collatorDialog.showWarning(err);
			}
			newParams.parameterSets.add(aSet.paramSet);
		}
		collatorControl.setCollatorParams(newParams);
		
		return true;
	}
	
	protected void addStreamSet(CollatorParamSet paramSet,  boolean edit) {
		StreamSet streamSet = new StreamSet(paramSet);
		mainPanel.add(streamSet);
		if (edit) {
			editStreamSet(streamSet);
		}
		streamSet.setParams();
		packLater();
	}

	protected void editStreamSet(StreamSet streamSet) {
		CollatorParamSet paramSet = streamSet.paramSet;
		if (paramSet == null) {
			paramSet = new CollatorParamSet();
		}
		CollatorSetPanel setPanel = new CollatorSetPanel(collatorControl,  collatorDialog, paramSet);
		setPanel.setParams();
		boolean ok = GenericSwingDialog.showDialog(collatorDialog, paramSet.setName + " settings", setPanel);
		if (ok == false) {
			return;
		}
		
		if (setPanel.getParams()) {
			streamSet.paramSet = setPanel.getCollatorParamSet();
			streamSet.setParams();
		}
		collatorDialog.pack();
	}
	
	// panel with a border and a couple of extra butons around a SetDisplay
	private class StreamSet extends JPanel {

		private CollatorParamSet paramSet;

		private CollatorSetDisplay collatorSetDisplay;

		private TitledBorder border;

		private StreamSet(CollatorParamSet paramSet) {
			this.paramSet = paramSet;
			setBorder(border = new TitledBorder("Unknown"));
			this.collatorSetDisplay = new CollatorSetDisplay(paramSet, false);
			setLayout(new GridBagLayout());
			GridBagConstraints c = new PamGridBagContraints();
			c.gridheight = 2;
			add(collatorSetDisplay.getDialogComponent(), c);
			c.gridheight = 1;
			c.gridx++;
			JButton editButton;
			add(editButton = new JButton("Edit"), c);
			c.gridy++;
			JButton removeButton;
			add(removeButton = new JButton("Remove"), c);

			editButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					edit();
				}
			});
			
			removeButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					remove();
				}
			});

		}

		public void setParams() {
			if (paramSet == null) {
				return;
			}
//			border.setTitle(paramSet.setName);
			setBorder(new TitledBorder(paramSet.setName));
			collatorSetDisplay.setParams(paramSet);
			packLater();
		}

		protected void edit() {
			editStreamSet(this);
			collatorDialog.pack();
		}

		protected void remove() {
			mainPanel.remove(this);			
			collatorDialog.pack();
		}
	}


}
