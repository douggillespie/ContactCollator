package contactcollator;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JMenuItem;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamProcess;
import PamguardMVC.datamenus.DataMenuParent;
import clipgenerator.ClipDataUnit;
import clipgenerator.ClipDisplayDataBlock;
import whistlesAndMoans.ConnectedRegionDataUnit;

public class CollatorDataBlock extends ClipDisplayDataBlock<CollatorDataUnit> {

	CollatorProcess collatorProcess;
	
	public CollatorDataBlock(String dataName, PamProcess parentProcess, int channelMap) {
		super(CollatorDataUnit.class, dataName, parentProcess, channelMap);
		collatorProcess = (CollatorProcess) parentProcess;
	}
	
	public CollatorDataUnit findClipFromCRDU(PamDataUnit du) {
		if(!(du instanceof ConnectedRegionDataUnit)) {
			return null;
		}
		ConnectedRegionDataUnit crdu = (ConnectedRegionDataUnit) du;
		
		CollatorDataUnit collatorDataUnit;
		
		synchronized (this.getSynchLock()) {
			
				ListIterator<CollatorDataUnit> iter = this.getListIterator(ITERATOR_END);
				while (iter.hasPrevious()) {
					collatorDataUnit = iter.previous();
					if(collatorDataUnit.triggerName.equals(crdu.getParentDataBlock().getLongDataName()) && collatorDataUnit.getTriggerUID()==crdu.getUID()) {
						return collatorDataUnit;
					}
					
				}
			}
		
		return null;
		
		
	}

	
	@Override
	public List<JMenuItem> getDataUnitMenuItems(DataMenuParent menuParent, Point mousePosition,
			PamDataUnit... dataUnits) {
		List<JMenuItem> standItems = super.getDataUnitMenuItems(menuParent, mousePosition, dataUnits);
		if (standItems == null) {
			standItems = new ArrayList<JMenuItem>();
		}
		
		for (int i = 0; i < Math.min(3,  dataUnits.length); i++) {
			if (dataUnits[i] instanceof CollatorDataUnit) {
				CollatorDataUnit collatorDataUnit = (CollatorDataUnit) dataUnits[i];
				JMenuItem menuItem = new JMenuItem("Export clip UID " + dataUnits[i].getUID()+" as wav");
				standItems.add(i, menuItem);
				menuItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						long fs = (long) collatorDataUnit.getTriggerData().getDataList().get(0).getParentDataBlock().getSampleRate();
						collatorProcess.saveWAV(fs, collatorDataUnit.getRawData(),collatorDataUnit.getUID());
					}
				});
			}
		}
		
		
		return standItems;
	}
}
