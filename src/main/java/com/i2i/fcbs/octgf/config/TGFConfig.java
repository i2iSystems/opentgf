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

package com.i2i.fcbs.octgf.config;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.i2i.fcbs.octgf.constant.AppModeType;
import com.i2i.fcbs.octgf.constant.TGFServiceType;

public class TGFConfig {
	private static final Logger		logger					= LogManager.getLogger(TGFConfig.class);
	private static String			applicationName;
	private static Integer			applicationInstanceID;
	private static TGFServiceType	serviceToTest;
	private static String			sessionTag;
	private static String			configFilePath			= "config/tgf.conf";
	private static String			databaseFilePath		= "config/database/sqliteTgf.db";
	private static String			diameterConfigPath		= "config/client-jdiameter-config.xml";
	private static String			timeZone				= "Asia/Istanbul";
	private static int				tps;
	private static int				trafficThreadCount;
	private static int				persisterThreadCount;
	private static int				persisterBatchSize;
	private static boolean			persisterSuccessFilter	= true;
	private static int				sessionSequenceLimit;
	private static long				loadTimeInMillis;
	private static int				maxTpsTargetCycle;
	private static String			originHost;
	private static String			originRealm;
	private static String			destinationHost;
	private static String			destinationRealm;
	private static String			voiceServiceContextId;
	private static String			smsServiceContextId;
	private static String			dataServiceContextId;
	private static String			imsEventType;
	private static List<Integer>	ratingGroupList;
	private static int				serviceIdentifier;
	private static String			sgsnAddress;
	private static int				warmUpDurationInSeconds;
	private static int				warmUpCustomerCount;
	private static int				trafficGeneratorSineFunctionWaveAmplitude;
	private static int				trafficGeneratorSineFunctionWaveLength;
	private static int				statsExportPeriodInSeconds;
	private static int[]			histogramBins			= new int[] { 10, 30, 40, 50, 70, 100, 200, 500, 1000, 5000 };
	private static int				customerIdStart;
	private static Long				testSessionId;
	private static AppModeType		app;
	private static Boolean			msccLookUpEnabled;
	private static Integer			customerSite;
	private static Long				trafficSlotLengthInMillis;
	private static boolean			useSipFormattedNumbers	= false;
	private static String			sipFormattedNumberUrl;
	private static String			imsAccessNetworkInformation;
	private static boolean			addSubscriptionImsi		= true;

	private TGFConfig() {
	}

	public static String getApplicationName() {
		return applicationName;
	}

	public static void setApplicationName(String applicationName) {
		TGFConfig.applicationName = applicationName;
	}

	public static Integer getApplicationInstanceID() {
		return applicationInstanceID;
	}

	public static void setApplicationInstanceID(Integer applicationInstanceID) {
		TGFConfig.applicationInstanceID = applicationInstanceID;
	}

	public static TGFServiceType getServiceToTest() {
		return serviceToTest;
	}

	public static void setServiceToTest(TGFServiceType serviceToTest) {
		TGFConfig.serviceToTest = serviceToTest;
	}

	public static String getSessionTag() {
		return sessionTag;
	}

	public static void setSessionTag(String sessionTag) {
		TGFConfig.sessionTag = sessionTag;
	}

	public static String getConfigFilePath() {
		return configFilePath;
	}

	public static void setConfigFilePath(String configFilePath) {
		TGFConfig.configFilePath = configFilePath;
	}

	public static String getDatabaseFilePath() {
		return databaseFilePath;
	}

	public static void setDatabaseFilePath(String databaseFilePath) {
		TGFConfig.databaseFilePath = databaseFilePath;
	}

	public static String getDiameterConfigPath() {
		return diameterConfigPath;
	}

	public static void setDiameterConfigPath(String diameterConfigPath) {
		TGFConfig.diameterConfigPath = diameterConfigPath;
	}

	public static String getTimeZone() {
		return timeZone;
	}

	public static void setTimeZone(String timeZone) {
		TGFConfig.timeZone = timeZone;
	}

	public static int getTps() {
		return tps;
	}

	public static void setTps(int tps) {
		TGFConfig.tps = tps;
	}

