# 活体检测
根据提示做出相应动作，SDK 实时采集动态信息，判断用户是否为活体、真人

## 兼容性
| 条目        | 说明                                                                      |
| ----------- | -----------------------------------------------------------------------  |
| 适配版本    | minSdkVersion 16 及以上版本                                                 |
| cpu 架构    | 内部提供了 armeabi-v7a 和 arm64-v8a 两种 so ，对于不兼容 arm 的 x86 机型不适配 |

## 注意事项
<font color=red>***和其他易盾产品一起使用需要考虑版本兼容性，若同时接多个易盾sdk，需要测试回归下是否有异常***</font>

## 资源引入

### 远程仓库依赖(推荐)
从 2.2.2 版本开始，提供远程依赖的方式，本地依赖的方式逐步淘汰。本地依赖集成替换为远程依赖请先去除干净本地包，避免重复依赖冲突

确认 Project 根目录的 build.gradle 中配置了 mavenCentral 支持

```
buildscript {
    repositories {
        mavenCentral()
    }
    ...
}

allprojects {
    repositories {
        mavenCentral()
    }
}
```
在对应 module 的 build.gradle 中添加依赖

```
implementation 'io.github.yidun:livedetect:3.2.7'
implementation 'com.squareup.okhttp3:okhttp:4.9.1'    //若项目中原本存在无需重复添加 
```
### 本地手动依赖

#### 获取 SDK 

