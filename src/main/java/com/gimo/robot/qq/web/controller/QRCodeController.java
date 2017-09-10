package com.gimo.robot.qq.web.controller;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

/**
 * 二维码控制器
 * Created by GimoXie on 2017/7/29.
 */
@Controller
public class QRCodeController {

    private static final Logger logger = LoggerFactory.getLogger(QRCodeController.class);

    @RequestMapping("/index")
    public void showQRCode(HttpServletResponse resp){
        resp.addHeader("Cache-Control", "no-store");

        OutputStream output = null;
        try {
            final String filePath = new File("qrcode.png").getCanonicalPath();
            final byte[] data = IOUtils.toByteArray(new FileInputStream(filePath));

            output = resp.getOutputStream();
            IOUtils.write(data, output);
            output.flush();
        } catch (final Exception e) {
            logger.error("在线显示二维码图片异常", e);
        } finally {
            IOUtils.closeQuietly(output);
        }
    }
}
