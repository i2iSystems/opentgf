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

package com.i2i.fcbs.octgf;

import java.io.File;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import javax.management.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;

import com.i2i.fcbs.octgf.cache.TGFReferenceData;
import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.config.parser.TGFConfigParser;
import com.i2i.fcbs.octgf.config.parser.cli.TGFCommandLineParser;
import com.i2i.fcbs.octgf.constant.AppModeType;
import com.i2i.fcbs.octgf.core.TGFThreadService;
import com.i2i.fcbs.octgf.core.diameter.StackInitializer;
import com.i2i.fcbs.octgf.core.traffic.LinearRateFunction;
import com.i2i.fcbs.octgf.core.worker.CallableWorkerFactory;
import com.i2i.fcbs.octgf.core.worker.PerfMon;
import com.i2i.fcbs.octgf.data.DatabaseConnector;
import com.i2i.fcbs.octgf.exception.TransmitException;
import com.i2i.fcbs.octgf.jmx.TGFPerformance;
import com.i2i.fcbs.octgf.jmx.TGFPerformanceFacade;
import com.i2i.fcbs.octgf.jmx.TGFPerformanceMBean;
import com.i2i.fcbs.octgf.service.ConfigService;
import com.i2i.fcbs.octgf.service.CustomerService;

import io.netty.util.concurrent.DefaultThreadFactory;

public class OCTGF {
    private static final Logger logger = LogManager.getLogger(OCTGF.class);

