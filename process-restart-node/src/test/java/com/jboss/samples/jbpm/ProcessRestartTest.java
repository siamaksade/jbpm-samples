package com.jboss.samples.jbpm;


import junit.framework.Assert;

import org.drools.KnowledgeBase;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.jbpm.workflow.instance.NodeInstance;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.junit.Test;

public class ProcessRestartTest {
	@Test
	public void restartSomeNode() throws Exception {
		// add process
		KnowledgeBuilder kbuilder = createKnowledgeBase("process/process-1.0.bpmn");
		KnowledgeBase kbase = kbuilder.newKnowledgeBase();
		StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();
		KnowledgeRuntimeLogger logger = KnowledgeRuntimeLoggerFactory.newThreadedFileLogger(ksession, "test", 1000);

		// start process
		ProcessInstance processInstance = ksession.startProcess("process-1.0");
		
		// wait
		sleep(1000);
		

		// re-run node 4
		WorkflowProcessInstanceImpl workflowProcessInstance = (WorkflowProcessInstanceImpl) ksession.getProcessInstance(processInstance.getId());
		NodeInstance nodeInstance = workflowProcessInstance.getNodeInstance(4);
		
		Assert.assertNotNull("Node is null", nodeInstance);
		
		nodeInstance.trigger(null, org.jbpm.workflow.core.Node.CONNECTION_DEFAULT_TYPE); 

		// wait
		sleep(1000);
		
		// signal continue
		System.out.println(">> Signaling Continue...");
		ksession.signalEvent("Continue", "", processInstance.getId());
		
		logger.close();
	}

	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}

	private static KnowledgeBuilder createKnowledgeBase(String bpmnProcess) throws Exception {
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		kbuilder.add(ResourceFactory.newClassPathResource(bpmnProcess), ResourceType.BPMN2);
		return kbuilder;
	}
}