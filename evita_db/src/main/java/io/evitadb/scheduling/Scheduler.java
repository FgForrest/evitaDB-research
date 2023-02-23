/*
 *                         _ _        ____  ____
 *               _____   _(_) |_ __ _|  _ \| __ )
 *              / _ \ \ / / | __/ _` | | | |  _ \
 *             |  __/\ V /| | || (_| | |_| | |_) |
 *              \___| \_/ |_|\__\__,_|____/|____/
 *
 *   Copyright (c) 2023
 *
 *   Licensed under the Business Source License, Version 1.1 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   https://github.com/FgForrest/evitaDB/blob/main/LICENSE
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.evitadb.scheduling;

import io.evitadb.api.configuration.EvitaConfiguration;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.DiscardPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduler spins up a new {@link ScheduledThreadPoolExecutor} that regularly executes Evita maintenance jobs such as
 * cache invalidation of file system cleaning.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@Log4j2
public class Scheduler {
	/**
	 * Java based scheduled executor service.
	 */
	private final ScheduledThreadPoolExecutor executorService;

	public Scheduler(@Nonnull EvitaConfiguration mainConfig) {
		this.executorService = new ScheduledThreadPoolExecutor(
			mainConfig.getBackgroundThreadCount(),
			new EvitaThreadFactory(mainConfig.getBackgroundThreadPriority())
		);
		this.executorService.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
		this.executorService.setKeepAliveTime(365, TimeUnit.DAYS);
		this.executorService.setMaximumPoolSize(mainConfig.getBackgroundThreadCount());
		this.executorService.setRejectedExecutionHandler(new DiscardPolicy() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
				log.warn("Background threads queue full - planned maintenance job was discarded. " +
					"Increase `backgroundThreadCount` or `backgroundThreadPriority` in the main Evita configuration to avoid such situation."
				);
				super.rejectedExecution(r, e);
			}
		});
	}

	/**
	 * Method schedules execution of `runnable` after `initialDelay` with frequency of `period`.
	 */
	public void scheduleAtFixedRate(@Nonnull Runnable runnable, int initialDelay, int period, @Nonnull TimeUnit timeUnit) {
		this.executorService.scheduleAtFixedRate(runnable, initialDelay, period, timeUnit);
	}

	/**
	 * Method schedules immediate execution of `runnable`. If there is no free thread left in the pool, the runnable
	 * will be executed "as soon as possible".
	 */
	public void execute(@Nonnull Runnable runnable) {
		this.executorService.submit(runnable);
	}

	/**
	 * Terminates the scheduler.
	 */
	public void terminate() {
		this.executorService.shutdown();
	}

	/**
	 * Custom thread factory to manage thread priority and naming.
	 */
	private static class EvitaThreadFactory implements ThreadFactory {
		/**
		 * Counter monitoring the number of threads this factory created.
		 */
		private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();
		/**
		 * Home group for the new threads.
		 */
		private final ThreadGroup group;
		/**
		 * Priority for threads that are created by this factory.
		 * Initialized from {@link EvitaConfiguration#getBackgroundThreadPriority()}.
		 */
		private final int priority;

		public EvitaThreadFactory(int priority) {
			final SecurityManager securityManager = System.getSecurityManager();
			this.group = securityManager != null ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
			this.priority = priority;
		}

		@Override
		public Thread newThread(@Nonnull Runnable runnable) {
			final Thread thread = new Thread(group, runnable, "Evita-" + THREAD_COUNTER.incrementAndGet());
			if (priority > 0 && thread.getPriority() != priority) {
				thread.setPriority(priority);
			}
			return thread;
		}
	}
}
