# WifiLinker
Wifi连接封装库，适用于智能硬件Wifi通讯。

<a href="https://github.com/kongzue/WifiLinker/">
<img src="https://img.shields.io/badge/WifiLinker-1.0.0-green.svg" alt="Kongzue WifiLinker">
</a>
<a href="https://bintray.com/myzchh/maven/WifiLinker">
<img src="https://img.shields.io/badge/Maven-1.0.0-blue.svg" alt="Maven">
</a>
<a href="http://www.apache.org/licenses/LICENSE-2.0">
<img src="https://img.shields.io/badge/License-Apache%202.0-red.svg" alt="License">
</a>
<a href="http://www.kongzue.com">
<img src="https://img.shields.io/badge/Homepage-Kongzue.com-brightgreen.svg" alt="Homepage">
</a>

Demo下载：<https://fir.im/WifiLinker>

## 前言

本库主要是针对Wifi连接过程的封装

## 使用方法
1) 从 Maven 仓库或 jCenter 引入：
Maven仓库：
```
<dependency>
  <groupId>com.kongzue.smart</groupId>
  <artifactId>wifilinker</artifactId>
  <version>1.0.0</version>
  <type>pom</type>
</dependency>
```
Gradle：
在dependencies{}中添加引用：
```
implementation 'com.kongzue.smart:wifilinker:1.0.0'
```

## 关于权限
您需要申请蓝牙权限后才可以正常使用

主要权限：
```
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

需要申请：
```
Manifest.permission.ACCESS_COARSE_LOCATION,
Manifest.permission.ACCESS_FINE_LOCATION
```

若不声明或申请权限，可能导致无法查找到要连接的目标设备，或无法正常使用功能。

创建 wifiUtil 对象：
```
wifiUtil = new WifiUtil(this);
```

### 查找

1) 查找附近 Wifi
```
wifiUtil.scan(new OnWifiScanListener() {
    @Override
    public void onScan(List<ScanResult> result) {
        //自行处理 result
    }
});
```

2) 停止查找
```
wifiUtil.stopScan();
```

### 连接

对于未知类型的 Wifi，需要先进行查找附近的 Wifi，然后执行：
```
wifiUtil.link(ssid, password, new OnWifiConnectStatusChangeListener() {
    @Override
    public void onStatusChange(boolean isSuccess, int statusCode) {
        //根据 statusCode 判断连接状态
        switch (statusCode){
                    case ERROR_DEVICE_NOT_HAVE_WIFI:
                        txtLog.setText("错误：设备无Wifi");
                        break;
                    case ERROR_CONNECT:
                        txtLog.setText("错误：连接失败");
                        break;
                    case ERROR_CONNECT_SYS_EXISTS_SAME_CONFIG:
                        txtLog.setText("错误：设备已存在相同Wifi配置");
                        break;
                    case ERROR_PASSWORD:
                        txtLog.setText("错误：密码错误");
                        break;
                    case CONNECT_FINISH:
                        txtLog.setText("已连接");
                        break;
                    case DISCONNECTED:
                        txtLog.setText("已断开连接");
                        break;
                }
    }
    @Override
    public void onConnect(WifiInfo wifiInfo) {
        //连接完成后获取 Wifi 信息
    }
});
```

对于已知类型的 Wifi，使用以下连接方式：
```
wifiUtil.link(ssid, password, WifiAutoConnectManager.WifiCipherType.WIFICIPHER_WPA,
        new OnWifiConnectStatusChangeListener() {
            @Override
            public void onStatusChange(boolean isSuccess, int statusCode) {
                switch (statusCode){
                    case ERROR_DEVICE_NOT_HAVE_WIFI:
                        txtLog.setText("错误：设备无Wifi");
                        break;
                    case ERROR_CONNECT:
                        txtLog.setText("错误：连接失败");
                        break;
                    case ERROR_CONNECT_SYS_EXISTS_SAME_CONFIG:
                        txtLog.setText("错误：设备已存在相同Wifi配置");
                        break;
                    case ERROR_PASSWORD:
                        txtLog.setText("错误：密码错误");
                        break;
                    case CONNECT_FINISH:
                        txtLog.setText("已连接");
                        break;
                    case DISCONNECTED:
                        txtLog.setText("已断开连接");
                        break;
                }
            }
            @Override
            public void onConnect(WifiInfo wifiInfo) {
                //连接完成后获取 Wifi 信息
            }
        }
);
```

额外方法：
```
//断开连接
wifiUtil.disconnect();
```

### 需要注意
请在 Activity 退出时执行 close() 方法：
```
@Override
protected void onDestroy() {
    wifiUtil.close();
    super.onDestroy();
}
```

## 开源协议
```
Copyright Kongzue WifiLinker

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## 更新日志
v1.0.0：
- 首次上传；