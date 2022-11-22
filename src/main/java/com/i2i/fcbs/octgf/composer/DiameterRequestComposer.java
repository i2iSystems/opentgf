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

package com.i2i.fcbs.octgf.composer;

import java.util.Optional;

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpSet;
import org.jdiameter.client.api.IRequest;
import org.jdiameter.client.impl.parser.TGFMessageImpl;

import com.i2i.fcbs.octgf.bean.CustomerBean;
import com.i2i.fcbs.octgf.bean.DiameterSessionBean;
import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.constant.TGFConstant;
import com.i2i.fcbs.octgf.core.worker.model.TGFRequestType;

public interface DiameterRequestComposer {
	IRequest compose(CustomerBean customer, DiameterSessionBean session) throws Exception;

	default int getRequestTypeValue(TGFRequestType requestType) {
		if (requestType == TGFRequestType.INITIAL)
			return getInitialRequestType();
		if (requestType == TGFRequestType.INTERMEDIATE)
			return getIntermediateRequestType();
		if (requestType == TGFRequestType.FINAL)
			return getFinalRequestType();
		return getEventRequestType();
	}

	default int getInitialRequestType() {
		return 1;
	}

	default int getIntermediateRequestType() {
		return 2;
	}

	default int getFinalRequestType() {
		return 3;
	}

	default int getEventRequestType() {
		return 4;
	}

	default IRequest composeRequestBody(long appId, int commandCode, CustomerBean customer, DiameterSessionBean session) {
		IRequest ccr = new TGFMessageImpl(commandCode, appId);
		ccr.setNetworkRequest(false);
		ccr.setRequest(true);
		AvpSet avpSet = ccr.getAvps();
		avpSet.addAvp(Avp.SESSION_ID, session.getSessionID(), true, false, false);
		avpSet.addAvp(Avp.AUTH_APPLICATION_ID, appId, true, false, true);
		avpSet.addAvp(Avp.ORIGIN_HOST, TGFConfig.getOriginHost(), true, false, true);
		avpSet.addAvp(Avp.ORIGIN_REALM, TGFConfig.getOriginRealm(), true, false, true);
		avpSet.addAvp(Avp.DESTINATION_HOST, TGFConfig.getDestinationHost(), true, false, true);
		avpSet.addAvp(Avp.DESTINATION_REALM, TGFConfig.getDestinationRealm(), true, false, true);
		avpSet.addAvp(Avp.CC_REQUEST_TYPE, session.getRequestTypeValue());
		avpSet.addAvp(Avp.CC_REQUEST_NUMBER, session.getRequestNumber());
		avpSet.addAvp(Avp.EVENT_TIMESTAMP, session.getEventDate());
		addSubscriptionAvp(customer, avpSet);
		return ccr;
	}

	default void addSubscriptionAvp(CustomerBean customer, AvpSet avpSet) {
		AvpSet subsriptionIdE164 = avpSet.addGroupedAvp(Avp.SUBSCRIPTION_ID);
		subsriptionIdE164.addAvp(Avp.SUBSCRIPTION_ID_TYPE, getSipAwareMSISDNType());
		subsriptionIdE164.addAvp(Avp.SUBSCRIPTION_ID_DATA, getSipAwareMSISDN(customer.getMsisdn()), true);
		if (TGFConfig.isAddSubscriptionImsi()) {
			AvpSet subsriptionIdIMSI = avpSet.addGroupedAvp(Avp.SUBSCRIPTION_ID);
			subsriptionIdIMSI.addAvp(Avp.SUBSCRIPTION_ID_TYPE, TGFConstant.END_USER_IMSI);
			subsriptionIdIMSI.addAvp(Avp.SUBSCRIPTION_ID_DATA, customer.getImsi(), true);
		}
	}

	default String getSipAwareMSISDN(String msisdn) {
		if (TGFConfig.isUseSipFormattedNumbers())
			return "sip:" + msisdn + "@" + TGFConfig.getSipFormattedNumberUrl();
		return msisdn;
	}

	default int getSipAwareMSISDNType() {
		if (TGFConfig.isUseSipFormattedNumbers())
			return TGFConstant.END_USER_SIP_URI;
		return TGFConstant.END_USER_E164;
	}

	default void createCCRMMCConstants(AvpSet creditControl, String originMSISDN) {
		creditControl.addAvp(Avp.SERVICE_IDENTIFIER_CCA, TGFConstant.SERVIVE_IDENTIFIER, true, false, true);
		AvpSet creditControlServiceSpecificInfo6001 = creditControl.addGroupedAvp(Avp.SERVICE_SPECIFIC_INFO, TGFConstant.VENDOR_ID, true, false);
		creditControlServiceSpecificInfo6001.addAvp(Avp.SERVICE_SPECIFIC_TYPE, TGFConstant.SERVICE_INFO_SERVICE_TYPE_6001, TGFConstant.VENDOR_ID, true, false, true);
		creditControlServiceSpecificInfo6001.addAvp(Avp.SERVICE_SPECIFIC_DATA, TGFConstant.SERVICE_INFO_SERVICE_DATA_6001, TGFConstant.VENDOR_ID, true, false, true);
		AvpSet creditControlServiceSpecificInfo6009 = creditControl.addGroupedAvp(Avp.SERVICE_SPECIFIC_INFO, TGFConstant.VENDOR_ID, true, false);
		creditControlServiceSpecificInfo6009.addAvp(Avp.SERVICE_SPECIFIC_TYPE, TGFConstant.SERVICE_INFO_SERVICE_TYPE_6009, TGFConstant.VENDOR_ID, true, false, true);
		creditControlServiceSpecificInfo6009.addAvp(Avp.SERVICE_SPECIFIC_DATA, Optional.ofNullable(originMSISDN).orElse(TGFConstant.SERVICE_INFO_SERVICE_DATA_6009),
				TGFConstant.VENDOR_ID, true, false, true);
	}
}
