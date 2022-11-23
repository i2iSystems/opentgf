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

package com.i2i.fcbs.octgf.constant;

import java.util.HashMap;
import java.util.Map;

public enum TGFServiceType {
	VOICE(1),
	SMS(2),
	DATA(3);

	private Integer								value;
	private static Map<Integer, TGFServiceType>	map	= new HashMap<>(5);
	static {
		for (TGFServiceType _enum : TGFServiceType.values()) {
			map.put(_enum.value, _enum);
		}
	}

	TGFServiceType(Integer srvc) {
		this.value = srvc;
	}

	public static TGFServiceType valueOf(Integer value) {
		TGFServiceType tgfServiceType = map.get(value);
		if (tgfServiceType == null)
			throw new IllegalArgumentException("No such TGFServiceType for value:" + value);
		return tgfServiceType;
	}

	public Integer getValue() {
		return value;
	}
}
