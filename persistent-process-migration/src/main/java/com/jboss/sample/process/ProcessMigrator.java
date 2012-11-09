package com.jboss.sample.process;

import static org.drools.io.ResourceFactory.newClassPathResource;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.sql.DataSource;

import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.command.Context;
import org.drools.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.command.impl.GenericCommand;
import org.drools.command.impl.KnowledgeCommandContext;
import org.drools.persistence.jpa.JPAKnowledgeService;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.StatefulKnowledgeSession;
import org.jbpm.workflow.instance.WorkflowProcessInstanceUpgrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.resource.jdbc.PoolingDataSource;

public class ProcessMigrator {
	private static final String DATASOURCE_CLASS = "org.h2.jdbcx.JdbcDataSource";

//	private static final String DB_URL = "jdbc:h2:tcp://10.33.46.75:9092/~/jbpm;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1";
	private static final String DB_URL = "jdbc:h2:file:" + System.getProperty("java.io.tmpdir") + "/db/jbpm;DB_CLOSE_DELAY=-1";
	

	private static final String DB_USERNAME = "sa";

	private static final String DB_PASSWORD = "sa";

	private static final Logger LOG = LoggerFactory.getLogger(ProcessMigrator.class);

	private StatefulKnowledgeSession knowledgeSession;

	private EntityManagerFactory emf;

	@SuppressWarnings("unused")
	private DataSource dataSource;

	public ProcessMigrator(final List<String> processFiles) {
		dataSource = createDataSource();
		emf = createEntityManagerFactory();
		knowledgeSession = createStatefulKnowledgeSession(processFiles);
	}

	@SuppressWarnings("serial")
	public <T> void upgradeProcessInstance(final long processInstanceId, final String processId, final Map<String, Long> nodeMapping) {
		((CommandBasedStatefulKnowledgeSession) knowledgeSession).execute(new GenericCommand<Void>() {
			@Override
			public Void execute(Context context) {
				StatefulKnowledgeSession ksession = ((KnowledgeCommandContext) context).getStatefulKnowledgesession();
				LOG.info("Upgrading process instance {} to {}", processInstanceId, processId);
				WorkflowProcessInstanceUpgrader.upgradeProcessInstance(ksession, processInstanceId, processId, nodeMapping);
				return (Void) null;
			}
		});
	}
	
	@SuppressWarnings("serial")
	public <T> void abortProcessInstance(final long processInstanceId) {
		((CommandBasedStatefulKnowledgeSession) knowledgeSession).execute(new GenericCommand<Void>() {
			@Override
			public Void execute(Context context) {
				StatefulKnowledgeSession ksession = ((KnowledgeCommandContext) context).getStatefulKnowledgesession();
				ksession.abortProcessInstance(processInstanceId);
				return (Void) null;
			}
		});
	}
	
	

	private StatefulKnowledgeSession createStatefulKnowledgeSession(final List<String> processFiles) {
		Environment env = KnowledgeBaseFactory.newEnvironment();
		env.set(EnvironmentName.ENTITY_MANAGER_FACTORY, emf);
		env.set(EnvironmentName.TRANSACTION_MANAGER, TransactionManagerServices.getTransactionManager());

		KnowledgeBuilder knowledgeBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();

		for (String processFile : processFiles) {
			knowledgeBuilder.add(newClassPathResource(processFile), ResourceType.BPMN2);
		}

		KnowledgeBase kbase = knowledgeBuilder.newKnowledgeBase();

		return JPAKnowledgeService.newStatefulKnowledgeSession(kbase, null, env);
	}

	private EntityManagerFactory createEntityManagerFactory() {
		return Persistence.createEntityManagerFactory("org.jbpm.persistence.jpa");
	}

	public PoolingDataSource createDataSource() {
		PoolingDataSource pds = new PoolingDataSource();
		pds.setUniqueName("java:jboss/datasources/jbpmDS");
		pds.setClassName(DATASOURCE_CLASS);
		pds.setMaxPoolSize(5);
		pds.setAllowLocalTransactions(true);
		pds.getDriverProperties().put("user", DB_USERNAME);
		pds.getDriverProperties().put("password", DB_PASSWORD);
		pds.getDriverProperties().put("URL", DB_URL);
		pds.init();
		return pds;
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}

	public StatefulKnowledgeSession getStatefulKnowledgeSession() {
		return knowledgeSession;
	}
}
