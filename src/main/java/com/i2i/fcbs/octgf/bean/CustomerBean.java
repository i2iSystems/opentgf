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

public class CustomerBean {
	private String	msisdn;
	private String	imsi;
	private String	destination;
	private String	originMSISDN;

	/* Constructor(s)*/
	public CustomerBean(String msisdn, String imsi, String destination, String originMSISDN) {
		super();
		this.msisdn = msisdn;
		this.imsi = imsi;
		this.destination = destination;
		this.originMSISDN = originMSISDN;
	}

	public String getMsisdn() {
		return msisdn;
	}

	public String getImsi() {
		return imsi;
	}

	public String getDestination() {
		return destination;
	}

	public String getOriginMSISDN() {
		return originMSISDN;
	}
}