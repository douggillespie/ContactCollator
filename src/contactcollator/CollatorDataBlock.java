package contactcollator;

import PamguardMVC.PamProcess;
import clipgenerator.ClipDisplayDataBlock;

public class CollatorDataBlock extends ClipDisplayDataBlock<CollatorDataUnit> {

	public CollatorDataBlock(String dataName, PamProcess parentProcess, int channelMap) {
		super(CollatorDataUnit.class, dataName, parentProcess, channelMap);
	}

}
