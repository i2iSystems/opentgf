package com.i2i.fcbs.octgf.core.worker;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

class AbstractTrafficWorkerTest {
    private static final Logger logger = LogManager.getLogger(AbstractTrafficWorkerTest.class);

    final int numberOfSlots = 50;
    final int timeSlotLengthInMillis = 20;

    @Test
    void testSend() throws Exception {
        sendAndAdjust(1, IntStream.rangeClosed(1, 46).boxed().collect(Collectors.toList()));
    }

    private int calculateRequestPerSlot(List<Integer> sessionList) {
        return (int) Math.ceil(sessionList.size() * 1.0 / numberOfSlots);
    }

    private int sendAndAdjust(int requestType, List<Integer> sessions) throws Exception {
        int requestsSent = 0;
        int requestPerSlot = calculateRequestPerSlot(sessions);
        long workUnitStartTime = System.currentTimeMillis();

        for (int slot = 0; slot < numberOfSlots; slot++) {
            long slotStartTimeInMillis = System.currentTimeMillis();

            List<Integer> slotSessions = sessions.subList(Math.min(sessions.size(), slot * requestPerSlot), Math.min(sessions.size(), (slot + 1) * requestPerSlot));
            logger.error("slotsessions {}", slotSessions);

            int slotRequestsSent = 0;

            for (Integer pair : slotSessions) {
                logger.error("Pair {}", pair);
            }

            long usedSlotTimeInMillis = System.currentTimeMillis() - slotStartTimeInMillis;
            logger.error("[sendAndAdjust()] [{}/{} reqs sent in slot {} in {} millis]", slotRequestsSent, slotSessions.size(), slot, usedSlotTimeInMillis);

            long usedWorkUnitTime = System.currentTimeMillis() - workUnitStartTime;
            long remainingWorkUnitTime = 1000 - usedWorkUnitTime;
            // adjustment for last slot
            long slotLength = Math.min(Math.max(0, remainingWorkUnitTime), timeSlotLengthInMillis);

            adjust(slot, slotLength, usedSlotTimeInMillis);

            requestsSent += slotRequestsSent;
        }

        return requestsSent;
    }

    private void adjust(int slot, long slotLength, long usedSlotTimeInMillis) {
        if (usedSlotTimeInMillis < slotLength) {
            long residual = slotLength - usedSlotTimeInMillis;

            logger.error("[adjust()] [Residl. time in slot {} is {} millis]", slot, residual);

            try {
                Thread.sleep(residual);
            } catch (InterruptedException e) {
                logger.error("[adjust()] [Interrupted while sleeping for adjustment]");
                Thread.currentThread().interrupt();
            }
        }
    }

}