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

package com.i2i.fcbs.octgf;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdiameter.api.BaseSession;

import com.i2i.fcbs.octgf.cache.TGFReferenceData;
import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.core.diameter.StackInitializer;

/**
 * ShutdownController
 * <p>
 * This class is used to catch application termination (kill signals, any
 * abnormal termination, etc.) and shutting down application normally.
 */
public class TGFShutdownController extends Thread {
	private static final Logger				logger				= LogManager.getLogger(TGFShutdownController.class);
	private final List<StackInitializer>	stackInitializers	= new ArrayList<>();

	@Override
	public void run() {
		logger.info("{} Shutdown Controller caught a kill signal!", TGFConfig.getApplicationName());
		try {
			TGFReferenceData.getDiameterSessions().forEach(BaseSession::release);
		} catch (Exception e) {
			logger.debug("[TGFShutdownController] [Unable to release sessions]", e);
		}
		logger.info("[run()] Diameter Sessions released... [OK]");
		for (StackInitializer stackInitializer : stackInitializers)
			try {
				stackInitializer.stopStack();
			} catch (Exception e) {
				logger.debug("[TGFShutdownController] [Attempting stop() for STACK failed] ", e);
			}
		try {
			stackInitializers.forEach(StackInitializer::destroyStack);
		} catch (Exception e) {
			logger.debug("[TGFShutdownController] [Attempting destroy() for STACK failed] ", e);
		}
		logger.info("[run()] Stack destroyed... [OK]");
		logger.info("{} is READY to terminate!", TGFConfig.getApplicationName());
	}

	public void addStackInitializer(StackInitializer stackInitializer) {
		this.stackInitializers.add(stackInitializer);
	}
}
