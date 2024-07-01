package com.DONGZJ.shortLink.project.mq.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RocketmqProducer {
    private final RocketMQTemplate rocketMQTemplate;
    public void sendMessage(String order) {
        // 创建订单逻辑
        // ...

        // 发送消息到RocketMQ
        rocketMQTemplate.convertAndSend("test", order);
        log.info("test消息发送成功");
    }
}
