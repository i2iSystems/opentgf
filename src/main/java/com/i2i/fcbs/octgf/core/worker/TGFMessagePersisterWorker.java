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

import static java.lang.String.format;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.i2i.fcbs.octgf.TGFObserver;
import com.i2i.fcbs.octgf.bean.TGFMessageBean;
import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.core.TGFSharedData;
import com.i2i.fcbs.octgf.data.DatabaseConnector;
import com.i2i.fcbs.octgf.service.MessageService;

public class TGFMessagePersisterWorker implements Callable<Integer> {
	private static final Logger	logger						= LogManager.getLogger(TGFMessagePersisterWorker.class);
	private int					taskID;
	private int					totalInsertedMessageCount	= 0;

	TGFMessagePersisterWorker(int taskID) {
		this.taskID = taskID;
	}

	@Override
	public Integer call() throws Exception {
		List<TGFMessageBean> messages = new ArrayList<>();
		Connection connection = null;
		try {
			connection = DatabaseConnector.getConnection();
			boolean runCondition = true;
			while (runCondition) {
				if (Thread.currentThread().isInterrupted()) {
					if (logger.isErrorEnabled())
						logger.error(format("[call()] [%s] || [%d] || [%d] says its been interrupted...[OK] ", Thread.currentThread().getName(),
								Thread.currentThread().getId(), taskID));
					runCondition = false;
				} else if (TGFObserver.getShutdownSignal().get()) {
					logger.error("[call()] Shutdown signal received. Thread will be terminated... [NOK]");
					runCondition = false;
				} else if (TGFObserver.isTgfTrafficCompleted()) {
					logger.error("[call()] Queue is empty. Traffic threads are dead. Im going to be terminated as well... [OK]");
					runCondition = false;
				} else {
					drainAndPersistMessages(messages, connection);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error("Interrupted. Thread will be cancelled!");
		} catch (Exception e) {
			logException(e);
			throw new Exception("An error occured while persisting data collections!", e);
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
		return totalInsertedMessageCount;
	}

	private void drainAndPersistMessages(List<TGFMessageBean> messages, Connection connection) throws Exception {
		messages.clear();
		int drainedElement = TGFSharedData.getMessageQueue().drainTo(messages, TGFConfig.getPersisterBatchSize());
		totalInsertedMessageCount += drainedElement;
		if (logger.isDebugEnabled())
			logger.debug(format("[call()] Total [%d] element(s) have been drained... [OK]", drainedElement));
		if (drainedElement > 0) {
			MessageService.insertMessages(connection, messages);
			connection.commit();
			logger.debug("[call()] All messages have been inserted... [OK]");
		} else {
			logger.debug("[call()] No element available at queue right now... [OK]");
			Thread.sleep(500);
		}
	}

	private void logException(Throwable t) {
		StringWriter sWriter = new StringWriter();
		t.printStackTrace(new PrintWriter(sWriter));
		if (logger.isErrorEnabled())
			logger.error(format("Exception : Current Thread: %s\n" +
					"[Exception StackTrace : %s], \n" +
					" [Exception : %s]", Thread.currentThread().getName(), sWriter.getBuffer().toString(), t));
	}
}
