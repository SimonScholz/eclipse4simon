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
	private EventLoopMonitorThread monitoringThread;
	private boolean monitorThreadRestartInProgress;

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

		monitoringThread = temporaryThread;
		// Final setup and start asynchronously on the display thread.
		display.asyncExec(() -> {
			// If we're still running when display gets disposed, shutdown the thread.
			display.disposeExec(() -> monitoringThread.shutdown());
			monitoringThread.start();
		});
	}

	@Override
	public void setPreferencesAndStartIfNecessary(IPreferenceStore preferenceStore, Display display) {
		this.preferenceStore = preferenceStore;
		this.preferenceStore.addPropertyChangeListener(event -> {
			String property = event.getProperty();
			restartMonitoringThread(preferenceStore.getBoolean(PreferenceConstants.MONITORING_ENABLED), display,
					property);
		});
		restartMonitoringThread(
				preferenceStore.getBoolean(PreferenceConstants.MONITORING_ENABLED),
				display, PreferenceConstants.MONITORING_ENABLED);
	}

	@Override
	public void setPreferencesAndStartIfNecessary(IEclipsePreferences eclipsePreferences, Display display) {
		this.eclipsePreferences = eclipsePreferences;
		eclipsePreferences.addPreferenceChangeListener(event -> {
			String key = event.getKey();
			restartMonitoringThread(PreferenceConstants.MONITORING_ENABLED.equals(event.getKey())
					&& Boolean.TRUE.equals(event.getNewValue()), display, key);
		});
		restartMonitoringThread(
				eclipsePreferences.getBoolean(PreferenceConstants.MONITORING_ENABLED, false),
				display, PreferenceConstants.MONITORING_ENABLED);
	}

	private void restartMonitoringThread(boolean isMonitoringEnabled, Display display, String key) {
		if (!isPreferenceChangeRelevant(key)) {
			return;
		}

		synchronized (this) {
			if (monitorThreadRestartInProgress) {
				return;
			}

			monitorThreadRestartInProgress = true;

			// Schedule the event to restart the thread after all preferences have had
			// enough time
			// to propagate.
			display.asyncExec(() -> refreshMonitoringThread(display, isMonitoringEnabled));
		}
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

	private synchronized void refreshMonitoringThread(Display display, boolean isMonitoringEnabled) {
		if (monitoringThread != null) {
			monitoringThread.shutdown();
			monitoringThread = null;
		}
		monitorThreadRestartInProgress = false;

		if (isMonitoringEnabled) {
			createAndStartMonitorThread(display);
		}
	}

	private boolean isPreferenceChangeRelevant(String key) {
		return key.equals(PreferenceConstants.MONITORING_ENABLED)
				|| key.equals(PreferenceConstants.DEADLOCK_REPORTING_THRESHOLD_MILLIS)
				|| key.equals(PreferenceConstants.LONG_EVENT_ERROR_THRESHOLD_MILLIS)
				|| key.equals(PreferenceConstants.LONG_EVENT_WARNING_THRESHOLD_MILLIS)
				|| key.equals(PreferenceConstants.LOG_TO_ERROR_LOG) || key.equals(PreferenceConstants.MAX_STACK_SAMPLES)
				|| key.equals(PreferenceConstants.UI_THREAD_FILTER)
				|| key.equals(PreferenceConstants.NONINTERESTING_THREAD_FILTER);
	}
}
