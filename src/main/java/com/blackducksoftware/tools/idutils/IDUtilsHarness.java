/*******************************************************************************
 * Copyright (C) 2015 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
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
