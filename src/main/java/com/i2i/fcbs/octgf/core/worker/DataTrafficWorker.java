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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.i2i.fcbs.octgf.bean.CustomerBean;
import com.i2i.fcbs.octgf.composer.DataCCRComposer;
import com.i2i.fcbs.octgf.core.traffic.TrafficRateFunction;

public class DataTrafficWorker extends AbstractTrafficWorker {
	private static final Logger logger = LogManager.getLogger(DataTrafficWorker.class);

	public DataTrafficWorker(List<CustomerBean> customers, PerfMon perfMon, int updateCount, int maxTpsTargetCycle, long loadTimeInMillis,
			TrafficRateFunction trafficRateFunction) {
		super(customers, new DataCCRComposer(), perfMon, 8000, updateCount, maxTpsTargetCycle, loadTimeInMillis, trafficRateFunction);
		logger.info("[DataTrafficWorker] initialized");
	}
}
