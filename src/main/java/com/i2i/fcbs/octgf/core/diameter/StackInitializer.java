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

package com.i2i.fcbs.octgf.core.diameter;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdiameter.api.*;
import org.jdiameter.server.impl.StackImpl;
import org.jdiameter.server.impl.helpers.XMLConfiguration;

import com.i2i.fcbs.octgf.config.TGFConfig;

public class StackInitializer {
	private static final Logger		logger	= LogManager.getLogger(StackInitializer.class);
	private final Stack				stack;
	private final SessionFactory	sessionFactory;
	private final Configuration		configuration;

	public StackInitializer(File configurationFile) throws Exception {
		configuration = getConfiguration(configurationFile);
		stack = new StackImpl();
		sessionFactory = stack.init(configuration);
	}

	public void initialize() throws Exception {
		try {
			Network network = stack.unwrap(Network.class);
			network.addNetworkReqListener(request -> null, ApplicationId.createByAuthAppId(TGFConfig.getApp().getAppId()));
			MetaData metaData = stack.getMetaData();
			StackType stackType = metaData.getStackType();
			ensureNotServerStack(stackType);
			validateMinorVersion(metaData);
			stack.start();
			if (stack.isActive()) {
				logger.info("[initialize()] STACK started successfully... [OK]");
			} else {
				logger.info("[initialize()] STACK is not running. Will wait it to be started during 5 MINUTES... [OK]");
				long deadLine = Instant.now().toEpochMilli() + TimeUnit.MINUTES.toMillis(5);
				boolean stackNotActive = true;
				while (stackNotActive) {
					stackNotActive = !isStackActive();
					if (Instant.now().toEpochMilli() >= deadLine) {
						throw new Exception("STACK is not running. Something is wrong. OCTGF will be down!");
					}
					Thread.sleep(200);
				}
			}
		} catch (Exception e) {
			destroyStack();
			throw new Exception("An error occured while initializing diameter stack!!", e);
		}
	}

	private boolean isStackActive() {
		if (stack.isActive()) {
			logger.info("[initialize()] STACK is running... [OK]");
			return true;
		}
		return false;
	}

	private void validateMinorVersion(MetaData metaData) throws Exception {
		if (metaData.getMinorVersion() <= 0) {
			stack.destroy();
			throw new Exception("Incorrect Minor Version: [" + metaData.getMinorVersion() + "]!... [NOK]");
		}
	}

	private void ensureNotServerStack(StackType stackType) throws Exception {
		if (stackType != StackType.TYPE_SERVER) {
			stack.destroy();
			throw new Exception("Incorrect Stack Type: [" + stackType + "]!... [NOK]");
		}
	}

	public void stopStack() throws InternalException, IllegalDiameterStateException {
		if (!Objects.isNull(stack)) {
			stack.stop(1, TimeUnit.SECONDS, 1);
		}
	}

	public void destroyStack() {
		if (!Objects.isNull(stack)) {
			stack.destroy();
		}
	}

	public Session getDiameterSession() throws InternalException, InterruptedException {
		Session session = sessionFactory.getNewSession();
		while (!session.isValid()) {
			logger.debug("[getDiameterSession()] waiting for session validation ... [OK]");
			Thread.sleep(100);
		}
		Thread.sleep(15000);
		return session;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	@Override
	public String toString() {
		return "StackInitializer:" + getConfiguration().getStringValue(1, "unknown-client-name");
	}

	public static Configuration getConfiguration(File configFile) throws Exception {
		InputStream is = null;
		Configuration config;
		try {
			String diameterConfigurationFile = configFile.getAbsolutePath();
			URL url = new URL("file:///" + diameterConfigurationFile);
			is = url.openStream();
			config = new XMLConfiguration(is);
		} catch (Exception e) {
			throw new Exception("An error occurred while parsing configuration file", e);
		} finally {
			if (is != null) {
				is.close();
			}
		}
		return config;
	}

	public static List<File> listConfigFiles(String tgfConfigFile) {
		String tgfConfigFilePattern = new File(tgfConfigFile).getName().replace(".xml", "").replace(".XML", "");
		System.out.println(tgfConfigFilePattern);
		File file = new File(TGFConfig.getDiameterConfigPath());
		if (file.getAbsolutePath().toLowerCase().endsWith(".xml"))
			file = file.getParentFile();
		System.out.println(file.getAbsolutePath());
		File[] files = file.listFiles((dir1, name) -> name.startsWith(tgfConfigFilePattern) && name.endsWith(".xml"));
		assert files != null;
		return Arrays.asList(files);
	}
}
