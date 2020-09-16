package io.kindx.backoffice.processor.notification;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TwilioNotificationProcessor implements NotificationProcessor<Message> {

    private static final Logger logger = LogManager.getLogger(TwilioNotificationProcessor.class);
    private String fromAddress;

    public TwilioNotificationProcessor(String twilioAccountSid,
                                       String twilioAuthToken,
                                       String fromAddress) {
        this.fromAddress = fromAddress;
        Twilio.init(twilioAccountSid, twilioAuthToken);
    }

    @Override
    public Message process(String identity, String body) {
        Message message =  Message.creator(
                new com.twilio.type.PhoneNumber(identity),
                new com.twilio.type.PhoneNumber(fromAddress),
                body)
                .create();
        logger.debug("Message sent: Error: '{}' - '{}'", message.getErrorCode(), message.getErrorMessage());
        return message;
    }
}
