package com.kongzue.wifilinker;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.kongzue.wifilinker.interfaces.OnWifiConnectStatusChangeListener;
import com.kongzue.wifilinker.interfaces.OnWifiScanListener;
import com.kongzue.wifilinker.util.WifiAutoConnectManager;
import com.kongzue.wifilinker.util.WifiInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author: Kongzue
 * @github: https://github.com/kongzue/
 * @homepage: http://kongzue.com/
 * @mail: myzcxhh@live.cn
 * @createTime: 2019/7/26 15:38
 */
public class WifiUtil {
    
    public static boolean DEBUGMODE = true;
    
    private Activity context;
    
    public static final int ERROR_DEVICE_NOT_HAVE_WIFI = -1;    //设备无Wifi模块
    public static final int ERROR_CONNECT = -2;                 //连接失败
    public static final int ERROR_CONNECT_SYS_EXISTS_SAME_CONFIG = -3;                 //连接失败：系统已存在相同Wifi配置（需手动删除已存储连接）
    public static final int ERROR_PASSWORD = -11;               //密码错误
    
    public static final int CONNECT_FINISH = 1;                 //已连接
    public static final int DISCONNECTED = 2;                   //已断开连接
    
    private boolean isLinked = false;
    
    private OnWifiScanListener onWifiScanListener;
    private OnWifiConnectStatusChangeListener onWifiConnectStatusChangeListener;
    
    private BroadcastReceiver mWifiSearchBroadcastReceiver;
    private IntentFilter mWifiSearchIntentFilter;
    private BroadcastReceiver mWifiConnectBroadcastReceiver;
    private IntentFilter mWifiConnectIntentFilter;
    private WifiAutoConnectManager mWifiAutoConnectManager;
    
    public WifiUtil(Activity context) {
        this.context = context;
        
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiAutoConnectManager = WifiAutoConnectManager.newInstance(wifiManager);
        
        init();
    }
    
    private List<ScanResult> mScanResultList = new ArrayList<>();
    
