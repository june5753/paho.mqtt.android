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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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

import java.util.ArrayList;

/**
 * @author juneyang
 * MQTT测试主界面，测试连接、订阅等功能。
 */
public class PahoExampleActivity extends AppCompatActivity {
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
    /**
     * 订阅的主题（消息事件）
     */
    final String subscriptionTopic = "chat/room/animals/client/";

    /**
     * 发布的主题（消息事件）
     */
    final String publishTopic = subscriptionTopic;

    final String publishMessageRequest = "请求的消息：request：{  \"action\":\"test\", \"num\":发送序号 ,\"time\": 设备A的发送时间戳, \"randomStr\":  \"随机字符串，要求长度60\"}";
    final String publishMessageResponse = "响应的消息：request：{  \"action\":\"test\", \"num\":发送序号 ,\"time\": 设备A的发送时间戳, \"randomStr\":  \"随机字符串，要求长度60\"}";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);

        Button fab = (Button) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                publishMessage(false);
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.history_recycler_view);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new HistoryAdapter(new ArrayList<String>());
        mRecyclerView.setAdapter(mAdapter);

        findViewById(R.id.btnClear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                addToHistory("The Connection was lost.");
            }

            //订阅的消息送达，推送notify
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                addToHistory("Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
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

    private void addToHistory(String mainText) {
        System.out.println("LOG: " + mainText);
        mAdapter.add(mainText);
        //自动滚动
        mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount());
    }

    public void subscribeToTopic() {
        try {
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

            // THIS DOES NOT WORK!
            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    System.out.println("Message: " + topic + " : " + new String(message.getPayload()));
                    addToHistory("Message: " + topic + " : " + new String(message.getPayload()));

                    //测试：收到消息后，马上再发回来 获取时间响应时间差
                    publishMessage(true);
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
            } else {
                message.setPayload(publishMessageRequest.getBytes());
            }
            mqttAndroidClient.publish(publishTopic, message);
            addToHistory("发布消息");

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
    protected void onDestroy() {
        super.onDestroy();
    }
}
