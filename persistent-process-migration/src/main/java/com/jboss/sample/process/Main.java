package com.jboss.sample.process;

import java.util.Arrays;
import java.util.Collections;

import org.drools.runtime.process.ProcessInstance;

public class Main {
	/**
	 *  Run 1, 2 and 3 should run one at a time
	 */
	public static void main(String[] args) throws InterruptedException {
		ProcessMigrator migrator = new ProcessMigrator(Arrays.asList("process/process-1.0.bpmn", "process/process-2.0.bpmn"));
		
		// run 1
		ProcessInstance processInstance = startProcess(migrator);
		System.out.println("ProcessInstanceID : " + processInstance.getId());
		
		// run 2
//		migrateProcess(migrator, 9);
		
		// run 3
//		signalProcess(migrator, 9);
	}
	
	public static void main_(String[] args) throws InterruptedException {
		ProcessMigrator migrator = new ProcessMigrator(Arrays.asList("process/process-1.0.bpmn", "process/process-2.0.bpmn"));
		ProcessInstance processInstance = startProcess(migrator);
		migrateProcess(migrator, processInstance.getId());
		signalProcess(migrator, processInstance.getId());
	}
	
	public static ProcessInstance startProcess(ProcessMigrator migrator) {
		return migrator.getStatefulKnowledgeSession().startProcess("process-1.0");
	}

	public static void migrateProcess(ProcessMigrator migrator, long id) {
		migrator.upgradeProcessInstance(id, "process-2.0", Collections.<String, Long> emptyMap());
	}
	
	public static void signalProcess(ProcessMigrator migrator, long id) {
		migrator.getStatefulKnowledgeSession().signalEvent("Continue", null, id);
	}
}
