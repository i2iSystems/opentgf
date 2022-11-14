package com.i2i.fcbs.octgf.core;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

class TGFThreadServiceTest {

    @Test
    void run() {
        List<Integer> numbers = IntStream.range(0,1000).boxed().collect(Collectors.toList());
        int partitionSize = numbers.size() / 11;

        System.out.println(partitionSize);

        List<List<Integer>> partitions = Lists.partition(numbers, partitionSize);

        System.out.println(partitions.size());
        partitions.forEach(System.out::println);
    }
}