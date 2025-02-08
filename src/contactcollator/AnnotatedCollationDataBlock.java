package contactcollator;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class AnnotatedCollationDataBlock extends PamDataBlock<CollatorDataUnit>{

	public AnnotatedCollationDataBlock(String dataName, PamProcess parentProcess, int channelMap) {
		super(CollatorDataUnit.class, dataName, parentProcess, channelMap);
	}

}
