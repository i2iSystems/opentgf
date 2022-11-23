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

package com.i2i.fcbs.octgf.core.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdiameter.api.*;

import com.i2i.fcbs.octgf.cache.TGFReferenceData;
import com.i2i.fcbs.octgf.core.diameter.StackInitializer;
import com.i2i.fcbs.octgf.exception.TransmitException;

public class DiameterMessageTransmitter extends MessageTransmitter {
	private static final Logger						logger	= LogManager.getLogger(DiameterMessageTransmitter.class);
	private final EventListener<Request, Answer>	eventListener;
	private final Session							diameterSession;
	private final StackInitializer					stackInitializer;

	public DiameterMessageTransmitter(EventListener<Request, Answer> eventListener) {
		this.eventListener = eventListener;
		this.stackInitializer = TGFReferenceData.pollStackInitializer();
		logger.debug("[DiameterMessageTransmitter] [Using stack initializer: {}]", stackInitializer);
		this.diameterSession = getSession(stackInitializer);
		logger.debug("[DiameterMessageTransmitter] [Using sessionId: {}]", diameterSession);
		TGFReferenceData.addDiameterSession(diameterSession);
	}

	public Session getSession(StackInitializer stackInitializer) {
		final Session session;
		try {
			session = stackInitializer.getDiameterSession();
		} catch (InterruptedException | InternalException e) {
			throw new RuntimeException(e);
		}
		return session;
	}

	public void doTransmit(Message message) throws TransmitException {
		try {
			message.setProxiable(true);
			diameterSession.send(message, eventListener);
		} catch (InternalException | IllegalDiameterStateException | RouteException | OverloadException e) {
			throw new TransmitException("Transmitting message failed!", e);
		}
	}

	@Override
	protected String getIdentificationString() {
		return "DiameterTransmitter:" + stackInitializer.getConfiguration().getStringValue(1, "unknown-client");
	}

	@Override
	protected void close() {
		logger.trace("DiameterMessageTransmitter closed");
	}
}