    public static void main(String[] args) throws Exception {
        PrintWriter bannerWriter = new PrintWriter(System.out, true);
        bannerWriter.println("");
        // generated using http://www.patorjk.com/software/taag/#p=display&f=Big%20Money-ne&t=OCTGF
        bannerWriter.println("  /$$$$$$   /$$$$$$  /$$$$$$$$ /$$$$$$  /$$$$$$$$\n" +
                " /$$__  $$ /$$__  $$|__  $$__//$$__  $$| $$_____/\n" +
                "| $$  \\ $$| $$  \\__/   | $$  | $$  \\__/| $$      \n" +
                "| $$  | $$| $$         | $$  | $$ /$$$$| $$$$$   \n" +
                "| $$  | $$| $$         | $$  | $$|_  $$| $$__/   \n" +
                "| $$  | $$| $$    $$   | $$  | $$  \\ $$| $$      \n" +
                "|  $$$$$$/|  $$$$$$/   | $$  |  $$$$$$/| $$      \n" +
                " \\______/  \\______/    |__/   \\______/ |__/      \n" +
                "");

        configureApplication(args, bannerWriter);
        bannerWriter.println("");

		logger.info("Application instance {} launch completed. {} services will run. App Mode: {}. Mark 2021-08-24T00:00:00+03:00",
				TGFConfig.getApplicationInstanceID(), TGFConfig.getServiceToTest(), TGFConfig.getApp());
		logger.info("Max heap size is {} MB", Runtime.getRuntime().maxMemory() / 1e+6);
        
        TGFShutdownController shutdownController = new TGFShutdownController();
        Runtime.getRuntime().addShutdownHook(shutdownController);

        initializeApplication(shutdownController);

        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName("fcbsJMX:jmx=TGFPerformanceMBean");
            TGFPerformanceMBean performanceBean = new TGFPerformance();
            mbs.registerMBean(performanceBean, name);

            TGFPerformanceFacade.setPerformanceBean(performanceBean);
        } catch (MBeanRegistrationException | InstanceAlreadyExistsException | NotCompliantMBeanException | MalformedObjectNameException e) {
            logger.warn("Unable for register TGFPerformanceMBean", e);
        }
    }

    private static void configureApplication(String[] args, PrintWriter bannerWriter) throws Exception {
        TGFConfig.setApp(AppModeType.CC);
        TGFCommandLineParser clp = new TGFCommandLineParser();
        clp.parse(args);
        bannerWriter.println("[parse()] reading configuration... [OK]");
        TGFConfigParser.parse();
        bannerWriter.println("[configureApplication()] Parsed Config files... [OK]");

        try (Connection connection = DatabaseConnector.getConnection()){
            ConfigService.fetchConfigs(connection);
            TimeZone.setDefault(TimeZone.getTimeZone(TGFConfig.getTimeZone()));
        }
    }

    private static void initializeApplication(TGFShutdownController shutdownController) throws Exception {
        int nPartitions = 1;

        try (Connection connection = DatabaseConnector.getConnection()) {
            TGFReferenceData.setAllCustomers(CustomerService.fetchCustomers(connection));
        } catch (Exception e) {
            logger.error("[initializeApplication()] [Error while fetching customer data: {}] [NOK]", e.getMessage());
            System.exit(0);
        }

        nPartitions = initializeDiameter(shutdownController);
        
        Runnable runnable = new TGFThreadService(TGFReferenceData.getAllCustomers(), nPartitions);
        Thread threadService = new Thread(runnable);
        threadService.start();
    }

    private static int initializeDiameter(TGFShutdownController shutdownController) throws Exception {
        List<File> configFiles = StackInitializer.listConfigFiles(TGFConfig.getDiameterConfigPath());
        logger.info("[initializeDiameter()] [{} config files found]", configFiles.size());
        logger.debug("[initializeDiameter()] [Config files {}]", configFiles);

        List<StackInitializer> stackInitializers = new LinkedList<>();

        for (File configFile : configFiles) {
            StackInitializer stackInitializer = initializeStack(configFile);
            shutdownController.addStackInitializer(stackInitializer);
            stackInitializers.add(stackInitializer);
        }

        if (TGFConfig.getWarmUpDurationInSeconds() > 0) {
            doWarmUp(stackInitializers);
        }

        stackInitializers.forEach(stackInitializer ->
                IntStream.range(0, TGFConfig.getTrafficThreadCount()).forEach(i -> TGFReferenceData.addStackInitializer(stackInitializer))
        );

        return stackInitializers.size();
    }

    private static void doWarmUp(List<StackInitializer> stackInitializers) {
        int warmUpIndex = 0;
        for (StackInitializer stackInitializer : stackInitializers) {
            logger.info("[initializeDiameter()] [Running warmUp for stack {}]", ++warmUpIndex);
            TGFReferenceData.addStackInitializer(stackInitializer);
            warmUp();
        }
    }

	private static StackInitializer initializeStack(File configFile) throws Exception {
		try {
            StackInitializer stackInitializer = new StackInitializer(configFile);
			stackInitializer.initialize();
			logger.info("[initializeStack()] SESSION obtained successfully.... [OK]");
			return stackInitializer;
		} catch (InternalException | IllegalDiameterStateException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("initializeStack failed", e);
		}
	}

    private static void warmUp() {
        int customers = TGFReferenceData.getAllCustomers().size();
        customers = Math.min(TGFConfig.getWarmUpCustomerCount(), customers);

        logger.info("[warmUp] Running warm up with {} customers for {} seconds", customers, TGFConfig.getWarmUpDurationInSeconds());

        Callable<Integer> callable = CallableWorkerFactory.create(new PerfMon.NullPerfMon(), TGFReferenceData.getAllCustomers().subList(0, customers), 10, 1, TimeUnit.SECONDS.toMillis(TGFConfig.getWarmUpDurationInSeconds()), new LinearRateFunction(1));
        ExecutorService executorService = Executors.newFixedThreadPool(1, new DefaultThreadFactory("warmUp-pool"));
        Future future = executorService.submit(callable);

        try {
            future.get();
            logger.info("[warmUp] Completed");
        } catch (InterruptedException e) {
            logger.error("[warmUp] failed.", e);
            Thread.currentThread().interrupt();
        } catch (Exception t) {
            logger.warn("[warmUp] [Failed with exception '{}' with message '{}'] [OK]", t.getCause().getClass().getSimpleName(), t.getCause().getMessage());

            if (t.getCause() instanceof TransmitException) {
                logger.error("[warmUp] [Cannot recover from diameter exception. Terminating...] [NOK]");
                System.exit(1);
            }
        } finally {
            executorService.shutdown();
            logger.info("[warmUp] *** COMPLETED ****************************************************");
        }
    }

}
