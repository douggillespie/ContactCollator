package contactcollator.swing;

import java.awt.Window;

import PamView.dialog.PamDialog;
import contactcollator.CollatorControl;
import contactcollator.CollatorParams;

public class CollatorDialog extends PamDialog {

	private static final long serialVersionUID = 1L;

	private CollatorDialogPanel collatorDialogPanel;
	
	private boolean cancelled = false;
	
	private CollatorDialog(CollatorControl collatorControl, Window parentFrame, boolean hasDefault) {
		super(parentFrame, collatorControl.getUnitName(), false);
		collatorDialogPanel = new CollatorDialogPanel(collatorControl, this);
		setDialogComponent(collatorDialogPanel.getDialogComponent());
	}
	
	public static CollatorParams showDialog(CollatorControl collatorControl, Window parentFrame) {
		CollatorDialog collatorDialog = new CollatorDialog(collatorControl, parentFrame, false);
		collatorDialog.cancelled = false;
		collatorDialog.setParams();
		collatorDialog.setVisible(true);
		return collatorDialog.cancelled ? null : collatorControl.getCollatorParams();
	}

	private void setParams() {
		collatorDialogPanel.setParams();
		pack();
	}

	@Override
	public boolean getParams() {
		return collatorDialogPanel.getParams();
	}

	@Override
	public void cancelButtonPressed() {
		cancelled = true;
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub

	}

}
