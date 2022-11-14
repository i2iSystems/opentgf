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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpDataException;
import org.jdiameter.api.AvpSet;
import org.jdiameter.api.Request;

import com.i2i.fcbs.octgf.bean.DiameterSessionBean;
import com.i2i.fcbs.octgf.bean.TGFMessageBean;

public class SessionState {
	private static final Logger	logger		= LogManager.getLogger(SessionState.class);
	private String				sessionId;
	private AtomicInteger		initials	= new AtomicInteger(-1);
	private AtomicInteger		updates		= new AtomicInteger();
	private AtomicInteger		terminates	= new AtomicInteger(-1);

	public SessionState(String sessionId) {
		this.sessionId = sessionId;
	}

	private Map<String, Long> requestTimes = new ConcurrentHashMap<>();

	public void setInitial(int result) {
		initials.set(result);
	}

	public int addUpdates(int update) {
		return updates.addAndGet(update);
	}

	public void setTerminate() {
		terminates.set(1);
	}

	public void eventSent(DiameterSessionBean sessionBean) {
		String requestId = getRequestId(sessionBean.getSessionID(), sessionBean.getRequestType().getType(), sessionBean.getRequestNumber());
		requestTimes.put(requestId, System.currentTimeMillis());
	}

	private String getRequestId(String sessionId, int eventType, int requestNumber) {
		return sessionId + "-" + eventType + "-" + requestNumber;
	}

	public int getRequestNumber(Request request) {
		int requestNumber = 0;
		try {
			requestNumber = (int) request.getAvps().getAvp(Avp.CC_REQUEST_NUMBER).getUnsigned32();
		} catch (AvpDataException e) {
			logger.warn("[getRequestNumber()] [request number cannot be read]", e);
		}
		return requestNumber;
	}

	public int getEventType(Request request) {
		int requestType = 0;
		try {
			requestType = (int) getRequestType(request.getAvps()).getUnsigned32();
		} catch (AvpDataException e) {
			logger.warn("[getEventType()] [request type cannot be read]", e);
		}
		return requestType;
	}

	private static Avp getRequestType(AvpSet requestAvps) {
		Avp requestType = requestAvps.getAvp(Avp.CC_REQUEST_TYPE);
		return requestType;
	}

	public long responseReceived(TGFMessageBean message, boolean isTerminate) {
		String requestId = getRequestId(message.getSessionID(), message.getRequestType().getType(), message.getRequestNumber());
		Long sentTime = requestTimes.remove(requestId);
		if (isTerminate && !requestTimes.isEmpty()) {
			logger.warn("[eventReceived()] [Terminate reached but there are {} awaiting sentTime data for session {}. " +
					"Possible indication of out of synch request processing.]", requestTimes.size(), sessionId);
		}
		if (Objects.isNull(sentTime)) {
			logger.warn("[eventReceived()] [sentTime is null for requestId {}. This is not expected.]", requestId);
			return message.getRequestEventDate();
		}
		return sentTime;
	}

	public void eventExpired(Request request) {
		String requestId = getRequestId(request.getSessionId(), getRequestNumber(request), getEventType(request));
		Long sentTime = requestTimes.remove(requestId);
		logger.debug("[eventExpired()] [Request expired for requestId {} since {}]", requestId, sentTime);
	}

	public AtomicInteger getInitials() {
		return initials;
	}

	public AtomicInteger getUpdates() {
		return updates;
	}

	public AtomicInteger getTerminates() {
		return terminates;
	}
}