	public static int getTrafficThreadCount() {
		return trafficThreadCount;
	}

	public static void setTrafficThreadCount(int trafficThreadCount) {
		TGFConfig.trafficThreadCount = trafficThreadCount;
	}

	public static int getPersisterThreadCount() {
		return persisterThreadCount;
	}

	public static void setPersisterThreadCount(int persisterThreadCount) {
		TGFConfig.persisterThreadCount = persisterThreadCount;
	}

	public static int getPersisterBatchSize() {
		return persisterBatchSize;
	}

	public static void setPersisterBatchSize(int persisterBatchSize) {
		TGFConfig.persisterBatchSize = persisterBatchSize;
	}

	public static boolean isPersisterSuccessFilter() {
		return persisterSuccessFilter;
	}

	public static void setPersisterSuccessFilter(boolean persisterSuccessFilter) {
		TGFConfig.persisterSuccessFilter = persisterSuccessFilter;
	}

	public static int getSessionSequenceLimit() {
		return sessionSequenceLimit;
	}

	public static void setSessionSequenceLimit(int sessionSequenceLimit) {
		TGFConfig.sessionSequenceLimit = sessionSequenceLimit;
	}

	public static long getLoadTimeInMillis() {
		return loadTimeInMillis;
	}

	public static void setLoadTimeInMillis(long loadTimeInMillis) {
		TGFConfig.loadTimeInMillis = loadTimeInMillis;
	}

	public static int getMaxTpsTargetCycle() {
		return maxTpsTargetCycle;
	}

	public static void setMaxTpsTargetCycle(int maxTpsTargetCycle) {
		TGFConfig.maxTpsTargetCycle = maxTpsTargetCycle;
	}

	public static String getOriginHost() {
		return originHost;
	}

	public static void setOriginHost(String originHost) {
		TGFConfig.originHost = originHost;
	}

	public static String getOriginRealm() {
		return originRealm;
	}

	public static void setOriginRealm(String originRealm) {
		TGFConfig.originRealm = originRealm;
	}

	public static String getDestinationHost() {
		return destinationHost;
	}

	public static void setDestinationHost(String destinationHost) {
		TGFConfig.destinationHost = destinationHost;
	}

	public static String getDestinationRealm() {
		return destinationRealm;
	}

	public static void setDestinationRealm(String destinationRealm) {
		TGFConfig.destinationRealm = destinationRealm;
	}

	public static String getVoiceServiceContextId() {
		return voiceServiceContextId;
	}

	public static void setVoiceServiceContextId(String voiceServiceContextId) {
		TGFConfig.voiceServiceContextId = voiceServiceContextId;
	}

	public static String getSmsServiceContextId() {
		return smsServiceContextId;
	}

	public static void setSmsServiceContextId(String smsServiceContextId) {
		TGFConfig.smsServiceContextId = smsServiceContextId;
	}

	public static String getDataServiceContextId() {
		return dataServiceContextId;
	}

	public static void setDataServiceContextId(String dataServiceContextId) {
		TGFConfig.dataServiceContextId = dataServiceContextId;
	}

	public static String getImsEventType() {
		return imsEventType;
	}

	public static void setImsEventType(String imsEventType) {
		TGFConfig.imsEventType = imsEventType;
	}

	public static List<Integer> getRatingGroupList() {
		return ratingGroupList;
	}

	public static void setRatingGroupList(List<Integer> ratingGroupList) {
		TGFConfig.ratingGroupList = ratingGroupList;
	}

	public static int getServiceIdentifier() {
		return serviceIdentifier;
	}

	public static void setServiceIdentifier(int serviceIdentifier) {
		TGFConfig.serviceIdentifier = serviceIdentifier;
	}

	public static String getSgsnAddress() {
		return sgsnAddress;
	}

	public static void setSgsnAddress(String sgsnAddress) {
		TGFConfig.sgsnAddress = sgsnAddress;
	}

	public static int getWarmUpDurationInSeconds() {
		return warmUpDurationInSeconds;
	}

	public static void setWarmUpDurationInSeconds(int warmUpDurationInSeconds) {
		TGFConfig.warmUpDurationInSeconds = warmUpDurationInSeconds;
	}

