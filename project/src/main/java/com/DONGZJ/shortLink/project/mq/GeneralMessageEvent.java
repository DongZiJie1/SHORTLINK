package com.DONGZJ.shortLink.project.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 普通消息发送事件
 *
 * @公众号：马丁玩编程，回复：加群，添加马哥微信（备注：ladder）获取更多项目资料
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralMessageEvent {

    /**
     * 消息内容，可以是 JSON 或者其它字符串
     */
    private String body;

    /**
     * RocketMQ 消息唯一标识，可用作幂等或其它用途
     */
    private String keys;
}
