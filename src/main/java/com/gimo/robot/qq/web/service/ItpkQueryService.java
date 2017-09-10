package com.gimo.robot.qq.web.service;

import com.gimo.robot.qq.web.util.PropReader;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * ITPK机器人
 * Created by GimoXie on 2017/7/29.
 */
@Service("itpkQueryService")
public class ItpkQueryService {

    private static final Logger logger = LoggerFactory.getLogger(ItpkQueryService.class);

    private static final String ITPK_API = PropReader.getString("itpk.api");
    private static final String ITPK_KEY = PropReader.getString("itpk.key");
    private static final String ITPK_SECRET = PropReader.getString("itpk.secret");

    /**
     * itpk机器人交互方法
     * @param msg 发送的消息
     * @return 机器人回复的消息
     */
    public String chat(String msg) {
    	String answer = "";
    	
        if (StringUtils.isBlank(msg))
            msg = "你好";

        try {
        	final HttpPost post = new HttpPost(new URL(ITPK_API).toURI());
        	final List<NameValuePair> params=new ArrayList<>();
            params.add(new BasicNameValuePair("api_key",ITPK_KEY));
            params.add(new BasicNameValuePair("limit","8"));
            params.add(new BasicNameValuePair("api_secret",ITPK_SECRET));
            params.add(new BasicNameValuePair("question",msg));
            post.setEntity(new UrlEncodedFormEntity(params,"utf-8"));
            final HttpResponse response = HttpClients.createDefault().execute(post);
            
            answer = new String(EntityUtils.toString(response.getEntity(), "utf-8")).substring(1);
        } catch (final Exception e) {
            logger.error("Chat with ITPK Robot failed", e);
        }

        return answer;
    }
}
