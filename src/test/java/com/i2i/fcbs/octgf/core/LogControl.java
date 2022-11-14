package com.i2i.fcbs.octgf.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Test;

 class LogControl {
	private static final Logger logger = LogManager.getLogger(LogControl.class);

	@Test
	  void testLogModes() {
		Configurator.initialize(null, "config/tgf_log4j_config.xml");
		logger.debug("DEBUG");
		logger.warn("WARN");
		logger.error("ERROR");
		logger.fatal("FATAL");
	}
}
