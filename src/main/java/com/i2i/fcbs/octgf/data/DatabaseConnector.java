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

package com.i2i.fcbs.octgf.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.i2i.fcbs.octgf.config.TGFConfig;

public final class DatabaseConnector {
	static String sqliteDatabaseFilePath;

	private DatabaseConnector() {
	}

	public static synchronized void initializeDataSource() {
		sqliteDatabaseFilePath = TGFConfig.getDatabaseFilePath();
	}

	public static synchronized Connection getConnection() throws SQLException {
		try {
			Connection connection = DriverManager.getConnection("jdbc:sqlite:" + sqliteDatabaseFilePath);
			connection.setAutoCommit(false);
			return connection;
		} catch (SQLException e) {
			throw new SQLException("Database connection error : ", e);
		}
	}
}
