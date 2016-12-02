package com.mario.entity.message.transcoder.binary;

import java.io.IOException;

import com.mario.entity.message.MessageRW;
import com.mario.entity.message.transcoder.MessageDecoder;
import com.nhb.common.BaseLoggable;
import com.nhb.common.data.PuElement;
import com.nhb.common.data.msgpkg.PuMsgpackHelper;

public class BinaryMessageDeserializer extends BaseLoggable implements MessageDecoder {

	@Override
	public void decode(Object data, MessageRW message) {
		if (data != null) {
			PuElement puo;
			try {
				puo = PuMsgpackHelper.unpack((byte[]) data);
				message.setData(puo);
			} catch (IOException e) {
				throw new RuntimeException("Unable to decode binary: " + new String((byte[]) data), e);
			}
		}
	}
}
