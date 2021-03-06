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
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNode;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNodeType;
import com.blackducksoftware.sdk.protex.project.codetree.PartialCodeTree;
import com.blackducksoftware.sdk.protex.project.codetree.PartialCodeTreeWithCount;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.Discovery;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.idutils.IDUtilConfig;

/**
 * It is unclear what this class actually does other than the name of it.
 * Although it is supposed to clear it for pattern matches, which patterns? Are
 * the patterns specified? TODO: Investigate
 * 
 * @author akamen
 * 
 */
public class PendingIdClearerForPatternMatches extends IdClearer {
    private final Logger log = Logger.getLogger(this.getClass().getName());
    private int count = 0;

    /**
     * Protex Server is still initialized twice when CodeMatch clearer invokes
     * this constructor.
     * 
     * @param configManager
     * @param protexServer
     * @throws Exception
     */
    public PendingIdClearerForPatternMatches(IDUtilConfig configManager,
	    ProtexServerWrapper<ProtexProjectPojo> protexServer) throws Exception {
	super(configManager, protexServer);
    }

    @Override
    public int process() {
	try {
	    // Do regular clearing
	    count = traverseCodeTree(projPath);
	    // Do it for patterns
	} catch (Exception e) {
	    log.error("Error while clearing pending IDs", e);
	}

	log.info(count
		+ " pending identification(s) have been cleared by Pattern clearer.");

	return count;
    }

    /**
     * Get a better understanding of what this does. Why is this different than
     * the CodeMatch? Other than the two trees
     * 
     * @param projPath
     * @return
     * @throws Exception
     */
    public int traverseCodeTree(String projPath) throws Exception {
	log.info("Traversing code for pattern matches");
	PartialCodeTree tree = protexServer.getInternalApiWrapper().getCodeTreeApi()
		.getCodeTree(projId, projPath, 1, false);
	PartialCodeTree tree0 = protexServer.getInternalApiWrapper().getCodeTreeApi()
		.getCodeTree(projId, projPath, 0, true);

	// boolean pendingthislevel = hasPendingIds(tree);
	boolean pendingatdepth = hasPendingIds(tree0);

	if (pendingatdepth) {
	    int internalcount = count;

	    for (CodeTreeNode node : tree.getNodes()) {

		if (node.getNodeType().equals(CodeTreeNodeType.FILE)) {
		    if (pendingatdepth) {
			handleFilesForPatterns(tree.getParentPath(), node);
		    }

		} else // for folders
		{
		    PartialCodeTree folderTree = new PartialCodeTree();
		    folderTree.setParentPath(tree.getParentPath());
		    folderTree.getNodes().add(node);

		    if (hasPendingIds(folderTree)) {
			handleFolderForPatterns(tree.getParentPath()
				+ (projPath.equals("/") ? "" : "/")
				+ node.getName());

		    }
		}
	    }

	    if (count > internalcount) {
		super.refreshBom();
	    }
	}
	super.writeToManifest();
	return count;
    }

    private void handleFolderForPatterns(String path) throws SdkFault {

	log.info("Inspecting folder: " + path);
	PartialCodeTree tree = protexServer.getInternalApiWrapper().getCodeTreeApi()
		.getCodeTree(projId, path, 1, false);

	PartialCodeTree tree0 = protexServer.getInternalApiWrapper().getCodeTreeApi()
		.getCodeTree(projId, path, 0, true);

	// boolean pendingthislevel = hasPendingIds(tree);
	boolean pendingatdepth = hasPendingIds(tree0);
	// String mesage = "diving " + pendingatdepth;
	// boolean pendingtree = hasPendingIds(tree);
	if (pendingatdepth) {
	    for (CodeTreeNode node : tree.getNodes()) {

		if (node.getNodeType().equals(CodeTreeNodeType.FILE)) {
		    // if(pendingatdepth)
		    handleFilesForPatterns(tree.getParentPath(), node);

		} else {

		    // if (pendingatdepth){
		    PartialCodeTree folderTree = new PartialCodeTree();
		    folderTree.setParentPath(tree.getParentPath());
		    folderTree.getNodes().add(node);

		    handleFolderForPatterns(tree.getParentPath() + "/"
			    + node.getName());
		    // }

		}

	    }

	}
    }

    private void handleFilesForPatterns(String parentPath, CodeTreeNode node)
	    throws SdkFault {
	String path = parentPath + (parentPath.equals("/") ? "" : "/")
		+ node.getName();

	PartialCodeTree tree = new PartialCodeTree();
	tree.setParentPath(parentPath);
	tree.getNodes().add(node);

	if (hasPendingIds(tree) && condition(tree, path)) {
	    log.info(path + " has pending ids");
	    idToComponent(path);
	    count++;

	    // Once the ID is made, add the path to eventual manifest write
	    super.savePathForManifest(path);
	} else {
	    // log.info(path + " does not have pending ids");
	}

    }

    /**
     * Pattern matches are declared
     * 
     * @param path
     * @throws SdkFault
     */
    private void idToComponent(String path) throws SdkFault {
	super.idComponentToDeclare(path);
    }

    public boolean hasPendingIds(PartialCodeTree tree) throws SdkFault {
	try {
	    PartialCodeTreeWithCount treeWithCount = protexServer
		    .getInternalApiWrapper().getDiscoveryApi()
		    .getFileDiscoveryPatternPendingIdFileCount(projId, tree);

	    return treeWithCount.getNodes().get(0).getCount() > 0;
	} catch (Exception e) {
	    return false;
	}

    }

    @Override
    public boolean condition(PartialCodeTree tree, String path) throws SdkFault {
	return true;
    }

    /**
     * Not used by Pattern Matcher
     */
    @Override
    public void idToComponent(String path, Discovery target) throws SdkFault {
	log.error("This code path should not be activated");

    }

}
