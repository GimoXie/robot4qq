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
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 图灵机器人
 * Created by GimoXie on 2017/7/29.
 */
@Service("turingQueryService")
public class TuringQueryService {

    private static final Logger logger = LoggerFactory.getLogger(TuringQueryService.class);

    private static final String TURING_API = PropReader.getString("turing.api");
    private static final String TURING_KEY = PropReader.getString("turing.key");

    /**
     * 图灵机器人交互方法
     * @param msg 发送的消息
     * @return 机器人回复的消息
     */
    public String chat(String userName, String msg) {
    	String answer = null;
        if (StringUtils.isBlank(msg))
            msg = "你好";

        if (StringUtils.isBlank(userName))
            userName = "张三";

        try {
            final HttpPost post = new HttpPost(new URL(TURING_API).toURI());
            final List<NameValuePair> params=new ArrayList<>();
            params.add(new BasicNameValuePair("key",TURING_KEY));
            params.add(new BasicNameValuePair("info",msg));
            params.add(new BasicNameValuePair("userid",userName));
            post.setEntity(new UrlEncodedFormEntity(params,"utf-8"));
            final HttpResponse response = HttpClients.createDefault().execute(post);
            final JSONObject data = new JSONObject(EntityUtils.toString(response.getEntity(), "utf-8"));
            final int code = data.optInt("code");

            switch (code) {
                case 40001:
                case 40002:
                case 40007:
                    logger.error(data.optString("text"));
                    break;
                case 40004:
                	answer = "聊累了，明天请早吧~";
                	break;
                case 100000:
                	answer = data.optString("text");
                	break;
                case 200000:
                	answer = data.optString("text") + " " + data.optString("url");
                	break;
                case 302000:
                    String ret302000 = data.optString("text") + " ";
                    final JSONArray list302000 = data.optJSONArray("list");
                    final StringBuilder builder302000 = new StringBuilder();
                    for (int i = 0; i < list302000.length(); i++) {
                        final JSONObject news = list302000.optJSONObject(i);
                        builder302000.append(news.optString("article")).append(news.optString("detailurl"))
                                .append("\n\n");
                    }
                    answer = ret302000 + " " + builder302000.toString();
                    break;
                case 308000:
                    String ret308000 = data.optString("text") + " ";
                    final JSONArray list308000 = data.optJSONArray("list");
                    final StringBuilder builder308000 = new StringBuilder();
                    for (int i = 0; i < list308000.length(); i++) {
                        final JSONObject news = list308000.optJSONObject(i);
                        builder308000.append(news.optString("name")).append(news.optString("detailurl"))
                                .append("\n\n");
                    }

                    answer = ret308000 + " " + builder308000.toString();
                    break;
                default:
                    logger.warn("Turing Robot default return [" + data.toString(4) + "]");
            }
        } catch (final Exception e) {
            logger.error("Chat with Turing Robot failed", e);
        }

        return answer;
    }
}
