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

package com.i2i.fcbs.octgf.composer;

import java.util.HashMap;
import java.util.Map;
import org.jdiameter.api.*;

import com.i2i.fcbs.octgf.bean.TGFMessageBean;
import com.i2i.fcbs.octgf.core.worker.model.TGFRequestType;

public class TGFMessageComposer {

	private TGFMessageComposer() {
	}

	public static TGFMessageBean compose(Request request, Answer answer, TGFRequestType requestType, boolean terminate) throws AvpDataException {
		AvpSet requestAvps = request.getAvps();
		int requestNumber = getRequestNumber(terminate, requestAvps);
		String sessionID = request.getSessionId();
		Long resultCode = getResultCode(answer, requestAvps);
		Map<Long, Long> mscControls = getMultipleServicesCreditControlResultCodes(answer);
		String imsi = getImsi(requestAvps);
		long startDate = request.getAvps().getAvp(Avp.EVENT_TIMESTAMP).getTime().getTime();
		long endDate = System.currentTimeMillis();
		return new TGFMessageBean(sessionID, requestNumber, requestType, resultCode, startDate, endDate, imsi, mscControls);
	}

		private static Map<Long, Long> getMultipleServicesCreditControlResultCodes(Answer answer) throws AvpDataException {
		Map<Long, Long> mscControls = new HashMap<>();
		if (answer != null) {
			AvpSet answerAvps = answer.getAvps();
			if (answerAvps.getAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL) != null) {
				AvpSet avps = answerAvps.getAvps(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
				for (int i = 0; i < avps.size(); i++) {
					Avp multipleCreditControlAvpSet = avps.getAvpByIndex(i);
					AvpSet grouped = multipleCreditControlAvpSet.getGrouped();
					Avp avp;
					long ratingGroup = 0;
					long resultCode = 0;
					if ((avp = grouped.getAvp(Avp.RATING_GROUP)) != null) {
						ratingGroup = avp.getUnsigned32();
					}
					if ((avp = grouped.getAvp(Avp.RESULT_CODE)) != null) {
						resultCode = avp.getUnsigned32();
					}
					mscControls.put(ratingGroup, resultCode);
				}
			}
		}
		return mscControls;
	}

	private static Long getResultCode(Answer answer, AvpSet requestAvps) throws AvpDataException {
		Long resultCode = null;
		if (answer != null) {
			Avp resultAvp = answer.getResultCode();
			if (resultAvp == null) {
				requestAvps.addAvp(Avp.RESULT_CODE, -999);
				resultAvp = answer.getResultCode();
			}
			resultCode = resultAvp.getUnsigned32();
		}
		return resultCode;
	}

	private static String getImsi(AvpSet avpSet) throws AvpDataException {
		String imsi = null;
		AvpSet avps = avpSet.getAvps(Avp.SUBSCRIPTION_ID);
		for (int i = 0; i < avps.size(); i++) {
			Avp avpByIndex = avps.getAvpByIndex(i);
			AvpSet grouped = avpByIndex.getGrouped();
			Avp avp;
			if ((avp = grouped.getAvp(Avp.SUBSCRIPTION_ID_DATA)) != null) {
				imsi = avp.getUTF8String();
			}
		}
		return imsi;
	}

	private static int getRequestNumber(boolean terminate, AvpSet requestAvps) throws AvpDataException {
		Avp requestNumberAvp = requestAvps.getAvp(Avp.CC_REQUEST_NUMBER);
		if (requestNumberAvp == null && !terminate) {
			requestAvps.addAvp(Avp.CC_REQUEST_NUMBER, -999);
			requestNumberAvp = requestAvps.getAvp(Avp.CC_REQUEST_NUMBER);
		}
		return terminate && requestNumberAvp == null ? 999 : requestNumberAvp.getInteger32();
	}
}
