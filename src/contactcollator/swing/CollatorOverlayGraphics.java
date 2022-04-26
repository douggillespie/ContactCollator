package contactcollator.swing;

import java.awt.Color;

import PamView.PamDetectionOverlayGraphics;
import PamView.PamSymbol;
import PamView.PamSymbolType;
import PamguardMVC.PamDataBlock;

public class CollatorOverlayGraphics extends PamDetectionOverlayGraphics {

	private static PamSymbol defaultSymbol = new PamSymbol(PamSymbolType.SYMBOL_DIAMOND, 10, 10, true, Color.RED, Color.BLUE);
	
	public CollatorOverlayGraphics(PamDataBlock parentDataBlock) {
		super(parentDataBlock, defaultSymbol);
	}

}
