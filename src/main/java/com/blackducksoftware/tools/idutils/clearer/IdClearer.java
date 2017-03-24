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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.common.BomRefreshMode;
import com.blackducksoftware.sdk.protex.common.Component;
import com.blackducksoftware.sdk.protex.common.ComponentType;
import com.blackducksoftware.sdk.protex.common.StringSearchPatternOriginType;
import com.blackducksoftware.sdk.protex.common.UsageLevel;
import com.blackducksoftware.sdk.protex.component.custom.CustomComponent;
import com.blackducksoftware.sdk.protex.component.standard.StandardComponent;
import com.blackducksoftware.sdk.protex.component.version.ComponentVersion;
import com.blackducksoftware.sdk.protex.license.GlobalLicense;
import com.blackducksoftware.sdk.protex.license.LicenseInfo;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNode;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNodeType;
import com.blackducksoftware.sdk.protex.project.codetree.PartialCodeTree;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.CodeMatchDiscovery;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.CodeMatchType;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.Discovery;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.IdentificationStatus;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.StringSearchDiscovery;
import com.blackducksoftware.sdk.protex.project.codetree.identification.CodeMatchIdentificationDirective;
import com.blackducksoftware.sdk.protex.project.codetree.identification.CodeMatchIdentificationRequest;
import com.blackducksoftware.sdk.protex.project.codetree.identification.DeclaredIdentificationRequest;
import com.blackducksoftware.sdk.protex.project.codetree.identification.IdentificationRequest;
import com.blackducksoftware.sdk.protex.project.localcomponent.LocalComponent;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.common.ProjectPojo;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.idutils.IDUtilConfig;
import com.blackducksoftware.tools.idutils.component.ComponentPojo;

/**
 * Helper abstract class that houses common private members
 * 
 * 
 * 
 * @author akamen
 * 
 */
public abstract class IdClearer {
    private final Logger log = Logger.getLogger(this.getClass().getName());

    protected int count = 0;
	
    // "dirty" bit meaning an ID was done so now we need to refresh the BOM
    // preferably after we just finished doing IDs to a directory.
    protected boolean needsRefresh = false;

    // FInalize both of these to make sure they do not get instantiated more than once.
    protected final IDUtilConfig configManager;
    protected final ProtexServerWrapper<ProtexProjectPojo> protexServer;

    // This is the optional component that the user specifies
    protected ComponentPojo ourComponent;

    // Derived
    protected File outputManifestFile;
    protected String projId;

    // Properties
    protected String projPath;
    protected List<String> extensionList;
    protected List<String> searchNames;

    // Internal storage
    // This is for all the paths that have been identified
    private List<String> identifiedPaths = new ArrayList<String>();

    protected IdClearer(IDUtilConfig configManager, ProtexServerWrapper<ProtexProjectPojo> protexServer) throws Exception {
	this.configManager = configManager;
	this.protexServer = protexServer;

	initializeProjectInfo();
	initialize(configManager);
    }

    /**
     * Populates project Information
     * 
     * @throws Exception
     */
    private void initializeProjectInfo() throws Exception {
	// Find the project ID and set it to the config.
	String projectName = configManager.getProjName();
	try {
	    log.info("Getting project information...");

	    ProjectPojo projectPojo = protexServer
		    .getProjectByName(projectName);

	    if (projectPojo != null) {
		log.info("Found your project!  With id: "
			+ projectPojo.getProjectKey());
		configManager.setProjId(projectPojo.getProjectKey());
	    } else {
		throw new Exception(
			"Unable to determine project ID, please check project name.");
	    }
	    
	    List<ProtexProjectPojo> projectList = protexServer
		    .getProjects(ProtexProjectPojo.class);

	    configManager.setProjectList(projectList);

	} catch (Exception e) {
	    throw new Exception("Fatal: Could not find project ID for name: "
		    + projectName, e);
	}
    }

