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

package com.i2i.fcbs.octgf.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.i2i.fcbs.octgf.TGFObserver;
import com.i2i.fcbs.octgf.bean.CustomerBean;
import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.core.worker.PerfMon;
import com.i2i.fcbs.octgf.core.worker.TGFPersister;
import com.i2i.fcbs.octgf.core.worker.TGFTraffic;
import com.i2i.fcbs.octgf.service.SessionService;

import io.netty.util.concurrent.DefaultThreadFactory;

public class TGFThreadService implements Runnable {
	private static final Logger			logger	= LogManager.getLogger(TGFThreadService.class);
	private final ExecutorService		threadPool;
	private final List<Future<Void>>	futures;
	private final List<CustomerBean>	allCustomers;
	private final int					nPartitions;

	public TGFThreadService(List<CustomerBean> allCustomers, int nPartitions) {
		this.allCustomers = allCustomers;
		this.nPartitions = nPartitions;
		this.threadPool = Executors.newFixedThreadPool(nPartitions + 1, new DefaultThreadFactory("tgf-thread-service"));
		this.futures = new ArrayList<>();
	}

	@Override
	public void run() {
		try {
			Future<Void> persisterFuture = threadPool.submit(new TGFPersister());
			int partitionSize = allCustomers.size() / nPartitions;
			logger.info("[TGFThreadService] [Total customer size: {}; {} partitions, partitionSize={}]", allCustomers.size(), nPartitions, partitionSize);
			List<List<CustomerBean>> customerPartitions = Lists.partition(allCustomers, partitionSize);
			TGFConfig.setTestSessionId(SessionService.insertSession());
			for (int taskIndex = 1; taskIndex <= nPartitions; taskIndex++) {
				List<CustomerBean> customerPartition = customerPartitions.get(taskIndex - 1);
				logger.info("[TGFThreadService] [Task {}. Customer partition size = {}]", taskIndex, customerPartition.size());
				Future<Void> trafficFuture = threadPool.submit(new TGFTraffic(customerPartition, taskIndex));
				futures.add(trafficFuture);
			}
			threadPool.shutdown();
			for (int taskIndex = 1; taskIndex <= futures.size(); taskIndex++) {
				Future<Void> future = futures.get(taskIndex - 1);
				future.get();
				logger.info("[TGFThreadService] [Task index {}/{} is completed]", taskIndex, futures.size());
			}
			TGFObserver.setTgfTrafficCompleted(true);
			TGFObserver.getShutdownSignal().set(true);
			logger.info("[run()] TGF Traffic Services completed. Waiting for persister service... [OK]");
			persisterFuture.get();
			SessionService.updateSession(TGFConfig.getTestSessionId(), PerfMon.getInstance());
			logger.info("[run()] TGF ThreadService completed. TGF will be terminated... [OK]");
		} catch (Exception e) {
			logger.error("[run()] Exception occurred", e);
		}
		System.exit(1);
	}
}
