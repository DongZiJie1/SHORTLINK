package com.DONGZJ.shortLink.project.mq.test;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "message",havingValue = "mq")
public class RabbitmqQueue implements MessageQueue{
    @Override
    public void sendMessage() {
        System.out.println("rabbit");
    }
}
