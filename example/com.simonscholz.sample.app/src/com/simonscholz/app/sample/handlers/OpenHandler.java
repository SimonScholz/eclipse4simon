package com.simonscholz.app.sample.handlers;

import java.util.concurrent.TimeUnit;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
public class OpenHandler {

	@Execute
	public void execute(Shell shell) throws InterruptedException{
		
		TimeUnit.SECONDS.sleep(30);
		
		FileDialog dialog = new FileDialog(shell);
		dialog.open();
	}
}
