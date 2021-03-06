package com.gimo.robot.qq.core.smartqq.callback;


import com.gimo.robot.qq.core.smartqq.model.DiscussMessage;
import com.gimo.robot.qq.core.smartqq.model.GroupMessage;
import com.gimo.robot.qq.core.smartqq.model.Message;

/**
 * 收到消息的回调
 * @author ScienJus
 * @date 2015/12/18.
 */
public interface MessageCallback {

    /**
     * 收到私聊消息后的回调
     * @param message
     */
    void onMessage(Message message);

    /**
     * 收到群消息后的回调
     * @param message
     */
    void onGroupMessage(GroupMessage message);

    /**
     * 收到讨论组消息后的回调
     * @param message
     */
    void onDiscussMessage(DiscussMessage message);
}
