package contactcollator.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;

import Localiser.algorithms.locErrors.LocErrorGraphics;
import Localiser.algorithms.locErrors.LocaliserError;
import PamUtils.Coordinate3d;
import PamUtils.LatLong;
import PamView.GeneralProjector;
import PamView.PamDetectionOverlayGraphics;
import PamView.PamSymbol;
import PamView.PamSymbolType;
import PamView.TransformShape;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import contactcollator.CollatorDataUnit;
import contactcollator.localisations.ContactLocalisationSummary;
import contactcollator.localisations.LocalisationSummary;

public class CollatorOverlayGraphics extends PamDetectionOverlayGraphics {

	private static PamSymbol defaultSymbol = new PamSymbol(PamSymbolType.SYMBOL_DIAMOND, 10, 10, true, Color.RED, Color.BLUE);
	
	public CollatorOverlayGraphics(PamDataBlock parentDataBlock) {
		super(parentDataBlock, defaultSymbol);
	}

	@Override
	protected Rectangle drawOnMap(Graphics g, PamDataUnit pamDetection, GeneralProjector generalProjector) {
		CollatorDataUnit collatorDataUnit = (CollatorDataUnit) pamDetection;
		ContactLocalisationSummary localisationSummary = collatorDataUnit.getLocalisationSummary();
		if (localisationSummary != null) {
			return drawLocalisationSummary(g, collatorDataUnit, generalProjector);
		}
		return super.drawOnMap(g, pamDetection, generalProjector);
	}

	// similar to other draw, but has multiple localisations. 
	private Rectangle drawLocalisationSummary(Graphics g, CollatorDataUnit collatorDataUnit,
			GeneralProjector generalProjector) {
		ArrayList<LocalisationSummary> locs = collatorDataUnit.getLocalisationSummary().getSummaries();
		for (int i = 0; i < locs.size(); i++) {
			LocalisationSummary ls = locs.get(i);
			LatLong latLong = ls.getLatLong();
			LocaliserError err = ls.getLocaliserError();
			if (latLong == null) {
				continue;
			}
			PamSymbol symbol = getPamSymbol(collatorDataUnit, generalProjector);
			Coordinate3d c3d = generalProjector.getCoord3d(latLong);
			symbol.draw(g, c3d.getXYPoint());
			if (err == null) {
				continue;
			}
			LocErrorGraphics errDraw = err.getErrorDraw();
			TransformShape shape = errDraw.drawOnMap(g, collatorDataUnit, latLong, generalProjector, symbol.getLineColor());
			generalProjector.addHoverData(c3d, collatorDataUnit);
		}
		return null;
	}

}
