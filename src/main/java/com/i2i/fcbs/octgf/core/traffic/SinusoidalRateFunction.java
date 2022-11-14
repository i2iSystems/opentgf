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

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author habanoz
 */
public class SinusoidalRateFunction implements TrafficRateFunction {
	private final int			baseRate;
	private final int			waveAmplitude;
	private final int			waveLength;
	private final AtomicLong	time	= new AtomicLong();

	public SinusoidalRateFunction(int baseRate, int waveAmplitude, int waveLength) {
		this.baseRate = baseRate;
		this.waveAmplitude = waveAmplitude;
		this.waveLength = waveLength;
		if (baseRate - waveAmplitude <= 0) {
			throw new IllegalArgumentException(
					"Amplitude is too big. Sine wave bottom level must be greater than zero. baseRate=" + baseRate + ", waveAmplitude=" + waveAmplitude);
		}
	}

	@Override
	public int next() {
		return (int) Math.ceil(waveAmplitude * Math.sin(2.0 * time.incrementAndGet() * Math.PI / waveLength) + baseRate);
	}

	@Override
	public int getWavelength() {
		return waveLength;
	}

	@Override
	public String toString() {
		return "SinusoidalRateFunction{" +
				"baseRate=" + baseRate +
				", waveAmplitude=" + waveAmplitude +
				", waveLength=" + waveLength +
				'}';
	}
}
