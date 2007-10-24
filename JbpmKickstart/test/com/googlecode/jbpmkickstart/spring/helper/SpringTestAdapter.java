package com.googlecode.jbpmkickstart.spring.helper;

/*
 * File: SpringTestAdpater.java
 *
 * Author: $Author
 * 
 * $Date$ $Revision$
 */
import org.junit.After;
import org.junit.Before;
import org.junit.internal.runners.TestClassRunner;
import org.junit.runner.RunWith;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Adapts AbstractDependencyInjectionSpringContextTests to JUnit 4 
 * Necessary because the above spring class inherits from JUnit 3 junit.framework.TestCase
 * 
 * <ul>
 * 	<li>Forces JUnit 4 to be used via RunWith annotation set to TestClassRunner</li>
 * 	<li>Keeps intact non-annotated JUnit 3 methods setUp() and tearDown()</li>
 * </ul>
 */

@RunWith(TestClassRunner.class)
public class SpringTestAdapter extends AbstractDependencyInjectionSpringContextTests
{
	
	/**
	 * Adapt to non-annotated JUnit 3 setUp()
	 * @throws Exception
	 */
	@Before public void callSetup() throws Exception
	{
		super.setUp();
	}

	
	/**
	 * Adap to non-annotated JUnit 3 tearDown()
	 * @throws Exception
	 */
	@After public void callTearDown() throws Exception
	{
		super.tearDown();
	}
	
	@Override
	protected String[] getConfigLocations()
	{
		return new String[] {"classpath:applicationContext.xml"};
	}

}


