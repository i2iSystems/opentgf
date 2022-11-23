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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.i2i.fcbs.octgf.config.TGFConfig;

public class ConfigService {
	private static final Logger	logger			= LogManager.getLogger(ConfigService.class);
	private static final String	SELECT_CONFIG	= "SELECT * FROM TGF_CONFIG WHERE INSC_ID = ?";

	private ConfigService() {
	}

	private static Config fetchConfigs(Connection connection, int instanceId) throws SQLException {
		ResultSet rs = null;
		try (PreparedStatement pStm = connection.prepareStatement(SELECT_CONFIG)) {
			pStm.setInt(1, instanceId);
			rs = pStm.executeQuery();
			if (rs.next()) {
				boolean tpsFound = false;
				boolean threadCountFound = false;
				boolean updateCountFound = false;
				boolean loadTimeFound = false;
				int tps = 0;
				int trafficThreadCount = 0;
				int sessionSequenceLimit = 0;
				long loadTime = 0;
				do {
					String name = rs.getString("NAME");
					int value = rs.getInt("VALUE");
					if (name.equalsIgnoreCase("tps")) {
						tps = value;
						tpsFound = true;
					} else if (name.equalsIgnoreCase("thread_cnt")) {
						trafficThreadCount = value;
						threadCountFound = true;
					} else if (name.equalsIgnoreCase("update_cnt")) {
						sessionSequenceLimit = value;
						updateCountFound = true;
					} else if (name.equalsIgnoreCase("load_time")) {
						loadTime = TimeUnit.MINUTES.toMillis(value);
						loadTimeFound = true;
					}
				} while (rs.next());
				if (!tpsFound || !threadCountFound || !updateCountFound || !loadTimeFound) {
					throw new Exception("Missing configurations.. (tpsFound[" + tpsFound + "], threadCountFound[" + threadCountFound + "], updateCountFound["
							+ updateCountFound + "], loadTimeFound[" + loadTimeFound + "])");
				}
				return new Config(trafficThreadCount, loadTime, tps, sessionSequenceLimit);
			} else {
				logger.error("[fetchConfigs()] [Unable to find configurations for instanceId={}. Check TGF_CONFIG table.]", instanceId);
				System.exit(1);
			}
		} catch (Exception e) {
			logger.error("[fetchConfigs()] [Unable to fetch configurations for instanceId={}.]", instanceId, e);
			System.exit(1);
		} finally {
			rs.close();
		}
		return null;
	}

	public static void fetchConfigs(Connection connection) throws SQLException {
		int start = 1;
		for (int i = 1; i < TGFConfig.getApplicationInstanceID(); i++) {
			Config config = fetchConfigs(connection, i);
			start += config.getTps();
		}
		Config config = fetchConfigs(connection, TGFConfig.getApplicationInstanceID());
		TGFConfig.setSessionSequenceLimit(config.getUpdateCnt());
		TGFConfig.setTrafficThreadCount(config.getThreadCnt());
		TGFConfig.setTps(config.getTps());
		TGFConfig.setLoadTimeInMillis(config.getLoadTime());
		TGFConfig.setCustomerIdStart(start);
	}

	private static class Config {
		private int		threadCnt;
		private long	loadTime;
		private int		tps;
		private int		updateCnt;

		public Config(int threadCnt, long loadTime, int tps, int updateCnt) {
			this.threadCnt = threadCnt;
			this.loadTime = loadTime;
			this.tps = tps;
			this.updateCnt = updateCnt;
		}

		public int getThreadCnt() {
			return threadCnt;
		}

		public long getLoadTime() {
			return loadTime;
		}

		public int getTps() {
			return tps;
		}

		public int getUpdateCnt() {
			return updateCnt;
		}
	}
}
