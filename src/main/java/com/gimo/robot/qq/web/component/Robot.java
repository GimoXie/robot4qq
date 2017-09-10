package com.gimo.robot.qq.web.component;

import com.gimo.robot.qq.core.smartqq.callback.MessageCallback;
import com.gimo.robot.qq.core.smartqq.client.SmartQQClient;
import com.gimo.robot.qq.core.smartqq.model.*;
import com.gimo.robot.qq.web.service.BaiduQueryService;
import com.gimo.robot.qq.web.service.ItpkQueryService;
import com.gimo.robot.qq.web.service.TuringQueryService;
import com.gimo.robot.qq.web.util.PropReader;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 机器人组件
 * Created by GimoXie on 2017/7/30.
 */
@Component
public class Robot {

    private static final Logger logger = LoggerFactory.getLogger(Robot.class);
    // 自我介绍
    private static final String ROBOT_INTRO = "啥事不能在群里说呢，私聊请联系我老大，qq:232571189";
    // 守护介绍
    private static final String ROBOT_LISTENER_INTRO = "我是管家的守护";
    // 广告
    private static final List<String> ADS = new ArrayList<>();
    // 记录未群推过的群 id 集合.
    private static final Set<Long> UNPUSH_GROUPS = new CopyOnWriteArraySet<>();
    // 超过 {@value #PUSH_GROUP_USER_COUNT} 个成员的群才推送.
    private static final int PUSH_GROUP_USER_COUNT = PropReader.getInt("qq.bot.pushGroupUserCnt");
    // 一次群推操作最多只推送 {@value #PUSH_GROUP_COUNT} 个群（为了尽量保证成功率）.
    private static final int PUSH_GROUP_COUNT = 5;
    // 是否启用robot的守护来进行消息送达确认.
    private static final boolean MSG_ACK_ENABLED = PropReader.getBoolean("qq.bot.ack");
    // 无监听时提示信息
    private static final String NO_LISTENER = "请把我的守护也拉进群，否则会造成大量消息重复（如果已经拉了，那就稍等 10 秒钟，我的守护可能在醒瞌睡...）\n\n";
    // qq群
    private final Map<Long, Group> QQ_GROUPS = new ConcurrentHashMap<>();
    // qq群最后一次发送广告时间
    private final Map<Long, Long> GROUP_AD_TIME = new ConcurrentHashMap<>();
    // qq群消息
    private final List<String> GROUP_SENT_MSGS = new CopyOnWriteArrayList<>();
    // 讨论组
    private final Map<Long, Discuss> QQ_DISCUSSES = new ConcurrentHashMap<>();
    // 讨论组最后一次发送广告时间
    private final Map<Long, Long> DISCUSS_AD_TIME = new ConcurrentHashMap<>();
    // 讨论组消息
    private final List<String> DISCUSS_SENT_MSGS = new CopyOnWriteArrayList<>();
    // 机器人类型
    private static final int QQ_BOT_TYPE = PropReader.getInt("qq.bot.type");

    @Resource
    private BaiduQueryService baiduQueryService;
    @Resource
    private TuringQueryService turingQueryService;
    @Resource
    private ItpkQueryService itpkQueryService;

    private SmartQQClient robot;
    private SmartQQClient robotListener;

    static {
        String adConf = PropReader.getString("ads");
        if (StringUtils.isNotBlank(adConf)) {
            final String[] ads = adConf.split("#");
            ADS.addAll(Arrays.asList(ads));
        }

        ADS.add("同志们辛苦了!");
        ADS.add("多写写代码吧...");
    }

