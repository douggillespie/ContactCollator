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
		else {
			return super.drawOnMap(g, pamDetection, generalProjector);
		}
	}

	// similar to other draw, but has multiple localisations. 
	private Rectangle drawLocalisationSummary(Graphics g, CollatorDataUnit collatorDataUnit,
			GeneralProjector generalProjector) {
		ArrayList<LocalisationSummary> locs = collatorDataUnit.getLocalisationSummary().getSummaries();
		for (int i = 0; i < locs.size(); i++) {
			LocalisationSummary ls = locs.get(i);
			LatLong latLong = ls.getLatLong();
			Double meanBering = ls.getMeanBearing();
			if (latLong != null) {
				drawLatLongSummary(g, collatorDataUnit, generalProjector, ls);
			}
			if (meanBering != null) {
				drawBearingSummary(g, collatorDataUnit, generalProjector, ls);
			}
			
		}
		return null;
	}
	
	private Rectangle drawLatLongSummary(Graphics g, CollatorDataUnit collatorDataUnit,
			GeneralProjector generalProjector, LocalisationSummary latLongSummary) {

		LatLong latLong = latLongSummary.getLatLong();
		PamSymbol symbol = getPamSymbol(collatorDataUnit, generalProjector);
		Coordinate3d c3d = generalProjector.getCoord3d(latLong);
		symbol.draw(g, c3d.getXYPoint());
		LocaliserError err = latLongSummary.getLocaliserError();
		if (err != null) {
			LocErrorGraphics errDraw = err.getErrorDraw();
			if (errDraw != null) {
				TransformShape shape = errDraw.drawOnMap(g, collatorDataUnit, latLong, generalProjector, symbol.getLineColor());
			}
		}
		generalProjector.addHoverData(c3d, collatorDataUnit);
		return null;
	}

	private Rectangle drawBearingSummary(Graphics g, CollatorDataUnit collatorDataUnit,
			GeneralProjector generalProjector, LocalisationSummary latLongSummary) {
		Rectangle md = super.drawOnMap(g, collatorDataUnit, generalProjector);
		return md;
	}

}
