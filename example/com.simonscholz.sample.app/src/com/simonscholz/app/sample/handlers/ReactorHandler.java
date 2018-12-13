 
package com.simonscholz.app.sample.handlers;

import java.util.concurrent.TimeUnit;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

import com.simonscholz.reactor.ui.ProgressMonitorDialogScheduler;

import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.swing.SwtScheduler;

public class ReactorHandler {
	@Execute
	public void execute(Shell shell) {
		Scheduler progressScheduler = ProgressMonitorDialogScheduler.create(shell, "Doing a reactor job");
		
		Mono<String> mono = Mono.create(sink -> {
			try {
				TimeUnit.SECONDS.sleep(3);
			} catch (InterruptedException e) {
				Exceptions.throwIfFatal(e);
			}
			sink.success("Simon Scholz");
		});
		
		mono.subscribeOn(progressScheduler).log().cache().publishOn(SwtScheduler.from(shell.getDisplay())).subscribe(s -> MessageDialog.openInformation(shell, "Info", s));
	}
		
}