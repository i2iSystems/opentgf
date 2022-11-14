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

import java.util.Date;

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpSet;
import org.jdiameter.client.api.IRequest;

import com.i2i.fcbs.octgf.bean.CustomerBean;
import com.i2i.fcbs.octgf.bean.DiameterSessionBean;
import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.constant.TGFConstant;
import com.i2i.fcbs.octgf.core.worker.model.TGFRequestType;

public class IMSCCRComposer implements DiameterRequestComposer {
	@Override
	public IRequest compose(CustomerBean customer, DiameterSessionBean session) throws Exception {
		IRequest ccr = composeRequestBody(TGFConstant.ccAppId, TGFConstant.ccrCommandCode, customer, session);
		AvpSet avpSet = ccr.getAvps();
		avpSet.addAvp(Avp.SERVICE_CONTEXT_ID, TGFConfig.getVoiceServiceContextId(), true, false, true);
		createIMSInformation(customer, avpSet, session.getEventDate());
		createVoiceMCC(avpSet, session.getRequestType(), session.getCcTime(), customer.getOriginMSISDN(), session);
		return ccr;
	}

	private void createVoiceMCC(AvpSet avpSet, TGFRequestType requestType, int volume, String originMSISDN, DiameterSessionBean session) {
		AvpSet creditControl = avpSet.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
		createCCRMMCConstants(creditControl, originMSISDN);
		AvpSet creditControlServiceSpecificInfo6001 = creditControl.addGroupedAvp(Avp.SERVICE_SPECIFIC_INFO, TGFConstant.VENDOR_ID, true, false);
		creditControlServiceSpecificInfo6001.addAvp(Avp.SERVICE_SPECIFIC_TYPE, TGFConstant.SERVICE_INFO_SERVICE_TYPE_6001, TGFConstant.VENDOR_ID, true, false, true);
		creditControlServiceSpecificInfo6001.addAvp(Avp.SERVICE_SPECIFIC_DATA, TGFConstant.SERVICE_INFO_SERVICE_DATA_6001, TGFConstant.VENDOR_ID, true, false, true);
		int requestNumber = session.getRequestNumber();
		if (requestType == TGFRequestType.FINAL) {
			AvpSet usedServiceUnit = creditControl.addGroupedAvp(Avp.USED_SERVICE_UNIT);
			usedServiceUnit.addAvp(Avp.CC_TIME, volume);
		} else if (requestType == TGFRequestType.INTERMEDIATE) {
			AvpSet requestedServiceUnit = creditControl.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT);
			requestedServiceUnit.addAvp(Avp.CC_TIME, volume, true, false, true);
			AvpSet usedServiceUnit = creditControl.addGroupedAvp(Avp.USED_SERVICE_UNIT);
			usedServiceUnit.addAvp(Avp.CC_TIME, requestNumber * volume);
		} else {
			AvpSet requestedServiceUnit = creditControl.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT);
			requestedServiceUnit.addAvp(Avp.CC_TIME, volume, true, false, true);
		}
	}

	private void createIMSInformation(CustomerBean customer, AvpSet avpSet, Date eventDate) {
		AvpSet serviceInformation = avpSet.addGroupedAvp(Avp.SERVICE_INFORMATION, TGFConstant.VENDOR_ID, true, false);
		AvpSet imsInformation = serviceInformation.addGroupedAvp(Avp.IMS_INFORMATION, TGFConstant.VENDOR_ID, true, false);
		AvpSet eventType = imsInformation.addGroupedAvp(Avp.EVENT_TYPE, TGFConstant.VENDOR_ID, true, false);
		eventType.addAvp(Avp.EVENT, TGFConfig.getImsEventType(), TGFConstant.VENDOR_ID, true, false, true);
		imsInformation.addAvp(Avp.NODE_FUNCTIONALITY, TGFConstant.NODE_FUNCTIONALITY, TGFConstant.VENDOR_ID, true, false, true);
		imsInformation.addAvp(Avp.ROLE_OF_NODE, TGFConstant.ROLE_OF_NODE_ORIGINATING, TGFConstant.VENDOR_ID, true, false, true);
		imsInformation.addAvp(Avp.CALLING_PARTY_ADDRESS, getSipAwareMSISDN(customer.getMsisdn()), TGFConstant.VENDOR_ID, true, false, true);
		imsInformation.addAvp(Avp.CALLED_PARTY_ADDRESS, getSipAwareMSISDN(customer.getDestination()), TGFConstant.VENDOR_ID, true, false, true);
		imsInformation.addAvp(Avp.ACCESS_NETWORK_INFORMATION, TGFConfig.getImsAccessNetworkInformation(), TGFConstant.VENDOR_ID, true, false, true);
		AvpSet timeStamp = imsInformation.addGroupedAvp(Avp.TIME_STAMPS, TGFConstant.VENDOR_ID, true, false);
		timeStamp.addAvp(Avp.SIP_RESPONSE_TIMESTAMP, eventDate, TGFConstant.VENDOR_ID, true, false);
		AvpSet applicationServerInformation = imsInformation.addGroupedAvp(Avp.APPLICATION_SERVER_INFORMATION, TGFConstant.VENDOR_ID, true, false);
		applicationServerInformation.addAvp(Avp.APPLICATION_PROVIDED_CALLED_PARTY_ADDRESS, TGFConstant.SERVICE_INFO_SERVICE_DATA_6009, TGFConstant.VENDOR_ID, true, false,
				true);
		addServiceSpecificInfo(customer, imsInformation);
	}

	private void addServiceSpecificInfo(CustomerBean customer, AvpSet imsInformation) {
		AvpSet imsInformationServiceSpecificInfo6006 = imsInformation.addGroupedAvp(Avp.SERVICE_SPECIFIC_INFO, TGFConstant.VENDOR_ID, true, false);
		imsInformationServiceSpecificInfo6006.addAvp(Avp.SERVICE_SPECIFIC_TYPE, TGFConstant.SERVICE_INFO_SERVICE_TYPE_6006, TGFConstant.VENDOR_ID, true, false, true);
		imsInformationServiceSpecificInfo6006.addAvp(Avp.SERVICE_SPECIFIC_DATA, customer.getMsisdn(), TGFConstant.VENDOR_ID, true, false, true);
		AvpSet imsInformationServiceSpecificInfo6007 = imsInformation.addGroupedAvp(Avp.SERVICE_SPECIFIC_INFO, TGFConstant.VENDOR_ID, true, false);
		imsInformationServiceSpecificInfo6007.addAvp(Avp.SERVICE_SPECIFIC_TYPE, TGFConstant.SERVICE_INFO_SERVICE_TYPE_6007, TGFConstant.VENDOR_ID, true, false, true);
		imsInformationServiceSpecificInfo6007.addAvp(Avp.SERVICE_SPECIFIC_DATA, customer.getDestination(), TGFConstant.VENDOR_ID, true, false, true);
	}
}
