package com.yy.rongyuntest.activity;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.yy.rongyuntest.R;
import com.yy.rongyuntest.activity.okhttptest.TestOkhttpActivity;
import com.yy.rongyuntest.bean.User;
import com.yy.rongyuntest.constant.ConstantUrl;
import com.yy.rongyuntest.utils.RongCloudMethodUtil;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.IOException;
import java.util.HashMap;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;
import cn.smssdk.gui.RegisterPage;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Request;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener {
    private Button id_bt_bind;
    private Button bt_register;
    private Button bt_okhttptest;

    private TextView tv_phone;
    private String phone;
    private String country;

    //添加
    private EditText et_nickname;
    private EditText et_password;
    private EditText et_confirm_password;

    private String token = null;  //获取的token
    private User user;

    private RongCloudMethodUtil RongUtil;

    private String userJsonString;


    private TextView tv_message;
    private static final String TAG = "RegisterActivity";

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String token_state = bundle.getString("token");
            if (token_state == "token_success") {
                //开始执行第二个请求
                System.out.println("开始执行第二个请求");
                tv_message.setText(token);
                try {
                    SendHttpRegister();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initView();
        initEvent();
    }


    private void initView() {
        id_bt_bind = (Button) findViewById(R.id.id_bt_bind);

        bt_register = (Button) findViewById(R.id.id_bt_register);
        bt_okhttptest = (Button) findViewById(R.id.id_bt_okhttptest);
        tv_phone = (TextView) findViewById(R.id.id_et_phone);

        et_nickname = (EditText) findViewById(R.id.id_et_nickname);
        et_password = (EditText) findViewById(R.id.id_et_password);
        et_confirm_password = (EditText) findViewById(R.id.id_et_confirm_password);

        tv_message = (TextView) findViewById(R.id.id_tv_message);
    }

    private void initEvent() {

        id_bt_bind.setOnClickListener(this);
        bt_okhttptest.setOnClickListener(this);
        bt_register.setOnClickListener(this);

    }

    /**
     * okhttp接口测试
     */
    private void OkhttpTest() {
        //测试界面
        Intent intent = new Intent(RegisterActivity.this, TestOkhttpActivity.class);
        startActivity(intent);
    }

    /**
     * 绑定手机号
     */
    private void BindPhone() {
        //打开注册页面
        RegisterPage registerPage = new RegisterPage();
        registerPage.setRegisterCallback(new EventHandler() {
            public void afterEvent(int event, int result, Object data) {
                // 解析注册结果
                if (result == SMSSDK.RESULT_COMPLETE) {
                    //短信验证
                    System.out.println("短信验证");
                    @SuppressWarnings("unchecked")
                    HashMap<String, Object> phoneMap = (HashMap<String, Object>) data;
                    country = (String) phoneMap.get("country");
                    phone = (String) phoneMap.get("phone");
                    System.out.println("手机号：" + phone);
                    System.out.println("国家：" + country);
                    tv_phone.setText(phone);
                } else if (result == SMSSDK.RESULT_ERROR) {  //验证错误
                    System.out.println("验证错误");
                    System.out.println("手机号：" + phone);
                    System.out.println("国家：" + country);
                }
            }
        });
        registerPage.show(RegisterActivity.this);
    }


    /**
     * 获取用户输入的数据
     *
     * @return
     */
    public void obtainUserRegisterInfo() {

        //获取用户输入的值
        //昵称
        String nickname = et_nickname.getText().toString();
        //密码
        String pass = et_password.getText().toString();

        //确认密码
        String confirm_pass = et_confirm_password.getText().toString();

        System.out.println(phone + "/" + pass + "/" + confirm_pass);

        //先验证填写是否为空
        if (phone == null) {
            Toast.makeText(RegisterActivity.this, "需要先进行手机验证", Toast.LENGTH_SHORT).show();
        } else if (nickname.equals("")) {
            Toast.makeText(RegisterActivity.this, "昵称不能为空", Toast.LENGTH_SHORT).show();
        } else if (pass.equals("") || confirm_pass.equals("") || (pass.equals("") && confirm_pass.equals(""))) {  //密码和确认密码不同
            Toast.makeText(RegisterActivity.this, "密码和确认密码不能为空", Toast.LENGTH_SHORT).show();
        } else if (!pass.equals(confirm_pass)) {  //密码和确认密码不同
            Toast.makeText(RegisterActivity.this, "密码和确认密码不同", Toast.LENGTH_SHORT).show();
        } else if (phone != null && !nickname.equals("") && !pass.equals("") && !confirm_pass.equals("") && pass.equals(confirm_pass)) {  //如果为空请重新填写
            user = new User();
            user.setPhone(phone);
            user.setNickname(nickname);
            user.setPassword(pass);

            token = obtainUserToken(phone, nickname, ConstantUrl.imageDefaultUrl);  //获取token
            user.setToken(token);
        }

    }


    /**
     * 获取token
     *
     * @param phone    用户id（手机号作为用户id)
     * @param nickname 用户昵称
     * @param urlPath  默认头像地址
     * @return
     */
    private String obtainUserToken(final String phone, final String nickname, final String urlPath) {

        if (phone != null && nickname != null && urlPath != null) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    token = RongUtil.getToken(phone, nickname, urlPath);  //获取token
                    System.out.println("RegisterActivity+token:" + token);
                    Message msg = handler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putString("token", "token_success");
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                }
            };
            thread.start();
        }
        return token;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.id_bt_bind:  //手机绑定
                BindPhone();
                break;
            case R.id.id_bt_okhttptest:
                OkhttpTest();
                break;
            case R.id.id_bt_register:
                obtainUserRegisterInfo();


                break;
            default:
                break;
        }
    }

    /**
     * 发起注册请求
     *
     * @throws IOException
     */
    private void SendHttpRegister() throws IOException {
        //添加圆形进度条（封装好，随时可以调用）

        userJsonString = new Gson().toJson(new User(phone, user.getNickname(), user.getPassword(), token));
        //发起注册请求
        OkHttpUtils
                .postString()
                .url(ConstantUrl.userUrl + ConstantUrl.userlogin_interface)
                .mediaType(MediaType.parse("application/json; charset=utf-8"))
                .content(userJsonString)
                .build()
                .execute(new MyStringCallback());
        System.out.println("json处理后格式：" + userJsonString);
    }

    public class MyStringCallback extends StringCallback {
        @Override
        public void onBefore(Request request, int id) {
            setTitle("loading...");
        }

        @Override
        public void onAfter(int id) {
            setTitle("Sample-okHttp");
        }

        @Override
        public void onError(Call call, Exception e, int id) {
            e.printStackTrace();
            System.out.println("请求地址：" + ConstantUrl.userUrl + ConstantUrl.userlogin_interface + userJsonString);
            tv_message.setText("onError:" + e.getMessage());
        }

        @Override
        public void onResponse(String response, int id) {
            switch (id) {
                case 100:
                    Toast.makeText(RegisterActivity.this, "http", Toast.LENGTH_SHORT).show();
                    break;
                case 101:
                    Toast.makeText(RegisterActivity.this, "https", Toast.LENGTH_SHORT).show();
                    break;
            }
            Log.e(TAG, "onResponse：complete");
            tv_message.setText("onResponse:" + response);
            System.out.println("注册成功");
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));  //跳转到登录界面
        }

        @Override
        public void inProgress(float progress, long total, int id) {
            Log.e(TAG, "inProgress:" + progress);
        }
    }
}
