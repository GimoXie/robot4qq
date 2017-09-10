package com.gimo.robot.qq.web;


import com.gimo.robot.qq.core.smartqq.callback.MessageCallback;
import com.gimo.robot.qq.core.smartqq.client.SmartQQClient;
import com.gimo.robot.qq.core.smartqq.model.DiscussMessage;
import com.gimo.robot.qq.core.smartqq.model.GroupMessage;
import com.gimo.robot.qq.core.smartqq.model.Message;

/**
 * Created by GimoXie on 2017/7/29.
 */
public class Test {


    public static void main(String[] args){
        SmartQQClient client = new SmartQQClient(new MessageCallback() {

            @Override//私聊消息监听
            public void onMessage(Message message) {
                System.err.println(message.getContent());
            }

            @Override//群聊消息监听
            public void onGroupMessage(GroupMessage groupMessage) {
                System.err.println(groupMessage.getContent());

            }

            @Override//讨论组消息监听
            public void onDiscussMessage(DiscussMessage discussMessage) {
                System.err.println(discussMessage.getContent());

            }
        });
    }
}
