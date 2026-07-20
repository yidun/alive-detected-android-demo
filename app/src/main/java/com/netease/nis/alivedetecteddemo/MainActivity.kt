package com.netease.nis.alivedetecteddemo

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.netease.cloud.nos.yidun.receiver.ConnectionChangeReceiver
import com.netease.nis.alivedetected.ActionType
import com.netease.nis.alivedetected.AliveDetector
import com.netease.nis.alivedetected.DetectedListener
import com.netease.nis.alivedetected.NISCameraPreview
import com.netease.nis.alivedetecteddemo.manager.BroadcastDispatcher
import com.netease.nis.alivedetecteddemo.utils.Util
import com.netease.nis.alivedetecteddemo.view.FaceAuraColorView
import com.netease.nis.alivedetecteddemo.view.FaceDetectRoundView
import com.netease.nis.alivedetecteddemo.view.SpaceLivenessView
import com.sfyc.ctpv.CountTimeProgressView
import java.io.IOException
import java.util.*

/**
 * @author liu
 * @date 2021/10/12
 * @desc
 * @email liulingfeng@mistong.com
 */
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private var mAliveDetector: AliveDetector? = null
    private var mActions: Array<ActionType>? = null
    private var mCurrentActionType = ActionType.ACTION_STRAIGHT_AHEAD
    private var isOpenVoice = true
    private var mPlayer: MediaPlayer? = null
    private var progressDialog: AlertDialog? = null
    private var connectionChangeReceiver: ConnectionChangeReceiver? = null

    private var mSpaceLivenessView: SpaceLivenessView? = null
    private var mFaceDetectView: FaceDetectRoundView? = null
    private var imgBtnBack: ImageView? = null
    private var ivVoice: ImageView? = null
    private var surfaceView: NISCameraPreview? = null
    private var pvCountTime: CountTimeProgressView? = null
    private var gifAction: ImageView? = null
    private var detectAura: FaceAuraColorView? = null
    private var tvTip: TextView? = null
    private var mLastSpaceCode = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Util.setWindowBrightness(this, WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        BroadcastDispatcher.registerScreenOff(this)
        registerNetChange()

        initView()
    }

    private fun registerNetChange() {
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        connectionChangeReceiver = ConnectionChangeReceiver()
        registerReceiver(connectionChangeReceiver, intentFilter)
    }

    private fun initView() {
        mPlayer = MediaPlayer()
        val progressBar = ProgressBar(this).apply {
            isIndeterminate = true
        }
        progressDialog = AlertDialog.Builder(this)
            .setTitle("云端检测中")
            .setView(progressBar)
            .setCancelable(false)
            .create()

        mSpaceLivenessView = findViewById(R.id.space_liveness_view)
        mFaceDetectView = findViewById(R.id.face_detect_view)
        imgBtnBack = findViewById(R.id.img_btn_back)
        ivVoice = findViewById(R.id.iv_voice)
        surfaceView = findViewById(R.id.surface_view)
        pvCountTime = findViewById(R.id.pv_count_time)
        gifAction = findViewById(R.id.gif_action)
        detectAura = findViewById(R.id.detect_aura)
        tvTip = findViewById(R.id.tv_tip)
        imgBtnBack?.setOnClickListener {
            mAliveDetector?.stopDetect()
            finish()
        }

        ivVoice?.setOnClickListener {
            isOpenVoice = !isOpenVoice
            if (isOpenVoice) {
                ivVoice?.setImageResource(R.mipmap.ico_voice_open_2x)
            } else {
                ivVoice?.setImageResource(R.mipmap.ico_voice_close_2x)
            }
        }

        BroadcastDispatcher.addScreenStatusChangedListener(object :
            BroadcastDispatcher.ScreenStatusChangedListener {
            override fun onForeground() {
                resetIndicator()
                resetGif()
                mAliveDetector?.startDetect()
            }

            override fun onBackground() {
                mAliveDetector?.stopDetect()
            }

        })

        initData()
    }

    private fun initData() {
        mAliveDetector = AliveDetector.getInstance()
        mAliveDetector?.setDebugMode(true)
        mAliveDetector?.init(this, surfaceView, "易盾业务id")
        mAliveDetector?.setDetectedListener(object : DetectedListener {
            override fun onReady(isInitSuccess: Boolean) {
                // 开始倒计时
                pvCountTime?.startCountTimeAnimation()
                // 引擎初始化完成
                if (isInitSuccess) Log.d(TAG, "活体检测引擎初始化完成") else Log.e(
                    TAG,
                    "活体检测引擎初始化失败"
                )
            }

            override fun onActionCommands(actionTypes: Array<ActionType>?) {
                // 此次活体检测下发的待检测动作指令序列
                mActions = actionTypes
                val commands = buildActionCommand(actionTypes)
                Log.d(TAG, "活体检测动作序列为:$commands")
            }

            override fun onStateTipChanged(actionType: ActionType, stateTip: String?, code: Int) {
                // 单步动作
                Log.d(TAG, "actionType:" + actionType.actionTip + " stateTip:" + stateTip)

                dealWithTipChanged(actionType, stateTip)
            }

            override fun onPassed(isPassed: Boolean, token: String?) {
                // 检测通过
                dealWithPassed(isPassed, token)
            }

            override fun onCheck() {
                // 云端检测
                if (!isFinishing) {
                    progressDialog?.show()
                }
            }

            override fun onError(code: Int, msg: String?, token: String?) {
                if (progressDialog?.isShowing == true) {
                    progressDialog?.dismiss()
                }
                Log.e(TAG, "listener [onError] 活体检测出错,原因:$msg token:$token")
            }

            override fun onOverTime() {
                Util.showDialog(
                    this@MainActivity, "检测超时", "请在规定时间内完成动作",
                    "重试", "返回首页", { _, _ ->
                        resetIndicator()
                        resetGif()
                        mAliveDetector?.startDetect()
                    }) { _, _ ->
                    val intent = Intent(
                        this@MainActivity,
                        WelcomeActivity::class.java
                    )
                    startActivity(intent)
                }
            }

            override fun onBackgroundColor(color: Int) {
                // -1说明是动作活体切换
                if (color != -1) {
                    Glide.with(this@MainActivity).load(R.mipmap.pic_front_2x).into(gifAction!!)
                    tvTip?.text = ""
                    detectAura?.visibility = View.VISIBLE
                    detectAura?.start(color)
                } else {
                    detectAura?.let {
                        if (it.visibility == View.VISIBLE) {
                            it.visibility = View.GONE
                        }
                    }
                }
            }

            override fun onSpaceLiveness(direction: Int) {
                enterSpaceModelUI()
            }

            override fun onSpaceStateTipChanged(
                actionType: ActionType?,
                stateTip: String?,
                code: Int
            ) {
                if (actionType == ActionType.ACTION_PASSED) {
                    exitSpaceModelUI()
                }
                when (actionType) {
                    ActionType.ACTION_SPACE_NEAR, ActionType.ACTION_SPACE_FAR -> {
                        when (code) {
                            100 -> {
                                mSpaceLivenessView?.reset()
                                mLastSpaceCode = -1
                            }

                            7 -> {
                                mSpaceLivenessView?.setDistanceType(SpaceLivenessView.DISTANCE_NEAR)
                                if (mLastSpaceCode != 7) mSpaceLivenessView?.startNearAnimation()
                                mSpaceLivenessView?.setRingType(SpaceLivenessView.RING_TYPE_COLOR)
                                mSpaceLivenessView?.setTipSecondText(stateTip ?: "请略微靠近屏幕")
                                mLastSpaceCode = 7
                            }

                            8 -> {
                                mSpaceLivenessView?.setDistanceType(SpaceLivenessView.DISTANCE_FAR)
                                if (mLastSpaceCode != 8) mSpaceLivenessView?.startFarAnimation()
                                mSpaceLivenessView?.setRingType(SpaceLivenessView.RING_TYPE_COLOR)
                                mSpaceLivenessView?.setTipSecondText(stateTip ?: "请略微远离屏幕")
                                mLastSpaceCode = 8
                            }

                            0 -> {
                                mSpaceLivenessView?.setRingType(SpaceLivenessView.RING_TYPE_BLUE)
                                mSpaceLivenessView?.setTipSecondText(stateTip ?: "请保持不动")
                                mLastSpaceCode = 0
                            }

                            else -> {}
                        }
                    }

                    else -> {}
                }
            }
        })

        mAliveDetector?.setTimeOut(30000)
        mAliveDetector?.startDetect()
    }

    /**
     * 处理单步动作
     */
    private fun dealWithTipChanged(actionType: ActionType, stateTip: String?) {
        when (actionType) {
            ActionType.ACTION_ERROR -> setTipText(stateTip, true)
            ActionType.ACTION_PASSED -> {
                Log.d(TAG, "检测通过")
            }

            else -> setTipText(stateTip, false)
        }

        if (actionType != ActionType.ACTION_PASSED && actionType != ActionType.ACTION_ERROR) {
            if (actionType != this.mCurrentActionType) {
                mCurrentActionType = actionType
                when (actionType) {
                    ActionType.ACTION_TURN_HEAD_TO_LEFT -> {
                        gifAction?.let {
                            Glide.with(applicationContext).asGif().load(R.drawable.turn_left)
                                .into(gifAction!!)
                        }
                        if (isOpenVoice) {
                            playSound(getAssetFileDescriptor("turn_head_to_left.wav"))
                        }
                    }

                    ActionType.ACTION_TURN_HEAD_TO_RIGHT -> {
                        gifAction?.let {
                            Glide.with(applicationContext).asGif().load(R.drawable.turn_right)
                                .into(gifAction!!)
                        }
                        if (isOpenVoice) {
                            playSound(getAssetFileDescriptor("turn_head_to_right.wav"))
                        }
                    }

                    ActionType.ACTION_OPEN_MOUTH -> {
                        gifAction?.let {
                            Glide.with(applicationContext).asGif().load(R.drawable.open_mouth)
                                .into(gifAction!!)
                        }
                        if (isOpenVoice) {
                            playSound(getAssetFileDescriptor("open_mouth.wav"))
                        }
                    }

                    ActionType.ACTION_BLINK_EYES -> {
                        gifAction?.let {
                            Glide.with(applicationContext).asGif().load(R.drawable.open_eyes)
                                .into(gifAction!!)
                        }
                        if (isOpenVoice) {
                            playSound(getAssetFileDescriptor("blink_eyes.wav"))
                        }
                    }

                    else -> {
                        Log.d(TAG, "不支持的类型")
                    }
                }
            }
        }
    }

    /**
     * 处理本地检测通过
     */
    private fun dealWithPassed(isPassed: Boolean, token: String?) {
        // 检测通过
        if (progressDialog?.isShowing == true) {
            progressDialog?.dismiss()
        }
        if (isPassed) {
            Log.d(TAG, "活体检测通过,token is:$token")
            finish()
            val intent = Intent(this@MainActivity, SuccessActivity::class.java)
            startActivity(intent)
        } else {
            Log.e(TAG, "活体检测不通过,token is:$token")
            finish()
            val intent = Intent(
                this@MainActivity,
                FailureActivity::class.java
            )
            intent.putExtra("token", token)
            startActivity(intent)
        }
    }

    /**
     * actionIds
     */
    private fun buildActionCommand(actionCommands: Array<ActionType>?): String {
        val commands = StringBuilder()
        actionCommands?.let {
            for (actionType in it) {
                commands.append(actionType.actionID)
            }
        }
        return if (TextUtils.isEmpty(commands.toString())) "" else commands.toString()
    }

    private fun resetIndicator() {
        mCurrentActionType = ActionType.ACTION_STRAIGHT_AHEAD
        mSpaceLivenessView?.visibility = View.GONE
        mSpaceLivenessView?.reset()
        mFaceDetectView?.visibility = View.VISIBLE
        mFaceDetectView?.reset()
    }

    private fun enterSpaceModelUI() {
        runOnUiThread {
            mLastSpaceCode = -1
            mSpaceLivenessView?.visibility = View.VISIBLE
            mSpaceLivenessView?.reset()
            mFaceDetectView?.visibility = View.GONE
            mFaceDetectView?.reset()
        }
    }

    private fun exitSpaceModelUI() {
        runOnUiThread {
            mSpaceLivenessView?.visibility = View.GONE
            mSpaceLivenessView?.reset()
            mFaceDetectView?.visibility = View.VISIBLE
        }
    }

    private fun resetGif() {
        gifAction?.let {
            Glide.with(applicationContext).load(R.mipmap.pic_front_2x).into(it)
        }
    }

    private fun playSound(fileDescriptor: AssetFileDescriptor?) {
        try {
            mPlayer?.reset()
            fileDescriptor?.let {
                mPlayer?.setDataSource(
                    it.fileDescriptor,
                    it.startOffset,
                    it.length
                )
            }
            mPlayer?.prepare()
            mPlayer?.start()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "playSound error$e")
        }
    }

    private fun getAssetFileDescriptor(assetName: String): AssetFileDescriptor? {
        try {
            return application.assets.openFd(assetName)
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "getAssetFileDescriptor error$e")
        }
        return null
    }

    @SuppressLint("SetTextI18n")
    private fun setTipText(tip: String?, isErrorType: Boolean) {
        if (isErrorType) {
            val display = when (tip) {
                "请移动人脸到摄像头视野中间" -> "请正对手机屏幕\n将面部移入框内"
                "请正视摄像头视野中间并保持不动" -> "请正视摄像头\n并保持不动"
                else -> tip ?: ""
            }
            mFaceDetectView?.setTipTopText(display)
            mSpaceLivenessView?.setTipTopText(display)
        } else {
            mFaceDetectView?.setTipTopText("")
            mFaceDetectView?.setTipSecondText(tip ?: "")
            mSpaceLivenessView?.setTipTopText("")
        }
    }

    override fun onPause() {
        super.onPause()

        progressDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
        if (mPlayer?.isPlaying == true) {
            mPlayer?.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        mPlayer?.stop()
    }

    override fun onDestroy() {
        Util.setWindowBrightness(this, WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        if (isFinishing) {
            mAliveDetector?.stopDetect()
            mAliveDetector?.destroy()
            mAliveDetector = null
            pvCountTime?.cancelCountTimeAnimation()
            BroadcastDispatcher.unRegisterScreenOff(this)
            connectionChangeReceiver?.let {
                unregisterReceiver(it)
            }
        }

        if (progressDialog?.isShowing == true) {
            progressDialog?.dismiss()
        }
        progressDialog = null
        if (mPlayer?.isPlaying == true) {
            mPlayer?.stop()
        }
        mPlayer?.reset()
        mPlayer?.release()
        mPlayer = null
        super.onDestroy()
    }
}