    private void init() {
        mWifiSearchBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {// 扫描结果改表
                    mScanResultList = WifiAutoConnectManager.getScanResults();
                    if (onWifiScanListener != null) {
                        onWifiScanListener.onScan(mScanResultList);
                    }
                }
            }
        };
        mWifiSearchIntentFilter = new IntentFilter();
        mWifiSearchIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mWifiSearchIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mWifiSearchIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        
        //wifi 状态变化接收广播
        mWifiConnectBroadcastReceiver = new BroadcastReceiver() {
            
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                    int wifState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                    if (wifState != WifiManager.WIFI_STATE_ENABLED) {
                        error("Wifi模块启动失败");
                        if (onWifiConnectStatusChangeListener != null) {
                            onWifiConnectStatusChangeListener.onStatusChange(false, ERROR_DEVICE_NOT_HAVE_WIFI);
                        }
                    }
                } else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                    int linkWifiResult = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 123);
                    if (linkWifiResult == WifiManager.ERROR_AUTHENTICATING) {
                        error("密码错误");
                        if (onWifiConnectStatusChangeListener != null) {
                            onWifiConnectStatusChangeListener.onStatusChange(false, ERROR_PASSWORD);
                        }
                    }
                } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo.DetailedState state = ((NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).getDetailedState();
                    setWifiState(state);
                }
            }
        };
        mWifiConnectIntentFilter = new IntentFilter();
        mWifiConnectIntentFilter.addAction(WifiManager.ACTION_PICK_WIFI_NETWORK);
        mWifiConnectIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mWifiConnectIntentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        mWifiConnectIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        
        //注册接收器
        context.registerReceiver(mWifiSearchBroadcastReceiver, mWifiSearchIntentFilter);
        context.registerReceiver(mWifiConnectBroadcastReceiver, mWifiConnectIntentFilter);
    }
    
    private void setWifiState(NetworkInfo.DetailedState state) {
        if (state == NetworkInfo.DetailedState.AUTHENTICATING) {
            log("认证中");
        } else if (state == NetworkInfo.DetailedState.BLOCKED) {
            log("阻塞");
        } else if (state == NetworkInfo.DetailedState.CONNECTED) {
            isLinked = true;
            log("连接成功");
            if (onWifiConnectStatusChangeListener != null) {
                onWifiConnectStatusChangeListener.onStatusChange(true, CONNECT_FINISH);
                onWifiConnectStatusChangeListener.onConnect(new WifiInfo(
                        WifiAutoConnectManager.getSSID(),
                        WifiAutoConnectManager.getIpAddress(),
                        WifiAutoConnectManager.getMacAddress(),
                        WifiAutoConnectManager.getGateway()
                ));
            }
        } else if (state == NetworkInfo.DetailedState.CONNECTING) {
            isLinked = false;
            log("连接中: " + WifiAutoConnectManager.getSSID());
        } else if (state == NetworkInfo.DetailedState.DISCONNECTED) {
            isLinked = false;
            log("已断开连接");
            if (onWifiConnectStatusChangeListener != null) {
                onWifiConnectStatusChangeListener.onStatusChange(true, DISCONNECTED);
            }
        } else if (state == NetworkInfo.DetailedState.DISCONNECTING) {
            isLinked = false;
            log("断开连接中");
        } else if (state == NetworkInfo.DetailedState.FAILED) {
            isLinked = false;
            if (onWifiConnectStatusChangeListener != null) {
                onWifiConnectStatusChangeListener.onStatusChange(false, ERROR_CONNECT);
            }
            log("连接失败");
        } else if (state == NetworkInfo.DetailedState.IDLE) {
        
        } else if (state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
        
        } else if (state == NetworkInfo.DetailedState.SCANNING) {
            log("搜索中");
        } else if (state == NetworkInfo.DetailedState.SUSPENDED) {
        
        }
    }
    
    public void close() {
        try {
            context.unregisterReceiver(mWifiSearchBroadcastReceiver);
            context.unregisterReceiver(mWifiConnectBroadcastReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private WifiAutoConnectManager.WifiCipherType type = WifiAutoConnectManager.WifiCipherType.WIFICIPHER_NOPASS;
    
    private String ssid;
    private String password;
    
    public void link(String ssid, String password, OnWifiConnectStatusChangeListener listener) {
        if (mScanResultList.isEmpty()) {
            error("此连接方式需要先进行查找");
            return;
        }
        this.ssid = ssid;
        this.password = password;
        onWifiConnectStatusChangeListener = listener;
        type = WifiAutoConnectManager.getCipherType(ssid);
        
        if (ssid.equals(WifiAutoConnectManager.getSSID())) {
            log("已连接");
            return;
        }
        if (mConnectAsyncTask != null) {
            mConnectAsyncTask.cancel(true);
            mConnectAsyncTask = null;
        }
        mConnectAsyncTask = new ConnectAsyncTask(ssid, password, type);
        mConnectAsyncTask.execute();
    }
    
    
    private ConnectAsyncTask mConnectAsyncTask = null;
    
    public void link(String ssid, String password, WifiAutoConnectManager.WifiCipherType wifiCipherType, OnWifiConnectStatusChangeListener listener) {
        this.ssid = ssid;
        this.password = password;
        type = wifiCipherType;
        onWifiConnectStatusChangeListener = listener;
        
        if (ssid.equals(WifiAutoConnectManager.getSSID())) {
            log("已连接");
            return;
        }
        if (mConnectAsyncTask != null) {
            mConnectAsyncTask.cancel(true);
            mConnectAsyncTask = null;
        }
        mConnectAsyncTask = new ConnectAsyncTask(ssid, password, type);
        mConnectAsyncTask.execute();
    }
    
    private WorkAsyncTask mWorkAsyncTask = null;
    
    public void scan(OnWifiScanListener listener) {
        onWifiScanListener = listener;
        if (mWorkAsyncTask != null) {
            mWorkAsyncTask.cancel(true);
            mWorkAsyncTask = null;
        }
        mWorkAsyncTask = new WorkAsyncTask();
        mWorkAsyncTask.execute();
    }
    
    /**
     * 获取wifi列表
     */
    private class WorkAsyncTask extends AsyncTask<Void, Void, List<ScanResult>> {
        private List<ScanResult> mScanResult = new ArrayList<>();
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            log("搜索中...");
            if (onWifiScanListener != null) {
                onWifiScanListener.onScanStart();
            }
        }
        
        @Override
        protected List<ScanResult> doInBackground(Void... params) {
            if (WifiAutoConnectManager.startStan()) {
                mScanResult = WifiAutoConnectManager.getScanResults();
            }
            List<ScanResult> filterScanResultList = new ArrayList<>();
            if (mScanResult != null) {
                for (ScanResult wifi : mScanResult) {
                    filterScanResultList.add(wifi);
                    log("查找到：" + wifi);
                }
            }
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return filterScanResultList;
        }
        
        @Override
        protected void onPostExecute(final List<ScanResult> result) {
            super.onPostExecute(result);
            mScanResultList = result;
            if (onWifiScanListener != null) {
                onWifiScanListener.onScanStop(mScanResultList);
            }
            
        }
    }
    
    /**
     * 连接指定的wifi
     */
    class ConnectAsyncTask extends AsyncTask<Void, Void, Boolean> {
        private String ssid;
        private String password;
        private WifiAutoConnectManager.WifiCipherType type;
        WifiConfiguration tempConfig;
        
        public ConnectAsyncTask(String ssid, String password, WifiAutoConnectManager.WifiCipherType type) {
            this.ssid = ssid;
            this.password = password;
            this.type = type;
        }
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            log("开始连接");
        }
        
        @Override
        protected Boolean doInBackground(Void... voids) {
            // 打开wifi
            mWifiAutoConnectManager.openWifi();
            // 开启wifi功能需要一段时间(我在手机上测试一般需要1-3秒左右)，所以要等到wifi，状态变成WIFI_STATE_ENABLED的时候才能执行下面的语句
            while (mWifiAutoConnectManager.wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    error("错误");
                    if (DEBUGMODE) ie.printStackTrace();
                }
            }
            
            tempConfig = mWifiAutoConnectManager.isExsits(ssid);
            //禁掉所有wifi
            for (WifiConfiguration c : mWifiAutoConnectManager.wifiManager.getConfiguredNetworks()) {
                mWifiAutoConnectManager.wifiManager.disableNetwork(c.networkId);
            }
            if (tempConfig != null) {
                log(ssid + "是已存在配置，尝试连接");
                boolean result = mWifiAutoConnectManager.wifiManager.enableNetwork(tempConfig.networkId, true);
                if (!isLinked && type != WifiAutoConnectManager.WifiCipherType.WIFICIPHER_NOPASS) {
                    try {
                        Thread.sleep(5000);//超过5s提示失败
                        if (!isLinked) {
                            log(ssid + "连接失败");
                            mWifiAutoConnectManager.wifiManager.disableNetwork(tempConfig.networkId);
                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (onWifiConnectStatusChangeListener != null) {
                                        onWifiConnectStatusChangeListener.onStatusChange(false, ERROR_CONNECT_SYS_EXISTS_SAME_CONFIG);
                                    }
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return result;
            } else {
                log(ssid + "是新的配置，开始连接");
                if (type != WifiAutoConnectManager.WifiCipherType.WIFICIPHER_NOPASS) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            WifiConfiguration wifiConfig = mWifiAutoConnectManager.createWifiInfo(ssid, password, type);
                            if (wifiConfig == null) {
                                error("错误：Wifi配置为null");
                                return;
                            }
                            log("开始连接：" + wifiConfig.SSID);
                            
                            int netID = mWifiAutoConnectManager.wifiManager.addNetwork(wifiConfig);
                            boolean enabled = mWifiAutoConnectManager.wifiManager.enableNetwork(netID, true);
                            
                            log("设置网络配置：" + enabled);
                        }
                    }).start();
                } else {
                    WifiConfiguration wifiConfig = mWifiAutoConnectManager.createWifiInfo(ssid, password, type);
                    if (wifiConfig == null) {
                        error("错误：Wifi配置为null");
                        return false;
                    }
                    log("开始连接：" + wifiConfig.SSID);
                    int netID = mWifiAutoConnectManager.wifiManager.addNetwork(wifiConfig);
                    boolean enabled = mWifiAutoConnectManager.wifiManager.enableNetwork(netID, true);
                    
                    log("设置网络配置：" + enabled);
                    return enabled;
                }
                return false;
            }
        }
        
        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            mConnectAsyncTask = null;
        }
    }
    
    public void disconnect(){
        WifiConfiguration tempConfig = mWifiAutoConnectManager.isExsits(ssid);
        if (tempConfig != null) {
            mWifiAutoConnectManager.wifiManager.removeNetwork(tempConfig.networkId);
            mWifiAutoConnectManager.wifiManager.saveConfiguration();
        }
    
        for (WifiConfiguration c : mWifiAutoConnectManager.wifiManager.getConfiguredNetworks()) {
            mWifiAutoConnectManager.wifiManager.disableNetwork(c.networkId);
        }
        mWifiAutoConnectManager.wifiManager.disconnect();
        mWifiAutoConnectManager.closeWifi();
    }
    
    private void error(Object msg) {
        Log.e(">>>", "WifiUtil: " + msg);
    }
    
    private void log(Object msg) {
        Log.i(">>>", "WifiUtil: " + msg);
    }
}
