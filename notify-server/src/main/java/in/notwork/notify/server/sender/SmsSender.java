package in.notwork.notify.server.sender;

import in.notwork.notify.protos.MessageProto;

import java.util.Map;

/**
 * @author rishabh.
 */
public class SmsSender extends MessageSender {

    public SmsSender(Map<String, String> config) {
        super(config);
    }

    @Override
    public void send(MessageProto.Message message) {

    }
}
