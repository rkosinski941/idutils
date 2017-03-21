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

import java.util.List;

import com.blackducksoftware.sdk.protex.common.Component;
import com.blackducksoftware.sdk.protex.common.UsageLevel;
import com.blackducksoftware.sdk.protex.license.LicenseInfo;

/**
 * Pojo to store component information
 * 
 * @author Ari Kamen
 * 
 */
public class ComponentPojo {

    private String componentName = "";
    private String version = "Unspecified";
    private String versionId;
    private String componentId;
    private Component internalComponent;
    private List<UsageLevel> usageList;
    private LicenseInfo license;
    private UsageLevel usageLevel = UsageLevel.ORIGINAL_CODE;

    public ComponentPojo() {
    }

    /**
     * This is the default ID, which should overwritten if a legitimate one
     * exists.
     * 
     * @param projId
     */
    public ComponentPojo(String projId) {
	setComponentId(projId);
    }

    public String getComponentName() {
	return componentName;
    }

    public void setComponentName(String componentName) {
	this.componentName = componentName;
    }

    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("Component Name: " + this.componentName);
	buf.append("\n");
	buf.append("Version: " + this.version);
	buf.append("\n");
	buf.append("Usage: " + this.usageLevel);
	buf.append("\n");
	if (license != null)
	    buf.append("License: " + this.license.getName());
	buf.append("\n");

	return buf.toString();
    }

    public Component getInternalComponent() {
	return internalComponent;
    }

    public void setInternalComponent(Component internalComponent) {
	this.internalComponent = internalComponent;
    }

    public List<UsageLevel> getUsageList() {
	return usageList;
    }

    public void setUsageList(List<UsageLevel> usageList) {
	this.usageList = usageList;
    }

    public String getVersionName() {
	return version;
    }

    public void setVersionName(String version) {
	this.version = version;
    }

    public LicenseInfo getLicense() {
	return license;
    }

    public void setLicense(LicenseInfo license) {
	this.license = license;
    }

    public UsageLevel getUsageLevel() {
	return usageLevel;
    }

    public void setUsageLevel(UsageLevel usageLevel) {
	this.usageLevel = usageLevel;
    }

    public String getComponentId() {
	return componentId;
    }

    public void setComponentId(String componentId) {
	this.componentId = componentId;
    }

    public String getVersionId() {
	return versionId;
    }

    public void setVersionId(String versionId) {
	this.versionId = versionId;
    }

}
