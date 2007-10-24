package com.googlecode.jbpmkickstart.spring;

/*
 * File: JbpmService.java 
 * 
 * Author: $Author$
 * 
 * $Date$ $Revision
 */

import java.util.Collection;

import org.hibernate.Hibernate;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.JbpmException;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.db.GraphSession;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.springframework.beans.factory.annotation.Required;
import org.springmodules.workflow.jbpm31.JbpmCallback;
import org.springmodules.workflow.jbpm31.JbpmTemplate;

/**
 * Defines a Contract for jBPM Process Lifecycle Operations
 */
public class JbpmService {
	protected JbpmConfiguration jbpmConfiguration;

	protected JbpmTemplate jbpmTemplate;

	/**
	 * Utility method for extracting a Process Definition from an Xml file
	 * 
	 * @param processXmlResource
	 *            String name of file
	 * @return ProcessDefinition parsed from file
	 */
	public ProcessDefinition parseXMLResource(String processXmlResource) {
		return ProcessDefinition.parseXmlResource(processXmlResource);
	}

	/**
	 * Deploys a process definition into database from xml file name
	 * 
	 * @param processXmlResource
	 *            path to process xml file
	 */
	public void deployProcessDefinition(String processXmlResource) {
		final ProcessDefinition processDefinition = ProcessDefinition
				.parseXmlResource(processXmlResource);

		this.jbpmTemplate.execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) {
				context.deployProcessDefinition(processDefinition);
				return null;
			}
		});
	}

	/**
	 * Deploys the given ProcessDefinition to the jBPM database
	 * 
	 * @param processDefinition
	 */
	public void deployProcessDefinition(
			final ProcessDefinition processDefinition) {
		this.jbpmTemplate.execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) {
				context.deployProcessDefinition(processDefinition);
				return null;
			}
		});
	}

	/**
	 * Finds the latest definition of a processs in jBPM database
	 * 
	 * @param processName
	 * @return latest process definition
	 */
	public ProcessDefinition findLatestProcessDefinition(
			final String processName) {
		return (ProcessDefinition) this.jbpmTemplate
				.execute(new JbpmCallback() {
					public Object doInJbpm(JbpmContext context) {
						GraphSession graphSession = context.getGraphSession();
						ProcessDefinition processDefinition = graphSession
								.findLatestProcessDefinition(processName);
						Hibernate.initialize(processDefinition.getNodes());
						return processDefinition;
					}
				});
	}

	/**
	 * Creates an instance of the latest process definition for the given
	 * process name. The returned process instance is left in the start state.
	 * 
	 * @param processName
	 * @return new instance of process definition
	 */
	public ProcessInstance createProcessInstance(final String processName) {
		return (ProcessInstance) this.jbpmTemplate.execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) {
				GraphSession graphSession = context.getGraphSession();
				ProcessDefinition processDefinition = graphSession
						.findLatestProcessDefinition(processName);
				ProcessInstance processInstance = new ProcessInstance(
						processDefinition);
				Hibernate.initialize(processInstance.getRootToken());
				context.save(processInstance);
				return processInstance;
			}
		});
	}

	/**
	 * Sets a (name, value) pair into the {@link ProcessInstance}'s
	 * {@link ContextInstance}
	 * 
	 * @param processInstanceId
	 * @param name
	 * @param value
	 */
	public void setContext(final long processInstanceId, final String name,
			final Object value) {
		this.jbpmTemplate.execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) {
				GraphSession graphSession = context.getGraphSession();
				ProcessInstance processInstance = graphSession
						.getProcessInstance(processInstanceId);
				processInstance.getContextInstance().setVariable(name, value);
				context.save(processInstance);
				return null;
			}
		});
	}

	/**
	 * Finds the {@link ContextInstance} of a {@link ProcessInstance}
	 * 
	 * @param processName
	 * @return latest process definition
	 */
	public Object getContextVariable(final long processInstanceId,
			final String name) {
		return (Object) this.jbpmTemplate.execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) {
				GraphSession graphSession = context.getGraphSession();
				ProcessInstance processInstance = graphSession
						.getProcessInstance(processInstanceId);
				ContextInstance ctx = processInstance.getContextInstance();
				return ctx.getVariable(name);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public Collection findTaskInstances(final long processInstanceId) {
		return (Collection) this.jbpmTemplate.execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) {
				GraphSession graphSession = context.getGraphSession();
				ProcessInstance processInstance = graphSession
						.getProcessInstance(processInstanceId);
				Collection taskInstances = processInstance
						.getTaskMgmtInstance().getTaskInstances();
				Hibernate.initialize(taskInstances);
				for (Object taskInstanceObj : taskInstances) {
					TaskInstance taskInstance = (TaskInstance) taskInstanceObj;
					Hibernate.initialize(taskInstance);
				}
				return taskInstances;
			}
		});
	}

	/**
	 * Instructs the main path of execution to continue by taking the default
	 * transition on the current node.
	 * 
	 * @param processInstanceId
	 */
	public void signalProcessInstance(final long processInstanceId) {
		this.jbpmTemplate.execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) {
				GraphSession graphSession = context.getGraphSession();
				ProcessInstance processInstance = graphSession
						.getProcessInstance(processInstanceId);
				processInstance.signal();
				context.save(processInstance);
				return null;
			}
		});
	}

	/**
	 * Instructs the path of execution associated with the given token to
	 * continue by taking the default transition to the next node.
	 * 
	 * @param processInstance
	 *            the ProcessInstance that the token is associated with
	 * @param tokenName
	 *            the full name of the token
	 * @throws JbpmException
	 *             if a token with the given name could not be found within the
	 *             tree of the processInstance's root token
	 */
	public void signalToken(final ProcessInstance processInstance,
			final String tokenName) {
		this.jbpmTemplate.execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) {
				GraphSession graphSession = context.getGraphSession();
				ProcessInstance signalProcessInstance = graphSession
						.getProcessInstance(processInstance.getId());
				Token token = signalProcessInstance.getRootToken().findToken(
						tokenName);
				if (token == null)
					throw new JbpmException("token '" + tokenName
							+ "' was not found in this processInstance (id="
							+ processInstance.getId() + ")");

				token.signal();
				context.save(signalProcessInstance);
				return null;
			}
		});
	}

	/**
	 * Retrieves an instance of a jBPM process definiton
	 * 
	 * @param processInstanceId
	 *            internal database Id of instance
	 * @return process instances from database
	 */
	public ProcessInstance getProcessInstance(final long processInstanceId) {
		return (ProcessInstance) this.jbpmTemplate.execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) {
				GraphSession graphSession = context.getGraphSession();
				ProcessInstance processInstance = graphSession
						.getProcessInstance(processInstanceId);
				if (processInstance != null) {
					Hibernate.initialize(processInstance);
					Hibernate.initialize(processInstance.getRootToken());
					Hibernate.initialize(processInstance.findAllTokens());
					Hibernate.initialize(processInstance.getInstances());
					for (Object tokenObj : processInstance.findAllTokens()) {
						Token token = (Token) tokenObj;
						Hibernate.initialize(token);
						Hibernate.initialize(token.getChildren());
					}
					for (Object nodeObj : processInstance
							.getProcessDefinition().getNodes()) {
						Node node = (Node) nodeObj;
						Hibernate.initialize(node.getLeavingTransitions());
					}
				}
				return processInstance;
			}
		});
	}

	/**
	 * Deletes the latest definition of a process from the jBPM database
	 * 
	 * @param processName
	 *            name of process definition
	 */
	public void deleteLatestProcessDefinition(final String processName) {
		this.jbpmTemplate.execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) {
				// First get ProcessDefnition out of database
				GraphSession graphSession = context.getGraphSession();
				ProcessDefinition processDefinition = graphSession
						.findLatestProcessDefinition(processName);

				// Make sure it was found and then delete it
				if (processDefinition != null)
					graphSession.deleteProcessDefinition(processDefinition);
				return null;
			}
		});
	}

	/**
	 * Deletes the latest definition of a process from the jBPM database
	 * 
	 * @param processName
	 *            name of process definition
	 */
	public void deleteProcessDefinition(final long processdefinitionId) {
		this.jbpmTemplate.execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) {
				// First get ProcessDefnition out of database
				GraphSession graphSession = context.getGraphSession();
				graphSession.deleteProcessDefinition(processdefinitionId);
				return null;
			}
		});
	}

	/**
	 * 
	 * @param processInstanceId
	 * @return process instances from database
	 */
	public boolean hasProcessInstanceEnded(final long processInstanceId) {
		return (Boolean) this.jbpmTemplate.execute(new JbpmCallback() {
			public Object doInJbpm(JbpmContext context) {
				ProcessInstance initializedProcessInstance = getProcessInstance(processInstanceId);
				return initializedProcessInstance.hasEnded();
			}
		});
	}

	/**
	 * Cascade Drops all jBPM tables and sequences
	 */
	public void dropSchema() {
		this.jbpmTemplate.getJbpmConfiguration().dropSchema();
	}

	/**
	 * Setter for spring
	 * 
	 * @param jbpmTemplate
	 */
	@Required
	public void setJbpmTemplate(JbpmTemplate jbpmTemplate) {
		this.jbpmTemplate = jbpmTemplate;
	}
}
