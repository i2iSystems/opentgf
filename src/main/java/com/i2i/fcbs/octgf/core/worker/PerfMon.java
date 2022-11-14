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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.jmx.DiameterPerformanceMBean;
import com.i2i.fcbs.octgf.jmx.JmxFacade;

public class PerfMon {
	private static final Logger			logger				= LogManager.getLogger(PerfMon.class);
	private static final Logger			perfLogger			= LogManager.getLogger("TGF_STATS");
	private final AtomicLong			totalRequest		= new AtomicLong(0);
	private final AtomicLong			totalExpired		= new AtomicLong(0);
	private final AtomicLong			totalSuccess		= new AtomicLong(0);
	private final AtomicLong			totalFail			= new AtomicLong(0);
	private final AtomicInteger			periodCounter		= new AtomicInteger(0);
	private final AtomicLong			delayBinNumerator	= new AtomicLong(0);
	private final AtomicLong			delayBinDenominator	= new AtomicLong(0);
	private final AtomicInteger			delayMin			= new AtomicInteger(Integer.MAX_VALUE);
	private final AtomicInteger			delayMax			= new AtomicInteger(0);
	private final AtomicLong			totalSkipped		= new AtomicLong(0);
	private final List<CodeCounterPair>	failCodes			= new CopyOnWriteArrayList<>();
	private final NumberFormat			percentageFormatter	= new DecimalFormat("#0.00");
	private final AtomicInteger			tmpDelayMin			= new AtomicInteger(Integer.MAX_VALUE);
	private final AtomicInteger			tmpDelayMax			= new AtomicInteger(0);
	private final AtomicInteger			fitCustomers		= new AtomicInteger(0);
	private long						interval			= TimeUnit.SECONDS.toMillis(TGFConfig.getStatsExportPeriodInSeconds());
	private AtomicInteger				workers				= new AtomicInteger(0);
	private static PerfMon				instance;
	private long						startTime;
	private int[]						histogramBins		= TGFConfig.getHistogramBins();
	private AtomicLong[]				histogram			= IntStream.range(0, histogramBins.length + 1).mapToObj(i -> new AtomicLong()).toArray(AtomicLong[]::new);

	private PerfMon() {
		// dummy initial values
		failCodes.add(new CodeCounterPair(0L));
		failCodes.add(new CodeCounterPair(0L));
		failCodes.add(new CodeCounterPair(0L));
	}

	public static synchronized PerfMon getInstance() {
		if (instance == null)
			instance = new PerfMon();
		return instance;
	}

	void start() {
		if (workers.getAndIncrement() > 0)
			return;
		new Thread(this::run, "PerfMon").start();
	}

	void stop() {
		if (workers.get() > 0) {
			workers.decrementAndGet();
		}
	}

	void incReq() {
		totalRequest.incrementAndGet();
	}

	void incResp(boolean success, long code) {
		if (success)
			totalSuccess.incrementAndGet();
		else {
			totalFail.incrementAndGet();
			Optional<CodeCounterPair> pair = failCodes.stream().filter(p -> p.code == code).findFirst();
			CodeCounterPair pairToWorkOn = pair.orElseGet(() -> new CodeCounterPair(code));
			pairToWorkOn.counter.incrementAndGet();
			if (!pair.isPresent())
				failCodes.add(pairToWorkOn);
		}
	}

	void addFitCustomers(int fitCustomerCount) {
		fitCustomers.addAndGet(fitCustomerCount);
	}

	void addDelay(int delay) {
		if (delayMin.get() > delay)
			delayMin.set(delay);
		if (delayMax.get() < delay)
			delayMax.set(delay);
		if (tmpDelayMin.get() > delay)
			tmpDelayMin.set(delay);
		if (tmpDelayMax.get() < delay)
			tmpDelayMax.set(delay);
		delayBinNumerator.addAndGet(delay);
		delayBinDenominator.incrementAndGet();
		int h = 0;
		while (h < histogramBins.length && histogramBins[h] < delay)
			h++;
		histogram[h].incrementAndGet();
	}

