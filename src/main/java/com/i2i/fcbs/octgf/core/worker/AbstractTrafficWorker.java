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
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdiameter.api.*;
import org.jdiameter.api.EventListener;
import org.jdiameter.client.api.IRequest;

import com.i2i.fcbs.octgf.TGFObserver;
import com.i2i.fcbs.octgf.bean.CustomerBean;
import com.i2i.fcbs.octgf.bean.DiameterSessionBean;
import com.i2i.fcbs.octgf.bean.TGFMessageBean;
import com.i2i.fcbs.octgf.composer.DiameterRequestComposer;
import com.i2i.fcbs.octgf.composer.TGFMessageComposer;
import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.constant.TGFServiceType;
import com.i2i.fcbs.octgf.core.TGFSharedData;
import com.i2i.fcbs.octgf.core.traffic.TrafficRateFunction;
import com.i2i.fcbs.octgf.core.traffic.TrafficRateHelper;
import com.i2i.fcbs.octgf.core.worker.model.TGFRequestType;
import com.i2i.fcbs.octgf.exception.TransmitException;
import com.i2i.fcbs.octgf.jmx.TGFPerformanceFacade;
import com.i2i.fcbs.octgf.jmx.TGFPerformanceMBean;

public class AbstractTrafficWorker implements Callable<Integer>, EventListener<Request, Answer> {
	private static final Logger					logger																		= LogManager
			.getLogger(AbstractTrafficWorker.class);
	private static final String					RECEIVED_SUCCESS_MESSAGE_MESSAGE_SESSION_ID_IS_NOT_IN_MY_SESSION_MAP_NOK	= "[receivedSuccessMessage()] Message SessionID ({}) [{}] is not in my sessionMap.... [NOK]";
	private static final String					RECEIVED_SUCCESS_MESSAGE_ANSWER_RECEIVED_MESSAGE_OK							= "[receivedSuccessMessage()] {} Answer received... Message [{}]... [OK]";
	private static final String					APP_INDICATOR																= "OCTGF";
	public static final String					SMS_DISCRIMINATOR															= "pppp";
	protected static final boolean				WAIT_FOR_RESPONSE															= false;
	private static final AtomicInteger			workerIndex																	= new AtomicInteger(-1);
	private final int							workerId																	= workerIndex.incrementAndGet();
	final int									volume;
	protected final int							sessionCount;
	protected final PerfMon						perfMon;
	protected final MessageTransmitter			messageTransmitter;
	protected final List<SessionCustomerPair>	customerList;
	protected final int							maxTpsTargetCycle;
	protected final long						deadLine;
	private final DiameterRequestComposer		diameterRequestComposer;
	private final int							updatePerSessionCount;
	private final TrafficRateFunction			trafficRateFunction;
	// session -> requestType -> -> resultList
	protected final Map<String, SessionState>	sessionStateMap;
	private static final long					UNIT_OF_TIME_IN_MILLIS														= Duration.ofSeconds(1).toMillis();
	private final long							timeSlotLengthInMillis														= TGFConfig.getTrafficSlotLengthInMillis();
	private final long							numberOfSlots																= UNIT_OF_TIME_IN_MILLIS / timeSlotLengthInMillis;
	private boolean								fatalErrorOccurred															= false;
	private int									totalUpdateRequestCount														= 0;
	private int									totalTerminateRequestCount													= 0;

	AbstractTrafficWorker(List<CustomerBean> customers, DiameterRequestComposer diameterRequestComposer, PerfMon perfMon, int volume, int updatePerSessionCount,
			int maxTpsTargetCycle, long loadTimeInMillis, TrafficRateFunction trafficRateFunction) {
		this.sessionCount = customers.size();
		this.perfMon = perfMon;
		this.diameterRequestComposer = diameterRequestComposer;
		this.updatePerSessionCount = updatePerSessionCount;
		this.maxTpsTargetCycle = Math.max(1, maxTpsTargetCycle);
		this.deadLine = System.currentTimeMillis() + loadTimeInMillis;
		this.trafficRateFunction = trafficRateFunction;
		sessionStateMap = new ConcurrentHashMap<>();
		customerList = new ArrayList<>();
		this.volume = volume;
		for (CustomerBean customerBean : customers) {
			customerList.add(new SessionCustomerPair(customerBean, new DiameterSessionBean()));
		}
		messageTransmitter = getMessageTransmitter();
		logger.info("[{}()] [Worker-{} :  numberOfSlots={}, totalSessions={}]", getClass().getSimpleName(), workerId, numberOfSlots, this.sessionCount);
	}

