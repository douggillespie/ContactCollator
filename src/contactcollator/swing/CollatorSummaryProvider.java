package contactcollator.swing;

import contactcollator.CollatorControl;
import userDisplay.UserDisplayComponent;
import userDisplay.UserDisplayControl;
import userDisplay.UserDisplayProvider;

/**
 * display provider for collator data - bearing histograms and the last data spectrogram.
 * @author dg50
 *
 */
public class CollatorSummaryProvider implements UserDisplayProvider {
	
	private CollatorControl collatorControl;

	public CollatorSummaryProvider(CollatorControl collatorControl) {
		super();
		this.collatorControl = collatorControl;
	}

	@Override
	public String getName() {
		return collatorControl.getUnitName() + " summary";
	}

	@Override
	public UserDisplayComponent getComponent(UserDisplayControl userDisplayControl, String uniqueDisplayName) {
		return new CollatorSummaryDisplay(collatorControl, this);
	}

	@Override
	public Class getComponentClass() {
		return CollatorSummaryDisplay.class;
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
