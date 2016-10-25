package nhb.mario3.entity.message.impl;

import nhb.mario3.entity.message.SocketMessage;
import nhb.mario3.gateway.socket.SocketMessageType;

public class BaseSocketMessage extends BaseMessage implements SocketMessage {

	private SocketMessageType messageType;
	private String sessionId;

	@Override
	public SocketMessageType getSocketMessageType() {
		return this.messageType;
	}

	@Override
	public void setSocketMessageType(SocketMessageType type) {
		this.messageType = type;
	}

	@Override
	public String getSessionId() {
		return this.sessionId;
	}

	@Override
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public void clear() {
		super.clear();
		this.sessionId = null;
		this.messageType = null;
	}
}