    public void init() {
        logger.info("开始初始化robot...");
        robot = new SmartQQClient(new MessageCallback() {

            // 私聊机器人
            @Override
            public void onMessage(final Message message) {
                logger.error(message.getContent());
                new Thread(()-> {
                    try {
                        Thread.sleep(500 + RandomUtils.nextInt(1000));
                        final String content = message.getContent();
                        final String key = PropReader.getString("qq.bot.key");
                        // 不是管理命令，只是普通的私聊
                        if (!StringUtils.startsWith(content, key)) {
                            robot.sendMessageToFriend(message.getUserId(), ROBOT_INTRO);
                            return;
                        }
                        final String msg = StringUtils.substringAfter(content, key);
                        logger.info("Received admin message: " + msg);
                        sendToPushQQGroups(msg);
                    } catch (final Exception e) {
                        logger.error("robot on group message error", e);
                    }
                }).start();
            }

            // 讨论组机器人
            @Override
            public void onDiscussMessage(final DiscussMessage message) {
                logger.error(message.getContent());
                new Thread(()-> {
                    try {
                        Thread.sleep(500 + RandomUtils.nextInt(1000));
                        onQQDiscussMessage(message);
                    } catch (final Exception e) {
                        logger.error("robot on group message error", e);
                    }
                }).start();
            }

            // 群聊机器人
            @Override
            public void onGroupMessage(final GroupMessage message) {
                logger.error(message.getContent());
                new Thread(()-> {
                    try {
                        Thread.sleep(500 + RandomUtils.nextInt(1000));
                        onQQGroupMessage(message);
                    } catch (final Exception e) {
                        logger.error("robot on group message error", e);
                    }
                }).start();
            }
        });
        // Load groups & disscusses
        reloadGroups();
        reloadDiscusses();
        logger.info("robot开始工作...");

        if (MSG_ACK_ENABLED) { // 如果启用了消息送达确认
            logger.info("开始初始化管家的守护");

            robotListener = new SmartQQClient(new MessageCallback() {
                @Override
                public void onMessage(final Message message) {
                    try {
                        final String content = message.getContent();
                        final String key = PropReader.getString("qq.bot.key");
                        if (!StringUtils.startsWith(content, key)) { // 不是管理命令
                            // 让管家的守护进行自我介绍
                            robotListener.sendMessageToFriend(message.getUserId(), ROBOT_LISTENER_INTRO);
                            return;
                        }
                        final String msg = StringUtils.substringAfter(content, key);
                        logger.info("Received admin message: " + msg);
                        sendToPushQQGroups(msg);
                    } catch (final Exception e) {
                        logger.error( "Robot on group message error", e);
                    }
                }

                @Override
                public void onGroupMessage(final GroupMessage message) {
                    final String content = message.getContent();
                    if (GROUP_SENT_MSGS.contains(content)) { // indicates message received
                        GROUP_SENT_MSGS.remove(content);
                    }
                }

                @Override
                public void onDiscussMessage(final DiscussMessage message) {
                    final String content = message.getContent();
                    if (DISCUSS_SENT_MSGS.contains(content)) { // indicates message received
                        DISCUSS_SENT_MSGS.remove(content);
                    }
                }
            });

            logger.info("管家的守护初始化完毕");
        }

        logger.info("管家 QQ 机器人服务开始工作！");
    }

