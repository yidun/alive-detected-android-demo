package com.netease.nis.alivedetecteddemo

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.res.AssetFileDescriptor
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.ViewStub
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.netease.nis.alivedetected.ActionType
import com.netease.nis.alivedetected.AliveDetector
import com.netease.nis.alivedetected.DetectedListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author liu
 * @date 2021/10/12
 * @desc
 * @email liulingfeng@mistong.com
 */
class MainActivity : AppCompatActivity() {
    companion object {
        private const val isDebug = false
        private const val TAG = "MainActivity"
    }

    private var mAliveDetector: AliveDetector? = null
    private var mActions: Array<ActionType>? = null
    private var mCurrentCheckStepIndex = 0
    private var mCurrentActionType = ActionType.ACTION_STRAIGHT_AHEAD
    private var isOpenVoice = true
    private var mPlayer: MediaPlayer? = null
    private val stateTipMap: Map<String, String> = mapOf(
        "straight_ahead" to """
    请正对手机屏幕
    将面部移入框内
    """.trimIndent(),
        "open_mouth" to "张张嘴",
        "turn_head_to_left" to "慢慢左转头",
        "turn_head_to_right" to "慢慢右转头",
        "blink_eyes" to "眨眨眼"
    )

    private var progressDialog: ProgressDialog? = null
    private var vsStep2: ViewStub? = null
    private var vsStep3: ViewStub? = null
    private var vsStep4: ViewStub? = null
    private var tvStep1: TextView? = null
    private var tvStep2: TextView? = null
    private var tvStep3: TextView? = null
    private var tvStep4: TextView? = null
    private var timer: Timer? = null
    private var isFirstTimer = true
    private val timeAtomic: AtomicInteger = AtomicInteger(1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timer = Timer()
        initView()
        Util.setWindowBrightness(this, WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL)
    }

    private fun initView() {
        mPlayer = MediaPlayer()
        progressDialog = ProgressDialog(this)
        progressDialog?.setTitle("云端检测中")

        surface_view?.holder?.setFormat(PixelFormat.TRANSLUCENT)
        tvStep1 = findViewById(R.id.tv_step_1)
        vsStep2 = findViewById(R.id.vs_step_2)
        vsStep3 = findViewById(R.id.vs_step_3)
        vsStep4 = findViewById(R.id.vs_step_4)
        img_btn_back?.setOnClickListener {
            mAliveDetector?.stopDetect()
            finish()
        }

        iv_voice?.setOnClickListener {
            isOpenVoice = !isOpenVoice
            if (isOpenVoice) {
                iv_voice?.setImageResource(R.mipmap.ico_voice_open_2x)
            } else {
                iv_voice?.setImageResource(R.mipmap.ico_voice_close_2x)
            }
        }

        initData()
    }