    /**
     * Sets internal config manager members for easy access to those classes
     * that extend
     * 
     * @param configManager
     * @param protexServer
     * @throws Exception
     */
    public void initialize(IDUtilConfig configManager) throws Exception {
	projPath = configManager.getProjPath();

	projId = configManager.getProjId();
	extensionList = configManager.getExtensionList();
	outputManifestFile = configManager.getOutputManifestFile();
	searchNames = configManager.getSearchNames();

	// These are optional strings for component name/license/version
	String userSuppliedId = configManager.getCompId();
	String userSuppliedLicense = configManager.getLicense();
	String userSuppliedVersion = configManager.getCompVersion();
	String userSuppliedUsage = configManager.getUsage();

	ourComponent = new ComponentPojo(projId);
	// Try to lookup the component
	if (userSuppliedId != null && userSuppliedId.length() > 0) {
	    try {
		Component component = protexServer.getInternalApiWrapper()
			.getProxy().getProjectApi()
			.getComponentById(projId, userSuppliedId);

		ComponentType type = component.getType();
		List<LicenseInfo> licenseInfos = new ArrayList<LicenseInfo>();
		if (type == ComponentType.STANDARD) {
		    StandardComponent sc = (StandardComponent) component;
		    licenseInfos = sc.getLicenses();
		} else if (type == ComponentType.CUSTOM) {
		    CustomComponent cc = (CustomComponent) component;
		    licenseInfos = cc.getLicenses();
		} else if (type == ComponentType.LOCAL) {
		    LocalComponent lc = (LocalComponent) component;

		    GlobalLicense gl = protexServer.getInternalApiWrapper()
			    .getLicenseApi().getLicenseById(lc.getLicenseId());
		    LicenseInfo linfo = new LicenseInfo();
		    linfo.setLicenseId(lc.getLicenseId());
		    linfo.setName(gl.getName());
		    licenseInfos.add(linfo);
		}

		// Potential version information
		// Tries to get the CV object using the supplied name, if name
		// is bad, we just move on.
		if (userSuppliedVersion != null
			&& userSuppliedVersion.length() > 0) {
		    try {
			ComponentVersion cv = protexServer
				.getInternalApiWrapper()
				.getComponentVersionApi()
				.getComponentVersionByName(component.getName(),
					userSuppliedVersion);

			ourComponent.setVersionId(cv.getVersionId());
			ourComponent.setVersionName(cv.getVersionName());
		    } catch (SdkFault e) {
			log.warn("Could not determine version ID for version name: "
				+ userSuppliedVersion);
		    }
		}

		// License Information
		LicenseInfo selectedLicenseInfo = null;
		if (userSuppliedLicense != null
			&& userSuppliedLicense.length() > 0) {
		    log.info("Looking up user supplied license: "
			    + userSuppliedLicense);
		    for (LicenseInfo potentialLic : licenseInfos) {
			if (potentialLic.getName()
				.contains(userSuppliedLicense)) {
			    selectedLicenseInfo = potentialLic;
			    break;
			}
		    }

		} else {
		    selectedLicenseInfo = licenseInfos.get(0);
		    if (licenseInfos.size() > 0)
			log.warn("Multiple licenses for this component, taking the first one: "
				+ selectedLicenseInfo.getName());

		}

		// Populate our component pojo
		ourComponent.setComponentId(component.getComponentId());
		ourComponent.setComponentName(component.getName());
		ourComponent.setLicense(selectedLicenseInfo);

	    } catch (SdkFault e) {
		log.error("Could not obtain component with ID: "
			+ userSuppliedId);
		log.error("Cause: " + e.getMessage());
	    }

	    // Usage
	    if (userSuppliedUsage != null) {
		UsageLevel ul = UsageLevel.valueOf(userSuppliedUsage);
		log.info("Using usage level: " + ul);
		ourComponent.setUsageLevel(ul);
	    }

	} else {
	    log.info("No component specified, will ID to original");
	}
    }

    /**
     * Different clearers have varying conditions they check against
     * 
     * @param tree
     * @param path
     * @return
     * @throws SdkFault
     */
    public abstract boolean condition(PartialCodeTree tree, String path)
	    throws SdkFault;

    public abstract int process();

    public abstract boolean hasPendingIds(PartialCodeTree tree) throws SdkFault;