    private void sendToPushQQGroups(final String msg) {
        try {
            final String pushGroupsConf = PropReader.getString("qq.bot.pushGroups");
            if (StringUtils.isBlank(pushGroupsConf))
                return;
            // Push to all groups
            if (StringUtils.equals(pushGroupsConf, "*")) {
                int totalUserCount = 0;
                int groupCount = 0;
                // 如果没有可供推送的群（群都推送过了）
                if (UNPUSH_GROUPS.isEmpty())
                    reloadGroups();

                for (final Map.Entry<Long, Group> entry : QQ_GROUPS.entrySet()) {
                    long groupId = 0;
                    int userCount = 0;

                    try {
                        final Group group = entry.getValue();
                        groupId = group.getId();

                        final GroupInfo groupInfo = robot.getGroupInfo(group.getCode());
                        userCount = groupInfo.getUsers().size();
                        // 把人不多的群过滤掉
                        if (userCount < PUSH_GROUP_USER_COUNT) {
                            UNPUSH_GROUPS.remove(groupId);
                            continue;
                        }
                        // 如果该群已经被推送过则跳过本次推送
                        if (!UNPUSH_GROUPS.contains(groupId))
                            continue;
                        // 如果本次群推操作已推送群数大于设定的阈值
                        if (groupCount >= PUSH_GROUP_COUNT)
                            break;

                        logger.info("群发 [" + msg + "] 到 QQ 群 [" + group.getName() + ", 成员数=" + userCount + "]");
                        robot.sendMessageToGroup(groupId, msg); // Without retry

                        UNPUSH_GROUPS.remove(groupId); // 从未推送中移除（说明已经推送过）

                        totalUserCount += userCount;
                        groupCount++;

                        Thread.sleep(1000 * 10);
                    } catch (final Exception e) {
                        logger.error("群发异常", e);
                    }
                }

                logger.info("一共推送了 [" + groupCount + "] 个群，覆盖 [" + totalUserCount + "] 个 QQ");

                return;
            }

            // Push to the specified groups
            final String[] groups = pushGroupsConf.split(",");
            for (final Map.Entry<Long, Group> entry : QQ_GROUPS.entrySet()) {
                final Group group = entry.getValue();
                final String name = group.getName();

                if (com.gimo.robot.qq.web.util.StringUtils.contains(name, groups)) {
                    final GroupInfo groupInfo = robot.getGroupInfo(group.getCode());
                    final int userCount = groupInfo.getUsers().size();

                    if (userCount < 100)
                        continue;
                    logger.info("Pushing [msg=" + msg + "] to QQ qun [" + group.getName() + "]");
                    robot.sendMessageToGroup(group.getId(), msg);
                    Thread.sleep(1000 * 10);
                }
            }
        } catch (final Exception e) {
            logger.error("Push message [" + msg + "] to groups failed", e);
        }

    }

    /**
     * 加载qq群
     */
    private void reloadGroups() {
        final List<Group> groups = robot.getGroupList();
        QQ_GROUPS.clear();
        GROUP_AD_TIME.clear();
        UNPUSH_GROUPS.clear();

        final StringBuilder msgBuilder = new StringBuilder();
        msgBuilder.append("Reloaded groups: \n");
        for (final Group g : groups) {
            QQ_GROUPS.put(g.getId(), g);
            GROUP_AD_TIME.put(g.getId(), 0L);
            UNPUSH_GROUPS.add(g.getId());
            msgBuilder.append("    ").append(g.getName()).append(": ").append(g.getId()).append("\n");
        }
        logger.info(msgBuilder.toString());
    }

    /**
     * 讨论组消息监听处理
     * @param message 消息
     */
    private void onQQDiscussMessage(final DiscussMessage message) {
        final long discussId = message.getDiscussId();

        final String content = message.getContent();
        final String userName = Long.toHexString(message.getUserId());

        String msg = "";
        if (StringUtils.contains(content, PropReader.QQ_BOT_NAME)
                || (StringUtils.length(content) > 6
                && (StringUtils.contains(content, "?") || StringUtils.contains(content, "？") || StringUtils.contains(content, "问"))))
            msg = answer(content, userName);

        if (StringUtils.isBlank(msg))
            return;

        if (RandomUtils.nextFloat() >= 0.9) {
            Long latestAdTime = DISCUSS_AD_TIME.get(discussId);
            if (null == latestAdTime) {
                latestAdTime = 0L;
            }

            final long now = System.currentTimeMillis();

            if (now - latestAdTime > 1000 * 60 * 30) {
                msg = msg + "\n(" + ADS.get(RandomUtils.nextInt(ADS.size())) + ")";
                DISCUSS_AD_TIME.put(discussId, now);
            }
        }

        /*DiscussInfo discussInfo = robot.getDiscussInfo(discussId);
        String nick = "";
        List<DiscussUser> users = discussInfo.getUsers();
        for (DiscussUser user : users) {
            if (user.getUin() == message.getUserId()) {
                nick = user.getNick();
            }
        }
        msg = "管家对[" + nick + "]说:“" + msg + "”";*/
        sendMessageToDiscuss(discussId, msg);
    }

