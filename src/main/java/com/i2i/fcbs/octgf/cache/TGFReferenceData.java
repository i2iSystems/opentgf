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

package com.i2i.fcbs.octgf.cache;

import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdiameter.api.Session;

import com.i2i.fcbs.octgf.bean.CustomerBean;
import com.i2i.fcbs.octgf.core.diameter.StackInitializer;

public class TGFReferenceData {
	private static final Logger						logger				= LogManager.getLogger(TGFReferenceData.class);
	private static List<CustomerBean>				allCustomers;
	private static final Queue<StackInitializer>	stackInitializers	= new LinkedList<>();
	private static final List<Session>				diameterSessions	= new LinkedList<>();

	private TGFReferenceData() {
	}

	public static List<CustomerBean> getAllCustomers() {
		return allCustomers;
	}

	public static void setAllCustomers(List<CustomerBean> allCustomers) {
		TGFReferenceData.allCustomers = allCustomers;
	}

	public static synchronized void addStackInitializer(StackInitializer stackInitializer) {
		stackInitializers.add(stackInitializer);
		logger.info("[addStackInitializer()] [Added {}. Total: {}]", stackInitializer, stackInitializers.size());
	}

	public static Collection<StackInitializer> getStackInitializers() {
		return Collections.unmodifiableCollection(stackInitializers);
	}

	public static synchronized StackInitializer pollStackInitializer() {
		StackInitializer stackInitializer = stackInitializers.poll();
		logger.info("[pollStackInitializer()] [Obtained {}. Remaining: {}]", stackInitializer, stackInitializers.size());
		return stackInitializer;
	}

	public static synchronized void addDiameterSession(Session diameterSession) {
		logger.info("[addDiameterSession()] [New diameter session id = {}]", diameterSession.getSessionId());
		diameterSessions.add(diameterSession);
	}

	public static List<Session> getDiameterSessions() {
		return Collections.unmodifiableList(diameterSessions);
	}
}
