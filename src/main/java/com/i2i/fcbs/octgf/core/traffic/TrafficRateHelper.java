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

package com.i2i.fcbs.octgf.core.traffic;

import java.util.ArrayList;
import java.util.List;

public class TrafficRateHelper {
	private TrafficRateHelper() {
	}

	public static List<Integer> calculateRatePartitions(TrafficRateFunction rateFunction, int sessionCount) {
		List<Integer> rates = new ArrayList<>();
		while (sessionCount > 0) {
			int rate = Math.min(rateFunction.next(), sessionCount);
			rates.add(rate);
			sessionCount -= rate;
		}
		return rates;
	}
}