    /**
     * 向讨论组发送消息 递归 包含ack验证
     * @param discussId 讨论组的id
     * @param msg 消息
     */
    private void sendMessageToDiscuss(final Long discussId, final String msg) {
        Discuss discuss = QQ_DISCUSSES.get(discussId);
        if (null == discuss) {
            reloadDiscusses();
            discuss = QQ_DISCUSSES.get(discussId);
        }

        if (null == discuss) {
            logger.error("Discuss list error [discussId=" + discussId + "]");
            return;
        }

        logger.info("Pushing [msg=" + msg + "] to QQ discuss [" + discuss.getName() + "]");
        robot.sendMessageToDiscuss(discussId, msg);

        if (MSG_ACK_ENABLED) { // 如果启用了消息送达确认
            // 进行消息重发
            DISCUSS_SENT_MSGS.add(msg);
            if (DISCUSS_SENT_MSGS.size() > QQ_DISCUSSES.size() * 5) {
                DISCUSS_SENT_MSGS.remove(0);
            }
            final int maxRetries = 3;
            int retries = 0;
            int sentTries = 0;
            while (retries < maxRetries) {
                retries++;
                try {
                    Thread.sleep(3500);
                } catch (final Exception e) {
                    continue;
                }
                if (GROUP_SENT_MSGS.contains(msg)) {
                    logger.info("Pushing [msg=" + msg + "] to QQ discuss [" + discuss.getName() + "] with retries ["
                            + retries + "]");
                    robot.sendMessageToDiscuss(discussId, msg);
                    sentTries++;
                }
            }
            if (maxRetries == sentTries) {
                logger.info("Pushing [msg=" + msg + "] to QQ discuss [" + discuss.getName() + "]");
                robot.sendMessageToDiscuss(discussId, NO_LISTENER);
            }
        }
    }

    /**
     * 加载讨论组
     */
    private void reloadDiscusses() {
        final List<Discuss> discusses = robot.getDiscussList();
        QQ_DISCUSSES.clear();
        DISCUSS_AD_TIME.clear();

        final StringBuilder msgBuilder = new StringBuilder();
        msgBuilder.append("Reloaded discusses: \n");
        for (final Discuss d : discusses) {
            QQ_DISCUSSES.put(d.getId(), d);
            DISCUSS_AD_TIME.put(d.getId(), 0L);
            msgBuilder.append("    ").append(d.getName()).append(": ").append(d.getId()).append("\n");
        }
        logger.info(msgBuilder.toString());
    }

    /**
     * qq群消息监听处理
     * @param message 消息
     */
    public void onQQGroupMessage(final GroupMessage message) {
        final long groupId = message.getGroupId();

        final String content = message.getContent();
        final String userName = Long.toHexString(message.getUserId());

        String msg = "";
        if (StringUtils.contains(content, PropReader.QQ_BOT_NAME)
                || (StringUtils.length(content) > 6
                && (StringUtils.contains(content, "?") || StringUtils.contains(content, "？") || StringUtils.contains(content, "问"))))
        msg = answer(content, userName);

        if (StringUtils.isBlank(msg)) {
            return;
        }

        if (RandomUtils.nextFloat() >= 0.9) {
            Long latestAdTime = GROUP_AD_TIME.get(groupId);
            if (null == latestAdTime) {
                latestAdTime = 0L;
            }

            final long now = System.currentTimeMillis();

            if (now - latestAdTime > 1000 * 60 * 30) {
                msg = msg + "\n(" + ADS.get(RandomUtils.nextInt(ADS.size())) + ")";

                GROUP_AD_TIME.put(groupId, now);
            }
        }

        reloadGroups();

        /*Long groupCode = 0l;
        for (final Map.Entry<Long, Group> entry : QQ_GROUPS.entrySet()) {
            final Group group = entry.getValue();
            if (group.getId() == message.getGroupId()) {
                groupCode = group.getCode();
                break;
            }
        }

        GroupInfo groupInfo = robot.getGroupInfo(groupCode);
        String nick = "";
        List<GroupUser> users = groupInfo.getUsers();
        for (GroupUser user : users) {
            if (user.getUin() == message.getUserId()) {
                nick = user.getNick();
                break;
            }
        }
        msg = "管家对[" + nick + "]说:“" + msg + "”";*/
        sendMessageToGroup(groupId, msg);
    }

