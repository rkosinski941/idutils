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
