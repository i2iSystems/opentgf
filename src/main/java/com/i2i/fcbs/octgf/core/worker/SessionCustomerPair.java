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

import com.i2i.fcbs.octgf.bean.CustomerBean;
import com.i2i.fcbs.octgf.bean.DiameterSessionBean;

public class SessionCustomerPair {
	private CustomerBean		customerBean;
	private DiameterSessionBean	diameterSessionBean;

	public SessionCustomerPair(CustomerBean customerBean, DiameterSessionBean diameterSessionBean) {
		this.customerBean = customerBean;
		this.diameterSessionBean = diameterSessionBean;
	}

	public CustomerBean getCustomerBean() {
		return customerBean;
	}

	public DiameterSessionBean getDiameterSessionBean() {
		return diameterSessionBean;
	}
}
