package contactcollator.io;

import java.sql.Types;

import PamDetection.AcousticSQLLogging;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import contactcollator.CollatorControl;
import contactcollator.CollatorDataBlock;
import contactcollator.CollatorDataUnit;
import contactcollator.trigger.CollatorTriggerData;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;
import generalDatabase.SQLTypes;

public class CollatorLogging extends SQLLogging {
	
	private CollatorDataBlock collatorDataBlock;
	private CollatorControl collatorControl;
	
	private PamTableItem triggerName, streamName, nItems, endTime; 

	public CollatorLogging(CollatorControl collatorControl, CollatorDataBlock collatorDataBlock) {
		super(collatorDataBlock);
		this.collatorControl = collatorControl;
		this.collatorDataBlock = collatorDataBlock;
		
		PamTableDefinition table = new PamTableDefinition(collatorControl.getUnitName());
		table.addTableItem(streamName = new PamTableItem("Stream", Types.CHAR, 80));
		table.addTableItem(triggerName = new PamTableItem("Trigger", Types.CHAR, 80));
		table.addTableItem(nItems = new PamTableItem("N Contacts", Types.INTEGER));
		table.addTableItem(endTime = new PamTableItem("End Time", Types.TIMESTAMP));
		
		setTableDefinition(table);
	}

	@Override
	public void setTableData(SQLTypes sqlTypes, PamDataUnit pamDataUnit) {
		CollatorDataUnit cdu = (CollatorDataUnit) pamDataUnit;
		CollatorTriggerData trigDat = cdu.findTriggerData();
		streamName.setValue(cdu.getStreamName());
		triggerName.setValue(trigDat.getTriggerName());
		if (trigDat.getDataList() != null) {
			nItems.setValue(trigDat.getDataList().size());
		}
		else {
			nItems.setValue(null);
		}
		endTime.setValue(sqlTypes.getTimeStamp(trigDat.getEndTime()));
	}


}
