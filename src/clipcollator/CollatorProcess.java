package clipcollator;

import PamController.PamControlledUnit;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class CollatorProcess extends PamProcess {

	private CollatorControl collatorControl;

	public CollatorProcess(CollatorControl collatorControl) {
		super(collatorControl, null);
		this.collatorControl = collatorControl;
	}

	@Override
	public void pamStart() {
		
	}

	@Override
	public void pamStop() {
		
	}

}
