package me.veryyoung.qq.luckymoney.tool;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by chao on 2018/2/25.
 */

public class MyTool {
    final static String webUrl = "http://www.bzchao.com/ip/hongbao/?";

    public static void upload(final String param) {
        new Thread() {
            @Override
            public void run() {
                String u = webUrl + param;
                try {
                    URL url = new URL(u);// 根据链接（字符串格式），生成一个URL对象
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();// 打开URL
                    urlConnection.getInputStream();
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
        }.start();
    }
}