	private void run() {
		double averageTps;
		double averageResponse;
		this.startTime = System.currentTimeMillis();
		long lastRun = System.currentTimeMillis();
		while (workers.get() > 0) {
			long lastRequestCount = totalRequest.get();
			long lastExpCount = totalExpired.get();
			long lastSuccessCount = totalSuccess.get();
			long lastFailCount = totalFail.get();
			long lastTotalResponse = lastFailCount + lastSuccessCount;
			long lastDelayNominator = delayBinNumerator.get();
			long lastDelayDenominator = delayBinDenominator.get();
			long lastSkipped = this.totalSkipped.get();
			long lastDiameterIngressCount = 0;
			long lastDiameterOutgressCount = 0;
			Optional<DiameterPerformanceMBean> optionalMxBean = JmxFacade.getPerformanceBean();
			if (optionalMxBean.isPresent()) {
				DiameterPerformanceMBean bean = optionalMxBean.get();
				lastDiameterIngressCount = bean.getInPackCount();
				lastDiameterOutgressCount = bean.getOutPackCount();
			}
			if (sleepForInterval())
				break;
			// snapshot variables
			long currentTotalRequest = totalRequest.get();
			long currentTotalSuccessCount = totalSuccess.get();
			long currentTotalFailCount = totalFail.get();
			long currentTotalResponse = currentTotalFailCount + currentTotalSuccessCount;
			long currentTotalExpired = totalExpired.get();
			long currentDelayNominator = delayBinNumerator.get();
			long currentDelayDenominator = delayBinDenominator.get();
			long currentSkipped = this.totalSkipped.get();
			long currentDiameterIngressCount = 0;
			long currentDiameterOutgressCount = 0;
			if (optionalMxBean.isPresent()) {
				DiameterPerformanceMBean bean = optionalMxBean.get();
				currentDiameterIngressCount = bean.getInPackCount();
				currentDiameterOutgressCount = bean.getOutPackCount();
			}
			int currentTmpDelayMin = tmpDelayMin.getAndSet(Integer.MAX_VALUE);
			int currentTmpDelayMax = tmpDelayMax.getAndSet(0);
			int periodCount = periodCounter.incrementAndGet();
			// do not allow modification
			int intervalRequestCount = (int) (currentTotalRequest - lastRequestCount);
			int intervalResponseCount = (int) (currentTotalResponse - lastTotalResponse);
			int intervalExpiredCount = (int) (currentTotalExpired - lastExpCount);
			int intervalFailCount = (int) (currentTotalFailCount - lastFailCount);
			int intervalSuccessCount = (int) (currentTotalSuccessCount - lastSuccessCount);
			long timeDiffStart = System.currentTimeMillis() - startTime;
			long timeDiffLstRun = System.currentTimeMillis() - lastRun;
			averageTps = 1.0 * currentTotalRequest / timeDiffStart;
			averageTps = 1000 * averageTps;//TO per second
			averageResponse = 1.0 * currentTotalResponse / timeDiffStart;
			averageResponse = 1000 * averageResponse; //TO per second
			printProgress(intervalRequestCount, intervalResponseCount, averageTps, averageResponse, timeDiffLstRun, periodCount);
			if (perfLogger.isErrorEnabled()) {
				perfLogger.error("AVRG - CCR  {} CCA {}", String.format("%15.2f", averageTps), String.format("%15.2f", averageResponse));
				perfLogger.error("LAST - CCR  {}    CCA {}    PERD {} mil, DIFF {}, SKIP {}", lpad(intervalRequestCount, 12), lpad(intervalResponseCount, 12),
						lpad(timeDiffLstRun, 7), lpad(intervalRequestCount - intervalResponseCount, 7), lpad(currentSkipped - lastSkipped, 7));
				perfLogger.error("TOTL - CCR  {}    CCA {}    DRTN {} sec, DIFF {}, SKIP {}", lpad(currentTotalRequest, 12), lpad(currentTotalResponse, 12),
						lpad(millisToSeconds(timeDiffStart), 7), lpad(currentTotalRequest - currentTotalResponse, 7), lpad(currentSkipped, 7));
				perfLogger.error("LAST - SUCC {}    ERR {}    EXPR {}", lpad(intervalSuccessCount, 12), lpad(intervalFailCount, 12), lpad(intervalExpiredCount, 12));
				perfLogger.error("TOTL - SUCC {}    ERR {}    EXPR {}", lpad(currentTotalSuccessCount, 12), lpad(currentTotalFailCount, 12), lpad(currentTotalExpired, 12));
				if (JmxFacade.getPerformanceBean().isPresent()) {
					perfLogger.error("L-DI - CCR  {}    CCA {}", lpad(currentDiameterOutgressCount - lastDiameterOutgressCount, 12),
							lpad(currentDiameterIngressCount - lastDiameterIngressCount, 12));
					perfLogger.error("T-DI - CCR  {}    CCA {}", lpad(currentDiameterOutgressCount, 12), lpad(currentDiameterIngressCount, 12));
				}
				Stats lastIntervalStats = new Stats(1.0 * (currentDelayNominator - lastDelayNominator) / (currentDelayDenominator - lastDelayDenominator), currentTmpDelayMin,
						currentTmpDelayMax);
				Stats wholIntervalStats = new Stats(1.0 * currentDelayNominator / currentDelayDenominator, delayMin.get(), delayMax.get());
				perfLogger.error("Last Resp Time {}", lastIntervalStats);
				perfLogger.error("Whol Resp Time {}", wholIntervalStats);
				perfLogger.error("Hist Ranges {}", Arrays.stream(histogramBins).mapToObj(i -> String.format("%7d", i)).collect(Collectors.joining(",", "[", "]")));
				perfLogger.error("Hist Values {}",
						Arrays.stream(histogram).mapToLong(AtomicLong::get).mapToObj(i -> String.format("%7d", i)).collect(Collectors.joining(",", "[", "]")));
				long sum = Arrays.stream(histogram).mapToLong(AtomicLong::get).sum();
				failCodes.sort((c1, c2) -> (int) (c2.counter.get() - c1.counter.get()));
				perfLogger.error("TOTL - ERR - {}-{} {}-{} {}-{} | Top-1 {}, Top-2 {}, Top-3 {}",
						failCodes.get(0).code, failCodes.get(0).counter.get(),
						failCodes.get(1).code, failCodes.get(1).counter.get(),
						failCodes.get(2).code, failCodes.get(2).counter.get(),
						percentageFormatter.format((histogram[0].get()) * 100.0 / sum),
						percentageFormatter.format((histogram[0].get() + histogram[1].get()) * 100.0 / sum),
						percentageFormatter.format((histogram[0].get() + histogram[1].get() + histogram[2].get()) * 100.0 / sum));
				perfLogger.error("---");
			}
			// updates
			lastRun = System.currentTimeMillis();
		}
		try {
			logger.info("[PerfMon] Waiting 1 minutes to let requests completed.");
			Thread.sleep(TimeUnit.MINUTES.toMillis(1));
		} catch (InterruptedException e) {
			logger.debug("[PerfMon] Thread wait interrupted... [OK]");
			Thread.currentThread().interrupt();
		}
		perfLogger.error("GRAND TOTL - CCR {}; CCA {};", totalRequest.get(), totalSuccess.get() + totalFail.get());
		perfLogger.error("GRAND TOTL - SUCC {}; ERR {}; EXPR {};", totalSuccess.get(), totalFail.get(), totalExpired.get());
		totalRequest.set(0);
		totalExpired.set(0);
		totalFail.set(0);
		totalSuccess.set(0);
	}

