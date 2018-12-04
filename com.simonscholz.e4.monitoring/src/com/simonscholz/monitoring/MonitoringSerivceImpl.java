package com.simonscholz.monitoring;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;

import com.simonscholz.monitoring.EventLoopMonitorThread.Parameters;
import com.simonscholz.services.monitoring.FreezeMonitorService;
import com.simonscholz.services.monitoring.PreferenceConstants;

/**
 * Starts the event loop monitoring thread. Initializes preferences from
 * {@link IPreferenceStore}.
 */
@Component
public class MonitoringSerivceImpl implements FreezeMonitorService {
	private IPreferenceStore preferenceStore;
	private IEclipsePreferences eclipsePreferences;

	/**
	 * Creates and starts a new monitoring thread.
	 */
	public void createAndStartMonitorThread(Display display) {
		EventLoopMonitorThread.Parameters args = loadPreferences();
		EventLoopMonitorThread temporaryThread = null;

		try {
			temporaryThread = new EventLoopMonitorThread(args, display);
		} catch (IllegalArgumentException e) {
			Bundle bundle = FrameworkUtil.getBundle(getClass());
			ILog log = Platform.getLog(bundle);
			log.log(new Status(IStatus.ERROR, bundle.getSymbolicName(), Messages.MonitoringStartup_initialization_error,
					e));
		}

		final EventLoopMonitorThread thread = temporaryThread;
		// Final setup and start asynchronously on the display thread.
		display.asyncExec(() -> {
			// If we're still running when display gets disposed, shutdown the thread.
			display.disposeExec(() -> thread.shutdown());
			thread.start();
		});
	}

	public void setPreferences(IPreferenceStore preferenceStore) {
		this.preferenceStore = preferenceStore;
	}

	public void setPreferences(IEclipsePreferences eclipsePreferences) {
		this.eclipsePreferences = eclipsePreferences;
	}

	private EventLoopMonitorThread.Parameters loadPreferences() {

		if (preferenceStore != null) {
			return getArgsFromPreferenceStore(preferenceStore);
		} else if (eclipsePreferences != null) {
			return getArgsFromEclipsePreferences(eclipsePreferences);
		}

		return defaultArguments();
	}

	private Parameters defaultArguments() {
		EventLoopMonitorThread.Parameters args = new EventLoopMonitorThread.Parameters();

		args.longEventWarningThreshold = 500;
		args.longEventErrorThreshold = 2000;
		args.deadlockThreshold = 300000;
		args.maxStackSamples = 3;
		args.uiThreadFilter = "";
		args.logToErrorLog = true;
		args.noninterestingThreadFilter = "java.*" //$NON-NLS-1$
				+ ",sun.*" //$NON-NLS-1$
				+ ",org.eclipse.core.internal.jobs.WorkerPool.sleep" //$NON-NLS-1$
				+ ",org.eclipse.core.internal.jobs.WorkerPool.startJob" //$NON-NLS-1$
				+ ",org.eclipse.core.internal.jobs.Worker.run" //$NON-NLS-1$
				+ ",org.eclipse.osgi.framework.eventmgr.EventManager$EventThread.getNextEvent" //$NON-NLS-1$
				+ ",org.eclipse.osgi.framework.eventmgr.EventManager$EventThread.run" //$NON-NLS-1$
				+ ",org.eclipse.equinox.internal.util.impl.tpt.timer.TimerImpl.run" //$NON-NLS-1$
				+ ",org.eclipse.equinox.internal.util.impl.tpt.threadpool.Executor.run"; //$NON-NLS-1$

		return args;
	}

	private Parameters getArgsFromEclipsePreferences(IEclipsePreferences eclipsePreferences) {
		EventLoopMonitorThread.Parameters args = new EventLoopMonitorThread.Parameters();

		args.longEventWarningThreshold = eclipsePreferences
				.getInt(PreferenceConstants.LONG_EVENT_WARNING_THRESHOLD_MILLIS, 500);
		args.longEventErrorThreshold = eclipsePreferences.getInt(PreferenceConstants.LONG_EVENT_ERROR_THRESHOLD_MILLIS,
				2000);
		args.deadlockThreshold = eclipsePreferences.getInt(PreferenceConstants.DEADLOCK_REPORTING_THRESHOLD_MILLIS,
				300000);
		args.maxStackSamples = eclipsePreferences.getInt(PreferenceConstants.MAX_STACK_SAMPLES, 3);
		args.uiThreadFilter = eclipsePreferences.get(PreferenceConstants.UI_THREAD_FILTER, "");
		args.logToErrorLog = eclipsePreferences.getBoolean(PreferenceConstants.LOG_TO_ERROR_LOG, true);
		args.noninterestingThreadFilter = eclipsePreferences.get(PreferenceConstants.NONINTERESTING_THREAD_FILTER,
				"java.*" //$NON-NLS-1$
						+ ",sun.*" //$NON-NLS-1$
						+ ",org.eclipse.core.internal.jobs.WorkerPool.sleep" //$NON-NLS-1$
						+ ",org.eclipse.core.internal.jobs.WorkerPool.startJob" //$NON-NLS-1$
						+ ",org.eclipse.core.internal.jobs.Worker.run" //$NON-NLS-1$
						+ ",org.eclipse.osgi.framework.eventmgr.EventManager$EventThread.getNextEvent" //$NON-NLS-1$
						+ ",org.eclipse.osgi.framework.eventmgr.EventManager$EventThread.run" //$NON-NLS-1$
						+ ",org.eclipse.equinox.internal.util.impl.tpt.timer.TimerImpl.run" //$NON-NLS-1$
						+ ",org.eclipse.equinox.internal.util.impl.tpt.threadpool.Executor.run"); //$NON-NLS-1$

		return args;
	}

	private EventLoopMonitorThread.Parameters getArgsFromPreferenceStore(IPreferenceStore preferenceStore) {
		EventLoopMonitorThread.Parameters args = new EventLoopMonitorThread.Parameters();

		args.longEventWarningThreshold = preferenceStore
				.getInt(PreferenceConstants.LONG_EVENT_WARNING_THRESHOLD_MILLIS);
		args.longEventErrorThreshold = preferenceStore.getInt(PreferenceConstants.LONG_EVENT_ERROR_THRESHOLD_MILLIS);
		args.deadlockThreshold = preferenceStore.getInt(PreferenceConstants.DEADLOCK_REPORTING_THRESHOLD_MILLIS);
		args.maxStackSamples = preferenceStore.getInt(PreferenceConstants.MAX_STACK_SAMPLES);
		args.uiThreadFilter = preferenceStore.getString(PreferenceConstants.UI_THREAD_FILTER);
		args.noninterestingThreadFilter = preferenceStore.getString(PreferenceConstants.NONINTERESTING_THREAD_FILTER);
		args.logToErrorLog = preferenceStore.getBoolean(PreferenceConstants.LOG_TO_ERROR_LOG);

		return args;
	}
}
