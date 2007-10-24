package com.googlecode.jbpmkickstart.spring.helper;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;

public class SetupAssayActionHandler implements ActionHandler {

	private static final long serialVersionUID = 1L;
	
	 public static boolean isSetupCompleted = false;

	public void execute(ExecutionContext executionContext) throws Exception {
		// TODO sophisticated business logic here, instance context setting, etc...
		isSetupCompleted = true;
	}
}
