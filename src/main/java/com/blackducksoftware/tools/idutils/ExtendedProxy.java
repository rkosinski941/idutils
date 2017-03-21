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