	private MessageTransmitter getMessageTransmitter() {
		return MessageTransmitterFactory.create(this);
	}

	private int calculateRequestPerSlot(List<SessionCustomerPair> sessionList) {
		return (int) Math.ceil(sessionList.size() * 1.0 / numberOfSlots);
	}

	private boolean isDeadLineExceed() {
		boolean isExceed = false;
		long now = System.currentTimeMillis();
		if (now >= deadLine) {
			isExceed = true;
		}
		return isExceed;
	}

	@Override
	public Integer call() throws Exception {
		int cycle = 1;
		boolean run = true;
		List<SessionCustomerPair> sessionList;
		try {
			perfMon.addFitCustomers(sessionCount);
			while (run) {
				if (WAIT_FOR_RESPONSE) {
					// those stats are important if wait for response is enabled
					sessionStateMap.clear();
					totalUpdateRequestCount = 0;
					totalTerminateRequestCount = 0;
				}
				logger.info("[call()] Sending initial requests... [OK]");
				int upperBound = (int) Math.min(customerList.size(), (cycle * 1.0) / this.maxTpsTargetCycle * customerList.size());
				logger.info("[call()] Upper bound is {} ... [OK]", upperBound);
				sessionList = customerList.subList(0, upperBound);
				resetSessionBeans(cycle, sessionList);
				sendInitial(sessionList);
				if (WAIT_FOR_RESPONSE) {
					int success = checkInitialRequests();
					if (success == 0) {
						throw new Exception("None of the initials succeeded.");
					}
				}
				logger.info("[call()] Sending update requests ... [OK]");
				sendUpdate(sessionList);
				if (WAIT_FOR_RESPONSE)
					checkUpdateRequests();
				logger.info("[call()] Sending termination requests... [OK]");
				sendTermination(sessionList);
				if (totalTerminateRequestCount > 0 && WAIT_FOR_RESPONSE)
					checkTerminateRequests();
				cycle++;
				run = checkForTestCompletion(run);
			}
		} catch (InterruptedException i) {
			logger.error("Thread has been canceled... [OK]");
			Thread.currentThread().interrupt();
		} catch (TransmitException exc) {
			logger.warn("[{}] [Traffic failed. Terminating task! Transmitter id='{}']", getClass().getSimpleName(), messageTransmitter.getIdentificationString(), exc);
		}
		messageTransmitter.close();
		return 0;
	}

	private boolean checkForTestCompletion(boolean run) {
		if (TGFObserver.getShutdownSignal().get()) {
			logger.error("[call()] Shutdown signal received. Thread will be terminated... [NOK]");
			run = false;
		}
		if (isDeadLineExceed()) {
			logger.info("[call()] DeadLine reached. Thread will be terminated... [OK]");
			run = false;
		}
		return run;
	}

	protected void resetSessionBeans(int cycle, List<SessionCustomerPair> sessions) {
		for (int sessionIndex = 0; sessionIndex < sessions.size(); sessionIndex++) {
			SessionCustomerPair sessionCustomerPair = sessions.get(sessionIndex);
			DiameterSessionBean sessionBean = sessionCustomerPair.getDiameterSessionBean();
			sessionBean.setSessionID(generateNewSessionID(cycle, sessionIndex));
			sessionBean.setRequestNumber(-1);
			sessionBean.setCcTime(volume);
			sessionBean.setRequestType(TGFRequestType.INITIAL);
			sessionBean.setEventDate(new Date(System.currentTimeMillis()));
			sessionStateMap.put(sessionBean.getSessionID(), new SessionState(sessionBean.getSessionID()));
		}
	}