	public static int getWarmUpCustomerCount() {
		return warmUpCustomerCount;
	}

	public static void setWarmUpCustomerCount(int warmUpCustomerCount) {
		TGFConfig.warmUpCustomerCount = warmUpCustomerCount;
	}

	public static int getStatsExportPeriodInSeconds() {
		return statsExportPeriodInSeconds;
	}

	public static void setStatsExportPeriodInSeconds(int statsExportPeriodInSeconds) {
		TGFConfig.statsExportPeriodInSeconds = statsExportPeriodInSeconds;
	}

	public static int getCustomerIdStart() {
		return customerIdStart;
	}

	public static void setCustomerIdStart(int customerIdStart) {
		TGFConfig.customerIdStart = customerIdStart;
	}

	public static Long getTestSessionId() {
		return testSessionId;
	}

	public static void setTestSessionId(Long testSessionId) {
		TGFConfig.testSessionId = testSessionId;
	}

	public static void setMsccLookUpEnabled(Boolean msccLookUpEnabled) {
		TGFConfig.msccLookUpEnabled = msccLookUpEnabled;
	}

	public static Boolean getMsccLookUpEnabled() {
		return msccLookUpEnabled;
	}

	public static void setCustomerSite(Integer site) {
		TGFConfig.customerSite = site;
	}

	public static Integer getCustomerSite() {
		return customerSite;
	}

	public static int getTrafficGeneratorSineFunctionWaveAmplitude() {
		return trafficGeneratorSineFunctionWaveAmplitude;
	}

	public static void setTrafficGeneratorSineFunctionWaveAmplitude(int trafficGeneratorSineFunctionWaveAmplitude) {
		TGFConfig.trafficGeneratorSineFunctionWaveAmplitude = trafficGeneratorSineFunctionWaveAmplitude;
	}

	public static int getTrafficGeneratorSineFunctionWaveLength() {
		return trafficGeneratorSineFunctionWaveLength;
	}

	public static void setTrafficGeneratorSineFunctionWaveLength(int trafficGeneratorSineFunctionWaveLength) {
		TGFConfig.trafficGeneratorSineFunctionWaveLength = trafficGeneratorSineFunctionWaveLength;
	}

	public static Long getTrafficSlotLengthInMillis() {
		return trafficSlotLengthInMillis;
	}

	public static void setTrafficSlotLengthInMillis(Long trafficSlotLengthInMillis) {
		if (trafficSlotLengthInMillis % 1000 != 0) {
			logger.warn("[setTrafficSlotLengthInMillis()] [Slot length must be divisible to 1000 ]");
		}
		TGFConfig.trafficSlotLengthInMillis = trafficSlotLengthInMillis;
	}

	public static AppModeType getApp() {
		return app;
	}

	public static void setApp(AppModeType app) {
		TGFConfig.app = app;
	}

	public static int[] getHistogramBins() {
		return histogramBins;
	}

	public static void setHistogramBins(int[] histogramBins) {
		TGFConfig.histogramBins = histogramBins;
	}

	public static boolean isUseSipFormattedNumbers() {
		return useSipFormattedNumbers;
	}

	public static void setUseSipFormattedNumbers(boolean useSipFormattedNumbers) {
		TGFConfig.useSipFormattedNumbers = useSipFormattedNumbers;
	}

	public static String getSipFormattedNumberUrl() {
		return sipFormattedNumberUrl;
	}

	public static void setSipFormattedNumberUrl(String sipFormattedNumberUrl) {
		TGFConfig.sipFormattedNumberUrl = sipFormattedNumberUrl;
	}

	public static boolean isAddSubscriptionImsi() {
		return addSubscriptionImsi;
	}

	public static void setAddSubscriptionImsi(boolean addSubscriptionImsi) {
		TGFConfig.addSubscriptionImsi = addSubscriptionImsi;
	}

	public static String getImsAccessNetworkInformation() {
		return imsAccessNetworkInformation;
	}

	public static void setImsAccessNetworkInformation(String imsAccessNetworkInformation) {
		TGFConfig.imsAccessNetworkInformation = imsAccessNetworkInformation;
	}
}
