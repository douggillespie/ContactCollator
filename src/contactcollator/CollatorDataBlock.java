package contactcollator;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class CollatorDataBlock extends PamDataBlock<CollatorDataUnit> {

	public CollatorDataBlock(String dataName, PamProcess parentProcess, int channelMap) {
		super(CollatorDataUnit.class, dataName, parentProcess, channelMap);
	}

}
