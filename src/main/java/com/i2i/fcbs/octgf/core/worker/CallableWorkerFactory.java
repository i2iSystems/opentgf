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
import java.util.concurrent.Callable;

import com.i2i.fcbs.octgf.bean.CustomerBean;
import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.constant.AppModeType;
import com.i2i.fcbs.octgf.constant.TGFServiceType;
import com.i2i.fcbs.octgf.core.traffic.TrafficRateFunction;

public class CallableWorkerFactory {
	private CallableWorkerFactory() {
	}

	public static Callable<Integer> create(PerfMon perfMon, List<CustomerBean> customerPartition, int updateCount, int maxTpsTargetCycle, long loadTimeInMillis,
			TrafficRateFunction trafficRateFunction) {
		Callable<Integer> callable;
		if (TGFConfig.getApp() == AppModeType.CC) {
			if (TGFConfig.getServiceToTest() == TGFServiceType.VOICE)
				callable = new IMSTrafficWorker(customerPartition, perfMon, updateCount, maxTpsTargetCycle, loadTimeInMillis, trafficRateFunction);
			else if (TGFConfig.getServiceToTest() == TGFServiceType.SMS)
				callable = new SMSTrafficWorker(customerPartition, perfMon, updateCount, maxTpsTargetCycle, loadTimeInMillis, trafficRateFunction);
			else if (TGFConfig.getServiceToTest() == TGFServiceType.DATA)
				callable = new DataTrafficWorker(customerPartition, perfMon, updateCount, maxTpsTargetCycle, loadTimeInMillis, trafficRateFunction);
			else
				throw new IllegalArgumentException("Unsupported service:" + TGFConfig.getServiceToTest());
		} else {
			throw new IllegalArgumentException("Unsupported app mode:" + TGFConfig.getApp());
		}
		return callable;
	}
}