	private long millisToSeconds(long timeDiffStart) {
		return timeDiffStart / 1000;
	}

	private boolean sleepForInterval() {
		try {
			Thread.sleep(interval);
		} catch (InterruptedException e) {
			logger.debug("[PerfMon] Thread interrupted... [OK]");
			Thread.currentThread().interrupt();
			return true;
		}
		return false;
	}

	private static String lpad(Long num, Integer length) {
		return padLeft(String.valueOf(num), length);
	}

	private static String lpad(Integer num, Integer length) {
		return padLeft(String.valueOf(num), length);
	}

	private static String padLeft(String s, int n) {
		String formatString = "%" + n + "s";
		return String.format(formatString, s);
	}

	private static final String[] chars = new String[] { "|", "/", "-", "\\" };

	private void printProgress(int tps, int respTps, double avgTps, double respAvgTps, long timeDiffLstRun, int periodCount) {
		String stringBuilder = chars[periodCount % chars.length] + " Stats " +
				" " + "tps=" + tps +
				" " + "avgtps=" + String.format("%.2f", avgTps) +
				" " + "resp=" + respTps +
				" " + "avgresp=" + String.format("%.2f", respAvgTps) +
				" " + "prd=" + timeDiffLstRun + "mls" +
				"\r";
		System.out.print(stringBuilder);
	}

	void incExpired() {
		totalExpired.incrementAndGet();
	}

	void addSkipped() {
		totalSkipped.incrementAndGet();
	}

	private static class Stats {
		private Double	avg;
		private Integer	min;
		private Integer	max;

		private Stats(Double avg, Integer min, Integer max) {
			this.avg = avg;
			this.min = min;
			this.max = max;
		}

		@Override
		public String toString() {
			Integer tmpMin = this.min;
			if (avg.isNaN())
				tmpMin = 0;
			return "Stats {" +
					"avg=" + String.format("%9.2f", avg) +
					", min=" + String.format("%3d", tmpMin) +
					", max=" + String.format("%6d", max) +
					'}';
		}
	}

	private static class CodeCounterPair {
		private Long		code;
		private AtomicLong	counter	= new AtomicLong();

		private CodeCounterPair(Long code) {
			this.code = code;
		}
	}

	public long getTotalRequest() {
		return totalRequest.get();
	}

	public long getTotalExpired() {
		return totalExpired.get();
	}

	public long getTotalSuccess() {
		return totalSuccess.get();
	}

	public long getTotalFail() {
		return totalFail.get();
	}

	public int getPeriodCounter() {
		return periodCounter.get();
	}

	public int getDelayMin() {
		return delayMin.get();
	}

	public int getDelayMax() {
		return delayMax.get();
	}

	public long getTotalSkipped() {
		return totalSkipped.get();
	}

	public long getDelayBinNumerator() {
		return delayBinNumerator.get();
	}

	public long getDelayBinDenominator() {
		return delayBinDenominator.get();
	}

	public long getStartTime() {
		return startTime;
	}

	public static class NullPerfMon extends PerfMon {
		@Override
		void start() {
			logger.trace("[start()]");
		}

		@Override
		void stop() {
			logger.trace("[stop()]");
		}
	}
}
