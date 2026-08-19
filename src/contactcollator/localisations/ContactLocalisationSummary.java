package contactcollator.localisations;

import java.util.ArrayList;

/**
 * Returned object from a LocalisationSummariser. Can now contain multiple
 * localisations, if data were split into multiple clusters. 
 */
public class ContactLocalisationSummary {

	private ArrayList<LocalisationSummary> summaries;
	
	private String method;

	/**
	 * @param method
	 * @param summaries
	 */
	public ContactLocalisationSummary(String method, ArrayList<LocalisationSummary> summaries) {
		super();
		this.method = method;
		this.summaries = summaries;
	}
	/**
	 * @param method
	 * @param summaries
	 */
	public ContactLocalisationSummary(String method, LocalisationSummary summary) {
		super();
		this.method = method;
		this.summaries = new ArrayList<LocalisationSummary>();
		this.summaries.add(summary);
	}
	/**
	 * @return the summaries
	 */
	public ArrayList<LocalisationSummary> getSummaries() {
		return summaries;
	}
	/**
	 * @param summaries the summaries to set
	 */
	public void setSummaries(ArrayList<LocalisationSummary> summaries) {
		this.summaries = summaries;
	}
	/**
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}
	/**
	 * @param method the method to set
	 */
	public void setMethod(String method) {
		this.method = method;
	}
	
	@Override
	public String toString() {
		String str = String.format("Contact Loc Summary: %d sumaries", summaries.size());
		return str;
	}
	
}
