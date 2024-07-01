package com.DONGZJ.shortLink.project.mq.consumer;

import com.DONGZJ.shortLink.project.mq.GeneralMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;

@RocketMQMessageListener(topic = "test", consumerGroup = "your_consumer_group", consumeMode = ConsumeMode.ORDERLY)
@Slf4j
public class RocketmqConsumer implements RocketMQListener<GeneralMessageEvent> {
    @Override
    public void onMessage(GeneralMessageEvent generalMessageEvent) {
        log.info("消费成功");
    }
}
