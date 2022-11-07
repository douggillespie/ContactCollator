package contactcollator;

import PamModel.PamDependency;
import PamModel.PamPluginInterface;

public class CollatorPlugin implements PamPluginInterface {

	private String jarFile;

	@Override
	public String getDefaultName() {
		return CollatorControl.unitType;
	}

	@Override
	public String getHelpSetName() {
		return "contactcollator/help/ContactCollatorHelpProj.hs";
	}

	@Override
	public void setJarFile(String jarFile) {
		this.jarFile = jarFile;
	}

	@Override
	public String getJarFile() {
		return jarFile;
	}

	@Override
	public String getDeveloperName() {
		return "Gillespie";
	}

	@Override
	public String getContactEmail() {
		return "info@pamguard.org";
	}

	@Override
	public String getVersion() {
		return "1.3";
	}

	@Override
	public String getPamVerDevelopedOn() {
		return "2.02.04";
	}

	@Override
	public String getPamVerTestedOn() {
		return "2.02.04";
	}

	@Override
	public String getAboutText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getClassName() {
		return CollatorControl.class.getName();
	}

	@Override
	public String getDescription() {
		return CollatorControl.unitType;
	}

	@Override
	public String getMenuGroup() {
		// TODO Auto-generated method stub
		return "Utilities";
	}

	@Override
	public String getToolTip() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PamDependency getDependency() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getMinNumber() {
		return 0;
	}

	@Override
	public int getMaxNumber() {
		return 0;
	}

	@Override
	public int getNInstances() {
		return 0;
	}

	@Override
	public boolean isItHidden() {
		return false;
	}

	@Override
	public int allowedModes() {
		return ALLMODES;
	}

}
