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
package com.blackducksoftware.tools.idutils;

import com.blackducksoftware.sdk.protex.client.util.ProtexServerProxyV6_3;

/**
 * A workaround for the CXF child size. In 7.0 this is part of the proxy object
 * 
 * @author Ari Kamen
 * @date Dec 18, 2014
 * 
 */
public class ExtendedProxy extends ProtexServerProxyV6_3 {

    private long maxChildElements;

    public ExtendedProxy(String serverUrl, String userName, String password,
	    long maxChildElements) {
	super(serverUrl, userName, password);
	this.maxChildElements = maxChildElements;
    }

    protected void instrumentService(Object serviceApi, String userName,
	    String password, long timeout) {
	super.instrumentService(serviceApi, userName, password, timeout);

	org.apache.cxf.endpoint.Client client = org.apache.cxf.frontend.ClientProxy
		.getClient(serviceApi);
	org.apache.cxf.endpoint.Endpoint cxfEndpoint = client.getEndpoint();

	cxfEndpoint.put("org.apache.cxf.stax.maxChildElements",
		maxChildElements);
    }
}