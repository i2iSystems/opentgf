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

import org.jdiameter.api.Message;

import com.i2i.fcbs.octgf.exception.TransmitException;
import com.i2i.fcbs.octgf.jmx.TGFPerformanceFacade;
import com.i2i.fcbs.octgf.jmx.TGFPerformanceMBean;

public abstract class MessageTransmitter {
	public void transmit(Message message) throws TransmitException {
		doTransmit(message);
		TGFPerformanceFacade.getPerformanceBean().ifPresent(TGFPerformanceMBean::incOutPackCount);
	}

	protected abstract void doTransmit(Message message) throws TransmitException;

	protected boolean isResponseExpected() {
		return true;
	}

	protected abstract String getIdentificationString();

	protected abstract void close();
}
