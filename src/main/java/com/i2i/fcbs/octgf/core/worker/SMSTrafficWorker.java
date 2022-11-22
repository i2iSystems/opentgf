/*
 * This file is part of i2i-Systems.
 * Copyright 2022, i2i-Systems <opensource@i2i-systems.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.i2i.fcbs.octgf.core.worker;

import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.i2i.fcbs.octgf.bean.CustomerBean;
import com.i2i.fcbs.octgf.bean.DiameterSessionBean;
import com.i2i.fcbs.octgf.composer.SMSCCRComposer;
import com.i2i.fcbs.octgf.constant.TGFConstant;
import com.i2i.fcbs.octgf.core.traffic.TrafficRateFunction;
import com.i2i.fcbs.octgf.core.worker.model.TGFRequestType;

public class SMSTrafficWorker extends AbstractTrafficWorker {
	private static final Logger logger = LogManager.getLogger(SMSTrafficWorker.class);

	SMSTrafficWorker(List<CustomerBean> customers, PerfMon perfMon, int updateCount, int maxTpsTargetCycle, long loadTimeInMillis, TrafficRateFunction trafficRateFunction) {
		super(customers, new SMSCCRComposer(), perfMon, 1, updateCount, maxTpsTargetCycle, loadTimeInMillis, trafficRateFunction);
		logger.info("[SMSTrafficWorker] initialized");
	}

	@Override
	protected void sendInitial(List<SessionCustomerPair> sessionList) {
		DiameterSessionBean sessionBean = null;
		try {
			for (SessionCustomerPair pair : sessionList) {
				sessionBean = pair.getDiameterSessionBean();
				sessionBean.setRequestNumber(0);
				sessionBean.setCcTime(volume);
				sessionBean.setRequestType(TGFRequestType.INITIAL);
				sessionBean.setRequestTypeValue(TGFConstant.INITIAL_REQUEST);
				sessionBean.setEventDate(new Date());
				SessionState sessionState = new SessionState(sessionBean.getSessionID());
				sessionState.setInitial(1);
				sessionStateMap.put(sessionBean.getSessionID(), sessionState);
			}
		} catch (Exception e) {
			logger.error("[sendInitial()] Exception occurred while sending initial for session {} ... [NOK]", sessionBean);
			throw e;
		}
	}

	@Override
	protected void sendTermination(List<SessionCustomerPair> sessionList) {
		logger.trace("[sendTermination()]");
	}

	@Override
	protected int checkInitialRequests() {
		return sessionStateMap.size();
	}

	@Override
	protected void checkTerminateRequests() {
		logger.trace("[checkTerminateRequests()]");
	}
}
