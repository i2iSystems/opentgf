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

import org.jdiameter.api.Avp;
import org.jdiameter.api.AvpSet;
import org.jdiameter.client.api.IRequest;

import com.i2i.fcbs.octgf.bean.CustomerBean;
import com.i2i.fcbs.octgf.bean.DiameterSessionBean;
import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.constant.TGFConstant;
import com.i2i.fcbs.octgf.core.worker.model.TGFRequestType;

public class DataCCRComposer implements DiameterRequestComposer {
	@Override
	public IRequest compose(CustomerBean customer, DiameterSessionBean session) throws Exception {
		IRequest requst = composeRequestBody(TGFConstant.ccAppId, TGFConstant.ccrCommandCode, customer, session);
		AvpSet avpSet = requst.getAvps();
		avpSet.addAvp(Avp.SERVICE_CONTEXT_ID, TGFConfig.getDataServiceContextId(), true, false, true);
		createDataInformation(avpSet);
		for (Integer ratingGroup : TGFConfig.getRatingGroupList())
			createDataMSCC(session, avpSet, ratingGroup);
		return requst;
	}

	private static void createDataInformation(AvpSet avpSet) {
		AvpSet serviceInformation = avpSet.addGroupedAvp(Avp.SERVICE_INFORMATION, TGFConstant.VENDOR_ID, true, false);
		AvpSet psInformation = serviceInformation.addGroupedAvp(Avp.PS_INFORMATION, TGFConstant.VENDOR_ID, true, false);
		psInformation.addAvp(Avp.GPP_SGSN_MCC_MNC, TGFConfig.getSgsnAddress(), TGFConstant.VENDOR_ID, true, false, true);
	}

	private static void createDataMSCC(DiameterSessionBean session, AvpSet avpSet, int ratingGroup) {
		AvpSet creditControl = avpSet.addGroupedAvp(Avp.MULTIPLE_SERVICES_CREDIT_CONTROL);
		creditControl.addAvp(Avp.RATING_GROUP, ratingGroup, TGFConstant.VENDOR_ID, true, false, true);
		creditControl.addAvp(Avp.SERVICE_IDENTIFIER_CCA, TGFConfig.getServiceIdentifier(), true, false, true);
		TGFRequestType requestType = session.getRequestType();
		if (requestType == TGFRequestType.FINAL) {
			AvpSet usedServiceUnit = creditControl.addGroupedAvp(Avp.USED_SERVICE_UNIT);
			usedServiceUnit.addAvp(Avp.CC_TOTAL_OCTETS, session.getCcTime(), TGFConstant.VENDOR_ID, true, false, false);
		} else if (requestType == TGFRequestType.INTERMEDIATE) {
			AvpSet requestedServiceUnit = creditControl.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT);
			requestedServiceUnit.addAvp(Avp.CC_TOTAL_OCTETS, session.getCcTime(), TGFConstant.VENDOR_ID, true, false, false);
			AvpSet usedServiceUnit = creditControl.addGroupedAvp(Avp.USED_SERVICE_UNIT);
			usedServiceUnit.addAvp(Avp.CC_TOTAL_OCTETS, session.getCcTime(), TGFConstant.VENDOR_ID, true, false, false);
		} else {
			AvpSet requestedServiceUnit = creditControl.addGroupedAvp(Avp.REQUESTED_SERVICE_UNIT);
			requestedServiceUnit.addAvp(Avp.CC_TOTAL_OCTETS, session.getCcTime(), TGFConstant.VENDOR_ID, true, false, false);
		}
	}
}