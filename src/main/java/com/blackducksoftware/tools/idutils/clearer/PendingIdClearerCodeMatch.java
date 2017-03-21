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
