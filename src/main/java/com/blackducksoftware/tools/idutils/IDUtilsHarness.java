/**
 * idutils
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.tools.idutils;

import org.apache.log4j.Logger;

/**
 * Entry class for ID Utils
 * 
 * Invokes configuration and handles business logic for the separate modules.
 * 
 * @author Ari Kamen
 * 
 */
public class IDUtilsHarness {
    // Static logger for class w/main
    private final static Logger log = Logger.getLogger(IDUtilsHarness.class);

    public IDUtilsHarness() {
    }

    /**
     * First argument should be config file Second (optional) argument should be
     * manifest file
     * 
     * @param args
     */
    public static void main(String args[]) {

	// Handle argument case
	// First argument should be name of a file
	String configFile = null;
	String manifestFile = null;

	if (args.length == 1) {
	    log.info("Processing config.");
	    configFile = args[0];
	} else if (args.length == 2) {
	    log.info("Processing manifest file.");
	    configFile = args[0];
	    manifestFile = args[1];
	} else {
	    log.info("Usage: Pass in config file (required) and manifest file (optional)");
	    System.exit(1);
	}

	try {
	    IDUtilConfig configManager = new IDUtilConfig(configFile);
	    IDUtilsProcessor processor = new IDUtilsProcessor(configManager);
	    processor.process(manifestFile);

	} catch (Exception e) {
	    log.info("Unable to process: " + e.getMessage());
	}

    }
}
