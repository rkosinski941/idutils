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
package com.blackducksoftware.tools.idutils.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.common.BomRefreshMode;
import com.blackducksoftware.sdk.protex.common.UsageLevel;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNode;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNodeType;
import com.blackducksoftware.sdk.protex.project.codetree.PartialCodeTree;
import com.blackducksoftware.sdk.protex.project.codetree.identification.CodeMatchIdentification;
import com.blackducksoftware.sdk.protex.project.codetree.identification.CodeMatchIdentificationRequest;
import com.blackducksoftware.sdk.protex.project.codetree.identification.CodeTreeIdentificationInfo;
import com.blackducksoftware.sdk.protex.project.codetree.identification.DeclaredIdentification;
import com.blackducksoftware.sdk.protex.project.codetree.identification.DeclaredIdentificationRequest;
import com.blackducksoftware.sdk.protex.project.codetree.identification.Identification;
import com.blackducksoftware.sdk.protex.project.codetree.identification.IdentificationType;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.idutils.IDUtilConfig;

/**
 * Worker class that is responsible for iterating through a 'Protex' project and
 * updating components based on user configurations.
 * 
 * User configuration checking should be done here, including all log warning of
 * certain dangerous combinations.
 * 
 * @author Ari Kamen
 * 
 */

public class ComponentUpdater {

    private final Logger log = Logger.getLogger(this.getClass().getName());

    private final IDUtilConfig config;
    private final ProtexServerWrapper<ProtexProjectPojo> protexServer;

    public ComponentUpdater(IDUtilConfig configManager) throws Exception {

	this.config = configManager;
	this.protexServer = new ProtexServerWrapper<ProtexProjectPojo>(config.getServerBean(),
		config, true);

    }

    public void updateMatches() throws Exception {
	String projId = config.getProjId();
	String configUsage = config.getUsage();
	String projPath = config.getProjPath();

	// Check for configuration
	String component = config.getCompName();
	if (component != null && component.length() > 0) {
	    throw new Exception(
		    "You are trying to update existing IDs to a component: "
			    + component
			    + ". That functionality does not exist.");
	}

	ArrayList<String> extensions = config.getExtensionList();

	log.info("Updating nodes for folder: " + projPath);
	log.info("Applying component usage: " + config.getUsage());

	PartialCodeTree ourSingletonTree = new PartialCodeTree();

	/**
	 * Setting the tree path to root since the identificationAPI will use
	 * the parent path plus full node path to resolve.
	 */
	ourSingletonTree.setParentPath("/");

	ourSingletonTree = buildWorkTree(projPath, projId, ourSingletonTree,
		extensions);

	if (ourSingletonTree != null) {
	    log.info("Number of files planning to process: "
		    + ourSingletonTree.getNodes().size());

	    List<CodeTreeIdentificationInfo> idInfos = null;
	    try {
		// TODO: More root path workaround nonsense.
		if (ourSingletonTree.getParentPath().equals("/"))
		    ourSingletonTree.setParentPath("");

		idInfos = protexServer.getInternalApiWrapper().getIdentificationApi()
			.getAppliedIdentifications(projId, ourSingletonTree);
	    } catch (SdkFault e) {
		log.error("Could not obtain  identifications for path: "
			+ projPath);

		// Trying to provide an explanation to end user, really wish we
		// had something in the SDK to do this. --AK
		if (e.getMessage().contains("nodes ="))
		    log.error("***Most likely the path you provided does not exist.***");

		throw new Exception(e);
	    }

	    for (CodeTreeIdentificationInfo idInfo : idInfos) {

		List<Identification> ids = idInfo.getIdentifications();

		for (Identification id : ids) {
		    log.info("Identified File to be modified: "
			    + idInfo.getName());

		    log.info("Current Component ID: "
			    + id.getIdentifiedComponentId());
		    log.info("Current Usage level: "
			    + id.getIdentifiedUsageLevel().name());

		    if (id.getIdentifiedLicenseInfo() != null)
			log.info("Current License: "
				+ id.getIdentifiedLicenseInfo().getName());

		    id.setIdentifiedUsageLevel(UsageLevel.valueOf(configUsage));

		    log.info("Updated usage level: "
			    + id.getIdentifiedUsageLevel().name());

		    if (id.getType() == IdentificationType.DECLARATION) {
			DeclaredIdentification did = (DeclaredIdentification) id;
			DeclaredIdentificationRequest req = new DeclaredIdentificationRequest();

			req.setIdentifiedComponentId(did
				.getIdentifiedComponentId());
			req.setIdentifiedLicenseInfo(did
				.getIdentifiedLicenseInfo());
			req.setIdentifiedUsageLevel(did
				.getIdentifiedUsageLevel());
			req.setIdentifiedVersionId(did.getIdentifiedVersionId());

			req.setComment(did.getComment());

			protexServer.getInternalApiWrapper().getIdentificationApi()
				.addDeclaredIdentification(projId,
					did.getAppliedToPath(), req,
					BomRefreshMode.SKIP);

			log.info("Updated: " + did.getAppliedToPath());
		    } else if (id.getType() == IdentificationType.CODE_MATCH) {
			CodeMatchIdentification cid = (CodeMatchIdentification) id;

			CodeMatchIdentificationRequest req = new CodeMatchIdentificationRequest();
			req.setCodeMatchIdentificationDirective(cid
				.getCodeMatchIdentificationDirective());

			req.setIdentifiedComponentId(cid
				.getIdentifiedComponentId());
			req.setIdentifiedLicenseInfo(cid
				.getIdentifiedLicenseInfo());
			req.setIdentifiedUsageLevel(cid
				.getIdentifiedUsageLevel());
			req.setIdentifiedVersionId(cid.getIdentifiedVersionId());

			req.setDiscoveredComponentId(cid
				.getDiscoveredComponentId());
			req.setDiscoveredVersionId(cid.getDiscoveredVersionId());

			req.setComment(cid.getComment());

			protexServer.getInternalApiWrapper().getIdentificationApi()
				.addCodeMatchIdentification(projId,
					cid.getPath(), req, BomRefreshMode.SKIP);

			log.info("Updated: " + cid.getPath());

		    }
		}
	    }

	    try {
		log.info("Start Refreshing BOM...");
		protexServer.getInternalApiWrapper().getBomApi().refreshBom(projId,
			true, false);
		log.info("End Refreshing BOM...");
	    } catch (SdkFault e) {
		log.error("Error Refreshing BOM for project " + projId, e);
	    }
	}
    }

