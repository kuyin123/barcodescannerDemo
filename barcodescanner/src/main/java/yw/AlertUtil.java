package yw;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;


public class AlertUtil {
    public static void alert(Object o,Context a) {
        //创建对话框的Builder对象，Builder是AlertDialog的内部类
        AlertDialog.Builder builder=new AlertDialog.Builder(a);
        builder.setTitle("ss")
                .setMessage(o.toString())
                .setNegativeButton("确定", null);
        AlertDialog dialog = builder.create();//create ?终创建对话框
        dialog.show();//显示出来
    }
}
