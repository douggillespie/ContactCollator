package contactcollator.io;

import java.sql.Types;

import PamguardMVC.PamDataUnit;
import contactcollator.CollatorControl;
import contactcollator.CollatorDataBlock;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLTypes;

public class CollatorLocationLogging extends CollatorLogging {
	
	private CollatorLocationJsonSource collatorLocationJsonSource = new CollatorLocationJsonSource();
	
	private PamTableItem jsonData;

	public CollatorLocationLogging(CollatorControl collatorControl, CollatorDataBlock collatorDataBlock) {
		super(collatorControl, collatorDataBlock, collatorControl.getUnitName() + " Locations");

		this.setUpdatePolicy(UPDATE_POLICY_WRITENEW);
		
		PamTableDefinition table = getTableDefinition();
		table.addTableItem(jsonData = new PamTableItem("Localisation", Types.VARCHAR));
	}

	@Override
	public void setTableData(SQLTypes sqlTypes, PamDataUnit pamDataUnit) {
		super.setTableData(sqlTypes, pamDataUnit);
		String str = collatorLocationJsonSource.getPackedObject(pamDataUnit);
		jsonData.setValue(str);
	}

}
