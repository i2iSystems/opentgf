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

package com.i2i.fcbs.octgf.config.parser;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.core.config.Configurator;

import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.data.DatabaseConnector;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class TGFConfigParser {
	private TGFConfigParser() {
	}

	public static void parse() {
		Config configuration = ConfigFactory.parseFile(new File(TGFConfig.getConfigFilePath())).resolve();
		Configurator.initialize(null, configuration.getString("logging.configuration.file"));
		TGFConfig.setApplicationName(configuration.getString("application.name"));
		TGFConfig.setOriginHost(configuration.getString("application.originHost"));
		TGFConfig.setOriginRealm(configuration.getString("application.originRealm"));
		TGFConfig.setDestinationHost(configuration.getString("application.destinationHost"));
		TGFConfig.setDestinationRealm(configuration.getString("application.destinationRealm"));
		TGFConfig.setVoiceServiceContextId(configuration.getString("application.voiceServiceContextId"));
		TGFConfig.setSmsServiceContextId(configuration.getString("application.smsServiceContextId"));
		TGFConfig.setDataServiceContextId(configuration.getString("application.dataServiceContextId"));
		TGFConfig.setImsEventType(configuration.getString("application.imsEventType"));
		TGFConfig.setDiameterConfigPath(configuration.getString("diameterClient.configuration.file"));
		TGFConfig.setPersisterThreadCount(configuration.getInt("application.persisterThreadCount"));
		TGFConfig.setPersisterBatchSize(configuration.getInt("application.persisterBatchSize"));
		TGFConfig.setPersisterSuccessFilter(configuration.getBoolean("application.persisterSuccessFilter"));
		String ratingGroup = configuration.getString("application.ratingGroup");
		TGFConfig.setRatingGroupList(Arrays.stream(ratingGroup.split(";")).map(s -> Integer.valueOf(s.trim())).collect(Collectors.toList()));
		TGFConfig.setMsccLookUpEnabled(Optional.of(configuration.getBoolean("application.msccLookUpEnabled")).orElse(true));
		TGFConfig.setServiceIdentifier(configuration.getInt("application.serviceIdentifier"));
		TGFConfig.setSgsnAddress(configuration.getString("application.sgsnAddress"));
		TGFConfig.setWarmUpDurationInSeconds(configuration.getInt("application.warmUpDurationInSeconds"));
		TGFConfig.setWarmUpCustomerCount(Optional.of(configuration.getInt("application.warmUpCustomerCount")).orElse(100));
		TGFConfig.setTrafficGeneratorSineFunctionWaveLength(Optional.of(configuration.getInt("application.traffic.function.wave.length")).orElse(0));
		TGFConfig.setTrafficGeneratorSineFunctionWaveAmplitude(Optional.of(configuration.getInt("application.traffic.function.wave.amplitude")).orElse(0));
		TGFConfig.setTrafficSlotLengthInMillis(Optional.of(configuration.getLong("application.traffic.slot.length.in.millis")).orElse(20L));
		TGFConfig.setStatsExportPeriodInSeconds(configuration.getInt("application.statsExportPeriodInSeconds"));
		TGFConfig.setUseSipFormattedNumbers(Optional.of(configuration.getBoolean("application.useSipFormattedNumbers")).orElse(false));
		TGFConfig.setAddSubscriptionImsi(Optional.of(configuration.getBoolean("application.addSubscriptionImsi")).orElse(true));
		TGFConfig.setSipFormattedNumberUrl(Optional.ofNullable(configuration.getString("application.sipFormattedNumberUrl")).orElse("ims.mnc510.mcc302.3gppnetwork.org"));
		TGFConfig.setImsAccessNetworkInformation(
				Optional.ofNullable(configuration.getString("application.imsAccessNetworkInformation")).orElse("3GPP-E-UTRAN-FDD;utran-cell-id-3gpp=30250061c6188ee02"));
		TGFConfig.setDatabaseFilePath(configuration.getString("database.configuration.file"));
		DatabaseConnector.initializeDataSource();
	}
}