    /**
     * This constructs our own custom tree consisting of elements that we are
     * interested in working on.
     * 
     * The elements are filtered to ensure that our custom tree only contains
     * objects we are interested in modifying.
     * 
     * @param projPath
     * @param projId
     * @throws SdkFault
     */
    private PartialCodeTree buildWorkTree(String projPath, String projId,
	    PartialCodeTree ourSingletonTree, ArrayList<String> extensions)
	    throws SdkFault {
	PartialCodeTree tree = protexServer.getInternalApiWrapper().getCodeTreeApi()
		.getCodeTree(projId, projPath, 1, false);

	for (CodeTreeNode node : tree.getNodes()) {

	    // Quite messy sadly, but is a limitation of the SDK
	    if (node.getNodeType().equals(CodeTreeNodeType.FILE)) {
		if (filterNode(node, extensions)) {
		    String fullNodePath = "";
		    if (tree.getParentPath().equals("/"))
			fullNodePath = node.getName();
		    else
			fullNodePath = tree.getParentPath() + "/"
				+ node.getName();

		    log.info("Adding file: " + fullNodePath);
		    node.setName(fullNodePath);
		    ourSingletonTree.getNodes().add(node);
		}
	    } else {

		String fullNodePath = "";
		if (tree.getParentPath().equals("/"))
		    fullNodePath = "/" + node.getName();
		else
		    fullNodePath = tree.getParentPath() + "/" + node.getName();

		log.debug("Examining folder: " + fullNodePath);
		buildWorkTree(fullNodePath, projId, ourSingletonTree,
			extensions);
	    }

	}

	return ourSingletonTree;
    }

    /**
     * Filters the node based on user specifications
     * 
     * @param node
     * @return
     */
    private boolean filterNode(CodeTreeNode node, ArrayList<String> extensions) {
	boolean match = false;

	String nodeName = node.getName();
	// Loop through the list of extensions and match our node
	// TODO: Consider optimizing this by setting up a map of found
	// extensions to avoid looping through the list every time.
	for (String extension : extensions) {
	    if (nodeName.endsWith(extension)) {
		match = true;
		break;
	    }
	}

	return match;
    }
}
