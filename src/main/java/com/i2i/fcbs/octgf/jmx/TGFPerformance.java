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

public class TGFPerformance implements TGFPerformanceMBean {
	private AtomicLong	outPackCount		= new AtomicLong();
	private AtomicLong	outPackCountLast	= new AtomicLong();
	private AtomicLong	inPackCount			= new AtomicLong();
	private AtomicLong	inPackCountLast		= new AtomicLong();
	private AtomicLong	inPackSuccCount		= new AtomicLong();
	private AtomicLong	inPackSuccCountLast	= new AtomicLong();
	private AtomicLong	inPackFailCount		= new AtomicLong();
	private AtomicLong	inPackFailCountLast	= new AtomicLong();
	private AtomicLong	inPackExprCount		= new AtomicLong();
	private AtomicLong	inPackExprCountLast	= new AtomicLong();

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

	@Override
	public long getInSuccCount() {
		return inPackSuccCount.get();
	}

	@Override
	public long getInSuccDelta() {
		long inPackSuccCountLong = inPackSuccCount.get();
		return inPackSuccCountLong - inPackSuccCountLast.getAndSet(inPackSuccCountLong);
	}

	@Override
	public long incInSuccCount() {
		return inPackSuccCount.incrementAndGet();
	}

	@Override
	public long getInFailCount() {
		return inPackFailCount.get();
	}

	@Override
	public long getInFailDelta() {
		long inFailCountLast = inPackFailCount.get();
		return inFailCountLast - inPackFailCountLast.getAndSet(inFailCountLast);
	}

	@Override
	public long incInFailCount() {
		return inPackFailCount.incrementAndGet();
	}

	@Override
	public long getInExprCount() {
		return inPackExprCount.get();
	}

	@Override
	public long getInExprDelta() {
		long inExprCountLast = inPackExprCount.get();
		return inExprCountLast - inPackExprCountLast.getAndSet(inExprCountLast);
	}

	@Override
	public long incInExprCount() {
		return inPackExprCount.incrementAndGet();
	}
}