    public abstract void idToComponent(String path, Discovery target)
	    throws SdkFault;

    public int traverseCodeTree(String projPath) throws Exception {

	PartialCodeTree root = protexServer.getInternalApiWrapper().getCodeTreeApi()
		.getCodeTree(projId, projPath, 0, true);
	
	int internalCount = 0;
	while (hasPendingIds(root)) {

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

	    refreshBom();
	}

	writeToManifest();
	return internalCount;
    }

    protected void writeToManifest() {
	try {
	    FileWriter fstream = new FileWriter(outputManifestFile, true);
	    BufferedWriter out = new BufferedWriter(fstream);
	    for (String path : identifiedPaths) {
		out.write(path);
		out.newLine();
	    }
	    out.close();
	} catch (IOException e) {
	    log.warn("Could not write to manifest file", e);
	}
    }

    protected int handleFolder(String path) throws SdkFault, Exception {
	int internalCount = 0;
	log.info("Inspecting folder: " + path);
	PartialCodeTree tree = protexServer.getInternalApiWrapper().getCodeTreeApi()
		.getCodeTree(projId, path, 1, false);

	PartialCodeTree tree0 = protexServer.getInternalApiWrapper().getCodeTreeApi()
		.getCodeTree(projId, path, 0, true);

	boolean pendingatdepth = hasPendingIds(tree0);

	for (CodeTreeNode node : tree.getNodes()) {

	    if (node.getNodeType() == CodeTreeNodeType.FILE) {
		if (pendingatdepth) {
		    internalCount += handleFiles(tree.getParentPath(), node);
		}
	    } else {

		if (pendingatdepth) {
		    PartialCodeTree folderTree = new PartialCodeTree();
		    folderTree.setParentPath(tree.getParentPath());
		    folderTree.getNodes().add(node);

		    internalCount += handleFolder(tree.getParentPath() + "/"
			    + node.getName());
		}
	    }

	}
	// We just came back from ID a directory, check to see if we performed and IDs
	// if we did, refresh the BOM
	if (needsRefresh) {
    	    log.info(internalCount
    			+ " pending identification(s) are being Cleaned");
    	    refreshBom();
        }
	return internalCount;
    }

    protected int handleFiles(String parentPath, CodeTreeNode node)
	    throws SdkFault {

	int internalCount = 0;
	String path = parentPath + (parentPath.equals("/") ? "" : "/")
		+ node.getName();

	PartialCodeTree tree = new PartialCodeTree();
	tree.setParentPath(parentPath);
	tree.getNodes().add(node);

	if (hasPendingIds(tree) && condition(tree, path)) {
	    log.info(path + " has pending ids");
	    // set the refresh bit to true so when the directory is completed the project will be refreshed
            needsRefresh=true;
	    List<CodeMatchType> codeMatchTypes = new ArrayList<CodeMatchType>();
	    codeMatchTypes.add(CodeMatchType.PRECISION);
	    List<CodeMatchDiscovery> codeDiscoveries = protexServer
		    .getInternalApiWrapper().getDiscoveryApi()
		    .getCodeMatchDiscoveries(projId, tree, codeMatchTypes);

	    List<StringSearchPatternOriginType> patternTypes = new ArrayList<StringSearchPatternOriginType>();
	    patternTypes.add(StringSearchPatternOriginType.CUSTOM);
	    patternTypes.add(StringSearchPatternOriginType.STANDARD);
	    patternTypes.add(StringSearchPatternOriginType.PROJECT_LOCAL);
	    List<StringSearchDiscovery> searchDiscoveries = protexServer
		    .getInternalApiWrapper().getDiscoveryApi()
		    .getStringSearchDiscoveries(projId, tree, patternTypes);

	    Discovery target = null;
	    for (CodeMatchDiscovery match : codeDiscoveries) {

		if (match.getIdentificationStatus() == IdentificationStatus.PENDING_IDENTIFICATION) {
		    target = match;
		    break;
		}
	    }

	    /**
	     * If the target is null, then it is only a search discovery - grab
	     * the first one. It does not matter at this juncture, because the
	     * search discovery is declared anyway. TODO: Not sure if this is
	     * the correct logic.
	     */
	    if (target == null) {
		for (StringSearchDiscovery match : searchDiscoveries) {
		    target = match;
		    break;
		}
	    }

	    // prevent having to take in a Code Match Discovery to Code Match
	    // Identify if none exists
	    if (target != null && condition(path)) {
		idToComponent(path, target);
		internalCount++;
		count++;
		// Once the ID is made, add the path to eventual manifest write
		savePathForManifest(path);
	    }
	} else {
	    // log.info(path + " does not have pending ids");

	}
	return internalCount++;
    }