    /**
     * 向qq群回复消息
     * @param groupId 群id
     * @param msg 消息
     */
    private void sendMessageToGroup(final Long groupId, final String msg) {
        Group group = QQ_GROUPS.get(groupId);
        if (null == group) {
            reloadGroups();
            group = QQ_GROUPS.get(groupId);
        }
        if (null == group) {
            logger.error("Group list error [groupId=" + groupId + "]");
            return;
        }

        logger.info("Pushing [msg=" + msg + "] to QQ qun [" + group.getName() + "]");
        robot.sendMessageToGroup(groupId, msg);

        // 如果启用了消息送达确认
        if (MSG_ACK_ENABLED) {
            // 进行消息重发
            GROUP_SENT_MSGS.add(msg);
            if (GROUP_SENT_MSGS.size() > QQ_GROUPS.size() * 5) {
                GROUP_SENT_MSGS.remove(0);
            }
            final int maxRetries = 3;
            int retries = 0;
            int sentTries = 0;
            while (retries < maxRetries) {
                retries++;
                try {
                    Thread.sleep(3500);
                } catch (final Exception e) {
                    continue;
                }
                if (GROUP_SENT_MSGS.contains(msg)) {
                    logger.info("Pushing [msg=" + msg + "] to QQ qun [" + group.getName() + "] with retries [" + retries + "]");
                    robot.sendMessageToGroup(groupId, msg);
                    sentTries++;
                }
            }
            if (maxRetries == sentTries) {
                logger.info("Pushing [msg=" + msg + "] to QQ qun [" + group.getName() + "]");
                robot.sendMessageToGroup(groupId, NO_LISTENER);
            }
        }
    }

    /**
     * 调用外部机器人api 返回应答消息
     * @param content 消息体
     * @param userName 发送者
     * @return 机器人答复
     */
    private String answer(final String content, final String userName) {
        String keyword = "";
        String[] keywords = StringUtils.split(PropReader.getString("bot.follow.keywords"), ",");
        keywords = com.gimo.robot.qq.web.util.StringUtils.trimAll(keywords);
        for (final String kw : keywords) {
            if (StringUtils.containsIgnoreCase(content, kw)) {
                keyword = kw;
                break;
            }
        }
        String ret = "";
        if (StringUtils.isNotBlank(keyword)) {
            try {
                ret = PropReader.getString("bot.follow.keywordAnswer");
                ret = StringUtils.replace(ret, "{keyword}", URLEncoder.encode(keyword, "UTF-8"));
            } catch (final UnsupportedEncodingException e) {
                logger.error("Search key encoding failed", e);
            }
        } else if (StringUtils.isBlank(keyword)) {
            if (1 == QQ_BOT_TYPE) {
                ret = turingQueryService.chat(userName, content);
                ret = StringUtils.replace(ret, "图灵机器人", PropReader.QQ_BOT_NAME + "机器人");
                ret = StringUtils.replace(ret, "默认机器人", PropReader.QQ_BOT_NAME + "机器人");
                ret = StringUtils.replace(ret, "<br>", "\n");
            } else if (2 == QQ_BOT_TYPE) {
                ret = baiduQueryService.chat(content);
            } else if (3 == QQ_BOT_TYPE) {
                ret = itpkQueryService.chat(content);
            }

            if (StringUtils.isBlank(ret))
                ret = "崩溃了,麻烦通知老大修修...";
        }
        return ret;
    }
}
