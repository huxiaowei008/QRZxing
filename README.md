# QRZxing
[ ![bintray](https://api.bintray.com/packages/huxiaowei008/maven/QRZxing/images/download.svg) ](https://bintray.com/huxiaowei008/maven/QRZxing/_latestVersion)
[![License](http://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square) ](http://www.apache.org/licenses/LICENSE-2.0)

二维码扫描库封装

## 下载
```gradle
compile 'com.hxw.qr:qr:<latestVersion>'
```
## 使用
在布局中
```xml
    <com.hxw.qr.ZxingView
     android:id="@+id/zxing"
     android:layout_width="match_parent"
     android:layout_height="match_parent"/>
```
让后在activity的周期中绑定
```
    @Override
    protected void onDestroy() {
        zxingView.onDestroy();
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    protected void onPause() {
        zxingView.onPause();
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        zxingView.onResume();
        Log.d(TAG, "onResume");
    }
```
会自动请求相机权限，所以
```
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        zxingView.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
```

在onCreate中可以设置一下配置
```
     zxingView.onCreate(this, new CameraSetting()
              .setOnResultListener(new CameraSetting.ZxingResultListener() {
                  @Override
                  public void result(CharSequence result) {
                      Log.d(TAG, "解码结果：" + result.toString());
                      Toast.makeText(MainActivity.this, result.toString(), Toast.LENGTH_SHORT).show();
                  }
              }).setVibrate(true)
              .setPlayBeep(false)
              .setLightMode(FrontLightMode.OFF)
              .setViewfinderView(new ScanView(this)));

```

## License
```
Copyright huxiaowei008

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
