package contactcollator;

/**
 * Observer class for updates to configurations.
 * @author dg50
 *
 */
public interface ConfigObserver {

	/**
	 * Called when the configuration changes
	 */
	public void configUpdate();
	
}
