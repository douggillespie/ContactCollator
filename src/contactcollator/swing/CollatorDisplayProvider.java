package contactcollator.swing;

import clipgenerator.clipDisplay.ClipDisplayPanel;
import contactcollator.CollatorControl;
import contactcollator.CollatorStreamProcess;
import userDisplay.UserDisplayComponent;
import userDisplay.UserDisplayControl;
import userDisplay.UserDisplayProvider;

public class CollatorDisplayProvider  implements UserDisplayProvider {
	
	private CollatorControl collatorControl;
	public CollatorStreamProcess collatorStreamProcess;
	public ClipDisplayPanel displayPanel;


	public CollatorDisplayProvider(CollatorControl collatorControl,	CollatorStreamProcess collatorStreamProcess) {
		super();
		this.collatorControl = collatorControl;
		this.collatorStreamProcess = collatorStreamProcess;
	}
	
	public CollatorDisplayProvider(CollatorControl collatorControl) {
		super();
		this.collatorControl = collatorControl;
	}

	@Override
	public String getName() {
		if(this.collatorStreamProcess!=null) {
			return collatorStreamProcess.getSetName() + " clips";
		}else {
			return this.collatorControl.getDisplayName()+" clips";
		}
	}

	@Override
	public UserDisplayComponent getComponent(UserDisplayControl userDisplayControl, String uniqueDisplayName) {
		if(this.collatorStreamProcess!=null) {
			displayPanel = new CollatorStreamDisplayPanel(collatorStreamProcess);
		}else {
			displayPanel = new CollatorCombinedDisplayPanel(this.collatorControl);
		}
		return displayPanel;
	}
	
	@Override
	public Class getComponentClass() {
		return ClipDisplayPanel.class;
	}

	@Override
	public int getMaxDisplays() {
		return 0;
	}

	@Override
	public boolean canCreate() {
		return true;
	}

	@Override
	public void removeDisplay(UserDisplayComponent component) {
		// TODO Auto-generated method stub
		
	}

}
