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

package com.i2i.fcbs.octgf.config.parser.cli;

import java.util.Arrays;

import org.apache.commons.cli.*;

import com.i2i.fcbs.octgf.config.TGFConfig;
import com.i2i.fcbs.octgf.constant.TGFServiceType;

/**
 * Parse and process the command line arguments
 *
 * @author ayaz
 */
public final class TGFCommandLineParser {
	private Options				options;
	private CommandLineParser	parser;

	public TGFCommandLineParser() {
		parser = new GnuParser();
		this.options = new Options();
		options.addOption(new Option("a", true, "application name"));
		options.addOption(new Option("c", true, "configuration file info <arg> config filename fullpath"));
		options.addOption(new Option("nr", true, "instance id"));
		options.addOption(new Option("h", false, "print help info"));
		options.addOption(new Option("i", false, "print version info"));
		options.addOption(new Option("srvc", true, "service id value. One of " + Arrays.toString(TGFServiceType.values())));
		options.addOption(new Option("site", true, "Customer site selection"));
		options.addOption(new Option("t", true, "TGF_SESSN table tag value"));
	}

	public void parse(String[] args) {
		try {
			// parse the command line arguments
			CommandLine cmdLine = parser.parse(this.options, args);
			if (cmdLine.hasOption('i')) {
				printVersion();
			} else if (cmdLine.hasOption('h')) {
				printHelp();
			}
			if (cmdLine.hasOption('a')) {
				TGFConfig.setApplicationName(cmdLine.getOptionValue('a'));
			}
			if (cmdLine.hasOption('c')) {
				TGFConfig.setConfigFilePath(cmdLine.getOptionValue('c'));
			}
			if (cmdLine.hasOption("nr")) {
				TGFConfig.setApplicationInstanceID(Integer.parseInt(cmdLine.getOptionValue("nr")));
			} else {
				TGFConfig.setApplicationInstanceID(1);
			}
			if (cmdLine.hasOption("srvc")) {
				TGFConfig.setServiceToTest(TGFServiceType.valueOf(cmdLine.getOptionValue("srvc")));
			} else {
				TGFConfig.setServiceToTest(TGFServiceType.VOICE);
			}
			if (cmdLine.hasOption("site")) {
				TGFConfig.setCustomerSite(Integer.valueOf(cmdLine.getOptionValue("site")));
			} else {
				TGFConfig.setCustomerSite(null);
			}
			if (cmdLine.hasOption("t")) {
				TGFConfig.setSessionTag(cmdLine.getOptionValue("t").trim());
			} else {
				TGFConfig.setSessionTag(null);
			}
		} catch (ParseException exp) {
			System.err.println("Invalid Arguments , " + exp.getMessage());
			printHelp();
			System.exit(-1);
		}
	}

	/**
	 * Prints Application Usage Data.
	 */
	private void printHelp() {
		HelpFormatter helpCLI = new HelpFormatter();
		helpCLI.printHelp("#  [-a] [-c] [-nr] [-srvc] [-app]", options);
		printVersion();
	}

	/**
	 * Prints Application Version Data.
	 */
	private void printVersion() {
		System.out.println(TGFCommandLineParser.class.getPackage().getImplementationVendor());
		System.out.println(TGFCommandLineParser.class.getPackage().getImplementationTitle());
		System.out.println(TGFCommandLineParser.class.getPackage().getImplementationVersion());
		System.exit(0);
	}
}
