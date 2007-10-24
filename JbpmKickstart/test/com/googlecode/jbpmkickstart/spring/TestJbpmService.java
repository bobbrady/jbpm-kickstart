package com.googlecode.jbpmkickstart.spring;

import java.util.List;

import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.googlecode.jbpmkickstart.spring.JbpmService;
import com.googlecode.jbpmkickstart.spring.helper.SpringTestAdapter;

public class TestJbpmService extends SpringTestAdapter {
	/** Name of jBPM simple processes */
	private static final String PROCESS_NAME = "simple process";

	/** Name of jBPM process files */
	private static final String PROCESS_FILE = "com/googlecode/jbpmkickstart/spring/helper/simpleProcess.xml";
	
	/** ProcessDefinition to be resused in test cases */
	public ProcessDefinition processDefinition;

	/** Name of first node in jBPM simple process */
	public static final String START_NODE_NAME = "start";

	/** Name of second (middle) node in jBPM simple process */
	public static final String MIDDLE_NODE_NAME = "middle";

	/** Name of last (end) node in jBPM simple process */
	public static final String END_NODE_NAME = "end";

	/** The jBPM service under test */
	public JbpmService jbpmService;

	/**
	 * Sets up a new transaction for each test case.
	 * 
	 * @see edu.mit.broad.cbip.util.helpers.SpringTestCaseAdapter#callSetup()
	 */
	@Override
	@Before
	public void callSetup() throws Exception
	{
		super.callSetup();
	}

	/**
	 * Rolls back the transaction for each test case.
	 * 
	 * @see edu.mit.broad.cbip.util.helpers.SpringTestCaseAdapter#callTearDown()
	 */
	@Override
	@After
	public void callTearDown() throws Exception
	{
		super.callTearDown();
	}

	/**
	 * Verifies Spring framework correctly instantiated the service
	 */
	@Test
	public void testServiceInstantiation()
	{
		// Spring should have injected a jbpmTemplate into the service
		assertNotNull("Error: Spring failed to instantiate jbpmService bean", this.jbpmService);

		// Initially, shouldn't have any instances
		assertNull(this.jbpmService.getProcessInstance(1));
	}

	/**
	 * Verifies the parsing of a process definition in an xml file format
	 */
	@Test
	public void testParseXmlResource()
	{
		this.processDefinition = this.jbpmService.parseXMLResource(PROCESS_FILE);

		// Make sure basic preconditions are met
		assertNotNull("Error: ProcessDefinition unable to parse Simple Process name from XML file.",
									this.processDefinition.getName());
		assertTrue("Error: ProcessDefinition parsed empty name from XML file for Simple Process.",
								this.processDefinition.getName().length() > 0);
		assertTrue("Error: ProcessDefinition parsed erroneous name from XML file \"" + this.processDefinition.getName()
								+ "\" for Simple Process.", this.processDefinition.getName().equals(PROCESS_NAME));
		assertNotNull("Error: ProcessDefinition parsed Simple Process to have null start state node.",
									this.processDefinition.getStartState());
	}

	/**
	 * Verifies the loading of a process definition in an xml file format
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testDeployAndFindProcessDefinition()
	{
		this.processDefinition = this.jbpmService.parseXMLResource(PROCESS_FILE);

		this.jbpmService.deployProcessDefinition(this.processDefinition);

		ProcessDefinition processDefFromDb = this.jbpmService.findLatestProcessDefinition(this.processDefinition.getName());
		
		// All nodes equal?
		List<Node> origNodes = this.processDefinition.getNodes();
		List<Node> rndTripNodes = processDefFromDb.getNodes();
		assertTrue(origNodes.size() == rndTripNodes.size());
		int idx = 0;
		for (Object origObjNode : origNodes)
		{
			Node origNode = (Node) origObjNode;
			Node rndTripNode = (Node) (rndTripNodes.get(idx++));
			assertTrue(origNode.getName().equals(rndTripNode.getName()));
			assertTrue(origNode.getId() == rndTripNode.getId());
			assertTrue(origNode.getParent().getName().equals(rndTripNode.getParent().getName()));
		}
	}

	/**
	 * Verifies process instantiated and at start of execution
	 */
	@Test
	public void testProcessInstanceExec()
	{
		this.processDefinition = this.jbpmService.parseXMLResource(PROCESS_FILE);

		this.jbpmService.deployProcessDefinition(this.processDefinition);

		// Create the instance based on the latest definition
		ProcessInstance processInstance = this.jbpmService.createProcessInstance(this.processDefinition.getName());

		// Make sure instance was created
		assertNotNull("Error: jbpmService failed to create process instance for process definition \""
									+ this.processDefinition.getName() + "\".", processInstance);

		// Should be at start node initially
		assertEquals(START_NODE_NAME, processInstance.getRootToken().getNode().getName());

		// Let's start the process execution
		this.jbpmService.signalProcessInstance(processInstance.getId());

		// Now the process should be in the middle state.
		// Reload the instance through the service so root token gets updated
		processInstance = this.jbpmService.getProcessInstance(processInstance.getId());
		assertEquals(MIDDLE_NODE_NAME, processInstance.getRootToken().getNode().getName());

		// Leave middle node and transition to the end node.
		this.jbpmService.signalProcessInstance(processInstance.getId());

		// After signal, should be at end-state of simple process
		assertTrue(this.jbpmService.hasProcessInstanceEnded(processInstance.getId()));


		// Verify we can retrieve state from database
		// Again, reload the instance through the service so root token is updated
		processInstance = this.jbpmService.getProcessInstance(processInstance.getId());
		assertEquals(END_NODE_NAME, processInstance.getRootToken().getNode().getName());
	}
	
	/**
	 * Setter, needed for spring injection
	 * 
	 * @param jbpmService
	 */
	public void setJbpmService(JbpmService jbpmService)
	{
		this.jbpmService = jbpmService;
	}
}