	private void checkUpdateRequests() throws Exception {
		logger.info("[checkUpdateRequests()] checking for {} update requests ... [OK]", totalUpdateRequestCount);
		long waitUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
		boolean check = true;
		while (check) {
			checkFatalException(fatalErrorOccurred, "[checkUpdateRequests()] FatalError occurred.. Thread will be die... [NOK]");
			int totalResult = 0;
			for (Entry<String, SessionState> e : sessionStateMap.entrySet()) {
				Integer updates = e.getValue().getUpdates().get();
				totalResult += updates;
			}
			if (totalResult == totalUpdateRequestCount) {
				logger.info("[checkUpdateRequests()] Update answers are ok!... [OK]");
				check = false;
				continue;
			}
			check = checkUpdateResponse(waitUntil, check, totalResult);
		}
	}

	private boolean checkUpdateResponse(long waitUntil, boolean check, int totalResult) throws InterruptedException {
		long now = System.currentTimeMillis();
		if (now >= waitUntil) {
			logger.error("[checkUpdateRequests()] -----------------------------------------------------------------");
			for (Entry<String, SessionState> sessionResult : sessionStateMap.entrySet()) {
				if (sessionResult.getValue().getInitials().get() != 1) {
					logger.debug("[checkUpdateRequests()] Update request was not sent! Initial request answer was not success!");
				} else if (sessionResult.getValue().getUpdates().get() == 0) {
					logger.error("[checkUpdateRequests()] Session [{}] Update answers are missing... [NOK]", sessionResult.getKey());
				} else if (sessionResult.getValue().getUpdates().get() < updatePerSessionCount) {
					logger.error("[checkUpdateRequests()] Session [{}] Some update answers are missing; {} expected {} found... [NOK]", sessionResult.getKey(),
							updatePerSessionCount, sessionResult.getValue().getUpdates().get());
				}
			}
			logger.error("[checkUpdateRequests()] ********");
			logger.error("Some update requests are missing. [{}] required but [{}] received... [NOK]", totalUpdateRequestCount, totalResult);
			check = false;
		} else {
			logger.trace("[checkUpdateRequests()] Update requests are not completed... Waiting... [OK]");
			Thread.sleep(100);
		}
		return check;
	}

	protected void checkTerminateRequests() throws Exception {
		long waitUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
		boolean check = true;
		while (check) {
			checkFatalException(fatalErrorOccurred, "[checkTerminateRequests()] FatalError occured.. Thread will be die... [NOK]");
			int totalResult = 0;
			for (Entry<String, SessionState> e : sessionStateMap.entrySet()) {
				Integer updates = e.getValue().getTerminates().get();
				totalResult += updates;
			}
			if (totalResult == totalTerminateRequestCount) {
				logger.info("[checkTerminateRequests()] Terminate answers are ok!... [OK]");
				check = false;
				continue;
			}
			check = checkTerminateResponse(waitUntil, check, totalResult);
		}
	}

	private boolean checkTerminateResponse(long waitUntil, boolean check, int totalResult) throws InterruptedException {
		long now = System.currentTimeMillis();
		if (now >= waitUntil) {
			logger.error("[checkTerminateRequests()] -----------------------------------------------------------------");
			for (Entry<String, SessionState> e : sessionStateMap.entrySet()) {
				if (e.getValue().getTerminates().get() == 0) {
					logger.error("[checkTerminateRequests()] Session [{}] terminate answer is missing... [NOK]", e.getKey());
				}
			}
			logger.error("[checkTerminateRequests()] ********");
			logger.error("[checkTerminateRequests()] Some terminate requests are missing. [{}] required but [{}] received... [NOK]", totalTerminateRequestCount, totalResult);
			check = false;
		} else {
			logger.trace("[checkTerminateRequests()] terminate requests are not completed... Waiting... [OK]");
			Thread.sleep(100);
		}
		return check;
	}

