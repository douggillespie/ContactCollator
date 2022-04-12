package clipcollator;

import java.io.Serializable;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamSettings;

public class CollatorControl extends PamControlledUnit implements PamSettings {
	
	public static final String unitType = "Clip Collator";

	public CollatorControl(String unitName) {
		super(unitType, unitName);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Serializable getSettingsReference() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getSettingsVersion() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		// TODO Auto-generated method stub
		return false;
	}

}
