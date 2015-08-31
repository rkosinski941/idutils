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
/**
 * 
 */
package com.blackducksoftware.tools.idutils.clearer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import org.apache.log4j.Logger;

import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.common.BomRefreshMode;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNode;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNodeType;
import com.blackducksoftware.sdk.protex.project.codetree.PartialCodeTree;
import com.blackducksoftware.sdk.protex.project.codetree.identification.CodeMatchIdentification;
import com.blackducksoftware.sdk.protex.project.codetree.identification.CodeTreeIdentificationInfo;
import com.blackducksoftware.sdk.protex.project.codetree.identification.DeclaredIdentification;
import com.blackducksoftware.sdk.protex.project.codetree.identification.Identification;
import com.blackducksoftware.sdk.protex.project.codetree.identification.IdentificationType;
import com.blackducksoftware.sdk.protex.project.codetree.identification.StringSearchIdentification;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.idutils.IDUtilConfig;

/**
 * Uses a manifest file to reverse the pending ID process
 * 
 * @author akamen
 * 
 */
public class PendingIdClearerReverse {
    
    private final Logger log = Logger.getLogger(this.getClass().getName());
    private final IDUtilConfig configManager;
    private final ProtexServerWrapper<ProtexProjectPojo> protexServer;
    
    private String projectID;
    private File manifest;

    public PendingIdClearerReverse(IDUtilConfig config, String manifestFile)
	    throws Exception {
	verifyManifest(manifestFile);
	configManager = config;
	protexServer = new ProtexServerWrapper<ProtexProjectPojo>(config.getServerBean(), config,
		config.isSkipValidation());
	projectID = configManager.getProjId();
    }

    private void verifyManifest(String manifestFile) throws Exception {
	manifest = new File(manifestFile);
	if (!manifest.exists())
	    throw new Exception("Unable to find manifest file at location: "
		    + manifestFile);

    }

    /**
     * Clears out all the identifications made that were listed in the manifest
     * file.
     * 
     * @return
     * @throws Exception
     */
    public int reverse() throws Exception {

	BufferedReader br = null;
	int deleteCount = 0;

	try {
	    try {
		br = new BufferedReader(new FileReader(manifest));
	    } catch (FileNotFoundException e) {
		throw new Exception("Could not read file: " + manifest, e);

	    }

	    String strLine = "";

	    try {
		while ((strLine = br.readLine()) != null) {
		    if (!strLine.equals("")) {
			log.info("Processing ID: " + strLine);
			deleteCount += undo(strLine);
		    }

		}
	    } catch (Exception e) {
		log.warn("Could not process ID: " + strLine + " : "
			+ e.getMessage());
	    }

	    try {
		log.info("Refreshing BOM");
		protexServer.getInternalApiWrapper().getBomApi().refreshBom(
			projectID, true, false);
	    } catch (SdkFault e) {
		throw new Exception("Error refreshing BOM for project "
			+ projectID, e);
	    }
	} catch (Exception e) {
	    throw new Exception("Error while cleaning up", e);
	}
	finally
	{
	    br.close();
	}

	log.info("Delete count is: " + deleteCount);

	return deleteCount;
    }

    private int undo(String path) throws Exception {
	int deleteCount = 0;

	CodeTreeNode node = new CodeTreeNode();
	node.setName(path);
	node.setNodeType(CodeTreeNodeType.FILE);

	log.info("Getting applied identifications for file: " + node.getName());

	PartialCodeTree singletonTree = new PartialCodeTree();
	singletonTree.setParentPath("");
	singletonTree.getNodes().add(node);

	List<CodeTreeIdentificationInfo> idInfos = null;
	try {
	    idInfos = protexServer.getInternalApiWrapper().getIdentificationApi()
		    .getAppliedIdentifications(projectID, singletonTree);
	} catch (SdkFault e) {
	    throw new Exception(
		    "Could not obtain applied identifications for: " + path, e);

	}

	for (CodeTreeIdentificationInfo idInfo : idInfos) {
	    List<Identification> ids = idInfo.getIdentifications();

	    for (Identification id : ids) {

		if (id.getType() == IdentificationType.DECLARATION) {
		    try {
			log.info("removing declared identification to "
				+ id.getIdentifiedComponentId() + " for "
				+ path);
			protexServer.getInternalApiWrapper().getIdentificationApi()
				.removeDeclaredIdentification(projectID,
					(DeclaredIdentification) id,
					BomRefreshMode.SKIP);
			deleteCount++;
		    } catch (SdkFault e) {
			log.error(
				"Could not remove declared identifications for: "
					+ path, e);

		    }

		} else if (id.getType() == IdentificationType.CODE_MATCH) {
		    try {
			log.info("removing code match identification to "
				+ id.getIdentifiedComponentId() + " for "
				+ path);
			protexServer.getInternalApiWrapper().getIdentificationApi()
				.removeCodeMatchIdentification(projectID,
					(CodeMatchIdentification) id,
					BomRefreshMode.SKIP);
			deleteCount++;
		    } catch (SdkFault e) {
			log.error(
				"Could not remove code match identifications for: "
					+ path, e);
		    }

		} else if (id.getType() == IdentificationType.STRING_SEARCH) {
		    try {
			log.info("Removing search match identification for:"
				+ path);

			String idEmpty = id.getIdentifiedComponentId();
			if (idEmpty == null)
			    id.setIdentifiedComponentId(projectID);

			protexServer.getInternalApiWrapper().getIdentificationApi()
				.removeStringSearchIdentification(projectID,
					(StringSearchIdentification) id,
					BomRefreshMode.SKIP);

			deleteCount++;
		    } catch (SdkFault e) {
			log.error(
				"Could not remove search match identifications for: "
					+ path, e);
		    }
		}
	    }
	}

	return deleteCount;
    }
}
