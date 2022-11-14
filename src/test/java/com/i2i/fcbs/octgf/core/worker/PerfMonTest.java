package com.i2i.fcbs.octgf.core.worker;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.junit.jupiter.api.Test;

class PerfMonTest {
    private final NumberFormat percentageFormatter = new DecimalFormat("#0.00");

    @Test
    public void testHist(){
        long histVal = 90;
        long sum = 121;
        System.out.println(percentageFormatter.format(histVal * 100.0 / sum));
    }

}