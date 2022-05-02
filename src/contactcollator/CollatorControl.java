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
import clipgenerator.ClipDataUnit;
import clipgenerator.ClipDisplayDataBlock;
import clipgenerator.clipDisplay.ClipDisplayDecorations;
import clipgenerator.clipDisplay.ClipDisplayParent;
import clipgenerator.clipDisplay.ClipDisplayUnit;
import contactcollator.swing.CollatorDialog;
import contactcollator.swing.CollatorDialogPanel;
import contactcollator.swing.CollatorDisplayProvider;
import contactcollator.swing.CollatorSummaryProvider;
import userDisplay.UserDisplayControl;

public class CollatorControl extends PamControlledUnit implements PamSettings, ClipDisplayParent {
	
	public static final String unitType = "Contact Collator";
	
	private CollatorParams collatorParams = new CollatorParams();
	
	private CollatorProcess collatorProcess;

	private CollatorDisplayProvider displayProvider;
	
	private ArrayList<ConfigObserver> configObservers = new ArrayList<>();
	
	public CollatorControl(String unitName) {
		super(unitType, unitName);
		collatorProcess = new CollatorProcess(this);
		addPamProcess(collatorProcess);
		
		// clip like display
		displayProvider = new CollatorDisplayProvider(this);
		UserDisplayControl.addUserDisplayProvider(displayProvider);
		// summary (bearing histogram and last clip for each stream)
		UserDisplayControl.addUserDisplayProvider(new CollatorSummaryProvider(this));
		
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
		CollatorParams newParams = CollatorDialog.showDialog(this, parentFrame);
		if (newParams != null) {
			collatorProcess.setupProcess();
			notifyConfigObservers();
		}
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

	@Override
	public ClipDisplayDataBlock getClipDataBlock() {
		return collatorProcess.getCollatorDataBlock();
	}

	@Override
	public String getDisplayName() {
		return getUnitName();
	}

	@Override
	public ClipDisplayDecorations getClipDecorations(ClipDisplayUnit clipDisplayUnit) {
		return new ClipDisplayDecorations(clipDisplayUnit);
	}

	@Override
	public void displaySettingChange() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Add a configuration observer to get updates whenever the configuration is changed. 
	 * @param configObserver
	 */
	public void addConfigObserver(ConfigObserver configObserver) {
		configObservers.add(configObserver);
	}
	
	/**
	 * Remove a configuration observer
	 * @param configObserver
	 */
	public void removeConfigObserver(ConfigObserver configObserver) {
		configObservers.remove(configObserver);
	}
	
	/**
	 * Notify configuration observers
	 */
	public void notifyConfigObservers() {
		for (ConfigObserver co : configObservers) {
			co.configUpdate();
		}
	}

	/**
	 * @return the collatorProcess
	 */
	public CollatorProcess getCollatorProcess() {
		return collatorProcess;
	}
	
	

}
