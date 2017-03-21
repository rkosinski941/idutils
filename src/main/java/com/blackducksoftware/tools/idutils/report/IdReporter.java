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
/**
 * 
 */
package com.blackducksoftware.tools.idutils.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNode;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNodeType;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNodeWithCount;
import com.blackducksoftware.sdk.protex.project.codetree.PartialCodeTree;
import com.blackducksoftware.sdk.protex.project.codetree.PartialCodeTreeWithCount;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.Discovery;
import com.blackducksoftware.sdk.protex.util.CodeTreeUtilities;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.idutils.IDUtilConfig;
import com.blackducksoftware.tools.idutils.clearer.IdClearer;

/**
 * @author Ari Kamen
 * @date Dec 17, 2014
 * 
 */
public class IdReporter extends IdClearer {
    
    private final Logger log = Logger.getLogger(this.getClass().getName());

    private final List<String> pathsToReport = new ArrayList<String>();

    private final Map<String, Boolean> processedFolders = new HashMap<String, Boolean>();

    /**
     * @param cf - Configuration Manager
     * @param psw
     * @throws Exception
     */
    public IdReporter(IDUtilConfig configManager, ProtexServerWrapper<ProtexProjectPojo> protexServer)
	    throws Exception {
	super(configManager, protexServer);
    }

    @Override
    public boolean condition(PartialCodeTree tree, String path) throws SdkFault {
	return false;
    }

    @Override
    public int process() {
	try {
	    PartialCodeTree tree = protexServer.getInternalApiWrapper().getCodeTreeApi()
		    .getCodeTree(projId, projPath, 1, false);

	    log.info("Scanning for remaining pending files in: " + projPath);
	    for (CodeTreeNode node : tree.getNodes()) {
		if (node.getNodeType() == CodeTreeNodeType.FOLDER) {
		    String root = tree.getParentPath();
		    if (root.equals("/"))
			root = root + node.getName();
		    handleFolder(root);
		}

		else
		    examineFiles(tree, node);

	    }
	} catch (Exception e) {
	    log.error("Error during tree traversal: " + e.getMessage());
	}

	reportPaths();

	return count;
    }

    /**
     * 
     */
    private void reportPaths() {
	if (pathsToReport.size() > 0) {
	    for (String path : pathsToReport) {
		log.info("Reporting path: " + path);
	    }
	    log.info("** Total count of collected paths: "
		    + pathsToReport.size() + "**");
	} else {
	    log.info("Nothing to report, no information collected.");
	}

    }

    protected int handleFolder(String path) throws SdkFault {
	int internalCount = 0;

	PartialCodeTree tree = protexServer.getInternalApiWrapper().getCodeTreeApi()
		.getCodeTree(projId, path, CodeTreeUtilities.DIRECT_CHILDREN,
			false);

	log.debug("Inspecting folder: " + path);
	for (CodeTreeNode node : tree.getNodes()) {
	    if (node.getNodeType() != CodeTreeNodeType.FOLDER) {
		examineFiles(tree, node);
	    } else {
		handleFolder(tree.getParentPath() + "/" + node.getName());
	    }
	}

	return internalCount;
    }


    /**
     * Examines the files within a particular tree Keeps track of which tree has
     * already been processed to avoid duplicate discovery lookups, as those are
     * very expensive.
     * 
     * @param tree
     * @param node
     * @return
     * @throws SdkFault
     */
    private int examineFiles(PartialCodeTree tree, CodeTreeNode node)
	    throws SdkFault {
	Boolean processed = processedFolders.get(tree.getParentPath());
	if (processed == null)
	    processedFolders.put(tree.getParentPath(), true);
	else
	    return -1;

	if (configManager.isReportFilePending()) {
	    PartialCodeTreeWithCount treeWithCount = protexServer
		    .getInternalApiWrapper().getDiscoveryApi()
		    .getAllDiscoveriesPendingIdFileCount(projId, tree);

	    for (CodeTreeNodeWithCount nodeWithCount : treeWithCount.getNodes()) {
		// Add only those that are still pending and are not folders,
		// since folder knowledge is redundant.
		if (nodeWithCount.getCount() > 0
			&& nodeWithCount.getNodeType() != CodeTreeNodeType.FOLDER) {
		    String path = treeWithCount.getParentPath() + "/"
			    + nodeWithCount.getName();
		    log.debug("Adding the path to pending list: " + path);
		    if (!pathsToReport.contains(path))
			pathsToReport.add(path);
		}
	    }

	}

	return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.blackducksoftware.proserv.idutils.clearer.IdClearer#hasPendingIds
     * (com.blackducksoftware.sdk.protex.project.codetree.PartialCodeTree)
     */
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

    @Override
    public void idToComponent(String path, Discovery target) throws SdkFault {
	// Nothing to do here

    }

}
