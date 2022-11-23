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

package com.i2i.fcbs.octgf.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.i2i.fcbs.octgf.bean.TGFMessageBean;
import com.i2i.fcbs.octgf.config.TGFConfig;

public class TGFSharedData {
	private static final BlockingQueue<TGFMessageBean> messageQueue = new LinkedBlockingQueue<>();

	private TGFSharedData() {
	}

	public static void addMessage(TGFMessageBean message, boolean status) {
		if (TGFConfig.getPersisterThreadCount() > 0 && TGFConfig.getPersisterBatchSize() > 0 && (!TGFConfig.isPersisterSuccessFilter() || !status))
			messageQueue.add(message);
	}

	public static BlockingQueue<TGFMessageBean> getMessageQueue() {
		return messageQueue;
	}
}
