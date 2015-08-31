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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.blackducksoftware.sdk.protex.common.Component;
import com.blackducksoftware.sdk.protex.common.UsageLevel;
import com.blackducksoftware.sdk.protex.component.version.ComponentVersion;
import com.blackducksoftware.sdk.protex.component.version.ComponentVersionApi;
import com.blackducksoftware.sdk.protex.project.ProjectApi;
import com.blackducksoftware.sdk.protex.project.bom.BomApi;
import com.blackducksoftware.sdk.protex.project.bom.BomComponent;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.common.ProjectPojo;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.idutils.IDUtilConfig;

/**
 * Handles the logical path that performs component location for a particular
 * server.
 * 
 * @author Ari Kamen
 * 
 */
public class ComponentLocator {

    private final Logger log = Logger.getLogger(this.getClass().getName());

    private final IDUtilConfig config;
    private final ProtexServerWrapper<ProtexProjectPojo> protexServer;

    private List<ComponentPojo> compPojoList = new ArrayList<ComponentPojo>();
    private String configUsage = "";

    public ComponentLocator(IDUtilConfig configManager) throws Exception {

	this.config = configManager;
	this.protexServer = new ProtexServerWrapper<ProtexProjectPojo>(
		configManager.getServerBean(), configManager, true);
    }

    public void findComponents() {
	if (config.getLibLocSingleProject()) {
	    log.info("Searching single project only...");
	    String projectName = config.getProjName();
	    findComponentsForProject(projectName, config.getProjId());
	    outputList(projectName);
	} else {
	    log.info("Searching all projects for user");
	    List<ProtexProjectPojo> projectList = config.getProjectList();
	    for (ProjectPojo pInfo : projectList) {
		String projectName = pInfo.getProjectName();
		String projectID = pInfo.getProjectKey();

		findComponentsForProject(projectName, projectID);
		outputList(projectName);
		compPojoList.clear();
	    }
	}
    }

    public void findComponentsForProject(String projectName, String projectID) {
	configUsage = config.getUsage();

	if (configUsage == null || configUsage.length() == 0) {
	    log.error("No usage selected, nothing to look for.");
	    return;
	}

	try {
	    if (projectID == null)
		throw new Exception("Project ID is null");

	    log.info("Examining project: " + projectName);

	    BomApi bomApi = protexServer.getInternalApiWrapper().getBomApi();
	    ProjectApi projApi = protexServer.getInternalApiWrapper().getProjectApi();
	    ComponentVersionApi compVersionApi = protexServer
		    .getInternalApiWrapper().getComponentVersionApi();

	    List<BomComponent> bomComponents = bomApi
		    .getBomComponents(projectID);

	    log.info("This project has the following number of TOTAL components: "
		    + bomComponents.size());
	    log.info("Looking to see if usage matches: " + configUsage);

	    // Look through all the BOM components
	    for (BomComponent comp : bomComponents) {
		String compId = comp.getComponentId();

		List<UsageLevel> usageLevels = comp.getUsageLevels();

		if (usageLevels.size() > 0) {
		    ComponentPojo compPojo = null;

		    for (UsageLevel usage : usageLevels) {
			if (configUsage.equals(usage.value())) {
			    Component component = projApi.getComponentById(
				    projectID, compId);

			    compPojo = new ComponentPojo();
			    compPojo.setComponentName(component.getName());
			    compPojo.setUsageList(usageLevels);
			    compPojo.setInternalComponent(component);

			    String versionID = comp.getVersionId();
			    String versionName = "";

			    try {
				ComponentVersion aComponentVersion = compVersionApi
					.getComponentVersionById(
						component.getComponentId(),
						versionID);
				versionName = aComponentVersion
					.getVersionName();
			    } catch (Exception e) {
				log.debug("No version for component: "
					+ component.getName());
			    }

			    compPojo.setVersionName(versionName);

			    compPojoList.add(compPojo);
			}
		    }
		}
	    }

	    log.info("Finished looking.");

	} catch (Exception e) {
	    log.error("Error while finding components", e);
	}

    }

    public void outputList(String projectName) {
	log.info("***List of matching components for project: " + projectName
		+ "***");
	log.info("These components all have at least one match to usage type: "
		+ configUsage);
	long total = 0;
	for (ComponentPojo compPojo : compPojoList) {
	    log.info(compPojo.toString());
	    total++;
	}

	log.info("***Total count: " + total + " for project: " + projectName
		+ "***");
    }

}
