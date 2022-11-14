/*
 *  Copyright 2022, i2i-Systems <opensource@i2i-systems.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.i2i.fcbs.octgf.core.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.i2i.fcbs.octgf.TGFObserver;
import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.core.TGFSharedData;

public class TGFPersister implements Callable<Void> {
	private static final Logger			logger	= LogManager.getLogger(TGFPersister.class);
	private ExecutorService				threadPool;
	private CompletionService<Integer>	completionService;

	public TGFPersister() {
		initialize();
	}

	private void initialize() {
		this.threadPool = Executors.newFixedThreadPool(TGFConfig.getPersisterThreadCount());
		this.completionService = new ExecutorCompletionService<>(threadPool);
		TGFSharedData.getMessageQueue().clear();
	}

	@Override
	public Void call() {
		List<Future<Integer>> futures = new ArrayList<>();
		int aggregatedInsertedRecords = 0;
		try {
			for (int i = 0; i < TGFConfig.getPersisterThreadCount(); i++) {
				Future<Integer> future = completionService.submit(new TGFMessagePersisterWorker(i));
				futures.add(future);
			}
			int taskSize = futures.size();
			for (int i = 0; i < taskSize; i++) {
				Future<Integer> completedFuture = completionService.take();
				aggregatedInsertedRecords += completedFuture.get();
			}
			threadPool.shutdown();
		} catch (Exception t) {
			logger.error("[call()] An error occured while persister pool running...", t);
			TGFObserver.getShutdownSignal().set(true);
			threadPool.shutdownNow();
		}
		logger.info("[call()] TGFPersister completed, {} records saved [OK]", aggregatedInsertedRecords);
		return null;
	}
}
