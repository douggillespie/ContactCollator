package contactcollator;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.JMenuItem;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamView.dialog.GenericSwingDialog;
import contactcollator.swing.CollatorDialog;
import contactcollator.swing.CollatorDialogPanel;

public class CollatorControl extends PamControlledUnit implements PamSettings {
	
	public static final String unitType = "Contact Collator";
	
	private CollatorParams collatorParams = new CollatorParams();
	
	private CollatorProcess collatorProcess;

	public CollatorControl(String unitName) {
		super(unitType, unitName);
		collatorProcess = new CollatorProcess(this);
		addPamProcess(collatorProcess);
		
		PamSettingManager.getInstance().registerSettings(this);
	}

	@Override
	public JMenuItem createDetectionMenu(Frame parentFrame) {
		JMenuItem menuItem = new JMenuItem(getUnitName() + " settings...");
		menuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showSettingsDialog(parentFrame);
			}
		});
		return menuItem;
	}

	protected void showSettingsDialog(Frame parentFrame) {
		CollatorDialog.showDialog(this, parentFrame);
	}

	@Override
	public Serializable getSettingsReference() {
		return collatorParams;
	}

	@Override
	public long getSettingsVersion() {
		return CollatorParams.serialVersionUID;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		this.collatorParams = ((CollatorParams) pamControlledUnitSettings.getSettings());
		return (this.collatorParams != null);
	}

	/**
	 * Check a parameter set ...
	 * @param paramSet parameter set to check. 
	 * @return null if OK, an error string if not OK. 
	 */
	public String checkParamSet(CollatorParamSet paramSet) {
		if (paramSet == null) {
			return "Null parameter set";
		}
		//TODO Needs to be written, check existence of source datablocks, sensibleness of parameters, etc. 
		
		return null;
	}

	/**
	 * Get a name for a data selector consisting of this module name and the stream name. 
	 * @param set name
	 * @return selector name
	 */
	public String getDataSelectorName(String setName) {
		return this.getUnitName() + ": " + setName;
	}

	/**
	 * @return the collatorParams
	 */
	public CollatorParams getCollatorParams() {
		return collatorParams;
	}

	/**
	 * @param collatorParams the collatorParams to set
	 */
	public void setCollatorParams(CollatorParams collatorParams) {
		this.collatorParams = collatorParams;
	}

	/**
	 * Find a parameter set by name. 
	 * @param name
	 * @return parameter set, or null
	 */
	public CollatorParamSet findParameterSet(String name) {
		try {
			ArrayList<CollatorParamSet> paramSets = collatorParams.parameterSets;
			for (CollatorParamSet ps : paramSets) {
				if (ps.setName.equals(name)) {
					return ps;
				}
			}
		}
		catch (NullPointerException e) {}
		return null;
	}

}
