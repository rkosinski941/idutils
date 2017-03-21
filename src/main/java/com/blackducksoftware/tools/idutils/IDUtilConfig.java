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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;

/**
 * Master configuration class
 * 
 * @author Ari Kamen
 * 
 */

public class IDUtilConfig extends ConfigurationManager {

    private final Logger log = Logger.getLogger(this.getClass().getName());

    // //////////////////////
    // // Config file values/
    // //////////////////////
    private String projName;
    private String projPath;

    private String compId;
    private String compName;
    private String compVersion;
    private String license;
    private String usage;

    private Boolean skipValidation = false;

    private Boolean updateOnly = false; // default behavior is false
    private ArrayList<String> extensionList = new ArrayList<String>();
    private Boolean libLoc = false;
    private Boolean libLocSingleProject = false;
    private List<String> targetStrings;

    // Config Reporting Options
    private Boolean reportMode = false;
    private Boolean reportFilePending = false;

    // List of projects for that user.
    private List<ProtexProjectPojo> projectList;

    // Derived values
    private String projId;
    private File outputManifestFile;

    public IDUtilConfig(String propFile) {
	super(propFile, APPLICATION.PROTEX);
	initLocalProperties();
	setupManifestFile();

    }

    private void setupManifestFile() {

	File outputManifestFile = new File(IDUtilsConstants.MANIFEST_PREFIX
		+ System.currentTimeMillis() + ".txt");
	setOutputManifestFile(outputManifestFile);
    }

    private void initLocalProperties() {
	// Required
	projName = getProperty(IDUtilsConstants.PROJECT_NAME);
	projPath = getProperty(IDUtilsConstants.PROJECT_PATH);
	if (projPath == null)
	    projPath = "/";

	if (projPath.length() > 1) {
	    if (projPath.endsWith("/")) {
		log.warn("Protex paths should not have trailing slahes.  Removing.");
		projPath = projPath.substring(0, projPath.length() - 1);
	    }
	}

	// //////////////
	// Optional
	// /////////////

	// Login behavior
	skipValidation = getOptionalProperty(
		IDUtilsConstants.PERFORM_VALIDATION, true, Boolean.class);

	// Search matching
	String searchNameList = getOptionalProperty(IDUtilsConstants.SEARCH_NAMES);
	setTargetStrings(setSearchNames(searchNameList));

	// Reporting
	reportMode = getOptionalProperty(IDUtilsConstants.REPORTING_MODE,
		false, Boolean.class);
	reportFilePending = getOptionalProperty(
		IDUtilsConstants.REPORTING_SECTION_PENDING, false,
		Boolean.class);

	// Component matching
	compId = getOptionalProperty("idutils.comp.id");
	compName = getOptionalProperty("idutils.comp.name");
	compVersion = getOptionalProperty("idutils.comp.version");
	license = getOptionalProperty("idutils.comp.license");
	usage = getOptionalProperty("idutils.comp.usage");
	if (usage == null || usage.length() == 0)
	    usage = "COMPONENT";

	String updateStr = getOptionalProperty("idutils.comp.update");
	if (updateStr != null) {
	    updateOnly = new Boolean(updateStr);
	}

	String extensions = getOptionalProperty("idutils.update.extensions");
	setExtensionList(processExtensionList(extensions));

	String libLocOption = getOptionalProperty("idutils.libloc");
	if (libLocOption != null && libLocOption.equals("true")) {
	    setLibLoc(new Boolean(libLocOption));
	    String libLocSingleProject = getOptionalProperty("idutils.libloc.single.project");
	    if (libLocSingleProject != null
		    && libLocSingleProject.equals("true")) {
		setLibLocSingleProject(true);
	    }
	}
    }

    /**
     * User specifies a comma delimmited list of search names, turn them into a
     * list
     * 
     * @param searchNameList
     * @return
     */
    private List<String> setSearchNames(String searchNameList) {
	List<String> searchList = new ArrayList<String>();

	if (searchNameList != null) {
	    String[] names = searchNameList.split(",");
	    for (String name : names)
		searchList.add(name.trim());
	}
	return searchList;
    }

    private ArrayList<String> processExtensionList(String extensions) {
	ArrayList<String> extensionList = new ArrayList<String>();

	if (extensions == null || extensions.length() == 0) {
	    log.warn("No extensions found at all.");
	    return extensionList;
	}
	StringTokenizer st = new StringTokenizer(extensions, ",");
	while (st.hasMoreElements()) {
	    String extension = st.nextToken();
	    log.debug("Found extension: " + extension);
	    extensionList.add(extension);
	}
	return extensionList;
    }

    public String getProjName() {
	return projName;
    }

    public void setProjName(String projName) {
	this.projName = projName;
    }

    public String getProjPath() {
	return projPath;
    }

    public void setProjPath(String projPath) {
	this.projPath = projPath;
    }

    public String getCompName() {
	return compName;
    }

    public void setCompName(String compName) {
	this.compName = compName;
    }

    public String getCompVersion() {
	return compVersion;
    }

    public void setCompVersion(String compVersion) {
	this.compVersion = compVersion;
    }

    public String getLicense() {
	return license;
    }

    public void setLicense(String license) {
	this.license = license;
    }

    public String getUsage() {
	return usage;
    }

    public void setUsage(String usage) {
	this.usage = usage;
    }

    public Boolean getUpdateOnly() {
	return updateOnly;
    }

    public void setUpdateOnly(Boolean updateOnly) {
	this.updateOnly = updateOnly;
    }

    public String getProjId() {
	return projId;
    }

    public void setProjId(String projId) {
	this.projId = projId;
    }

    public ArrayList<String> getExtensionList() {
	return extensionList;
    }

    public void setExtensionList(ArrayList<String> extensionList) {
	this.extensionList = extensionList;
    }

    public Boolean getLibLoc() {
	return libLoc;
    }

    public void setLibLoc(Boolean libLoc) {
	this.libLoc = libLoc;
    }

    public List<ProtexProjectPojo> getProjectList() {
	return projectList;
    }

    public void setProjectList(List<ProtexProjectPojo> projectList2) {
	this.projectList = projectList2;
    }

    public Boolean getLibLocSingleProject() {
	return libLocSingleProject;
    }

    public void setLibLocSingleProject(Boolean libLocSingleProject) {
	this.libLocSingleProject = libLocSingleProject;
    }

    public File getOutputManifestFile() {
	return outputManifestFile;
    }

    private void setOutputManifestFile(File outputManifestFile) {
	this.outputManifestFile = outputManifestFile;
    }

    public List<String> getSearchNames() {
	return targetStrings;
    }

    private void setTargetStrings(List<String> targetStrings) {
	this.targetStrings = targetStrings;
    }

    public Boolean isSkipValidation() {
	return skipValidation;
    }

    public void setSkipValidation(Boolean skipValidation) {
	this.skipValidation = skipValidation;
    }

    public String getCompId() {
	return compId;
    }

    public void setCompId(String compId) {
	this.compId = compId;
    }

    public Boolean isReportMode() {
	return reportMode;
    }

    public void setReportMode(Boolean reportMode) {
	this.reportMode = reportMode;
    }

    public Boolean isReportFilePending() {
	return reportFilePending;
    }

    public void setReportFilePending(Boolean reportFilePending) {
	this.reportFilePending = reportFilePending;
    }

}