从易盾官网下载活体检测 sdk 的 aar 包 [包地址](https://support.dun.163.com/documents/391676076156063744?docId=391718656914821120)

#### 添加 aar 包依赖

将获取到的 aar 文件拷贝到对应 module 的 libs 文件夹下（如没有该目录需新建），然后在 build.gradle 文件中增加如下代码

```
android{
    repositories {
        flatDir {
            dirs 'libs'
        }
    } 
}    

dependencies {
    implementation(name:'alive_detected_libary', ext: 'aar')      
    implementation(name: 'openCVLibrary343-release', ext: 'aar')  
    implementation 'com.squareup.okhttp3:okhttp:4.9.1'    //若项目中原本存在无需添加        
    implementation 'com.google.code.gson:gson:2.8.6'      //若项目中原本存在无需添加          
}
```

### 注意点
1. 在 app 的 build.gradle android 域下添加如下配置
```
 packagingOptions {
        doNotStrip "/arm64-v8a/libalive_detected.so"
        doNotStrip "/armeabi-v7a/libalive_detected.so"
    }
```
2. 如果同时使用易盾的活体检测和身份证OCR SDK，请务必先引用OCR SDK； 遇到so冲突，请用以下方式解决

```
packagingOptions {
        pickFirst  'lib/arm64-v8a/libc++_shared.so'
        pickFirst  'lib/armeabi-v7a/libc++_shared.so'
    }
```

## 各种配置

### 权限配置

SDK 依赖如下权限

```    
<uses-permission android:name="android.permission.CAMERA" />
```

其中 CAMERA 权限是隐私权限，Android 6.0 及以上需要动态申请。使用前务必先动态申请权限

```
ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 0);
```

### 混淆配置

在 proguard-rules.pro 文件中添加如下混淆规则

```
-keeppackagenames com.netease.nis.alivedetected
-keep class com.netease.nis.alivedetected.**{*;}
-dontwarn com.netease.nis.alivedetected.**
-keep class com.netease.cloud.nos.yidun.**{*;}
-dontwarn com.netease.cloud.nos.yidun.**
```

## 快速调用示例

### 在 layout 布局文件中使用活体检测相机预览 View

#### 注意点

- 为了避免在某些中低端机型上检测卡顿，建议预览控件的宽与高不要设置为全屏，过大的预览控件会导致处理的数据过大，降低检测流畅度
- 预览宽高不要随意设置，请遵守大部分相机支持的预览宽高比，3：4 或 9：16
- 最好限制竖屏，横屏会影响效果

#### 示例

```
 <com.netease.nis.alivedetected.NISCameraPreview
            android:id="@+id/surface_view"
            android:layout_width="360dp"
            android:layout_height="480dp" />
```

### 在代码中调用相对应 api 开启活体检测

```
public class DemoActivity extends AppCompatActivity {
    private AliveDetector aliveDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        NISCameraPreview cameraPreview = findViewById(R.id.surface_view);
        aliveDetector = AliveDetector.getInstance();
        aliveDetector.init(this, cameraPreview, "申请的业务id");
        aliveDetector.setDetectedListener(new DetectedListener() {
            @Override
            public void onReady(boolean isInitSuccess) {

            }

            @Override
            public void onActionCommands(ActionType[] actionTypes) {

            }

            @Override
            public void onStateTipChanged(ActionType actionType, String stateTip, int code) {
                //单步动作 actionType.getActionID()为 0：正视前方 1：向右转头 2：向左转头 3：张嘴动作 4：眨眼动作 5：动作错误
                6：动作通过
            }

            @Override
            public void onPassed(boolean isPassed, String token) {

            }
            
            @Override
            public void onCheck() {

            }

            @Override
            public void onError(int code, String msg, String token) {

            }

            @Override
            public void onOverTime() {

            }
        });
        aliveDetector.startDetect();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing()) {
            if (aliveDetector != null) {
                aliveDetector.stopDetect();
                aliveDetector.destroy();
            }
        }
    }
}
```

更多使用场景请参考
[demo](https://github.com/yidun/alive-detected-android-demo)

## SDK 方法说明

### 1. 获取 AliveDetector 单例对象

#### 代码说明

```
AliveDetector aliveDetector = AliveDetector.getInstance();
```

### 2. 初始化

#### 代码说明

```
aliveDetector.init(Context context, NISCameraPreview cameraPreview, String businessId)
```

#### 参数说明

|参数|类型|是否必填|默认值|描述|
|----|----|--------|------|----|
|context|Context|是|无| 上下文 |
|cameraPreview|NISCameraPreview|是|无|相机预览 View |
|businessId|String|是|无| 活体检测业务 id |

### 3. 设置回调监听

#### 代码说明

代码添加在 init 之后 startDetect 之前调用

```
aliveDetector.setDetectedListener(DetectedListener detectedListener)
```

#### 参数说明

|参数|类型|是否必填|默认值|描述|
|----|----|--------|------|----|
|detectedListener|DetectedListener|是|无| 监听接口 |

#### DetectedListener 接口说明

```
public interface DetectedListener {
    /**
     * 活体检测引擎初始化时回调
     *
     * @param isInitSuccess 活体检测引擎是否初始化成功：
     *                      1）true，初始化完成可以开始检测
     *                      2）false，初始化失败，可尝试重新启动活体检测流程 {@link AliveDetector#startDetect()}
     */
    void onReady(boolean isInitSuccess);

    /**
     * 此次活体检测下发的待检测动作指令序列，{@link ActionType}
     *
     * @param actionTypes
     */
    void onActionCommands(ActionType[] actionTypes);

    /**
     * 活体检测状态是否改变，当引擎检测到状态改变时会回调该接口
     *
     * @param actionType 当前动作类型，枚举值，总共6种类型:
     *     ACTION_STRAIGHT_AHEAD("0", "正视前方"),
     *     ACTION_TURN_HEAD_TO_RIGHT("1", "向右转头"),
     *     ACTION_TURN_HEAD_TO_LEFT("2", "向左转头"),
     *     ACTION_OPEN_MOUTH("3", "张嘴动作"),
     *     ACTION_BLINK_EYES("4", "眨眼动作"),
     *     ACTION_ERROR("5", "动作错误"),
     *     ACTION_PASSED("6", "动作通过")
     *                   
     * @param stateTip   引擎检测到的实时状态
     * @param code 错误码，在ACTION_ERROR时用于国际化使用（1：请移动人脸到摄像头视野中间、2：环境光线暗、3：环境光线过亮、4：图像质量模糊）
     */
    void onStateTipChanged(ActionType actionType, String stateTip, int code);

    /**
     * 活体检测是否通过回调
     *
     * @param isPassed 活体检测是否通过，true：通过，false:不通过
     * @param token    此次活体检测返回的易盾token
     */
    void onPassed(boolean isPassed, String token);
    /**
     * 活体检测本地检测通过
     * 启动远程检测
     */    
    void onCheck();
    /**
     * 活体检测过程中出现错误时回调
     *
     * @param code 错误码
     * 1：sdk内部异常 2：服务端返回数据异常 3：上传图片异常
     * @param msg  出错原因
     */
    void onError(int code, String msg, String token);

    /**
     * 活体检测过程超时回调
     */
    void onOverTime();
}
```

### 4. 开始活体检测

代码添加在 init 之后调用

#### 代码说明

```
aliveDetector.startDetect()
```

### 5. 停止活体检测

#### 代码说明

可在onStop、onDestroy中调用

```
aliveDetector.stopDetect()
```

### 6. 释放资源(建议放在 onDestroy)

#### 代码说明

```
aliveDetector.destroy()
```

### 7. 是否开启调试模式(非必须)

#### 代码说明

```
aliveDetector.setDebugMode(boolean isDebug)
```

#### 参数说明

|参数|类型|是否必填|默认值|描述|
|----|----|--------|------|----|
|isDebug|boolean|是|false| 是否打印日志 |

### 8. 设置检测动作灵敏度(非必须)

#### 代码说明

```
aliveDetector.setSensitivity(int sensitivity)
```

#### 参数说明

|参数|类型|是否必填|默认值|描述|
|----|----|--------|------|----|
|sensitivity|int|是|AliveDetector.SENSITIVITY_NORMAL| 可取值 AliveDetector.SENSITIVITY_EASY = 0、AliveDetector.SENSITIVITY_NORMAL = 1、AliveDetector.SENSITIVITY_HARD = 2 分别对应容易、普通、难 |

### 9. 设置超时时间(非必须)

代码添加在 startDetect 之前调用

#### 代码说明

```
aliveDetector.setTimeOut(long timeout)
```

#### 参数说明

|参数|类型|是否必填|默认值|描述|
|----|----|--------|------|----|
|timeout|long|是| 30000 | 单位毫秒 |

### 10. 设置域名(非必须)

用于海外域名失效的场景，可以设置私有化的域名列表，内部会自动切换

#### 代码说明

```
aliveDetector.setHosts(String[] hosts)
```

#### 参数说明

|参数|类型|是否必填|默认值|描述|
|----|----|--------|------|----|
|hosts|String[]|是| 无 | 多域名 |