    protected void savePathForManifest(String path) {
	identifiedPaths.add(path);
    }

    protected boolean condition(String path) {
	if (extensionList.isEmpty())
	    return true;
	else {
	    boolean match = false;
	    for (String extension : extensionList) {
		if (path.endsWith(extension)) {
		    match = true;
		    break;
		}
	    }
	    return match;
	}
    }

    public void refreshBom() throws Exception {
	log.info("Refreshing BOM to account for new pending IDs made");
	try {
	    // IDUtilsHarness.bomApi.refreshBom(projId, false, false);
	    Date d = new Date();
	    Long start = d.getTime();
	    protexServer.getInternalApiWrapper().getBomApi().refreshBom(
		    configManager.getProjId(), true, false);
	    // clear the refresh bit
	    needsRefresh = false;
	    d = new Date();
	    Long finish = d.getTime();
	    Long refreshmill = finish - start;
	    int refreshsec = (int) (refreshmill / 1000);
	    log.info("Refresh time=" + refreshsec + " sec");
	} catch (SdkFault e) {
	    throw new Exception("Error refreshing BOM for project "
		    + configManager.getProjId(), e);
	}

    }

    /**
     * Performs a Code Match identification Shared by multiple clearers
     * 
     * @param path
     * @param target
     * @throws SdkFault
     */
    public void idComponentToCodeMatch(String path, Discovery discoveryTarget)
	    throws SdkFault {
	CodeMatchDiscovery target = (CodeMatchDiscovery) discoveryTarget;
	CodeMatchIdentificationRequest idRequest = new CodeMatchIdentificationRequest();
	idRequest
		.setCodeMatchIdentificationDirective(CodeMatchIdentificationDirective.SNIPPET_AND_FILE);

	idRequest.setDiscoveredComponentId(target.getMatchingComponentId());
	idRequest.setDiscoveredVersionId(target.getMatchingVersionId());

	populateIdentificationRequest(idRequest);

	idRequest.setComment("Code Match Id-ed by IDUtils at " + new Date());

	// log.info("Adding Identification for " + path);
	protexServer.getInternalApiWrapper().getIdentificationApi()
		.addCodeMatchIdentification(projId, path, idRequest,
			BomRefreshMode.SKIP);

    }

    /**
     * Performs a declaration.
     * 
     * @param path
     * @throws SdkFault
     */
    public void idComponentToDeclare(String path) throws SdkFault {
	DeclaredIdentificationRequest declRequest = new DeclaredIdentificationRequest();

	populateIdentificationRequest(declRequest);

	declRequest.setComment("Declare Id-ed by IDUtils at \" + new Date()");

	log.info("Adding Declaration for " + path);
	try {
	    protexServer.getInternalApiWrapper().getIdentificationApi()
		    .addDeclaredIdentification(projId, path, declRequest,
			    BomRefreshMode.SKIP);
	} catch (SdkFault e) {
	    log.error("Error performing declaration: " + e.getMessage());
	}

    }

    private IdentificationRequest populateIdentificationRequest(
	    IdentificationRequest req) {
	log.info("Setting request to component: " + ourComponent.toString());

	req.setIdentifiedComponentId(ourComponent.getComponentId());
	req.setIdentifiedUsageLevel(ourComponent.getUsageLevel());
	req.setIdentifiedVersionId(ourComponent.getVersionId());
	req.setIdentifiedUsageLevel(ourComponent.getUsageLevel());
	req.setIdentifiedLicenseInfo(ourComponent.getLicense());

	return req;
    }

}
