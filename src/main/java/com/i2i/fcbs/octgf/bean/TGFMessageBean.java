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

package com.i2i.fcbs.octgf.bean;

import java.util.Map;

import com.i2i.fcbs.octgf.core.worker.model.TGFRequestType;

public class TGFMessageBean {
	private String			sessionID;
	private Integer			requestNumber;
	private TGFRequestType	requestType;
	private Long			resultCode;
	private Long			eventDate;
	private Long			requestEventDate;
	private Long			responseEventDate;
	private String			imsi;
	private Map<Long, Long>	mscControls;

	public TGFMessageBean(String sessionID, Integer requestNumber, TGFRequestType requestType, Long resultCode, Long eventDate, Long responseEventDate, String imsi,
			Map<Long, Long> mscControls) {
		super();
		this.sessionID = sessionID;
		this.requestNumber = requestNumber;
		this.requestType = requestType;
		this.resultCode = resultCode;
		this.eventDate = eventDate;
		this.requestEventDate = eventDate;
		this.responseEventDate = responseEventDate;
		this.imsi = imsi;
		this.mscControls = mscControls;
	}

	@Override
	public String toString() {
		return "TGFMessageBean{" +
				"sessionID='" + sessionID + '\'' +
				", requestNumber=" + requestNumber +
				", requestType=" + requestType +
				", resultCode=" + resultCode +
				", eventDate=" + eventDate +
				", requestEventDate=" + requestEventDate +
				", responseEventDate=" + responseEventDate +
				", imsi='" + imsi + '\'' +
				'}';
	}

	/* Getter - Setter */
	public String getSessionID() {
		return sessionID;
	}

	public Integer getRequestNumber() {
		return requestNumber;
	}

	public TGFRequestType getRequestType() {
		return requestType;
	}

	public Long getRequestEventDate() {
		return requestEventDate;
	}

	public Long getResultCode() {
		return resultCode;
	}

	public Long getEventDate() {
		return eventDate;
	}

	public Map<Long, Long> getMscControls() {
		return mscControls;
	}

	public Long getResponseEventDate() {
		return responseEventDate;
	}

	public String getImsi() {
		return imsi;
	}

	public void setRequestEventDate(Long requestEventDate) {
		this.requestEventDate = requestEventDate;
	}
}
