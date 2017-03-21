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

import java.util.List;

import org.apache.log4j.Logger;

import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.idutils.clearer.IdClearer;
import com.blackducksoftware.tools.idutils.clearer.PendingIdClearerBySearch;
import com.blackducksoftware.tools.idutils.clearer.PendingIdClearerCodeMatch;
import com.blackducksoftware.tools.idutils.clearer.PendingIdClearerReverse;
import com.blackducksoftware.tools.idutils.component.ComponentLocator;
import com.blackducksoftware.tools.idutils.component.ComponentUpdater;
import com.blackducksoftware.tools.idutils.report.IdReporter;

/**
 * Reads the configuration file then invokes the factory to determine which type
 * of ID Clearer to invoke
 * 
 * @author akamen
 * 
 */
public class IDUtilsProcessor {
    
    private final Logger log = Logger.getLogger(this.getClass().getName());
    private final IDUtilConfig configManager;

    public IDUtilsProcessor(IDUtilConfig configManager) {
	this.configManager = configManager;
    }

    /**
     * Processes the config file, if manifestFile is not null then an undo
     * operation is performed.
     * 
     * @param manifestFile
     * @throws Exception
     */
    public void process(String manifestFile) throws Exception {

	// TODO: Get rid of this nasty else if and use class instantiation to
	// drive logic.
	if (manifestFile != null && manifestFile.length() > 0) {
	    // this is the undo option
	    PendingIdClearerReverse pendingIdClearer = new PendingIdClearerReverse(
		    configManager, manifestFile);
	    pendingIdClearer.reverse();

	    log.info("Finished, manifest processed.");

	    return;
	} else {
	    if (configManager.getLibLoc()) {
		ComponentLocator compLocator = new ComponentLocator(
			configManager);
		compLocator.findComponents();
	    } else if (configManager.getUpdateOnly()) {
		try {
		    ComponentUpdater compUpdater = new ComponentUpdater(
			    configManager);
		    compUpdater.updateMatches();
		} catch (Exception e) {
		    log.error("Problems during component updating", e);
		}
		return;
	    } else {
		ProtexServerWrapper<ProtexProjectPojo> psw = new ProtexServerWrapper<ProtexProjectPojo>(
			configManager.getServerBean(), configManager,
			configManager.isSkipValidation());
		// TODO: Create a factory
		IdClearer idClearer = null;
		List<String> searchNames = configManager.getSearchNames();
		if (configManager.isReportMode()) {
		    configManager.setChildElementCount(new Long(1000000));
		    idClearer = new IdReporter(configManager, psw);
		} else if (searchNames.size() > 0) {
		    idClearer = new PendingIdClearerBySearch(configManager, psw);
		} else {
		    idClearer = new PendingIdClearerCodeMatch(configManager,
			    psw);
		}

		idClearer.process();
	    }
	}

	log.info("Finished!");
    }

}