	protected int checkInitialRequests() throws Exception {
		long waitUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
		while (true) {
			checkFatalException(fatalErrorOccurred, "[checkInitialRequests()] FatalError occured.. Thread will be die... [NOK]");
			int totalResult = 0;
			int totalSuccess = 0;
			for (Entry<String, SessionState> e : sessionStateMap.entrySet()) {
				int initials = e.getValue().getInitials().get();
				totalSuccess += initials > 0 ? 1 : 0;
				totalResult += initials >= 0 ? 1 : 0;
			}
			if (totalResult == sessionStateMap.size()) {
				if (totalResult != totalSuccess) {
					logger.warn("Some initial requests failed. {} succeeded out of {} initial requests. [OK]", totalSuccess, totalResult);
				}
				logger.info("[checkInitialRequests()] All answers are ok!... [OK]");
				return totalSuccess;
			}
			checkInitialResponse(waitUntil, totalResult);
		}
	}

	private void checkFatalException(boolean fatalErrorOccured, String s) throws Exception {
		if (fatalErrorOccured) {
			throw new Exception(s);
		}
	}

	private void checkInitialResponse(long waitUntil, int totalResult) throws Exception {
		long now = System.currentTimeMillis();
		if (now >= waitUntil) {
			logger.error("[checkInitialRequests()] -----------------------------------------------------------------");
			for (Entry<String, SessionState> sessionResult : sessionStateMap.entrySet()) {
				if (sessionResult.getValue().getInitials().get() < 0) {
					logger.error("[checkInitialRequests()] Session [{}] initialRequest answer is missing... [NOK]", sessionResult.getKey());
				}
			}
			logger.error("[checkInitialRequests()] -----------------------------------------------------------------");
			throw new Exception("Some initial requests are missing. [" + sessionCount + "] required but [" + totalResult + "] received... [NOK]");
		}
		logger.trace("[checkInitialRequests()] Initial answers are not completed... Waiting... [OK]");
		Thread.sleep(100);
	}

	protected void sendInitial(List<SessionCustomerPair> sessions) throws TransmitException {
		long updatesStart = System.currentTimeMillis();
		int requestsSent = sendAtRates(TGFRequestType.INITIAL, sessions);
		perfMon.addFitCustomers(-1 * requestsSent);
		logger.debug("[sendInitial()] [{} initial requests sent in total in {} millis]", requestsSent, System.currentTimeMillis() - updatesStart);
	}

	private int sendAtRates(TGFRequestType requestType, List<SessionCustomerPair> sessions) throws TransmitException {
		int requestsSent = 0;
		int offset = 0;
		List<Integer> rates = TrafficRateHelper.calculateRatePartitions(trafficRateFunction, sessions.size());
		logger.debug("[sendAtRates()] [Rates={}]", rates);
		for (int rate : rates) {
			requestsSent += sendAndAdjust(requestType, sessions.subList(offset, offset + rate));
			offset += rate;
		}
		return requestsSent;
	}

	protected void sendTermination(List<SessionCustomerPair> sessions) throws TransmitException {
		int requestsSent = 0;
		long updatesStart = System.currentTimeMillis();
		requestsSent += sendAtRates(TGFRequestType.FINAL, sessions);
		totalTerminateRequestCount += requestsSent;
		logger.debug("[sendTermination()] [{} termination requests sent in total in {} millis]", requestsSent, System.currentTimeMillis() - updatesStart);
	}

