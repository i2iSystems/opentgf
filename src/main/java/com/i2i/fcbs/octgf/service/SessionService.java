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

package com.i2i.fcbs.octgf.service;

import static java.sql.Types.NVARCHAR;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.i2i.fcbs.octgf.cache.TGFReferenceData;
import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.core.worker.PerfMon;
import com.i2i.fcbs.octgf.data.DatabaseConnector;

public class SessionService {
    private static final Logger logger = LogManager.getLogger(SessionService.class);
    private static final String INSERT_MESSAGE = "INSERT INTO TGF_SESSN (INST_ID, START_DATE, CFG_TPS, CFG_TYPE, CFG_LOAD_TIME, CFG_UPDATE_CNT, CFG_THREAD_CNT, TAG) VALUES (?,?,?,?,?,?,?,?)";
    private static final String UPDATE_MESSAGE = "UPDATE TGF_SESSN SET TOTAL_REQUEST=?, TOTAL_EXPIRED=?, TOTAL_SUCCESS=?, TOTAL_FAIL=?, TOTAL_RESPONSE=?, TOTAL_SKIPPED=?, TOTAL_PERIOD_CNT=?, AVRG_REQ=?, "
            + "	AVRG_RESP=?,  AVRG_DELAY=?,  MIN_DELAY=?,  MAX_DELAY=?,  END_DATE=?  WHERE SESSN_ID=?";


    private SessionService() {
    }

	public static Long insertSession() throws Exception {
		try (Connection connection = DatabaseConnector.getConnection(); PreparedStatement pStm = connection.prepareStatement(INSERT_MESSAGE)) {
			pStm.setInt(1, TGFConfig.getApplicationInstanceID());
			pStm.setDate(2, new Date(System.currentTimeMillis()));
			pStm.setLong(3, TGFReferenceData.getAllCustomers().size());
			pStm.setString(4, TGFConfig.getServiceToTest().name());
			pStm.setLong(5, TimeUnit.MILLISECONDS.toMinutes(TGFConfig.getLoadTimeInMillis()));
			pStm.setInt(6, TGFConfig.getSessionSequenceLimit());
			pStm.setInt(7, TGFConfig.getTrafficThreadCount());
			if (TGFConfig.getSessionTag() == null)
				pStm.setNull(8, NVARCHAR);
			else
				pStm.setString(8, TGFConfig.getSessionTag());
			int recordCount = pStm.executeUpdate();
			Long sessionId = null;
			try (ResultSet generatedKeys = pStm.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					sessionId = generatedKeys.getLong(1);
				}
			}
			logger.info("[insertSession()] [{} session inserted, session id is {}] [OK]", recordCount, sessionId);
			connection.commit();
			return sessionId;
		} catch (Exception e) {
			throw new Exception("An error occurred while inserting messages...", e);
		}
	}

    public static void updateSession(Long sessionId, PerfMon perfMon) throws Exception {
        long currentTime = System.currentTimeMillis();
        long startTime = perfMon.getStartTime();

        double testDuration = Math.min(millisToSeconds(currentTime - startTime), 1);

		logger.info(
				"[updateSession()] [{} session details are= test duration {} millis, request:{}, success:{}, fail:{}, expired:{}, delay numerator:{}, delay denominator:{} ]",
				sessionId, testDuration, perfMon.getTotalRequest(), perfMon.getTotalSuccess(), perfMon.getTotalFail(), perfMon.getTotalExpired(),
				perfMon.getDelayBinNumerator(), perfMon.getDelayBinDenominator());
        
		long totalResponse = perfMon.getTotalFail() + perfMon.getTotalSuccess() + perfMon.getTotalExpired();
		double averageRequest = perfMon.getTotalRequest() > 0
				? BigDecimal.valueOf(perfMon.getTotalRequest()).divide(BigDecimal.valueOf(testDuration), RoundingMode.HALF_EVEN).doubleValue()
				: 0f;
		double averageResponse = totalResponse > 0 ? BigDecimal.valueOf(totalResponse).divide(BigDecimal.valueOf(testDuration), RoundingMode.HALF_EVEN).doubleValue() : 0f;
		double averageDelay = perfMon.getDelayBinNumerator() > 0 && perfMon.getDelayBinDenominator() > 0
				? BigDecimal.valueOf(perfMon.getDelayBinNumerator()).divide(BigDecimal.valueOf(perfMon.getDelayBinDenominator()), RoundingMode.HALF_EVEN).doubleValue()
				: 0f;
        try (Connection connection = DatabaseConnector.getConnection(); PreparedStatement pStm = connection.prepareStatement(UPDATE_MESSAGE);) {
            pStm.setLong(1, perfMon.getTotalRequest());
            pStm.setLong(2, perfMon.getTotalExpired());
            pStm.setLong(3, perfMon.getTotalSuccess());
            pStm.setLong(4, perfMon.getTotalFail());
            pStm.setLong(5, totalResponse);
            pStm.setLong(6, perfMon.getTotalSkipped());
            pStm.setLong(7, perfMon.getPeriodCounter());
            pStm.setDouble(8, averageRequest);
            pStm.setDouble(9, averageResponse);
            pStm.setDouble(10, averageDelay);
            pStm.setInt(11, perfMon.getDelayMin());
            pStm.setInt(12, perfMon.getDelayMax());
            pStm.setDate(13, new Date(currentTime));

            // where
            pStm.setLong(14, sessionId);

            int recordCount = pStm.executeUpdate();
            logger.info("[updateSession()] [{} session updated {}] [OK]", sessionId, recordCount);

            connection.commit();

        } catch (Exception e) {
            throw new Exception("An error occurred while updating session", e);
        }
    }

    private static long millisToSeconds(long durationInMillis) {
        return durationInMillis / 1000;
    }
}