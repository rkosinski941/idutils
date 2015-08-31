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
