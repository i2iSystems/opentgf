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

package com.i2i.fcbs.octgf.bean;

import java.util.Date;

import com.i2i.fcbs.octgf.core.worker.model.TGFRequestType;

public class DiameterSessionBean {
	private String			sessionID;
	private int				requestNumber	= -1;
	private TGFRequestType	requestType;
	private int				requestTypeValue;
	private Date			eventDate;
	private int				ccTime;

	/* Getter - Setter */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("SessionBean { ");
		builder.append("[SessionID: ").append(sessionID).append("], ");
		builder.append("[RequestType: ").append(requestType).append("], ");
		builder.append("[RequestNumber: ").append(requestNumber).append("], ");
		builder.append("[EventDate: ").append(eventDate).append("], ");
		builder.append("[CCTime: ").append(ccTime).append("], ");
		builder.append("}");
		return builder.toString();
	}

	public String getSessionID() {
		return sessionID;
	}

	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}

	public int getRequestNumber() {
		return requestNumber;
	}

	public void setRequestNumber(int requestNumber) {
		this.requestNumber = requestNumber;
	}

	public TGFRequestType getRequestType() {
		return requestType;
	}

	public int getRequestTypeValue() {
		return requestTypeValue;
	}

	public void setRequestTypeValue(int requestTypeValue) {
		this.requestTypeValue = requestTypeValue;
	}

	public void setRequestType(TGFRequestType requestType) {
		this.requestType = requestType;
	}

	public Date getEventDate() {
		return eventDate;
	}

	public void setEventDate(Date eventDate) {
		this.eventDate = eventDate;
	}

	public int getCcTime() {
		return ccTime;
	}

	public void setCcTime(int ccTime) {
		this.ccTime = ccTime;
	}
}
