package nhb.mario3.entity.message.impl;

import nhb.mario3.config.gateway.GatewayType;
import nhb.mario3.entity.message.KafkaMessage;

public class BaseKafkaMessage extends BaseMessage implements KafkaMessage {

	private byte[] key;
	private String topic;

	{
		this.setGatewayType(GatewayType.KAFKA);
	}
	
	@Override
	public void setKey(byte[] key) {
		this.key = key;
	}

	@Override
	public byte[] getKey() {
		return this.key;
	}

	@Override
	public String getTopic() {
		return this.topic;
	}

	@Override
	public void setTopic(String topic) {
		this.topic = topic;
	}

}
