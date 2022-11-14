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

package com.i2i.fcbs.octgf.constant;

public class TGFConstant {
	private TGFConstant() {
	}

	public static final long	ccAppId							= 4;
	// definition of codes, IDs
	public static final int		ccrCommandCode					= 272;
	public static final int		VENDOR_ID						= 10415;
	// enum values for Exchange-Type AVP
	public static final int		INITIAL_REQUEST					= 1;
	public static final int		EVENT_REQUEST					= 4;
	public static final int		END_USER_E164					= 0;
	public static final int		END_USER_IMSI					= 1;
	public static final int		END_USER_SIP_URI				= 2;
	public static final long	SERVICE_INFO_SERVICE_TYPE_6001	= 6001;
	public static final long	SERVICE_INFO_SERVICE_TYPE_6006	= 6006;
	public static final long	SERVICE_INFO_SERVICE_TYPE_6007	= 6007;
	public static final long	SERVICE_INFO_SERVICE_TYPE_6009	= 6009;
	public static final String	SERVICE_INFO_SERVICE_DATA_6001	= "414486495312";
	public static final String	SERVICE_INFO_SERVICE_DATA_6009	= "994607123456";
	public static final long	SERVIVE_IDENTIFIER				= 26;
	public static final int		NODE_FUNCTIONALITY				= 0;
	public static final int		ROLE_OF_NODE_ORIGINATING		= 0;
}
