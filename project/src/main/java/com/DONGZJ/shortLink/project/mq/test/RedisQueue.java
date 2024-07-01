package com.DONGZJ.shortLink.project.mq.test;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "message",havingValue = "redis")
public class RedisQueue implements MessageQueue{
    @Override
    public void sendMessage() {
        //发送消息
        System.out.println("redis");
    }
}
