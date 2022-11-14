package com.i2i.fcbs.octgf.core.traffic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

class TrafficRateHelperTest {

	@org.junit.jupiter.api.Test
	void calculateSinusoidalRatePartitions() {
		int baseRate = 50;
		int amplitude = 25;
		int length = 50;
		int sessionCount = 10025;
		SinusoidalRateFunction rateFunction = new SinusoidalRateFunction(baseRate, amplitude, length);

		List<Integer> rates = TrafficRateHelper.calculateRatePartitions(rateFunction, sessionCount);

		assertEquals(sessionCount, rates.stream().mapToInt(p -> p).sum());
		assertTrue(baseRate + amplitude >= rates.stream().mapToInt(p -> p).max().orElse(-1));
		assertTrue(baseRate - amplitude <= rates.subList(0, rates.size() - 1).stream().mapToInt(p -> p).min().orElse(-1));
	}

	@org.junit.jupiter.api.Test
	void calculateLinearRatePartitions() {
		int baseRate = 50;
		int sessionCount = 1000;

		List<Integer> rates = TrafficRateHelper.calculateRatePartitions(new LinearRateFunction(baseRate), sessionCount);
		assertEquals(sessionCount, rates.stream().mapToInt(p -> p).sum());
		rates.forEach(rate -> assertEquals(baseRate, rate));

		rates = TrafficRateHelper.calculateRatePartitions(new LinearRateFunction(sessionCount), sessionCount);
		assertEquals(sessionCount, rates.stream().mapToInt(p -> p).sum());
		assertEquals(1, rates.size());
		rates.forEach(rate -> assertEquals(sessionCount, rate));

		int nonDividingBaseRate = 41;
		rates = TrafficRateHelper.calculateRatePartitions(new LinearRateFunction(nonDividingBaseRate), sessionCount);

		assertEquals(sessionCount, rates.stream().mapToInt(p -> p).sum());
		assertEquals(sessionCount % nonDividingBaseRate, rates.get(rates.size() - 1));
		rates.subList(0, rates.size() - 1).forEach(rate -> assertEquals(nonDividingBaseRate, rate));

	}

}