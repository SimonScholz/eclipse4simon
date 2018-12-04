/*******************************************************************************
 * Copyright (C) 2014, 2015 Google Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Marcus Eng (Google) - initial API and implementation
 *     Sergey Prigogin (Google)
 *******************************************************************************/
package com.simonscholz.services.monitoring;

/**
 * Responsible for holding the stack traces for a UI event.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @since 1.0
 */
public class UiFreezeEvent {
	private final long startTimestamp;
	private final long totalDuration;
	private final StackSample[] stackTraceSamples;
	private final boolean isStillRunning;
	private final boolean isStarvedAwake;
	private final boolean isStarvedAsleep;

	/**
	 * Creates a UiFreezeEvent.
	 *
	 * @param startTime initial dispatch time for the event in milliseconds since January 1,
	 *     1970 UTC
	 * @param duration duration of the event in milliseconds
	 * @param samples array of {@link StackSample}s containing thread information
	 * @param stillRunning whether or not the event was still running when this UiFreezeEvent
	 *     was created. If {@code true}, this UiFreezeEvent may indicate a deadlock.
	 */
	public UiFreezeEvent(long startTime, long duration, StackSample[] samples,
			boolean stillRunning, boolean starvedAwake, boolean starvedAsleep) {
		this.startTimestamp = startTime;
		this.stackTraceSamples = samples;
		this.totalDuration = duration;
		this.isStillRunning = stillRunning;
		this.isStarvedAwake = starvedAwake;
		this.isStarvedAsleep = starvedAsleep;
	}

	/**
	 * Returns the time when the UI thread froze, in milliseconds since January 1, 1970 UTC.
	 */
	public final long getStartTimestamp() {
		return startTimestamp;
	}

	/**
	 * Returns the total amount of time in milliseconds that the UI thread remained frozen.
	 */
	public final long getTotalDuration() {
		return totalDuration;
	}

	/**
	 * Returns the stack trace samples obtained during the event.
	 */
	public final StackSample[] getStackTraceSamples() {
		return stackTraceSamples;
	}

	/**
	 * Returns {@code true} if this event was still ongoing at the time the event was logged,
	 * which can happen for deadlocks.
	 */
	public final boolean isStillRunning() {
		return isStillRunning;
	}

	/**
	 * Returns {@code true} if the monitoring thread starved for CPU while awake.
	 */
	public final boolean isStarvedAwake() {
		return isStarvedAwake;
	}

	/**
	 * Returns {@code true} if the monitoring thread starved for CPU while asleep.
	 */
	public final boolean isStarvedAsleep() {
		return isStarvedAsleep;
	}

	/** For debugging only. */
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder();
		buf.append("Freeze started at "); //$NON-NLS-1$
		buf.append(startTimestamp);
		if (isStillRunning) {
			buf.append(" still ongoing after "); //$NON-NLS-1$
		} else {
			buf.append(" lasted "); //$NON-NLS-1$
		}
		buf.append(totalDuration);
		buf.append("ms"); //$NON-NLS-1$
		if (isStarvedAwake || isStarvedAsleep) {
			String when =
					isStarvedAwake && isStarvedAsleep ?	"awake and asleep" : //$NON-NLS-1$
					isStarvedAwake ? "awake" : "asleep"; //$NON-NLS-1$ //$NON-NLS-2$
			buf.append(", monitoring thread starved for CPU while " + when); //$NON-NLS-1$
		}
		if (stackTraceSamples.length != 0) {
			buf.append("\nStack trace samples:"); //$NON-NLS-1$
			for (StackSample stackTraceSample : stackTraceSamples) {
				buf.append('\n');
				buf.append(stackTraceSample.toString());
			}
		}
		return buf.toString();
	}
}
