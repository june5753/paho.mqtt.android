/*******************************************************************************
 * Copyright (c) 1999, 2016 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 */
package paho.mqtt.java.example;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import paho.mqtt.java.example.utils.TimeUtils;

/**
 * @author juneyang
 * MQTT测试主界面，测试连接、订阅等功能。
 */
public class PahoExampleActivity extends AppCompatActivity {
    private static final String TAG = "PahoExampleActivity";
    private HistoryAdapter mAdapter;

    MqttAndroidClient mqttAndroidClient;

    //    final String serverUri = "tcp://iot.eclipse.org:1883";
    /**
     * 更换为当前电脑联的IP地址
     */
    final String serverUri = "tcp://10.1.2.105:1883";

    /**
     * 客户端ID唯一，相同的会被逼下线,MQTT 也可能异常掉线
     */
    private String clientId = "Mi4-LTE";
    private RecyclerView mRecyclerView;
    private TextView tvSendCount;
    private TextView tvResponseCount;

    private long sendTime;
    private long responseTime;

    /*
     **
     * 订阅的主题（消息事件） pubAndroidTopic
     */

    final String subscriptionTopic = "subAndroidTopic";
    /**
     * 发布的主题（消息事件） subAndroidTopic
     */
    final String publishTopic = "pubAndroidTopic";

    final String publishMessageRequest = "请求的消息：request：{  \"action\":\"test\", \"num\":发送序号 ,\"time\": 设备A的发送时间戳, \"randomStr\":  \"随机字符串，要求长度60\"}" + TimeUtils.formatTime(System.currentTimeMillis());
    final String publishMessageResponse = "响应的消息：request：{  \"action\":\"test\", \"num\":发送序号 ,\"time\": 设备A的发送时间戳, \"randomStr\":  \"随机字符串，要求长度60\"}" + TimeUtils.formatTime(System.currentTimeMillis());

    private static final int MSG_SEND = 737;
    private static final int MSG_RESPONSE = 738;

    private MyHandler handler = new MyHandler(this);

    private static final int MAX = 2;
    private Boolean isBack = false;

    /**
     * 测试请求的次数
     */
    private int sendCount = 0;

    private long time1;
    private long time2;

    /**
     * 测试响应的次数
     */
    private int responseCount = 0;

    //Handler静态内部类
    private class MyHandler extends Handler {
        //弱引用
        WeakReference<PahoExampleActivity> weakReference;

        public MyHandler(PahoExampleActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            PahoExampleActivity activity = weakReference.get();
            if (activity != null) {
                switch (msg.what) {
                    case MSG_SEND:
                        if (sendCount == MAX) {
                            return;
                        }
                        //发送
                        sendCount++;
                        activity.publishMessage(false);
                        activity.tvSendCount.setText("当前请求次数:" + sendCount);
                        break;

                    case MSG_RESPONSE:
                        //响应后再次发送
                        if (responseCount == MAX) {
                            return;
                        }
                        responseCount++;
                        activity.publishMessage(true);
                        activity.tvResponseCount.setText("当前响应次数:" + responseCount);
                        break;
                    default:
                        break;
                }
                activity.mRecyclerView.scrollToPosition(activity.mAdapter.getItemCount());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Button fab = (Button) findViewById(R.id.fab);

        //发送
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isBack = false;
                sendCount = 0;
                responseCount = 0;

                tvSendCount.setText("当前请求次数:" + sendCount);
                tvResponseCount.setText("当前响应次数:" + responseCount);

                if (!mqttAndroidClient.isConnected()) {
                    Toast.makeText(PahoExampleActivity.this, "连接断开", Toast.LENGTH_SHORT).show();
                    return;
                }
                //并发测试
                for (int i = 0; i < MAX; i++) {
                }
                handler.sendEmptyMessage(MSG_SEND);
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.history_recycler_view);

        tvSendCount = (TextView) findViewById(R.id.tvCount);

        tvResponseCount = (TextView) findViewById(R.id.tvRespond);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new HistoryAdapter(new ArrayList<String>());
        mRecyclerView.setAdapter(mAdapter);

        findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isBack = false;
                sendCount = 0;
                responseCount = 0;
                mAdapter.clear();
            }
        });

        clientId = clientId + System.currentTimeMillis();

        ((TextView) findViewById(R.id.tv_ClientId)).setText("当前Client Id:" + clientId);

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);

        // 设置MQTT监听并且接受消息
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                if (reconnect) {
                    addToHistory("Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    subscribeToTopic();
                } else {
                    addToHistory("Connected to: " + serverURI);
                }
            }

            //连接断开
            @Override
            public void connectionLost(Throwable cause) {
                addToHistory("The Connection was lost,cause:" + cause.toString());
            }

            //订阅的消息送达，推送notify TODO:
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                addToHistory("topic" + topic + ",Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();

        //设置断开后重新连接
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        try {
            //addToHistory("Connecting to " + serverUri);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);

                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to connect to: " + serverUri);
                }
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }

    private void addToHistory(final String mainText) {
        System.out.println("LOG: " + mainText);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.add(mainText);
            }
        });
    }

    public void subscribeToTopic() {
        try {
            //订阅
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    addToHistory("Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to subscribe");
                }
            });

            //收到消息
            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    System.out.println("Message--: " + topic + " : " + new String(message.getPayload()));
                    handler.sendEmptyMessage(MSG_RESPONSE);
                    Log.d(TAG, "stop: 已到最大的次数");

                    if (!isBack) {
                        isBack = true;
                        addToHistory("第一次收到Message: " + topic + " : " + new String(message.getPayload()));
                        time1 = System.currentTimeMillis();
                        //响应之后发消息一次
                        publishMessage(true);
                    } else {
                        time2 = System.currentTimeMillis();
                        addToHistory("收到消息后响应: " + topic + " : " + new String(message.getPayload()) + "时间差：" + (time2 - time1));
                    }
                }
            });


        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public void publishMessage(Boolean isBack) {
        try {
            MqttMessage message = new MqttMessage();
            if (isBack) {
                message.setPayload(publishMessageResponse.getBytes());
                sendTime = System.currentTimeMillis();
                addToHistory("响应消息" + TimeUtils.formatTime(sendTime));
            } else {
                responseTime = System.currentTimeMillis();
                message.setPayload(publishMessageRequest.getBytes());
                addToHistory("发布消息:" + TimeUtils.formatTime(responseTime));
            }

            if (responseCount == MAX || sendCount == MAX) {
                Log.d(TAG, "stop: 已到最大的次数");
                return;
            }
            mqttAndroidClient.publish(publishTopic, message);

            //如果网络中断，消息进行缓存
            if (!mqttAndroidClient.isConnected()) {
                addToHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        try {
            mqttAndroidClient.disconnect(); //断开连接
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    //TODO:计算500次的响应时间
}
