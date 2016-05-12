package com.blackducksoftware.tools.idutils.component;

import org.junit.Test;

import junit.framework.Assert;

public class ComponentPojoTest {
	@Test
	public void testObjectCreated() {
		// just a simple test to get coverage reports to run
		final ComponentPojo componentPojo = new ComponentPojo();
		Assert.assertNotNull(componentPojo);
	}

}
