package com.gimo.robot.qq.web.component;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 启动组件
 * Created by GimoXie on 2017/7/29.
 */
@Component
public class InitComponent implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(InitComponent.class);

    @Resource
    Robot robot;
    /**
     *  Robot应用初始入口
     */
    public void afterPropertiesSet() throws Exception {
        new Thread(() -> {
            logger.info("准备启动了...");
            try {
                Thread.sleep(3000);
            } catch (final Exception e) {
                logger.error(e.getMessage());
            }
            robot.init();
        }).start();
    }

}
