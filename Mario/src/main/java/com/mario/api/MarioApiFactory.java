package com.mario.api;

import com.mario.cache.CacheManager;
import com.mario.entity.EntityManager;
import com.mario.extension.ExtensionManager;
import com.mario.gateway.GatewayManager;
import com.mario.gateway.serverwrapper.ServerWrapperManager;
import com.mario.monitor.MonitorAgentManager;
import com.mario.producer.MessageProducerManager;
import com.mario.schedule.impl.SchedulerFactory;
import com.mario.zookeeper.ZooKeeperClientManager;
import com.nhb.common.data.PuObjectRO;
import com.nhb.common.db.cassandra.CassandraDatasourceManager;
import com.nhb.common.db.mongodb.MongoDBSourceManager;
import com.nhb.common.db.sql.SQLDataSourceManager;

public final class MarioApiFactory {

	private CassandraDatasourceManager cassandraDatasourceManager;
	private SQLDataSourceManager sqlDataSourceManager;
	private SchedulerFactory schedulerFactory;
	private CacheManager cacheManager;
	private EntityManager entityManager;
	private MongoDBSourceManager mongoDBSourceManager;
	private GatewayManager gatewayManager;
	private MessageProducerManager producerManager;
	private ZooKeeperClientManager zkClientManager;

	private MonitorAgentManager monitorAgentManager;
	private ExtensionManager extensionManager;

	private PuObjectRO globalProperties;
	private ServerWrapperManager serverWrapperManager;

	public MarioApiFactory(SQLDataSourceManager sqlDataSourceManager,
			CassandraDatasourceManager cassandraDatasourceManager, SchedulerFactory schedulerFactory,
			MongoDBSourceManager mongoDBSourceManager, CacheManager cacheManager,
			MonitorAgentManager monitorAgentManager, MessageProducerManager producerManager,
			GatewayManager gatewayManager, ZooKeeperClientManager zkClientManager, ExtensionManager extensionManager,
			ServerWrapperManager serverWrapperManager, PuObjectRO globalProperties) {
		this.cassandraDatasourceManager = cassandraDatasourceManager;
		this.sqlDataSourceManager = sqlDataSourceManager;
		this.schedulerFactory = schedulerFactory;
		this.cacheManager = cacheManager;
		this.mongoDBSourceManager = mongoDBSourceManager;
		this.monitorAgentManager = monitorAgentManager;
		this.producerManager = producerManager;
		this.gatewayManager = gatewayManager;
		this.zkClientManager = zkClientManager;
		this.extensionManager = extensionManager;
		this.serverWrapperManager = serverWrapperManager;

		this.globalProperties = globalProperties;
	}

	public MarioApi newApi() {
		return new MarioApiImpl(this.sqlDataSourceManager, this.cassandraDatasourceManager,
				this.schedulerFactory.newSchedulerInstance(), this.cacheManager, this.mongoDBSourceManager,
				this.entityManager, this.gatewayManager.getSocketSessionManager(), this.monitorAgentManager,
				this.producerManager, this.gatewayManager, this.zkClientManager, this.extensionManager,
				this.serverWrapperManager, this.globalProperties.deepClone());
	}

	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	public void setGatewayManager(GatewayManager gatewayManager) {
		this.gatewayManager = gatewayManager;
	}

}
