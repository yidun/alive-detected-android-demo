根据提示做出相应的活体动作，SDK 实时采集动态信息，判断用户是否为活体、真人。在黑灰产对抗激烈，“AI换脸”频发的场景下，活体检测SDK同时也集成了
**风控设备环境检测能力，能够有效拦截摄像头被外部流注入的情况，同时独有的人脸鉴伪技术也能很好的识别合成脸攻击**

> <span style="color: red;">⚠️ 接入活体SDK注意事项：</span>
>
> 1. **本地手动依赖，默认提供了活体检测、风控设备环境检测能力（推荐）**
> 2. **远程仓库依赖，仅集成了基础活体检测，不具备环境风险感知能力，如果您对SDK体积比较敏感，可以这种方式**
> 3. **如果您的产品服务的用户主要在海外，请联系技术支持提供海外专属技术方案，耗时更低**
> 4. <span style="color: red; font-weight: bold;">若您在易盾后台需要开启RGB活体或远近空间活体功能，务必需要接入方实现相关UI的配合，详细参考[Demo](https://github.com/yidun/alive-detected-android-demo/tree/master/app)，否则可能导致对应动作检测触发失败导致流程阻塞。</span>

## 兼容性
| 条目        | 说明                                                                      |
| ----------- | -----------------------------------------------------------------------  |
| 适配版本    | minSdkVersion 21 及以上版本                                                 |
| cpu 架构    | 内部提供了 armeabi-v7a 和 arm64-v8a 两种 so ，对于不兼容 arm 的 x86 机型不适配 |

## 资源引入

### 本地手动依赖(推荐)

#### 获取 SDK

从易盾后台下载活体检测相关 sdk 的 aar 包 

#### SDK模块介绍

| **模块名称**            | **功能说明**   | **支持裁剪** | **SDK包体积** | **编译后体积增量** |
|---------------------|------------| ----------- |------------| ------------------ |
| livedetect-xxx.aar  | 活体人脸检测     | 不可裁剪     | 3.7MB      | 3.8MB             |
| crashreport-xxx.aar | 活体人脸检测异常上报 | 不可裁剪      | 172KB      | 173KB            |
| base-core.aar | 活体人脸检测基本库 | 不可裁剪      | 34KB      | 35KB             |
| HTProtectLib-xxx.aar | 风控设备环境检测     | 可裁剪       | 6.1MB      | 6.4MB             |

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
    implementation(name:'livedetect-xxx', ext: 'aar')      
    implementation(name: 'crashreport-xxx', ext: 'aar')
    implementation(name: 'base-core-xxx', ext: 'aar')  
	implementation(name: 'HTProtectLib-xxx', ext: 'aar')
    implementation 'com.squareup.okhttp3:okhttp:4.9.1'    //若项目中原本存在无需添加        
    implementation 'com.google.code.gson:gson:2.8.6'      //若项目中原本存在无需添加          
}
```

#### SDK包裁剪说明
如果您对 SDK 体积比较敏感，可以不集成 HTProtectLib-xxx.aar 包，但是这意味着您放弃了风控设备环境检测能力

### 远程仓库依赖

#### 获取 SDK
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
implementation 'io.github.yidun:livedetect:3.3.8'
```

#### 集成风控能力
如果需要在活体检测过程中检测设备环境，那需要额外接入风控 SDK，风控 SDK 仅提供本地依赖的方式

将易盾后台获取到的 aar 文件拷贝到对应 module 的 libs 文件夹下（如没有该目录需新建），然后在 build.gradle 文件中增加如下代码

```
android{
    repositories {
        flatDir {
            dirs 'libs'
        }
    } 
}    

dependencies {
	implementation(name: 'HTProtectLib-xxx', ext: 'aar')
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
2. 如果同时使用易盾的活体检测和身份证 ocr sdk，请务必先引用 ocr sdk； 遇到 so 冲突，请用以下方式解决

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

CAMERA 权限属于隐私权限，Android 6.0 及以上版本需要在使用前动态申请。

```
ActivityCompat.requestPermissions(
        this,
        new String[]{
                Manifest.permission.CAMERA
        },
        0
);
```

### 申明与其他应用交互的意图范畴

在 Manifest 配置文件的权限配置下方添加如下配置

```
 <queries>
        <intent>
            <action android:name="android.intent.action.MAIN"/>
        </intent>
 </queries>
```

### 混淆配置

在 proguard-rules.pro 文件中添加如下混淆规则

```
-keeppackagenames com.netease.nis.alivedetected
-keep class com.netease.nis.alivedetected.**{*;}
-dontwarn com.netease.nis.alivedetected.**
-keep class com.netease.cloud.nos.yidun.**{*;}
-dontwarn com.netease.cloud.nos.yidun.**
-keep class com.netease.htprotect.**{*;}
-keep class com.netease.mobsec.**{*;}
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

            @Override
            public void onBackgroundColor(int color) {
                // RGB 活体模式下，根据 SDK 返回的颜色值更新检测页面背景颜色
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
     * @param code 错误码，在ACTION_ERROR时用于国际化使用（0:手机抖动，请保持稳定、1：请移动人脸到摄像头视野中间、2：环境光线暗、3：环境光线过亮、4：图像质量模糊、5：多人脸、6：距离太远、7：距离太近）
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

    /**
     * RGB 活体检测背景颜色变化回调
     *
     * @param color SDK 返回的背景颜色值
     */
    void onBackgroundColor(int color);

    /**
     * 远近空间活体配置回调
     *
     * @param direction 方向 0：由近到远 1：由远到近
     */
    void onSpaceLiveness(int direction);

    /**
     * 远近空间活体状态变化回调
     *
     * @param actionType 当前类型，ActionType.ACTION_PASSED：远近空间活体通过、ActionType.ACTION_SPACE_NEAR：近、ActionType.ACTION_SPACE_FAR：远
     * @param stateTip 提示词，总共五种情况：请略微靠近屏幕、保持不动、请略微远离屏幕、切换阶段、动作成功
     * @param code 状态码 0：保持不动 100：切换阶段 200：动作成功 7：请略微靠近屏幕 8：请略微远离屏幕
     */
    void onSpaceStateTipChanged(ActionType actionType, String stateTip, int code);
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

### 11. 是否允许多人脸(非必须)

#### 代码说明

```
aliveDetector.setAllowMultipleFace(boolean allowMultipleFace)
```

#### 参数说明

|参数|类型|是否必填|默认值|描述|
|----|----|--------|------|----|
|allowMultipleFace|boolean|是| false | 是否允许多人脸 |

### 12. 设置设备抖动检测阈值(非必须)

值越小越灵敏

#### 代码说明

```
aliveDetector.setShakeThreshold(int shakeThreshold)
```

#### 参数说明

|参数|类型|是否必填|默认值|描述|
|----|----|--------|------|----|
|shakeThreshold|int|是| 35 | 抖动检测阈值 |

### 13. 设置人脸占比阈值(非必须)

#### 代码说明

```
aliveDetector.setFaceThreshold(int faceThreshold)
```

#### 参数说明

|参数|类型|是否必填| 默认值 | 描述            |
|----|----|--------|-----|---------------|
|faceThreshold|int|是| 7   | 人脸占比阈值，范围7-50 |
