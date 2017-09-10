package com.gimo.robot.qq.core.smartqq;


import com.gimo.robot.qq.core.smartqq.model.*;
import com.gimo.robot.qq.core.smartqq.callback.MessageCallback;
import com.gimo.robot.qq.core.smartqq.client.SmartQQClient;

import java.util.List;

/**
 * @author ScienJus
 * @date 2015/12/18.
 */
public class Application {

    public static void main(String[] args) {
        //创建一个新对象时需要扫描二维码登录，并且传一个处理接收到消息的回调，如果你不需要接收消息，可以传null
        SmartQQClient client = new SmartQQClient(new MessageCallback() {
            @Override
            public void onMessage(Message message) {
                System.out.println(message.getContent());
            }

            @Override
            public void onGroupMessage(GroupMessage message) {
                System.out.println(message.getContent());
            }

            @Override
            public void onDiscussMessage(DiscussMessage message) {
                System.out.println(message.getContent());
            }
        });
        //登录成功后便可以编写你自己的业务逻辑了
        List<Category> categories = client.getFriendListWithCategory();
        for (Category category : categories) {
            System.out.println(category.getName());
            for (Friend friend : category.getFriends()) {
                System.out.println("————" + friend.getNickname());
            }
        }

        while (true) ;
    }
}
