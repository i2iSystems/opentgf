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

import static com.i2i.fcbs.octgf.core.worker.AbstractTrafficWorker.SMS_DISCRIMINATOR;

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpSet;
import org.jdiameter.client.api.IRequest;

import com.i2i.fcbs.octgf.bean.CustomerBean;
import com.i2i.fcbs.octgf.bean.DiameterSessionBean;
import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.constant.TGFConstant;
import com.i2i.fcbs.octgf.core.worker.model.TGFRequestType;

public final class SMSCCRComposer implements DiameterRequestComposer {
	@Override
	public IRequest compose(CustomerBean customer, DiameterSessionBean session) {
		// override request type for sms
		session.setRequestTypeValue(TGFConstant.EVENT_REQUEST);
		session.setRequestType(TGFRequestType.EVENT);
		// seessionId+ppp+timestamp+request_number
		// remove part starting with ppp and add a new part
		String sessionId = session.getSessionID();
		if (sessionId.contains(SMS_DISCRIMINATOR))
			sessionId = sessionId.split(SMS_DISCRIMINATOR)[0];
		sessionId = sessionId + SMS_DISCRIMINATOR + System.currentTimeMillis() + "_" + session.getRequestNumber();
		session.setSessionID(sessionId);
		IRequest requst = composeRequestBody(TGFConstant.ccAppId, TGFConstant.ccrCommandCode, customer, session);
		AvpSet avpSet = requst.getAvps();
		avpSet.addAvp(Avp.SERVICE_CONTEXT_ID, TGFConfig.getSmsServiceContextId(), true, false, true);
		createIMSInformation(customer, avpSet);
		createVoiceMSCC(session.getCcTime(), avpSet);
		return requst;
	}

	private static void createIMSInformation(CustomerBean customerBean, AvpSet avpSet) {
		AvpSet serviceInformation = avpSet.addGroupedAvp(Avp.SERVICE_INFORMATION, TGFConstant.VENDOR_ID, true, false);
		AvpSet smsInformation = serviceInformation.addGroupedAvp(Avp.SMS_INFORMATION, TGFConstant.VENDOR_ID, true, false);
		smsInformation.addAvp(Avp.SMSC_ADDRESS, encode(8, customerBean.getOriginMSISDN().getBytes()), true, false);
		smsInformation.addAvp(Avp.ORIGINATOR_SCCP_ADDRESS, encode(8, customerBean.getOriginMSISDN().getBytes()), true, false);
		AvpSet recepientInfos = smsInformation.addGroupedAvp(Avp.RECIPIENT_INFO, TGFConstant.VENDOR_ID, true, false);
		AvpSet recepientAddress = recepientInfos.addGroupedAvp(Avp.RECIPIENT_ADDRESS);
		recepientAddress.addAvp(Avp.ADDRESS_TYPE, customerBean.getMsisdn(), true, false, true);
		recepientAddress.addAvp(Avp.ADDRESS_DATA, customerBean.getDestination(), TGFConstant.VENDOR_ID, true, false, true);
		recepientAddress.addAvp(Avp.ADDRESSEE_TYPE, 0, TGFConstant.VENDOR_ID, true, false, true);
	}

	private static void createVoiceMSCC(int volume, AvpSet avpSet) {
		AvpSet creditControl = avpSet.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
		AvpSet requestedServiceUnit = creditControl.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT);
		requestedServiceUnit.addAvp(Avp.CC_SERVICE_SPECIFIC_UNITS, volume, true, false, false);
	}

	public static byte[] encode(int address, byte[] addressByte) {
		byte[] encodedAddress = new byte[addressByte.length + 2];
		encodedAddress[0] = (byte) (address >> 8);
		encodedAddress[1] = (byte) address;
		System.arraycopy(addressByte, 0, encodedAddress, 2, addressByte.length);
		return encodedAddress;
	}
}