    private fun initData() {
        mAliveDetector = AliveDetector.getInstance()
        mAliveDetector?.setDebugMode(true)
        mAliveDetector?.init(this, surface_view, "6a1a399443a54d31b91896a4208bf6e0")
        mAliveDetector?.setDetectedListener(object : DetectedListener {
            override fun onReady(isInitSuccess: Boolean) {
                // 开始倒计时
                pv_count_time?.startCountTimeAnimation()
                // 引擎初始化完成
                if (isInitSuccess) Log.d(TAG, "活体检测引擎初始化完成") else Log.e(TAG, "活体检测引擎初始化失败")
            }

            override fun onActionCommands(actionTypes: Array<ActionType>?) {
                // 此次活体检测下发的待检测动作指令序列
                mActions = actionTypes
                val commands = buildActionCommand(actionTypes)
                Log.d(TAG, "活体检测动作序列为:$commands")
                showIndicatorOnUiThread(commands.length - 1)
            }

            override fun onStateTipChanged(actionType: ActionType?, stateTip: String?) {
                // 单步动作
                Log.d(
                    TAG,
                    "actionType:" + actionType?.actionTip + " stateTip:" + stateTip + " CurrentCheckStepIndex:" + mCurrentCheckStepIndex
                )
                if (actionType == ActionType.ACTION_PASSED && actionType.actionID != mCurrentActionType.actionID) {
                    mCurrentCheckStepIndex++
                    mActions?.let {
                        if (mCurrentCheckStepIndex < it.size) {
                            updateIndicatorOnUiThread(mCurrentCheckStepIndex)
                            if (isOpenVoice) {
                                playSounds(mCurrentCheckStepIndex)
                            }
                            mCurrentActionType = it[mCurrentCheckStepIndex]
                        }
                    }
                }

                when (actionType) {
                    ActionType.ACTION_STRAIGHT_AHEAD -> setTipText("", false)
                    ActionType.ACTION_OPEN_MOUTH -> setTipText(
                        stateTipMap["open_mouth"],
                        false
                    )
                    ActionType.ACTION_TURN_HEAD_TO_LEFT -> setTipText(
                        stateTipMap["turn_head_to_left"], false
                    )
                    ActionType.ACTION_TURN_HEAD_TO_RIGHT -> setTipText(
                        stateTipMap["turn_head_to_right"], false
                    )
                    ActionType.ACTION_BLINK_EYES -> setTipText(
                        stateTipMap["blink_eyes"],
                        false
                    )
                    ActionType.ACTION_ERROR -> setTipText(stateTip, true)
                    else -> {
                    }
                }
            }

            override fun onPassed(isPassed: Boolean, token: String?) {
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

            override fun onCheck() {
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
                Util.showDialog(this@MainActivity, "检测超时", "请在规定时间内完成动作",
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

        })

        mAliveDetector?.sensitivity = AliveDetector.SENSITIVITY_NORMAL
        mAliveDetector?.setTimeOut(30000)
        mAliveDetector?.startDetect()
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

    // 显示所有步骤
    private fun showIndicatorOnUiThread(commandLength: Int) {
        when (commandLength) {
            2 -> {
                vsStep2?.visibility = View.VISIBLE
                tvStep2 = findViewById(R.id.tv_step_2)
            }
            3 -> {
                vsStep2?.visibility = View.VISIBLE
                tvStep2 = findViewById(R.id.tv_step_2)
                vsStep3?.visibility = View.VISIBLE
                tvStep3 = findViewById(R.id.tv_step_3)
            }
            4 -> {
                vsStep2?.visibility = View.VISIBLE
                tvStep2 = findViewById(R.id.tv_step_2)
                vsStep3?.visibility = View.VISIBLE
                tvStep3 = findViewById(R.id.tv_step_3)
                vsStep4?.visibility = View.VISIBLE
                tvStep4 = findViewById(R.id.tv_step_4)
            }
        }
    }

    private fun updateIndicatorOnUiThread(currentActionIndex: Int) {
        updateIndicator(currentActionIndex)
        updateGif(currentActionIndex)
    }

    private fun updateIndicator(currentActionPassedCount: Int) {
        when (currentActionPassedCount) {
            2 -> {
                tvStep1?.text = ""
                tvStep2?.text = "2"
                setTextViewFocus(tvStep2)
            }
            3 -> {
                tvStep1?.text = ""
                tvStep2?.text = ""
                setTextViewFocus(tvStep2)
                tvStep3?.text = "3"
                setTextViewFocus(tvStep3)
            }
            4 -> {
                tvStep1?.text = ""
                tvStep2?.text = ""
                setTextViewFocus(tvStep2)
                tvStep3?.text = ""
                setTextViewFocus(tvStep3)
                tvStep4?.text = "4"
                setTextViewFocus(tvStep4)
            }
        }
    }

    private fun updateGif(currentActionIndex: Int) {
        mActions?.let {
            when (it[currentActionIndex]) {
                ActionType.ACTION_TURN_HEAD_TO_LEFT -> gif_action?.setImageResource(R.drawable.turn_left)
                ActionType.ACTION_TURN_HEAD_TO_RIGHT -> gif_action?.setImageResource(R.drawable.turn_right)
                ActionType.ACTION_OPEN_MOUTH -> gif_action?.setImageResource(R.drawable.open_mouth)
                ActionType.ACTION_BLINK_EYES -> gif_action?.setImageResource(R.drawable.open_eyes)
                else -> {
                    Log.d(TAG, "不支持的类型")
                }
            }
        }
    }

    private fun resetIndicator() {
        mCurrentCheckStepIndex = 0
        mCurrentActionType = ActionType.ACTION_STRAIGHT_AHEAD
        tvStep1?.text = "1"
        tv_tip?.text = ""
        if (tvStep2 != null && tvStep2?.visibility == View.VISIBLE) {
            tvStep2?.text = ""
            setTextViewUnFocus(tvStep2)
            tvStep2?.visibility = View.GONE
        }
        if (tvStep3 != null && tvStep3?.visibility == View.VISIBLE) {
            tvStep3?.text = ""
            setTextViewUnFocus(tvStep3)
            tvStep3?.visibility = View.GONE
        }
        if (tvStep4 != null && tvStep4?.visibility == View.VISIBLE) {
            tvStep4?.text = ""
            setTextViewUnFocus(tvStep4)
            tvStep4?.visibility = View.GONE
        }
    }

    private fun resetGif() {
        gif_action?.setImageResource(R.mipmap.pic_front_2x)
    }

    private fun setTextViewFocus(tv: TextView?) {
        tv?.setBackgroundResource(R.drawable.circle_tv_focus)
    }

    private fun setTextViewUnFocus(tv: TextView?) {
        tv?.setBackgroundResource(R.drawable.circle_tv_un_focus)
    }

    private fun playSounds(currentActionIndex: Int) {
        mActions?.let {
            when (it[currentActionIndex]) {
                ActionType.ACTION_TURN_HEAD_TO_LEFT -> playSound(getAssetFileDescriptor("turn_head_to_left.wav"))
                ActionType.ACTION_TURN_HEAD_TO_RIGHT -> playSound(getAssetFileDescriptor("turn_head_to_right.wav"))
                ActionType.ACTION_OPEN_MOUTH -> playSound(getAssetFileDescriptor("open_mouth.wav"))
                ActionType.ACTION_BLINK_EYES -> playSound(getAssetFileDescriptor("blink_eyes.wav"))
                else -> {
                }
            }
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
            if (timeAtomic.get() == 1) {
                when (tip) {
                    "请移动人脸到摄像头视野中间" -> tv_error_tip?.text = "请正对手机屏幕\n将面部移入框内"
                    "请正视摄像头视野中间并保持不动" -> tv_error_tip?.text = "请正视摄像头\n并保持不动"
                    else -> tv_error_tip?.text = tip
                }
                view_tip_background?.visibility = View.VISIBLE
                blur_view?.visibility = View.VISIBLE
                timeAtomic.set(0)
                if (isFirstTimer) {
                    // 处理提示过于频繁，修改为1.5s提示一次
                    startTimer()
                    isFirstTimer = false
                }
            }
        } else {
            view_tip_background?.visibility = View.INVISIBLE
            blur_view?.visibility = View.INVISIBLE
            tv_tip?.text = tip
            tv_error_tip?.text = ""
        }
    }

    private fun startTimer() {
        timer?.schedule(object : TimerTask() {
            override fun run() {
                timeAtomic.set(1)
            }
        }, 1500, 1500)
    }

    override fun onPause() {
        super.onPause()

        progressDialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
    }

    override fun onDestroy() {
        Util.setWindowBrightness(this, WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        if (isFinishing) {
            mAliveDetector?.stopDetect()
            mAliveDetector?.destroy()
            pv_count_time?.cancelCountTimeAnimation()
        }

        timer?.cancel()

        if (mPlayer?.isPlaying == true) {
            mPlayer?.stop()
        }
        mPlayer?.reset()
        mPlayer?.release()
        mPlayer = null
        super.onDestroy()
    }
}