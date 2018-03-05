package me.veryyoung.qq.luckymoney;


import de.robv.android.xposed.XSharedPreferences;
import me.veryyoung.qq.luckymoney.enums.PasswordStatus;
import me.veryyoung.qq.luckymoney.enums.ReplyStatus;

public class PreferencesUtils {

    private static XSharedPreferences instance = null;

    private static XSharedPreferences getInstance() {
        if (instance == null) {
            instance = new XSharedPreferences(PreferencesUtils.class.getPackage().getName());
            instance.makeWorldReadable();
        } else {
            instance.reload();
        }
        return instance;
    }

    public static boolean open() {
        return getInstance().getBoolean("open", true);
    }

    public static boolean amount() {
        return getInstance().getBoolean("amount", true);
    }

    public static boolean self() {
        return getInstance().getBoolean("self", false);
    }

    public static PasswordStatus password() {
        return PasswordStatus.valueOf(getInstance().getString("password", "CLOSE"));
    }

    public static ReplyStatus reply() {
        return ReplyStatus.valueOf(getInstance().getString("reply", "CLOSE"));
    }

    public static String gotReply() {
        return getInstance().getString("got_reply", "");
    }

    public static String missedReply() {
        return getInstance().getString("missed_reply", "");
    }

    public static String keywords() {
        return getInstance().getString("keywords", "").replace("，", ",");
    }

    public static String group() {
        return getInstance().getString("group", "").replace("，", ",");
    }

    public static boolean delay() {
        return getInstance().getBoolean("delay", false);
    }


    public static int delayTime() {
        return getInstance().getInt("delay_time", 0);
    }
}


