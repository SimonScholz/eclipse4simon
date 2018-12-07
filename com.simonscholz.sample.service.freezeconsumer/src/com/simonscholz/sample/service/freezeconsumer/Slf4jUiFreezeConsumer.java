package com.simonscholz.sample.service.freezeconsumer;

import java.lang.management.LockInfo;
import java.lang.management.ThreadInfo;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.osgi.util.NLS;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simonscholz.services.monitoring.StackSample;
import com.simonscholz.services.monitoring.UiFreezeConsumer;
import com.simonscholz.services.monitoring.UiFreezeEvent;

@Component
public class Slf4jUiFreezeConsumer implements UiFreezeConsumer {

	private static final Logger LOG = LoggerFactory.getLogger(Slf4jUiFreezeConsumer.class);

	private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS"); //$NON-NLS-1$
	private long longEventErrorThresholdMillis;

	private static class StackTrace extends Throwable {
		private static final long serialVersionUID = -2829405667536819137L;

		StackTrace(StackTraceElement[] stackTraceElements) {
			setStackTrace(stackTraceElements);
		}
	}

	@Override
	public void accept(UiFreezeEvent event) {
		long lastTimestamp = event.getStartTimestamp();
		String startTime = dateFormat.format(new Date(lastTimestamp));

		long duration = event.getTotalDuration();
		BiConsumer<String, Throwable> severityThrowableLogger;
		Consumer<String> severityMessageLogger;
		if (duration >= longEventErrorThresholdMillis) {
			severityThrowableLogger = LOG::error;
			severityMessageLogger = LOG::error;
		} else {
			severityThrowableLogger = LOG::warn;
			severityMessageLogger = LOG::warn;
		}

		String template = event.isStillRunning() ? "Messages.DefaultUiFreezeEventLogger_ui_freeze_ongoing_header_2"
				: "Messages.DefaultUiFreezeEventLogger_ui_freeze_finished_header_2";
		String format = duration >= 100000 ? "%.0f" : duration >= 10 ? "%.2g" : "%.1g"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String header = NLS.bind(template, String.format(format, duration / 1000.0), startTime);

		StackSample[] stackTraceSamples = event.getStackTraceSamples();
		if (stackTraceSamples.length == 0 && (event.isStarvedAwake() || event.isStarvedAsleep())) {
			String note = (event.isStarvedAwake() || event.isStarvedAsleep())
					? "Messages.DefaultUiFreezeEventLogger_starved_awake_and_asleep "
					: event.isStarvedAwake() ? "Messages.DefaultUiFreezeEventLogger_starved_awake "
							: "Messages.DefaultUiFreezeEventLogger_starved_asleep";
			header += note;
		}

		severityMessageLogger.accept(header);

		for (StackSample sample : stackTraceSamples) {
			double deltaInSeconds = (sample.getTimestamp() - lastTimestamp) / 1000.0;
			ThreadInfo[] threads = sample.getStackTraces();

			// The first thread is guaranteed to be the display thread.
			Throwable stackTrace = new StackTrace(threads[0].getStackTrace());
			String traceText = NLS.bind("Messages.DefaultUiFreezeEventLogger_sample_header_2",
					dateFormat.format(sample.getTimestamp()), String.format("%.3f", deltaInSeconds)); //$NON-NLS-1$

			severityThrowableLogger.accept(String.format("%s\n%s", traceText, createThreadMessage(threads[0])),
					stackTrace);

			for (int j = 1; j < threads.length; j++) {
				logThreadStatus(threads[j], severityThrowableLogger);
			}

			lastTimestamp = sample.getTimestamp();
		}
	}

	private static StringBuilder createThreadMessage(ThreadInfo thread) {
		String threadDetails = NLS.bind("Messages.DefaultUiFreezeEventLogger_thread_details", thread.getThreadId(),
				thread.getThreadState());

		StringBuilder threadText = new StringBuilder(
				NLS.bind("Messages.DefaultUiFreezeEventLogger_thread_header_2", thread.getThreadName(), threadDetails));

		return threadText;
	}

	private static void logThreadStatus(ThreadInfo thread, BiConsumer<String, Throwable> severityThrowableLogger) {
		Throwable stackTrace = new StackTrace(thread.getStackTrace());
		StringBuilder threadText = createThreadMessage(thread);
		String lockName = thread.getLockName();
		if (lockName != null && !lockName.isEmpty()) {
			LockInfo lock = thread.getLockInfo();
			String lockOwnerName = thread.getLockOwnerName();
			if (lockOwnerName == null) {
				threadText.append(
						NLS.bind("Messages.DefaultUiFreezeEventLogger_waiting_for_1", getClassAndHashCode(lock)));
			} else {
				threadText.append(NLS.bind("Messages.DefaultUiFreezeEventLogger_waiting_for_with_lock_owner_3",
						new Object[] { getClassAndHashCode(lock), lockOwnerName, thread.getLockOwnerId() }));
			}
		}

		for (LockInfo lockInfo : thread.getLockedSynchronizers()) {
			threadText.append(NLS.bind("Messages.DefaultUiFreezeEventLogger_holding_1", getClassAndHashCode(lockInfo)));
		}

		severityThrowableLogger.accept(threadText.toString(), stackTrace);
	}

	private static String getClassAndHashCode(LockInfo info) {
		return String.format("%s@%08x", info.getClassName(), info.getIdentityHashCode()); //$NON-NLS-1$
	}

}
