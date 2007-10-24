package com.googlecode.jbpmkickstart.spring;

import static com.googlecode.jbpmkickstart.spring.helper.MorphologyPatternDecisionHandler.variableName;
import static com.googlecode.jbpmkickstart.spring.helper.SetupAssayActionHandler.isSetupCompleted;
import static com.googlecode.jbpmkickstart.spring.helper.CleanupAssayAssignmentHandler.UNDERGRAD_ID;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.googlecode.jbpmkickstart.spring.helper.SpringTestAdapter;

public class TestRunAssayProcess extends SpringTestAdapter {
	
	/** log4j logger */
	private final static Logger log = Logger.getLogger(TestRunAssayProcess.class);
	
	/** Name of jBPM process files */
	private static final String PROCESS_FILE = "com/googlecode/jbpmkickstart/spring/helper/runAssay.xml";
	
	/** ProcessDefinition to be resused in test cases */
	private ProcessDefinition processDefinition;
	
	private ProcessInstance processInstance;
	
	private static final String CLEANUP_TASK_NAME = "Move plates back to home freezer and retire spent plates";
	
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
		this.processDefinition = this.jbpmService.parseXMLResource(PROCESS_FILE);

		this.jbpmService.deployProcessDefinition(this.processDefinition);

		// Create the instance based on the latest definition
		this.processInstance = this.jbpmService.createProcessInstance(this.processDefinition.getName());

		// Make sure instance was created
		assertNotNull("Error: jbpmService failed to create process instance for process definition \""
									+ this.processDefinition.getName() + "\".", processInstance);

		// Should be at start node initially
		assertEquals("Start Assay Run", processInstance.getRootToken().getNode().getName());
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
	
	@Test
	public void testSetupAssay()
	{
		// Let's start the process execution
		this.jbpmService.signalProcessInstance(this.processInstance.getId());

		// Now the process should be in setup state.
		// Reload the instance through the service so root token gets updated
		this.processInstance = this.jbpmService.getProcessInstance(this.processInstance.getId());
		assertEquals("Setup Assay", this.processInstance.getRootToken().getNode().getName());
		
		// Haven't left node yet, so setup of assay should be false
		assertEquals(false, isSetupCompleted);
		
		// Now leave node and check state
		this.jbpmService.signalProcessInstance(this.processInstance.getId());
		assertEquals(true, isSetupCompleted);

	}
	
	/**
	 * Verifies process instantiated, execution started,
	 * and move to fork w/correct branches present
	 */
	@Test
	public void testFork()
	{
		// Let's start the process execution
		this.jbpmService.signalProcessInstance(this.processInstance.getId());

		// Now the process should be in setup state.
		// Reload the instance through the service so root token gets updated
		this.processInstance = this.jbpmService.getProcessInstance(this.processInstance.getId());
		assertEquals("Setup Assay", this.processInstance.getRootToken().getNode().getName());
		
		// Now move to the fork
		this.jbpmService.signalProcessInstance(processInstance.getId());
		this.processInstance = this.jbpmService.getProcessInstance(this.processInstance.getId());
		
		// Verify the move successfully completed
		String currentNodeName = this.processInstance.getRootToken().getNode().getName();
		log.debug("After move to fork, token node name: " + currentNodeName);
		assertEquals("fork1", currentNodeName);
		
		logTokens("testFork");
		
		// Are the fork branches present?
		String forkNode = this.processInstance.findToken("/Gene Expression Track").getNode().getName();
		assertEquals("Run Gene Expression Plates", forkNode);
		forkNode = this.processInstance.findToken("/Morphology Track").getNode().getName();
		assertEquals("Run Cell Morphology Plates", forkNode);
	}
	
	/**
	 * Verifies the assay run fork w/pattern matching 
	 * decision successfully completes
	 */
	@Test
	public void testForkWithDecisionFalse()
	{
		testFork();

		// Seed the context
		jbpmService.setContext(this.processInstance.getId(), variableName, new Boolean(false));
		
		// Context set correctly?
		Boolean variable = (Boolean)this.jbpmService.getContextVariable(processInstance.getId(), variableName);
		assertEquals(Boolean.FALSE, variable);
		
		jbpmService.signalToken(this.processInstance, "/Gene Expression Track");
		jbpmService.signalToken(this.processInstance, "/Morphology Track");
		
		// Update the instance
		this.processInstance = this.jbpmService.getProcessInstance(this.processInstance.getId());
		
		logTokens("testForkWithDecisionFalse");
		
		// Verify the join successfully completed
		String currentNodeName = this.processInstance.getRootToken().getNode().getName();
		log.debug("After join root token node name: " + currentNodeName);
		assertEquals("Cleanup Assay", currentNodeName);
	}
	
	
	/**
	 * Verifies the assay run fork w/pattern matching 
	 * decision successfully completes
	 */
	@Test
	public void testForkWithDecisionTrue()
	{
		testFork();

		// Seed the context
		jbpmService.setContext(this.processInstance.getId(), variableName, new Boolean(true));
		
		// Context set correctly?
		Boolean variable = (Boolean)this.jbpmService.getContextVariable(processInstance.getId(), variableName);
		assertEquals(Boolean.TRUE, variable);
		
		jbpmService.signalToken(this.processInstance, "/Gene Expression Track");
		jbpmService.signalToken(this.processInstance, "/Morphology Track");
		
		// Update the instance
		this.processInstance = this.jbpmService.getProcessInstance(this.processInstance.getId());
		
		logTokens("testForkWithDecisionTrue");
		
		// Verify morphology track token moved to setup assay node
		String morphologyTrackNode = this.processInstance.findToken("/Morphology Track").getNode().getName();
		assertEquals("Setup Assay", morphologyTrackNode);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testCleanupAssay()
	{

		// Verify initial conditions
		Collection taskInstances = this.jbpmService.findTaskInstances(this.processInstance.getId());
		assertEquals(0, taskInstances.size());
		
		// Execute to cleanup node
		testForkWithDecisionFalse();
		
		taskInstances = this.jbpmService.findTaskInstances(this.processInstance.getId());
		assertEquals(4, taskInstances.size());
		assertEquals(true, isActorIdSet(taskInstances, CLEANUP_TASK_NAME, UNDERGRAD_ID));
	}
	
	@SuppressWarnings("unchecked")
	private boolean isActorIdSet(Collection taskInstances, String taskName, String actorId)
	{
		for(Object taskInstanceObj : taskInstances)
		{
			TaskInstance taskInstance = (TaskInstance)taskInstanceObj;
			if(taskInstance.getName().equals(taskName) && taskInstance.getActorId().equals(actorId))
			{
				return true;
			}
		}
		return false;
	}
	
	
	@SuppressWarnings("unchecked" )
	private void logTokens(String header)
	{
		log.debug("===============> " + header + " logTokens() <================");
		List<Token> tokens = this.processInstance.findAllTokens();
		for(Token token : tokens)
		{
			log.debug("Token: " + token.getFullName());
			log.debug("Token Node Name: " + token.getNode().getName());
		}	
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