	private void sendUpdate(List<SessionCustomerPair> sessions) throws TransmitException {
		int requestsSent = 0;
		long updatesStart = System.currentTimeMillis();
		for (int sequenceIndex = 0; sequenceIndex < updatePerSessionCount; sequenceIndex++) {
			long startASecond = System.currentTimeMillis();
			requestsSent += sendAtRates(TGFRequestType.INTERMEDIATE, sessions);
			totalUpdateRequestCount += requestsSent;
			long lengthOfASecond = System.currentTimeMillis() - startASecond;
			logger.debug("[sendUpdate()] [{} update requests sent in total for update sequence {} in {} millis]", requestsSent, sequenceIndex, lengthOfASecond);
		}
		logger.debug("[sendUpdate()] [{} update requests sent in total in {} millis]", requestsSent, System.currentTimeMillis() - updatesStart);
	}

	private int sendAndAdjust(TGFRequestType requestType, List<SessionCustomerPair> sessions) throws TransmitException {
		int requestsSent = 0;
		int requestPerSlot = calculateRequestPerSlot(sessions);
		long workUnitStartTime = System.currentTimeMillis();
		for (int slot = 0; slot < numberOfSlots; slot++) {
			long slotStartTimeInMillis = System.currentTimeMillis();
			List<SessionCustomerPair> slotSessions = sessions.subList(Math.min(sessions.size(), slot * requestPerSlot),
					Math.min(sessions.size(), (slot + 1) * requestPerSlot));
			int slotRequestsSent = 0;
			for (SessionCustomerPair pair : slotSessions) {
				DiameterSessionBean sessionBean = pair.getDiameterSessionBean();
				String sessionId = sessionBean.getSessionID();
				if (TGFConfig.getServiceToTest() == TGFServiceType.SMS && sessionId.contains(SMS_DISCRIMINATOR)) {
					sessionId = removeSmsDiscriminator(sessionId);
				}
				SessionState sessionState = sessionStateMap.get(sessionId);
				int initialResults = sessionState.getInitials().get();
				if (!isEligable(requestType, sessionId, initialResults))
					continue;
				sessionBean.setRequestType(requestType);
				sessionBean.setRequestTypeValue(diameterRequestComposer.getRequestTypeValue(requestType));
				sessionBean.setRequestNumber(sessionBean.getRequestNumber() + 1);
				sessionBean.setEventDate(new Date());
				// for terminal voice request, cctime is sum of all cctime requests
				sessionBean.setCcTime(
						TGFConfig.getServiceToTest() == TGFServiceType.VOICE && requestType == TGFRequestType.FINAL ? (sessionBean.getRequestNumber() + 1) * volume : volume);
				IRequest request = null;
				try {
					request = diameterRequestComposer.compose(pair.getCustomerBean(), sessionBean);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				sessionState.eventSent(sessionBean);
				messageTransmitter.transmit(request);
				logger.debug("[sendAndAdjust()] [Req sent for session {}  type {} req-num {}]", sessionId, requestType, pair.getDiameterSessionBean().getRequestNumber());
				slotRequestsSent++;
				perfMon.incReq();
			}
			long usedSlotTimeInMillis = System.currentTimeMillis() - slotStartTimeInMillis;
			logger.debug("[sendAndAdjust()] [{}/{} reqs sent in slot {} in {} millis]", slotRequestsSent, slotSessions.size(), slot, usedSlotTimeInMillis);
			long usedWorkUnitTime = System.currentTimeMillis() - workUnitStartTime;
			long remainingWorkUnitTime = UNIT_OF_TIME_IN_MILLIS - usedWorkUnitTime;
			// adjustment for last slot
			long slotLength = Math.min(Math.max(0, remainingWorkUnitTime), timeSlotLengthInMillis);
			adjust(slot, slotLength, usedSlotTimeInMillis);
			requestsSent += slotRequestsSent;
		}
		return requestsSent;
	}

	private boolean isEligable(TGFRequestType requestType, String sessionId, int initialResults) {
		if (initialResults == 0) {
			// only successful/untested initial requests
			logger.trace("[sendAndAdjust()] [initial is failed, no more request sent for session {}]", sessionId);
			return false;
		}
		if (requestType != TGFRequestType.INITIAL && initialResults == -1 && messageTransmitter.isResponseExpected()) {
			logger.trace("[sendAndAdjust()] [Initial is not responded, skipping request sent for session {}]", sessionId);
			perfMon.addSkipped();
			return false;
		}
		return true;
	}

	private void adjust(int slot, long slotLength, long usedSlotTimeInMillis) {
		if (usedSlotTimeInMillis < slotLength) {
			long residual = slotLength - usedSlotTimeInMillis;
			logger.debug("[adjust()    ] [Residl. time in slot {} is {} millis]", slot, residual);
			try {
				Thread.sleep(residual);
			} catch (InterruptedException e) {
				logger.warn("[adjust()] [Interrupted while sleeping for adjustment]");
				Thread.currentThread().interrupt();
			}
		}
	}

	private String removeSmsDiscriminator(String sessionId) {
		return sessionId.split(SMS_DISCRIMINATOR)[0];
	}

	private String generateNewSessionID(int cycle, int customerNo) {
		return APP_INDICATOR + "-" + TGFConfig.getTestSessionId() + "-" + TGFConfig.getApplicationInstanceID() + "-" +
				workerId +
				"-" +
				cycle +
				"-" +
				customerNo +
				"-" +
				System.currentTimeMillis();
	}

	@Override
	public void receivedSuccessMessage(Request request, Answer answer) {
		handleResponse(request, answer);
	}

	private void handleResponse(Request request, Answer answer) {
		try {
			Optional<TGFPerformanceMBean> tgfPerformanceMBean = TGFPerformanceFacade.getPerformanceBean();
			if (answer.getApplicationId() != TGFConfig.getApp().getAppId()) {
				logger.error("[receivedSuccessMessage()] Received bad answer: {}. Discarding...", answer.getCommandCode());
			}
			String sessionId = request.getAvps().getAvp(Avp.SESSION_ID).getUTF8String();
			if (sessionId.contains(SMS_DISCRIMINATOR))
				sessionId = removeSmsDiscriminator(sessionId);
			boolean terminate = getRequestType(request) == TGFRequestType.FINAL;
			TGFRequestType requestType = getRequestType(request);
			TGFMessageBean message = TGFMessageComposer.compose(request, answer, requestType, terminate);
			StatusCodePair stCodePair = msccSuccess(message);
			if (stCodePair.status) {
				logger.debug("[receivedSuccessMessage()] Message SessionID [{}] and RequestType [{}] returned [{}] resultCode... [OK]", sessionId, message.getRequestType(),
						message.getResultCode());
				tgfPerformanceMBean.ifPresent(TGFPerformanceMBean::incInSuccCount);
			} else {
				logger.error("[receivedSuccessMessage()] Message SessionID [{}] and RequestType [{}] returned [{}] resultCode... [NOK]", sessionId, message.getRequestType(),
						message.getResultCode());
				tgfPerformanceMBean.ifPresent(TGFPerformanceMBean::incInFailCount);
			}
			tgfPerformanceMBean.ifPresent(TGFPerformanceMBean::incInPackCount);
			perfMon.incResp(stCodePair.status, stCodePair.code);
			logger.debug(RECEIVED_SUCCESS_MESSAGE_ANSWER_RECEIVED_MESSAGE_OK, requestType, message);
			if (sessionStateMap.containsKey(sessionId)) {
				SessionState resultMap = sessionStateMap.get(sessionId);
				if (resultMap == null) {
					logger.warn("[receivedSuccessMessage()] [resultMap is null for sessionId ['{}'], event type['{}']. Creating a new one. This is not expected.] [NOK]",
							sessionId, requestType);
					resultMap = new SessionState(sessionId);
					sessionStateMap.put(sessionId, resultMap);
				}
				if (terminate) {
					sessionStateMap.remove(sessionId);
				}
				updateCounters(message, message.getRequestType(), resultMap);
				TGFSharedData.addMessage(message, stCodePair.status);
			} else {
				fatalErrorOccurred = true;
				logger.error(RECEIVED_SUCCESS_MESSAGE_MESSAGE_SESSION_ID_IS_NOT_IN_MY_SESSION_MAP_NOK, requestType, sessionId);
			}
		} catch (Exception t) {
			logger.error("[receivedSuccessMessage()] [Error at receive response for session {}]", request.getSessionId(), t);
		}
	}

	private TGFRequestType getRequestType(Request request) throws AvpDataException {
		return getRequestType(getRequestTypeValue(request));
	}

	private TGFRequestType getRequestType(int requestType) {
		if (diameterRequestComposer.getFinalRequestType() == requestType)
			return TGFRequestType.FINAL;
		if (diameterRequestComposer.getIntermediateRequestType() == requestType)
			return TGFRequestType.INTERMEDIATE;
		if (diameterRequestComposer.getInitialRequestType() == requestType)
			return TGFRequestType.INITIAL;
		return TGFRequestType.EVENT;
	}

	protected int getRequestTypeValue(Request request) throws AvpDataException {
		return request.getAvps().getAvp(Avp.CC_REQUEST_TYPE).getInteger32();
	}

	private void updateCounters(TGFMessageBean message, TGFRequestType eventTypeValue, SessionState sessionState) {
		if (eventTypeValue == TGFRequestType.INITIAL) {
			boolean status = msccSuccess(message).status;
			sessionState.setInitial(status ? 1 : 0);//fail:0, true: 1
			perfMon.addFitCustomers(status ? 1 : 0);
		} else if (eventTypeValue == TGFRequestType.FINAL) {
			sessionState.setTerminate();
		} else {
			sessionState.addUpdates(1);
		}
		long sentTime = sessionState.responseReceived(message, eventTypeValue == TGFRequestType.FINAL);
		logger.debug("[updateCounters()] [Sent time for session {} is {}]", message.getSessionID(), sentTime);
		message.setRequestEventDate(sentTime);
		perfMon.addDelay((int) (message.getResponseEventDate() - message.getRequestEventDate()));
	}

	private StatusCodePair msccSuccess(TGFMessageBean message) {
		boolean status = message.getResultCode() == 2001L;
		if (!status || message.getMscControls().isEmpty())
			return new StatusCodePair(status, message.getResultCode());
		if (Boolean.TRUE.equals(TGFConfig.getMsccLookUpEnabled())) {
			for (Entry<Long, Long> entry : message.getMscControls().entrySet()) {
				boolean msccStatus = entry.getValue() == 2001L;
				if (!msccStatus) {
					logger.warn("[receivedSuccessMessage()] [MSCC result lookup {} failed with code {}]", entry.getKey(), entry.getValue());
					return new StatusCodePair(false, entry.getValue());
				}
			}
		}
		return new StatusCodePair(true, message.getResultCode());
	}

	private static class StatusCodePair {
		private boolean	status;
		private long	code;

		StatusCodePair(boolean status, long code) {
			this.status = status;
			this.code = code;
		}
	}

	@Override
	public void timeoutExpired(Request request) {
		perfMon.incExpired();
		Optional<TGFPerformanceMBean> tgfPerformanceMBean = TGFPerformanceFacade.getPerformanceBean();
		tgfPerformanceMBean.ifPresent(TGFPerformanceMBean::incInPackCount);
		SessionState sessionState = sessionStateMap.get(request.getSessionId());
		if (sessionState != null) {
			sessionState.eventExpired(request);
			try {
				TGFSharedData.addMessage(TGFMessageComposer.compose(request, null, getRequestType(request), false), false);
			} catch (AvpDataException e) {
				logger.warn("[timeoutExpired()] [Error while adding to message queue for session-id {}]", request.getSessionId(), e);
			}
		}
		try {
			logger.warn("[timeoutExpired()] now={} Session[{}], Request type [{}]  expired... [NOK] ", System.currentTimeMillis(), request.getSessionId(),
					getRequestType(request));
		} catch (AvpDataException e) {
			logger.error("[timeoutExpired()] [Error while trying to parse avp for session {}]", request.getSessionId());
		}
	}
}
