package com.googlecode.jbpmkickstart.spring.helper;

import org.apache.log4j.Logger;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.node.DecisionHandler;

public class MorphologyPatternDecisionHandler implements DecisionHandler {

	private static final long serialVersionUID = 1L;
	
	public static final String variableName = "hasRulesMatch";
	
	/** log4j logger */
	private final static Logger log = Logger.getLogger(MorphologyPatternDecisionHandler.class);

	public String decide(ExecutionContext executionContext) throws Exception {
		ContextInstance ctx = executionContext.getContextInstance();
		boolean hasRulesMatch = (Boolean)ctx.getVariable(variableName);
		log.debug(variableName + ": " + hasRulesMatch);
		return String.valueOf(hasRulesMatch).toLowerCase();
	}
}
