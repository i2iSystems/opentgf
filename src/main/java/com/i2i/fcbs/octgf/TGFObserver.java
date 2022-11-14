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

package com.i2i.fcbs.octgf;

import java.util.concurrent.atomic.AtomicBoolean;

public class TGFObserver {
	private static AtomicBoolean	shutdownSignal		= new AtomicBoolean(false);
	private static boolean			tgfTrafficCompleted	= false;

	private TGFObserver() {
	}

	public static AtomicBoolean getShutdownSignal() {
		return shutdownSignal;
	}

	public static boolean isTgfTrafficCompleted() {
		return tgfTrafficCompleted;
	}

	public static void setTgfTrafficCompleted(boolean tgfTrafficCompleted) {
		TGFObserver.tgfTrafficCompleted = tgfTrafficCompleted;
	}
}
