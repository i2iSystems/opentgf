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

package com.i2i.fcbs.octgf.jmx;

import java.util.concurrent.atomic.AtomicLong;

public class DiameterPerformance implements DiameterPerformanceMBean {
	private AtomicLong	outPackCount		= new AtomicLong();
	private AtomicLong	outPackCountLast	= new AtomicLong();
	private AtomicLong	inPackCount			= new AtomicLong();
	private AtomicLong	inPackCountLast		= new AtomicLong();

	@Override
	public long getOutPackCount() {
		return outPackCount.get();
	}

	@Override
	public long incOutPackCount() {
		return outPackCount.incrementAndGet();
	}

	@Override
	public long getOutPackDelta() {
		long outPackCountLong = outPackCount.get();
		return outPackCountLong - outPackCountLast.getAndSet(outPackCountLong);
	}

	@Override
	public long getInPackCount() {
		return inPackCount.get();
	}

	@Override
	public long incInPackCount() {
		return inPackCount.incrementAndGet();
	}

	@Override
	public long getInPackDelta() {
		long inPackCountLong = inPackCount.get();
		return inPackCountLong - inPackCountLast.getAndSet(inPackCountLong);
	}
}
