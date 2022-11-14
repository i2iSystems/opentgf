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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.i2i.fcbs.octgf.bean.CustomerBean;
import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.core.traffic.LinearRateFunction;
import com.i2i.fcbs.octgf.core.traffic.SinusoidalRateFunction;
import com.i2i.fcbs.octgf.core.traffic.TrafficRateFunction;

import io.netty.util.concurrent.DefaultThreadFactory;

public class TGFTraffic implements Callable<Void> {
	private static final Logger					logger	= LogManager.getLogger(TGFTraffic.class);
	private final List<List<CustomerBean>>		taskCustomerPartitions;
	private final ExecutorService				threadPool;
	private final CompletionService<Integer>	completionService;
	private final int							trafficIndex;
	private final int							totalCustomers;

	public TGFTraffic(List<CustomerBean> customers, int trafficIndex) {
		this.trafficIndex = trafficIndex;
		this.totalCustomers = customers.size();
		int taskCustomerSize = TGFConfig.getTps() / TGFConfig.getTrafficThreadCount();
		this.taskCustomerPartitions = Lists.partition(customers, taskCustomerSize);
		this.threadPool = Executors.newFixedThreadPool(TGFConfig.getTrafficThreadCount(), new DefaultThreadFactory("tgf-traffic-worker"));
		this.completionService = new ExecutorCompletionService<>(threadPool);
	}

	@Override
	public Void call() {
		List<Future<Integer>> futures = new ArrayList<>();
		try {
			logger.info(
					"[call()] [Session is created with id={}, trafficIndex={}, instance id={}, load time={} minutes, update_count={}, thread count={}, total customers: {}]",
					TGFConfig.getTestSessionId(), trafficIndex, TGFConfig.getApplicationInstanceID(), TimeUnit.MILLISECONDS.toMinutes(TGFConfig.getLoadTimeInMillis()),
					TGFConfig.getSessionSequenceLimit(), TGFConfig.getTrafficThreadCount(), totalCustomers);
			PerfMon perfMon = PerfMon.getInstance();
			for (int threadIndex = 1; threadIndex <= TGFConfig.getTrafficThreadCount(); threadIndex++) {
				List<CustomerBean> taskCustomerPartition = taskCustomerPartitions.get(threadIndex - 1);
				perfMon.start();
				Callable<Integer> callable = CallableWorkerFactory.create(perfMon, taskCustomerPartition, TGFConfig.getSessionSequenceLimit(),
						TGFConfig.getMaxTpsTargetCycle(), TGFConfig.getLoadTimeInMillis(), buildRateFunction());
				futures.add(completionService.submit(callable));
			}
			int taskSize = futures.size();
			for (int threadIndex = 1; threadIndex <= taskSize; threadIndex++) {
				Future<Integer> completedFuture = completionService.take();
				completedFuture.get();
				logger.info("[call()] [Sub-task {}/{} of task {} completed!]", threadIndex, taskSize, trafficIndex);
			}
			logger.info("[call()] [Sub-tasks of task {} completed!]", trafficIndex);
			taskCustomerPartitions.forEach(ignored -> perfMon.stop());
			logger.info("[call()] [Wait for 1 minutes to let responses arrive]");
			Thread.sleep(Duration.ofMinutes(1).toMillis());
			threadPool.shutdown();
		} catch (InterruptedException | ExecutionException t) {
			logger.error("[call()] An error occurred while running task {}", trafficIndex, t);
			threadPool.shutdownNow();
		}
		logger.info("[call()] TGFTraffic task {} completed... [OK]", trafficIndex);
		return null;
	}

	private TrafficRateFunction buildRateFunction() {
		TrafficRateFunction trafficRateFunction;
		if (TGFConfig.getTrafficGeneratorSineFunctionWaveLength() != 0 && TGFConfig.getTrafficGeneratorSineFunctionWaveAmplitude() != 0) {
			logger.info("[buildRateFunction()] [TPS={}, Amplitude={}, Wave Length={}]", TGFConfig.getTps(), TGFConfig.getTrafficGeneratorSineFunctionWaveAmplitude(),
					TGFConfig.getTrafficGeneratorSineFunctionWaveLength());
			trafficRateFunction = new SinusoidalRateFunction(TGFConfig.getTps() - TGFConfig.getTrafficGeneratorSineFunctionWaveAmplitude(),
					TGFConfig.getTrafficGeneratorSineFunctionWaveAmplitude(), TGFConfig.getTrafficGeneratorSineFunctionWaveLength());
		} else {
			trafficRateFunction = new LinearRateFunction(TGFConfig.getTps());
		}
		logger.info("[buildRateFunction()] [Returning {}]", trafficRateFunction);
		return trafficRateFunction;
	}
}
