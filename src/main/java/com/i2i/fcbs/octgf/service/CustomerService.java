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
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.i2i.fcbs.octgf.bean.CustomerBean;
import com.i2i.fcbs.octgf.config.TGFConfig;

public class CustomerService {
	private static final Logger	logger				= LogManager.getLogger(CustomerService.class);
	private static final String	SELECT_CUSTOMERS	= "SELECT * FROM (SELECT ROW_NUMBER () OVER (ORDER BY SERVEDPARTY_MSISDN) rn, SERVEDPARTY_MSISDN,SERVEDPARTY_IMSI,SERVINGHOST_ADDRESS,OTHERPARTY_MSISDN from TGF_INPUT) a WHERE rn BETWEEN ? AND ?";

	private CustomerService() {
	}

	public static List<CustomerBean> fetchCustomers(Connection connection) throws Exception {
		ResultSet rs = null;
		List<CustomerBean> customers = new ArrayList<>();
		int lowerBound = TGFConfig.getCustomerIdStart();
		int upperBound = TGFConfig.getCustomerIdStart() + TGFConfig.getTps() - 1;
		String query = SELECT_CUSTOMERS;
		if (TGFConfig.getCustomerSite() != null) {
			query += " AND SITE=" + TGFConfig.getCustomerSite();
		}
		logger.info("[fetchCustomers()] [Customer query = {}]", query);
		try (PreparedStatement pStm = connection.prepareStatement(query)) {
			pStm.setInt(1, lowerBound);
			pStm.setInt(2, upperBound);
			logger.info("[fetchCustomers()] Fetching customers between [{}] and [{}]... [OK]", lowerBound, upperBound);
			rs = pStm.executeQuery();
			if (rs.next()) {
				do {
					CustomerBean customer = new CustomerBean(rs.getString("SERVEDPARTY_MSISDN"), rs.getString("SERVEDPARTY_IMSI"), rs.getString("SERVINGHOST_ADDRESS"),
							rs.getString("OTHERPARTY_MSISDN"));
					customers.add(customer);
				} while (rs.next());
				logger.info("[fetchCustomers()] Total [{}] customer fetched... [OK]", customers.size());
			} else {
				throw new Exception(String.format("No customer found with criteria [between %s and %s] AND [service type %s] AND [instance id %s] !", lowerBound, upperBound,
						TGFConfig.getServiceToTest(), TGFConfig.getApplicationInstanceID()));
			}
		} catch (SQLException e) {
			throw new SQLException("An error occured while fetching customers", e);
		} finally {
			if (rs != null)
				rs.close();
		}
		return customers;
	}
}
