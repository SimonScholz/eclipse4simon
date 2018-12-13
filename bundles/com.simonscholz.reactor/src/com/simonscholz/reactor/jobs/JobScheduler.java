/*******************************************************************************
 * Copyright (c) 2018 Simon Scholz and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *	   Simon Scholz <simon.scholz@vogella.com>
 *******************************************************************************/
package com.simonscholz.reactor.jobs;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.jobs.Job;

import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Operators;
import reactor.core.scheduler.Scheduler;
import reactor.util.context.Context;

/**
 * Scheduler that runs tasks on a {@link Job}.
 */
public final class JobScheduler implements Scheduler {

	private String jobName;

	JobScheduler(String jobName) {
		this.jobName = jobName;
	}

	/**
	 * @return a new {@link Scheduler}
	 */
	public static Scheduler create(String jobName) {
		return new JobScheduler(jobName);
	}

	@Override
	public Disposable schedule(Runnable task) {
		JobScheduledDirectAction a = new JobScheduledDirectAction(task);

		Job.create(jobName, monitor -> a.run()).schedule();

		return a;
	}

	@Override
	public Disposable schedule(Runnable task, long delay, TimeUnit unit) {
		if (delay <= 0) {
			return schedule(task);
		}

		JobScheduledDirectAction a = new JobScheduledDirectAction(task);
		Job.create(jobName, monitor -> a.run()).schedule(unit.toMillis(delay));

		return a;
	}

	@Override
	public Disposable schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {

		long initialDelayMillis = unit.toMillis(initialDelay);

		JobPeriodicDirectAction a = new JobPeriodicDirectAction(jobName, task, System.currentTimeMillis() + initialDelayMillis,
				unit.toMillis(period));

		if (initialDelay <= 0) {
			Job.create(jobName, monitor -> a.run()).schedule();
		} else {
			Job.create(jobName, monitor -> a.run()).schedule(initialDelayMillis);
		}

		return a;

	}

	@Override
	public Worker createWorker() {
		return new JobWorker(jobName);
	}

	static final class JobWorker implements Worker {
		volatile boolean unsubscribed;
		private String jobName;
		
		public JobWorker(String jobName) {
			this.jobName = jobName;
		}

		@Override
		public void dispose() {
			if (unsubscribed) {
				return;
			}
			unsubscribed = true;
		}

		@Override
		public Disposable schedule(Runnable action) {
			if (!unsubscribed) {
				JobScheduledAction a = new JobScheduledAction(action, this);

				Job.create(jobName, monitor -> a.run()).schedule();

				return a;
			}

			throw Exceptions.failWithRejected();
		}

		@Override
		public Disposable schedule(Runnable action, long delayTime, TimeUnit unit) {
			if (delayTime <= 0) {
				return schedule(action);
			}

			if (!unsubscribed) {
				JobScheduledAction a = new JobScheduledAction(action, this);

				Job.create(jobName, monitor -> a.run()).schedule(unit.toMillis(delayTime));

				return a;
			}

			throw Exceptions.failWithRejected();
		}

		@Override
		public Disposable schedulePeriodically(Runnable task, long initialDelay, long period, TimeUnit unit) {
			long initialDelayMillis = unit.toMillis(initialDelay);

			JobPeriodicAction a = new JobPeriodicAction(jobName, task, this, System.currentTimeMillis() + initialDelayMillis,
					unit.toMillis(period));

			if (initialDelay <= 0) {
				Job.create(jobName, monitor -> a.run()).schedule();
			} else {
				Job.create(jobName, monitor -> a.run()).schedule(initialDelayMillis);
			}

			return a;
		}

		/**
		 * Represents a cancellable asynchronous Runnable that wraps an action and
		 * manages the associated Worker lifecycle.
		 */
		static final class JobScheduledAction extends AtomicBoolean implements Runnable, Disposable {

			/**  */
			private static final long serialVersionUID = -2864452628218128444L;

			final Runnable action;

			final JobWorker parent;

			public JobScheduledAction(Runnable action, JobWorker parent) {
				this.action = action;
				this.parent = parent;
			}

			@Override
			public void run() {
				if (!parent.unsubscribed && !get()) {
					try {
						action.run();
					} catch (Throwable ex) {
						Exceptions.throwIfFatal(ex);
						Operators.onErrorDropped(ex, Context.empty());
					}
				}
			}

			@Override
			public void dispose() {
				set(true);
			}
		}
	}

	static final class JobScheduledDirectAction extends AtomicBoolean implements Runnable, Disposable {

		/**  */
		private static final long serialVersionUID = 2378266891882031635L;

		final Runnable action;

		public JobScheduledDirectAction(Runnable action) {
			this.action = action;
		}

		@Override
		public void run() {
			if (!get()) {
				try {
					action.run();
				} catch (Throwable ex) {
					Exceptions.throwIfFatal(ex);
					Operators.onErrorDropped(ex, Context.empty());
				}
			}
		}

		@Override
		public void dispose() {
			set(true);
		}
	}

	static final class JobPeriodicDirectAction extends AtomicBoolean implements Runnable, Disposable {

		/**  */
		private static final long serialVersionUID = 1890399765810263705L;

		final Runnable task;

		final long periodMillis;

		final long start;

		long count;

		private String jobName;

		public JobPeriodicDirectAction(String jobName, Runnable task, long start, long periodMillis) {
			this.jobName = jobName;
			this.task = task;
			this.start = start;
			this.periodMillis = periodMillis;
		}

		@Override
		public void run() {
			if (get()) {
				return;
			}

			try {
				task.run();
			} catch (Throwable ex) {
				Exceptions.throwIfFatal(ex);
				Operators.onErrorDropped(ex, Context.empty());
				return;
			}

			if (get()) {
				return;
			}

			long now = System.currentTimeMillis();
			long next = start + ((++count) * periodMillis);
			long delta = Math.max(0, next - now);

			if (delta == 0) {
				Job.create(jobName, monitor -> this.run()).schedule();
			} else {
				Job.create(jobName, monitor -> this.run()).schedule(delta);
			}
		}

		@Override
		public void dispose() {
			set(true);
		}
	}

	static final class JobPeriodicAction extends AtomicBoolean implements Runnable, Disposable {

		/**  */
		private static final long serialVersionUID = 1890399765810263705L;

		final Runnable task;

		final long periodMillis;

		final long start;

		final JobWorker parent;

		long count;

		private String jobName;

		public JobPeriodicAction(String jobName,Runnable task, JobWorker parent, long start, long periodMillis) {
			this.jobName = jobName;
			this.task = task;
			this.start = start;
			this.periodMillis = periodMillis;
			this.parent = parent;
		}

		@Override
		public void run() {
			if (get() || parent.unsubscribed) {
				return;
			}

			try {
				task.run();
			} catch (Throwable ex) {
				Exceptions.throwIfFatal(ex);
				Operators.onErrorDropped(ex, Context.empty());
				return;
			}

			if (get() || parent.unsubscribed) {
				return;
			}

			long now = System.currentTimeMillis();
			long next = start + ((++count) * periodMillis);
			long delta = Math.max(0, next - now);

			if (delta == 0) {
				Job.create(jobName, monitor -> this.run()).schedule();
			} else {
				Job.create(jobName, monitor -> this.run()).schedule(delta);
			}
		}

		@Override
		public void dispose() {
			set(true);
		}
	}

}