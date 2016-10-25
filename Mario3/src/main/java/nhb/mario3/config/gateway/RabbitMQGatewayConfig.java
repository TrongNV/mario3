package nhb.mario3.config.gateway;

import nhb.common.data.PuObjectRO;
import nhb.mario3.entity.message.transcoder.binary.BinaryMessageSerializer;
import nhb.mario3.entity.message.transcoder.rabbitmq.RabbitMQMessageDeserializer;
import nhb.messaging.rabbit.RabbitMQQueueConfig;

public class RabbitMQGatewayConfig extends GatewayConfig {

	{
		this.setType(GatewayType.RABBITMQ);
		this.setDeserializerClassName(RabbitMQMessageDeserializer.class.getName());
		this.setSerializerClassName(BinaryMessageSerializer.class.getName());
	}

	private RabbitMQQueueConfig queueConfig;
	private String serverWrapperName;

	@Override
	protected void _readPuObject(PuObjectRO data) {
		super._readPuObject(data);
		if (data.variableExists("serverWrapperName")) {
			this.setServerWrapperName(data.getString("serverWrapperName"));
		} else if (data.variableExists("server")) {
			this.setServerWrapperName(data.getString("server"));
		}
		if (data.variableExists("queueConfig")) {
			if (this.queueConfig == null) {
				this.queueConfig = new RabbitMQQueueConfig();
			}
			this.queueConfig.readPuObject(data.getPuObject("queueConfig"));
		}
	}

	public RabbitMQQueueConfig getQueueConfig() {
		return queueConfig;
	}

	public void setQueueConfig(RabbitMQQueueConfig queueConfig) {
		this.queueConfig = queueConfig;
	}

	public String getServerWrapperName() {
		return serverWrapperName;
	}

	public void setServerWrapperName(String serverWrapperName) {
		this.serverWrapperName = serverWrapperName;
	}

}
