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
package com.blackducksoftware.tools.idutils.clearer;

import org.apache.log4j.Logger;

import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.project.codetree.PartialCodeTree;
import com.blackducksoftware.sdk.protex.project.codetree.PartialCodeTreeWithCount;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.Discovery;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.idutils.IDUtilConfig;

/**
 * Most basic type of clearer Simply traverses the tree and anything that is
 * discovered is set to Original
 * 
 * @author akamen
 * 
 */
public class PendingIdClearerCodeMatch extends IdClearer {

    private final Logger log = Logger.getLogger(this.getClass().getName());

    private int count = 0;

    private PendingIdClearerForPatternMatches patternClearer;

    /**
     * Bit of a messy constructor - Grabs project info - Creates manifest file -
     * Sets super class with config manager members TODO: This is messy as it
     * attempts to preserve old code, need to refactor the entire thing.
     * 
     * 11/3/14 - Removed most of the old code, relying on super's initializer to
     * find the right component. Otherwise behavior defaults to Original code
     * automatically.
     * 
     * @param configManager
     * @throws Exception
     */
    public PendingIdClearerCodeMatch(IDUtilConfig configManager,
	    ProtexServerWrapper<ProtexProjectPojo> protexServer) throws Exception {
	super(configManager, protexServer);

	patternClearer = new PendingIdClearerForPatternMatches(configManager,
		protexServer);

    }

    public int process() {

	try {
	    // Do regular clearing
	    count = traverseCodeTree(projPath);
	    // Do it for patterns
	    count += patternClearer.traverseCodeTree(projPath);
	} catch (Exception e) {
	    log.error("Error while clearing pending IDs", e);
	}

	log.info(count
		+ " pending identification(s) have been cleared by Code Match Clearer");

	return count;
    }

    @Override
    public boolean condition(PartialCodeTree tree, String path) throws SdkFault {
	return true;
    }

    @Override
    public boolean hasPendingIds(PartialCodeTree tree) throws SdkFault {
	try {
	    PartialCodeTreeWithCount treeWithCount = protexServer
		    .getInternalApiWrapper().getDiscoveryApi()
		    .getCodeMatchPendingIdFileCount(projId, tree);

	    return treeWithCount.getNodes().get(0).getCount() > 0;
	} catch (Exception e) {
	    return false;
	}
    }

    /**
     * Code Match attemps to do a code match identification, as opposed to
     * declare file.
     */
    public void idToComponent(String path, Discovery target) throws SdkFault {
	idComponentToCodeMatch(path, target);
    }

}
