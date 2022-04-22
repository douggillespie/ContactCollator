package contactcollator.swing;

import clipgenerator.clipDisplay.ClipDisplayPanel;
import contactcollator.CollatorControl;
import userDisplay.UserDisplayComponent;
import userDisplay.UserDisplayControl;
import userDisplay.UserDisplayProvider;

public class CollatorDisplayProvider  implements UserDisplayProvider {
	
	private CollatorControl collatorControl;

	public CollatorDisplayProvider(CollatorControl collatorControl) {
		super();
		this.collatorControl = collatorControl;
	}

	@Override
	public String getName() {
		return collatorControl.getUnitName() + " display";
	}

	@Override
	public UserDisplayComponent getComponent(UserDisplayControl userDisplayControl, String uniqueDisplayName) {
		return new ClipDisplayPanel(collatorControl);
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
