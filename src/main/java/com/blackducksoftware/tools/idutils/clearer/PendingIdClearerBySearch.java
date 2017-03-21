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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.common.StringSearchPattern;
import com.blackducksoftware.sdk.protex.common.StringSearchPatternOriginType;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNode;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNodeType;
import com.blackducksoftware.sdk.protex.project.codetree.PartialCodeTree;
import com.blackducksoftware.sdk.protex.project.codetree.PartialCodeTreeWithCount;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.Discovery;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.StringSearchDiscovery;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.idutils.IDUtilConfig;

/**
 * Took over old class from Jad Adopted to new ID Utils
 * 
 * @author akamen
 * 
 */
public class PendingIdClearerBySearch extends IdClearer {
    private final Logger log = Logger.getLogger(this.getClass().getName());

    // This map keeps track of all the user specified search IDs and their
    // counts.
    // Counts will be incremented upon discovery.
    private Map<String, Integer> searchNameIdMap = new HashMap<String, Integer>();

    public PendingIdClearerBySearch(IDUtilConfig configManager,
	    ProtexServerWrapper<ProtexProjectPojo> protexServer) throws Exception {
	super(configManager, protexServer);
	buildSearchNameCache(configManager.getSearchNames());
    }

    /**
     * Traversal for Pending by Search only. This is different than the code as
     * it only looks through things once.
     * 
     * This assumes that identifications are being made successfully.
     */
    public int traverseCodeTree(String projPath) throws Exception {
	int internalCount = 0;

	PartialCodeTree tree = protexServer.getInternalApiWrapper().getCodeTreeApi()
		.getCodeTree(projId, projPath, 1, false);

	for (CodeTreeNode node : tree.getNodes()) {

	    if (node.getNodeType().equals(CodeTreeNodeType.FILE)) {
		internalCount += handleFiles(tree.getParentPath(), node);

	    } else
	    // for folders
	    {
		PartialCodeTree folderTree = new PartialCodeTree();
		folderTree.setParentPath(tree.getParentPath());
		folderTree.getNodes().add(node);

		if (hasPendingIds(folderTree)) {
		    internalCount += handleFolder(tree.getParentPath()
			    + (projPath.equals("/") ? "" : "/")
			    + node.getName());
		}
	    }
	}

	if (internalCount > 0)
	    refreshBom();

	writeToManifest();
	return count;
    }

    /**
     * This builds an ID map, because our search discoveries will be found based
     * on IDs, not maps. This also informs the user whether their name was
     * correct
     * 
     * @param list
     * @throws Exception
     */
    private void buildSearchNameCache(List<String> list) throws Exception {
	try {
	    for (String name : list) {
		StringSearchPattern pattern = protexServer
			.getInternalApiWrapper().getProjectApi()
			.getStringSearchPatternByName(super.projId, name);

		if (pattern != null) {
		    String searchPatternId = pattern.getStringSearchPatternId();
		    Integer count = searchNameIdMap.get(searchPatternId);
		    if (count == null)
			searchNameIdMap.put(searchPatternId, new Integer(0));
		} else {
		    log.warn("Unable to find pattern with the name you specified, please correct spelling: "
			    + name);
		}

	    }
	} catch (Exception e) {
	    log.warn("Unable to find pattern with the name you specified!", e);
	}
    }

    @Override
    public int process() {
	try {
	    count = traverseCodeTree(projPath);

	} catch (Exception e) {
	    log.error("Error while clearing pending IDs", e);
	}

	log.info(count
		+ " pending identification(s) have been cleared by Search");

	return count;
    }

    /**
     * Checks to see if there are pending search discoveries that are discovered
     * based on the user specified search pattern.
     * 
     * @param tree
     * @return
     * @throws SdkFault
     */
    public boolean hasPendingIds(PartialCodeTree tree) throws SdkFault {
	boolean hasPending = false;
	try {
	    // First figure out if there are pending matches
	    PartialCodeTreeWithCount treeWithCount = protexServer
		    .getInternalApiWrapper().getDiscoveryApi()
		    .getStringSearchPendingIdFileCount(projId, tree);

	    if (treeWithCount.getNodes().get(0).getCount() > 0) {
		return true;
	    }

	} catch (Exception e) {
	    log.warn("Unable to get search discovered pending id count: "
		    + e.getMessage());
	    return false;
	}

	return hasPending;
    }

    /**
     * Looks up path to see if the discovery is part of a search match
     * 
     * @param tree
     * @param path
     * @return
     * @throws SdkFault
     */
    public boolean condition(PartialCodeTree tree, String path) throws SdkFault {
	boolean searchNameMatch = false;
	if (searchNames.size() == 0)
	    return searchNameMatch;

	try {
	    List<StringSearchPatternOriginType> patternTypes = new ArrayList<StringSearchPatternOriginType>();
	    patternTypes.add(StringSearchPatternOriginType.CUSTOM);
	    patternTypes.add(StringSearchPatternOriginType.STANDARD);
	    patternTypes.add(StringSearchPatternOriginType.PROJECT_LOCAL);

	    List<StringSearchDiscovery> searchDiscoveries = protexServer
		    .getInternalApiWrapper().getDiscoveryApi()
		    .getStringSearchDiscoveries(projId, tree, patternTypes);

	    for (StringSearchDiscovery discovery : searchDiscoveries) {
		String searchId = discovery.getStringSearchId();
		Integer count = searchNameIdMap.get(searchId);
		if (count != null) {
		    // Set the count for tracking
		    log.debug("Found discovery based on search id: " + searchId);
		    searchNameIdMap.put(searchId, ++count);
		    searchNameMatch = true;
		    break;
		}
	    }

	} catch (SdkFault e) {
	    log.error("Error getting search discoveries: " + e.getMessage());
	    searchNameMatch = false;
	}

	return searchNameMatch;
    }

    /**
     * Right now, we are going to just declare this file and be done with it.
     * However in time, we will need to be more judicious and perform the
     * correct type of identification request.
     */
    public void idToComponent(String path, Discovery target) throws SdkFault {

	idComponentToDeclare(path);

	// Removing a block of code that attempted to set an identification, but it was buggy and did not work
	// Look into this when upgrading to 7.x	
    }

}
