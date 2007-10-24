package com.googlecode.jbpmkickstart.spring.helper;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;

public class CleanupAssayAssignmentHandler implements AssignmentHandler {

	private static final long serialVersionUID = 1L;

	public static final String UNDERGRAD_ID = "undergrad";

	public void assign(Assignable assignable, ExecutionContext executionContext) throws Exception {
		// TODO sophisticated business logic goes here
		assignable.setActorId(UNDERGRAD_ID);
	}
}
