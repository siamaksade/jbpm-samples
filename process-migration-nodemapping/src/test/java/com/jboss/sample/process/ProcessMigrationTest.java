package com.jboss.sample.process;

import java.util.HashMap;
import java.util.Map;

import org.drools.KnowledgeBase;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.event.DebugProcessEventListener;
import org.drools.io.ResourceFactory;
import org.drools.logger.KnowledgeRuntimeLogger;
import org.drools.logger.KnowledgeRuntimeLoggerFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.ProcessInstance;
import org.jbpm.workflow.instance.WorkflowProcessInstanceUpgrader;
import org.junit.Test;

public class ProcessMigrationTest {
	@Test
	public void migrateProcess() throws Exception {
		// add process
		KnowledgeBuilder kbuilder = createKnowledgeBase("process/process-1.0.bpmn", "process/process-2.0.bpmn");
		KnowledgeBase kbase = kbuilder.newKnowledgeBase();
		StatefulKnowledgeSession ksession = kbase.newStatefulKnowledgeSession();
		KnowledgeRuntimeLogger logger = KnowledgeRuntimeLoggerFactory.newThreadedFileLogger(ksession, "target/trace", 1000);
		ksession.addEventListener(new DebugProcessEventListener());

		// start process
		ProcessInstance processInstance = ksession.startProcess("process-1.0");
		
		// wait
		sleep(5000);
		
		// go back to first converge node
		Map<String, Long> nodeMapping = new HashMap<String, Long>();
		nodeMapping.put("3", 8L);
//		nodeMapping.put("6", 7L);
		WorkflowProcessInstanceUpgrader.upgradeProcessInstance(ksession, processInstance.getId(), "process-2.0", nodeMapping);
		
		sleep(5000);
		
		// signal continue
		System.out.println("Signaling Continue...");
		ksession.signalEvent("Continue", "", processInstance.getId());
		
		logger.close();
	}

	private void sleep(long ms) {
		System.out.println("Waiting...");
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}

	private static KnowledgeBuilder createKnowledgeBase(String...bpmnProcesses) throws Exception {
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
		
		if (bpmnProcesses != null) {
			for (String bpmnProcess : bpmnProcesses) {
				kbuilder.add(ResourceFactory.newClassPathResource(bpmnProcess), ResourceType.BPMN2);
			}
		}
		
		return kbuilder;
	}
}