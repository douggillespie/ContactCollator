package contactcollator.localisations;

import java.util.Set;

import PamguardMVC.PamDataUnit;
import contactcollator.trigger.CollatorTriggerData;

public interface LocalisationSummariser {

	public LocalisationSummary summariseLocalisations(CollatorTriggerData trigger);
	
}
