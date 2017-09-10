package com.gimo.robot.qq.web.service;

import com.gimo.robot.qq.web.util.PropReader;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * 百度机器人
 * Created by GimoXie on 2017/7/29.
 */
@Service("baiduQueryService")
public class BaiduQueryService {

    private static final Logger logger = LoggerFactory.getLogger(BaiduQueryService.class);
    private static final String BAIDU_COOKIE = PropReader.getString("baidu.cookie");

    private static String BAIDU_URL = "https://sp0.baidu.com/yLsHczq6KgQFm2e88IuM_a/s?sample_name=bear_brain&request_query=#MSG#&bear_type=2";

    /**
     * 百度机器人交互方法
     * @param msg 发送的消息
     * @return 机器人回复的消息
     */
    public String chat(String msg) {
    	String answer = null ;

        if (StringUtils.isBlank(msg))
            msg = "你好~";

        try {

            BAIDU_URL = BAIDU_URL.replace("#MSG#", URLEncoder.encode(msg, "UTF-8"));
        } catch (final UnsupportedEncodingException e) {
            logger.error("Chat with Baidu Robot failed", e);
        }

        try {
        	final HttpPost post = new HttpPost(new URL(BAIDU_URL).toURI());
        	post.addHeader("Cookie", BAIDU_COOKIE);
        	final HttpResponse response = HttpClients.createDefault().execute(post);
            final JSONObject data = new JSONObject(EntityUtils.toString(response.getEntity(), "utf-8"));

            logger.info(EntityUtils.toString(response.getEntity(), "utf-8"));

            final String content = (String) data.getJSONArray("result_list").getJSONObject(0).get("result_content");
            answer = (String) new JSONObject(content).get("answer");
            answer = answer.replaceAll("小度", PropReader.QQ_BOT_NAME);
        } catch (final Exception e) {
            logger.error("Chat with Baidu Robot failed", e);
        }

        return answer;
    }
}
