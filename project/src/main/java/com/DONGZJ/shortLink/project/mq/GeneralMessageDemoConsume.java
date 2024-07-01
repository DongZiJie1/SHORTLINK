package com.DONGZJ.shortLink.project.mq;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 普通消息消费者
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "DONG",
        selectorExpression = "dong",
        consumerGroup = "rocketmq-demo_general-dong-short-link"
)
public class GeneralMessageDemoConsume implements RocketMQListener<GeneralMessageEvent> {

    @Override
    public void onMessage(GeneralMessageEvent message) {
        log.info("接到到RocketMQ消息，消息体：{}", JSON.toJSONString(message));
    }
}
