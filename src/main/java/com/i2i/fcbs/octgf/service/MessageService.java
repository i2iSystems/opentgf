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

package com.i2i.fcbs.octgf.service;

import java.sql.*;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.i2i.fcbs.octgf.bean.TGFMessageBean;
import com.i2i.fcbs.octgf.config.TGFConfig;

public class MessageService {
	private static final Logger	logger			= LogManager.getLogger(MessageService.class);
	private static final String	INSERT_MESSAGE	= "INSERT INTO TGF_SESSN_DTL (SESSN_ID, DMTR_SESSN_ID, IMSI, REQ_TP, REQ_NUM, EVENT_START, EVENT_DURATION, RESULT_CODE, CCR_EVENT_TIMESTAMP, METRICS) VALUES (?,?,?,?,?,?,?,?,?,?)";

	private MessageService() {
	}

	public static void insertMessages(Connection connection, List<TGFMessageBean> messages) {
		if (!messages.isEmpty()) {
			try (PreparedStatement pStm = connection.prepareStatement(INSERT_MESSAGE)) {
				for (TGFMessageBean m : messages) {
					pStm.setLong(1, TGFConfig.getTestSessionId());
					pStm.setString(2, m.getSessionID());
					pStm.setString(3, m.getImsi());
					pStm.setInt(4, m.getRequestType().getType());
					pStm.setInt(5, m.getRequestNumber());
					pStm.setTimestamp(6, new Timestamp(m.getRequestEventDate()));
					pStm.setLong(7, m.getResponseEventDate() - m.getRequestEventDate());
					pStm.setObject(8, m.getResultCode());
					pStm.setDate(9, new Date(m.getEventDate()));
					String metrics = null;
					pStm.setObject(10, metrics);
					pStm.addBatch();
				}
				int recordCount = pStm.executeBatch().length;
				logger.debug("[insertMessages()] Total [{}] record(s) have been inserted... [OK]", recordCount);
			} catch (SQLException e) {
				throw new RuntimeException("An error occured while inserting messages...", e);
			}
		} else {
			logger.debug("[insertMessages()] No message found to be inserted... [OK]");
		}
	}
}