package com.rosisit.idcardcapture;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.selvasai.selvyimageprocessing.camera.ui.CameraView;
import com.selvasai.selvyimageprocessing.idcard.ImageController;
import com.selvasai.selvyimageprocessing.idcard.util.LicenseChecker;
import com.selvasai.selvyimageprocessing.imagecrop.ui.ImageCropView;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 2023.02.20 수정
 * 주요 동작 흐름 설명 <br/>
 * <p/>
 * 1. 자동 촬영  : {@link FindDocumentAreaThread} 프리뷰 영상에서 촬영할지 여부 판단하여 자동 촬영 <br/>
 * 2. {@link ImageProcessingThread} 촬영된 이미지에서 신분증 영역만 자르고 perspective 수행하여 보정 <br/>
 * 3. {@link #moveToResultActivity()} 보정된 이미지를 다른 액티비티로 반환
 */
public class DocumentCameraActivity extends Activity {

    // Intent를 통해 데이터를 주고 받기 위한 값 정의
    public static final String DATA_RESULT_TEXT = "resultstrings";
    public static final String DATA_ENCRYT_IMAGE_BYTE_ARRAY = "encryptbytearray";
    public static final String DATA_RRN_RECT = "rrnrect";
    public static final String DATA_LICENSE_NUMBER_RECT = "licensenumberrect";
    public static final String DATA_PHOTO_RECT = "photorect";
    public static final String DATA_BW_JUDGEMENT = "bwjudgement";
    public static final String DATA_ENCRYPT_KEY = "encryptkey";
    public static final String DATA_TITLE_MESSAGE_AUTO_TOP = "titlemessageauto_TOP";
    public static final String DATA_TITLE_MESSAGE_AUTO_BOT = "titlemessageauto_BOT";
    public static final String DATA_TITLE_MESSAGE_MANUAL_TOP = "titlemessagemanual_TOP";
    public static final String DATA_TITLE_MESSAGE_MANUAL_BOT = "titlemessagemanual_BOT";
    public static final String DATA_TOP_COLOR = "datatopcolor";
    public static final String DATA_BOT_COLOR = "databotclolor";
    public static final String DATA_MONOCHECK = "monocheck";
    public static final String DOCUMENT_DETECTION_SOUND = "detectsound";
    public static final String IS_AUTO_MODE ="isautomode";

    private static final String TAG = "rosis_debug";
    private static final String DATA_GUIDE_RECT = "guiderect";
    private static final String DATA_TOUCH_EVENT = "touchevent";
    private static final String DATA_DOC_ARRAY = "output";
//    private static final double MIN_BLUR_VALUE = 0.75;
//    private static final int DOCUMENT_WIDTH_PORT = 2500;
//    private static final int DOCUMENT_WIDTH_LAND = 4000;



    public final static int RETURN_OK = 1;
    public final static int RETURN_CANCEL = 2;
    public final static int RETURN_NETWORK_ERROR = 3;
    public final static int RETURN_PERMISSION_ERROR = 4;
    public final static int RETURN_CAMERA_ERROR = 5;
    public final static int RETURN_LIBRARY_ERROR = 6;
    //public final static int RETURN_SCR_RETRY = 7;
    public final static int RETURN_END = 8;
    public final static int RETURN_OVERTIME = 9;
    //public final static int RETURN_OCR_RETRY = 10;
    public final static int RETURN_OCR_FAIL = 11;

    private final static int NOTFIND_SOUND = 0;
    private final static int FOUND_SOUND = 1;
    private final static int STOP_SOUND = 2;


    private static final int REQ_PERMISSON_RESULT = 10001;

    public static final String DATA_DOCUMENT_TYPE = "documenttype_data";
    public static final String DATA_DOCUMENT_ORIENTATION = "documentorientation";
//    public static final String LIST_CLEAN = "imagelistclean";

//    private static final String CAPTURE_IMAGE_NAME_ID = "captureimagename.jpg";
//    private String CAPTURE_IMAGE_NAME_DOC = ""; // 보정된 이미지를 저장하는 파일명

    private static final int MSG_IMAGE_NEED_CROP = 1; // 영역검출이 실패하여 Crop 화면으로 변경
    private static final int MSG_IMAGE_PROCESSING_END = 2;  // Perspective 수행 완료
    private static final int MSG_IMAGE_SAVE_END = 3;  // 이미지 저장 완료
    private static final int MSG_IMAGE_SAVE_FAIL = 4;  // 이미지 저장 실패
    private static final int MSG_REMOVE_NOTI_FOCUS = 5; // 촬영 진행 중에 문구 숨김
    private static final int MSG_START_AUTO_FOCUS = 6; // 오토 포커싱 수행
    private static final int MSG_OVERLAY_DRAW_AREA = 7; // 검출된 영역을 프리뷰에 그림
    private static final int MSG_OVERLAY_CLEAR_AREA = 8; // 프리뷰에 그린 영역을 Clear
    private static final int MSG_RECOGNIZE_END = 9;
    private static final int MSG_RECOGNIZE_ERROR = 10;

    private static final boolean AUTO_MODE = true;
    private static final boolean MANUAL_MODE = false;

    private String mEncryptKey;
    private boolean isDetectSound;

//    private static final int IDCARD_MINIMUM_CAMERA_SIZE = 2000000;
    private static final int DOCUMENT_MINIMUM_CAMERA_SIZE = 10000000;

    // 신분증 찾았을때 신분증 윤곽선
    //private Paint mDocumentAreaBoaderPaint;

    private Paint mDocumentAreaPaint;
    private Paint mDocumentAreaBoarderPaint;
    private Paint mDocumentAreaFillPaint;

    //private TextView mNotiTextTop;//, mNotiTextBottom;
    private CameraView mCameraView;  // 카메라 화면
    private ImageCropView mImageCropView; // 이미지 크롭화면

    //Label
    private TextView mCameraTopTxt;
    private TextView mCameraMiddleTxt;
    private TextView mCameraBottomTxt;

    //Button
    private ImageView mBtnCrop;
    private ImageView mBtnTakePicture;
    private ImageView mBtnSwitch;
    private ImageView mBtnClose;

    //Frame
    private ImageView mFrame01;
    private ImageView mFrame02;
    private ImageView mFrame03;
    private ImageView mFrame04;

    //LprGuideRect
    private Rect lprGuideRect;
    private ImageView mBackGround01;
    private ImageView mBackGround02;

    public static final int TYPE_ID_CARD = 1;
    public static final int TYPE_DOCUMENT_A4 = 2;
    public static final int TYPE_DOCUMENT_ETC = 3;
    public static final int TYPE_DOCUMENT_LPR = 4;
    public static final int ORIENTATION_PORTRAIT = 256;
    public static final int ORIENTATION_LANDSCAPE = 512;
    public static final boolean MONO_DATA = true;

    private AtomicBoolean mIsAutoFocusStart; //오토포커싱 진행중인지 여부
    private AtomicBoolean mIsTakePictureStart; //촬영 진행중인지 여부
    private AtomicBoolean mIsFindDocumentAreaRunning; //프리뷰 영역검출 진행중인지 여부

    private boolean mIsInit = false;
    private boolean mIsFirstFrame = true;

    private Rect mFocusRect; // 포커싱 영역
    private double mBlurValue = 1.0; // 초점이 흐린지 여부(0~1), 1 이면 아주 흐림
    private boolean mIsCropMode = false; // 크롭 화면인지 여부
    private int mFocusRetryCount = 0; // 포커싱 반복을 위한 카운트
    private long mLastFocusTime = 0;
    private Bitmap mCapturedImage;
    private int mSimilarCount = 0; // 자동촬영을 위한 카운트
    private int[] mPreviewIdCardArea; // 프리뷰에서 검출한 신분증 영역

    private Activity mThisActivity;
    private ProgressDialog mDialog;

    private int mDocumentType = TYPE_ID_CARD;
    private int mDocumentOrientation = ORIENTATION_PORTRAIT;

    private boolean mMonoCheck;

    private SpannableStringBuilder ssbAutoTop;
    private SpannableStringBuilder ssbAutoBot;
    private SpannableStringBuilder ssbManualTop;
    private SpannableStringBuilder ssbManualBot;

    //private int mClearList = 0;

    //true : 자동촬영 , false : 수동촬영
    private boolean mFlagAutoManual = AUTO_MODE;
    private boolean mIsRestart = false;

    //beef 음 재생
    private SoundPool soundPool;
    private int soundBeepFound;
    private int soundBeepNotFind;
    private int streamNotFind;
    private List<Integer> streams;
    private boolean mFoundFlag = false;
    private boolean mNotFindFlag = false;
    private boolean misActionButton = false;

    private boolean isFirstTime = true;
    private int errorCode = 0;
    private int shootCount = 0;
    private boolean isEmptyKey = false;
    private boolean closeFlag = true;
    private boolean isEndFlag = false;
    private Rect mGuideRect; // 가이드 영역
    private Rect mGuideRect_Large; // 가이드 영역

    /**
     * 인식결과값<br/>
     * 신분증 종류별 필드인덱스 설명 <br/>
     * - 주민등록증 ( 0 : 주민등록증, 1 : 이름, 2: 주민등록번호, 3 : 발급일, 5 : 발급처) <br/>
     * - 운전면허증 ( 0 : 자동차운전면허증, 1 : 이름, 2: 주민등록번호, 3 : 발급일, 4 : 면허번호, 5 : 발급처, 8 : 위변조방지코드, 9 : 만료일, 10 : 종별구분) <br/>
     * - 외국인등록증 ( 0 : 외국인등록증, 1 : 이름, 2: 외국인등록번호, 3 : 발급일, 6 : 국가 지역, 7 : 체류자격 ) <br/>
     */
    private ArrayList<String> mResultText;
    private Rect mRRNRect; // 주민등록번호 마스킹 영역
    private Rect mLicenseNumberRect; // 면허번호 마스킹 영역
    private Rect mPhotoRect; // 신분증 증명사진 영역
    boolean isMonochrome = false;
    boolean initSuccessSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mDocumentType = getIntent().getIntExtra(DATA_DOCUMENT_TYPE, TYPE_ID_CARD);
        mDocumentOrientation = getIntent().getIntExtra(DATA_DOCUMENT_ORIENTATION, ORIENTATION_PORTRAIT);
        isDetectSound = getIntent().getBooleanExtra(DOCUMENT_DETECTION_SOUND, true);
        lprGuideRect =  new Rect();
        mResultText = new ArrayList<>();

        mGuideRect = new Rect(); // 가이드 영역
        mGuideRect_Large = new Rect(); // 가이드 영역
        streams = new ArrayList<>();
        mFocusRect = new Rect();

        mIsAutoFocusStart = new AtomicBoolean(false); // 오토포커싱 진행중인지 여부
        mIsTakePictureStart = new AtomicBoolean(false); // 촬영 진행중인지 여부
        mIsFindDocumentAreaRunning = new AtomicBoolean(false); // 프리뷰 영역검출 진행중인지 여부
        mMonoCheck = getIntent().getBooleanExtra(DATA_MONOCHECK, MONO_DATA);

        String mTitleAutoTop;
        String mTitleAutoBot;
        String mTitleManualTop;
        String mTitleManualBot;
        String mTopColor;
        String[] mTopColorArray;
        String mBotColor;
        String[] mBotColorArray;
        try {
            mTitleAutoTop = getIntent().getStringExtra(DATA_TITLE_MESSAGE_AUTO_TOP);
            mTitleAutoBot = getIntent().getStringExtra(DATA_TITLE_MESSAGE_AUTO_BOT);
            mTitleManualTop = getIntent().getStringExtra(DATA_TITLE_MESSAGE_MANUAL_TOP);
            mTitleManualBot = getIntent().getStringExtra(DATA_TITLE_MESSAGE_MANUAL_BOT);

            mTopColor = getIntent().getStringExtra(DATA_TOP_COLOR);
            mTopColorArray = mTopColor.split("-");
            mBotColor = getIntent().getStringExtra(DATA_BOT_COLOR);
            mBotColorArray= mBotColor.split("-");

            int TopR = Integer.parseInt(mTopColorArray[0]);
            int TopG = Integer.parseInt(mTopColorArray[1]);
            int TopB = Integer.parseInt(mTopColorArray[2]);
            int BotR = Integer.parseInt(mBotColorArray[0]);
            int BotG = Integer.parseInt(mBotColorArray[1]);
            int BotB = Integer.parseInt(mBotColorArray[2]);

            ssbAutoTop = new SpannableStringBuilder(mTitleAutoTop);
            ssbAutoTop.setSpan(new ForegroundColorSpan(Color.rgb(TopR, TopG, TopB)), 0, mTitleAutoTop.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //ssb_Auto_Top.setSpan(new ForegroundColorSpan(Color.parseColor("#66FFFF")), 1,15, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            ssbAutoBot = new SpannableStringBuilder(mTitleAutoBot);
            ssbAutoBot.setSpan(new ForegroundColorSpan(Color.rgb(BotR, BotG, BotB)), 0, mTitleAutoBot.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //ssb_Auto_Bot.setSpan(new ForegroundColorSpan(Color.parseColor("#66FFFF")), 1,15, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            ssbManualTop = new SpannableStringBuilder(mTitleManualTop);
            ssbManualTop.setSpan(new ForegroundColorSpan(Color.rgb(TopR, TopG, TopB)), 0, mTitleManualTop.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //ssb_Manual_Top.setSpan(new ForegroundColorSpan(Color.parseColor("#66FFFF")), 1,15, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            ssbManualBot = new SpannableStringBuilder(mTitleManualBot);
            ssbManualBot.setSpan(new ForegroundColorSpan(Color.rgb(BotR, BotG, BotB)), 0, mTitleManualBot.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            //ssb_Manual_Bot.setSpan(new ForegroundColorSpan(Color.parseColor("#66FFFF")), 1,15, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }catch(Exception e){

        }
        mFlagAutoManual = getIntent().getBooleanExtra(IS_AUTO_MODE, MANUAL_MODE);
        mEncryptKey = getIntent().getStringExtra(DATA_ENCRYPT_KEY);
        if(mEncryptKey.length() < 1) {
            mEncryptKey = "rosisEncryptkey";
            isEmptyKey = true;
        }
        if (mDocumentType == TYPE_ID_CARD) {
            if (mDocumentOrientation == ORIENTATION_PORTRAIT)
                setContentView(R.layout.activity_camera_port);
            if (mDocumentOrientation == ORIENTATION_LANDSCAPE)
                setContentView(R.layout.activity_camera_land);
        }
        if (mDocumentType == TYPE_DOCUMENT_A4)
            setContentView(R.layout.activity_camera_port);
        if (mDocumentType == TYPE_DOCUMENT_ETC) {
            if (mDocumentOrientation == ORIENTATION_LANDSCAPE)
                setContentView(R.layout.activity_camera_land);
            if (mDocumentOrientation == ORIENTATION_PORTRAIT)
                setContentView(R.layout.activity_camera_port);
            mFlagAutoManual = MANUAL_MODE;
        }
        if (mDocumentType == TYPE_DOCUMENT_LPR) {
            mFlagAutoManual = MANUAL_MODE;
            setContentView(R.layout.activity_camera_land);
        }
        initView(); // view 초기화
        initSound();

        mIsInit = true;
        mThisActivity = this;
        mDialog = new ProgressDialog(mThisActivity, R.style.mDialog);

        isFirstTime = true;
        requestPermission(mThisActivity);
    }

    private void initSound() {
        try {
            soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
            soundBeepFound = soundPool.load(getApplicationContext(), R.raw.camera_beep_found, 1);
            soundBeepNotFind = soundPool.load(getApplicationContext(), R.raw.camera_beep_notfind, 1);
            streamNotFind = soundPool.play(soundBeepNotFind, 1f, 1f, 0, -1, 1f);
            streams.add(streamNotFind);
            initSuccessSound = true;
        }catch(Exception e){
            initSuccessSound = false;
        }
    }

    private void playSound(int mType) {
        if(!isDetectSound)
            return;

        if (!initSuccessSound) {
            try {
                initSound();
            }catch(Exception e){
                e.printStackTrace();
            }
            return;
        }
        //mType = 0 영역을 찾고있는중
        if(mType == NOTFIND_SOUND && !mNotFindFlag) {
            streamNotFind = soundPool.play(soundBeepNotFind, 1f, 1f, 0, -1, 1f);
            for(int i = 0; i < streams.size(); i++ )
                soundPool.stop(streams.get(i));
            mNotFindFlag = true;
            mFoundFlag = false;
           streams.add(streamNotFind);
        }
        //mType = 1 영역을 찾았을때
        else if(mType == FOUND_SOUND && !mFoundFlag) {
            int streamFound = soundPool.play(soundBeepFound, 1f, 1f, 0, -1, 2f);
            for(int i = 0; i < streams.size(); i++ )
                soundPool.stop(streams.get(i));
            mFoundFlag = true;
            mNotFindFlag = false;

            streams.add(streamFound);

            // Log.i(TAG, "FOUND_SOUND Called");
        }
        //mType = 2 재생을 멈춤
        else if(mType == STOP_SOUND){
            for(int i = 0; i < streams.size(); i++ )
                soundPool.stop(streams.get(i));
            streams.clear();
            mNotFindFlag = false;
            mFoundFlag = false;
        }
    }

    private void runCamera(boolean isChecked) {
        if (!isChecked) {
            setResult(RETURN_PERMISSION_ERROR);
            finish();
        }
        if (!LicenseChecker.isValidLicense(getApplicationContext())) {
                setResult(RETURN_LIBRARY_ERROR);
                finish();
        }
        if (!checkCameraHardware(mThisActivity)) {
                setResult(RETURN_CAMERA_ERROR);
                finish();
        }

    }

    private void requestPermission(Activity activity) {
        ArrayList<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE ) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (permissions.size() > 0) {
            String[] temp = new String[permissions.size()];
            permissions.toArray(temp);
            ActivityCompat.requestPermissions(activity, temp, REQ_PERMISSON_RESULT);
        } else {
            runCamera(true);
        }
    }

    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDocumentType == TYPE_ID_CARD || mDocumentType == TYPE_DOCUMENT_A4)
            playSound(NOTFIND_SOUND);

        misActionButton = false;

        if (mFlagAutoManual == AUTO_MODE) {
            mCameraTopTxt.setText(ssbAutoTop);
            mCameraBottomTxt.setText(ssbAutoBot);
        }
        else {
            mCameraTopTxt.setText(ssbManualTop);
            mCameraBottomTxt.setText(ssbManualBot);
        }

        if (mIsInit) {
            mIsAutoFocusStart.set(false);
            mIsTakePictureStart.set(false);
            mIsFindDocumentAreaRunning.set(false);
            mSimilarCount = 0;
        }

        if (mCameraView != null) {
            mCameraView.onResume(); // onPause로 멈춘 카메라 재실행
        }

        if (mIsRestart) { // 재시작이 필요한 경우
            restart(0);
            mIsRestart = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCameraView != null) {
            playSound(STOP_SOUND);
            mCameraView.onPause(); // 다른 앱에서 카메라 사용을 위해 카메라 중지
        }
        if (isEndFlag) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onDestroy() {
        progressDialog(null, false, null); // 프로그래스 종료

        if (mCameraView != null) {
            mCameraView.setPreviewCallback(null);
            mCameraView.destroyDrawingCache();
            mCameraView = null;
        }
        if (mImageCropView != null) {
            mImageCropView.destroyDrawingCache();
            mImageCropView = null;
        }
        ImageController.recycleBitmap(mCapturedImage); // 비트맵 메모리 해제
        playSound(STOP_SOUND);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mCameraView != null)
            mCameraView.setCameraDisplayOrientationByWindow(getWindowManager()); // 카메라 방향 업데이트
    }

    /**
     * 화면 터치시에 Auto focus 로 초점을 잡음
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsTakePictureStart.get() && !mIsAutoFocusStart.get() && event.getAction() == MotionEvent.ACTION_UP) {
            mIsAutoFocusStart.set(true);
            Message msg = new Message();
            msg.what = MSG_START_AUTO_FOCUS;
            Bundle data = new Bundle();
            data.putFloatArray(DATA_TOUCH_EVENT, new float[]{event.getX(), event.getY()}); // 터치 좌표 전달
            msg.setData(data);
            mFindDocumentAreaHandler.sendMessageDelayed(msg, 100);
        }
        return super.onTouchEvent(event);
    }

    /**
     * Auto focus 실행 결과가 반환되는 콜백 <br/>
     * 촬영 버튼에 의한 focusing 이면 캡쳐 진행
     */
    private final Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            mLastFocusTime = Calendar.getInstance().getTimeInMillis();
            if (mIsTakePictureStart.get()) {
                //if (!success || (mBlurValue > 0.6 || Double.isNaN(mBlurValue))) {
                if (!success && (mBlurValue > 0.6 || Double.isNaN(mBlurValue))) {
                    mIsAutoFocusStart.set(true);
                    mFindDocumentAreaHandler.sendEmptyMessageDelayed(MSG_START_AUTO_FOCUS, 200);
                } else {

                    Log.i(TAG, "mBlueValue : " + mBlurValue);

                    if (mCameraView != null) {
                        mCameraView.takePicture(mPictureCallback);  // 촬영, 입력받은 콜백으로 촬영 결과 반환
                    }
                }
            } else {
                if (!success) {
                    ++mFocusRetryCount;
                    mIsAutoFocusStart.set(true);
                    mFindDocumentAreaHandler.sendEmptyMessageDelayed(MSG_START_AUTO_FOCUS, 300);
                } else {
                    mFocusRetryCount = 0;
                }
            }
            mIsAutoFocusStart.set(false);
        }
    };

    /**
     * 캡쳐 버튼 클릭 리스너 <br/>
     * 캡쳐 버튼을 클릭하면 auto focus 후 캡쳐 진행
     */
    public void onCaptureBtnClick(View view) {
        playSound(STOP_SOUND);
        if (mIsTakePictureStart.get())
            return;

        mFindDocumentAreaHandler.sendEmptyMessage(MSG_REMOVE_NOTI_FOCUS);
        mBtnCrop.setEnabled(false);
        mIsTakePictureStart.set(true);

        if (mIsCropMode) { // Crop 화면인 경우
            mImageCropView.setTouchable(false);
            progressDialog(mThisActivity, true, getString(R.string.image_processing));
            new ImageProcessingThread(null).start();
        } else { // 촬영 화면인 경우
            mFindDocumentAreaHandler.removeMessages(MSG_START_AUTO_FOCUS);
            mFindDocumentAreaHandler.sendEmptyMessage(MSG_START_AUTO_FOCUS);
        }
    }

    public void onSwitchBtnClick(View view) {
        if(mFlagAutoManual == MANUAL_MODE) {
            mBtnTakePicture.setVisibility(View.INVISIBLE);
            if (mDocumentOrientation == ORIENTATION_LANDSCAPE)
                mBtnSwitch.setImageResource(R.drawable.btn_single_auto);
            else
                mBtnSwitch.setImageResource(R.drawable.btn_grid_auto);
            mCameraTopTxt.setText(ssbAutoTop);
            mCameraBottomTxt.setText(ssbAutoBot);
            mFlagAutoManual = AUTO_MODE;
        }
        else {
            mBtnTakePicture.setVisibility(View.VISIBLE);
            if (mDocumentOrientation == ORIENTATION_LANDSCAPE)
                mBtnSwitch.setImageResource(R.drawable.btn_single_manual);
            else
                mBtnSwitch.setImageResource(R.drawable.btn_grid_manual);
            mCameraTopTxt.setText(ssbManualTop);
            mCameraBottomTxt.setText(ssbManualBot);

            mFlagAutoManual = MANUAL_MODE;
        }
    }

    public void onCloseBtnClick(View view) {
        if (mIsCropMode) { // Crop 화면인 경우
            restart(100);
        } else { // 촬영 화면인 경우
            if(closeFlag) {
                closeFlag = false;
                setResult(RETURN_CANCEL);
                finish();
            }
        }
    }

    /**
     * 초기화
     */
    private void initView() {

        //Typeface fontFace = Typeface.createFromAsset(getAssets(), "fonts/ygothic530.ttf");

        mDocumentAreaFillPaint = new Paint();
        mDocumentAreaFillPaint.setStyle(Paint.Style.FILL);
        mDocumentAreaFillPaint.setColor(Color.argb(50, 0, 255, 0));

        mDocumentAreaBoarderPaint = new Paint();
        mDocumentAreaBoarderPaint.setColor(Color.WHITE);
        mDocumentAreaBoarderPaint.setStrokeWidth(7);
        mDocumentAreaBoarderPaint.setPathEffect(new DashPathEffect(new float[] {10,10}, 0));
        mDocumentAreaBoarderPaint.setStyle(Paint.Style.STROKE);

        mDocumentAreaPaint = new Paint();
        mDocumentAreaPaint.setColor(Color.argb(100, 0, 0, 0));
        mDocumentAreaPaint.setStyle(Paint.Style.FILL);
        mDocumentAreaPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        int mScreenWidth = this.getWindow().getWindowManager().getDefaultDisplay().getWidth();
        int mScreenHeight = this.getWindow().getWindowManager().getDefaultDisplay().getHeight();

            // 가로/세로 크기를 잘못 가져올때 수정 (2018.03.19)
            if(mScreenWidth <= mScreenHeight) {
                mScreenWidth = this.getWindow().getWindowManager().getDefaultDisplay().getWidth();
                mScreenHeight = this.getWindow().getWindowManager().getDefaultDisplay().getHeight();
            }
            else {
                mScreenWidth = this.getWindow().getWindowManager().getDefaultDisplay().getHeight();
                mScreenHeight = this.getWindow().getWindowManager().getDefaultDisplay().getWidth();

                Log.i(TAG, "!!! [Width | Height] SWAP !!!");
            }

        double mRatioWidth = (double) mScreenWidth / (double) 750;
        double mRatioHeight = (double) mScreenHeight / (double) 1334;

            double mScreenRatio = (double) mScreenHeight / (double) mScreenWidth;
            double mRatio_Value = 1 - (1.8 - mScreenRatio);

            mCameraView = (CameraView) findViewById(R.id.camera_view);

            mCameraView.setMinimumCameraSize(DOCUMENT_MINIMUM_CAMERA_SIZE);
            Log.i(TAG, "Document minimum setting");

            mCameraView.setCameraViewCallback(mCameraViewCallback);
            mImageCropView = (ImageCropView) findViewById(R.id.image_crop_view_crop);

            //Frame
            mFrame01 = (ImageView) findViewById(R.id.Frame_01);
            mFrame01.setX((int)(mRatioWidth * 0));
            mFrame01.setY((int)(mRatioHeight * 156));
            mFrame01.getLayoutParams().width = (int)(mRatioWidth * 80);
            mFrame01.getLayoutParams().height = (int)(mRatioHeight * 80);

            mFrame02 = (ImageView) findViewById(R.id.Frame_02);
            mFrame02.setX((int)(mRatioWidth * (750 - 80)));
            mFrame02.setY((int)(mRatioHeight * 156));
            mFrame02.getLayoutParams().width = (int)(mRatioWidth * 80);
            mFrame02.getLayoutParams().height = (int)(mRatioHeight * 80);

            mFrame03 = (ImageView) findViewById(R.id.Frame_03);
            mFrame03.setX((int)(mRatioWidth * 0));
            mFrame03.setY((int)(mRatioHeight * (1191 - 80)));
            mFrame03.getLayoutParams().width = (int)(mRatioWidth * 80);
            mFrame03.getLayoutParams().height = (int)(mRatioHeight * 80);

            mFrame04 = (ImageView) findViewById(R.id.Frame_04);
            mFrame04.setX((int)(mRatioWidth * (750 - 80)));
            mFrame04.setY((int)(mRatioHeight * (1191 - 80)));
            mFrame04.getLayoutParams().width = (int)(mRatioWidth * 80);
            mFrame04.getLayoutParams().height = (int)(mRatioHeight * 80);

            mBackGround01 = (ImageView) findViewById(R.id.bgFrame_01);
            mBackGround01.setX((int)(mRatioWidth * 0));
            mBackGround01.setY((int)(mRatioHeight * 0));
            mBackGround01.getLayoutParams().width = (int)(mRatioWidth * 750);
            mBackGround01.getLayoutParams().height = (int)(mRatioHeight * 156);

            mBackGround02 = (ImageView) findViewById(R.id.bgFrame_02);
            mBackGround02.setX((int)(mRatioWidth * 0));
            mBackGround02.setY((int)(mRatioHeight * 1191));
            mBackGround02.getLayoutParams().width = (int)(mRatioWidth * 750);
            mBackGround02.getLayoutParams().height = (int)(mRatioHeight * 193);

            mCameraTopTxt = (TextView) findViewById(R.id.cameraToptxt);
            mCameraTopTxt.setX((int)(mRatioWidth * 0));
            mCameraTopTxt.setY((int)(mRatioHeight * 220));
            mCameraTopTxt.getLayoutParams().width = (int)(mRatioWidth * 750);
            mCameraTopTxt.getLayoutParams().height = (int)(mRatioHeight * 140);
            //mCameraTopTxt.setTypeface(fontFace);

            mCameraMiddleTxt = (TextView) findViewById(R.id.cameraMiddletxt);
            mCameraMiddleTxt.setX((int)(mRatioWidth * 125));
            mCameraMiddleTxt.setY((int)(mRatioHeight * 567));
            mCameraMiddleTxt.getLayoutParams().width = (int)(mRatioWidth * 500);
            mCameraMiddleTxt.getLayoutParams().height = (int)(mRatioHeight * 200);
           //mCameraMiddleTxt.setTypeface(fontFace);

            mCameraBottomTxt = (TextView) findViewById(R.id.cameraBottomtxt);
            mCameraBottomTxt.setX((int)(mRatioWidth * 0));
            mCameraBottomTxt.setY((int)(mRatioHeight * 900));
            mCameraBottomTxt.getLayoutParams().width = (int)(mRatioWidth * 750);
            mCameraBottomTxt.getLayoutParams().height = (int)(mRatioHeight * 140);
            //mCameraBottomTxt.setTypeface(fontFace);

            //button
            mBtnCrop = (ImageView) findViewById(R.id.btn_crop);
            mBtnCrop.setY((int)(mRatioHeight * (1263 - 58)));
            mBtnCrop.getLayoutParams().width = (int)(mRatioWidth * 116);
            mBtnCrop.getLayoutParams().height = (int)(mRatioHeight * 116);

            mBtnTakePicture = (ImageView) findViewById(R.id.btn_takepicture);
            mBtnTakePicture.setY((int)(mRatioHeight * (1263 - 58)));
            mBtnTakePicture.getLayoutParams().width = (int)(mRatioWidth * 116);
            mBtnTakePicture.getLayoutParams().height = (int)(mRatioWidth * 116);
            mBtnTakePicture.setVisibility(mFlagAutoManual?View.GONE:View.VISIBLE);

            mBtnSwitch = (ImageView) findViewById(R.id.btn_Switch);
            mBtnSwitch.setX((int) (mRatioWidth * 34));
            mBtnSwitch.setY((int) (mRatioHeight * (1263 - 40)));
            mBtnSwitch.getLayoutParams().width = (int) (mRatioWidth * 186 * mRatio_Value);
            mBtnSwitch.getLayoutParams().height = (int) (mRatioHeight * 80);
            mBtnSwitch.setImageResource(mFlagAutoManual?R.drawable.btn_grid_auto:R.drawable.btn_grid_manual);

            mBtnClose = (ImageView) findViewById(R.id.btn_Close);
            mBtnClose.setX((int)(mRatioWidth * 664));
            mBtnClose.setY((int)(mRatioHeight * (78 - 26)));
            mBtnClose.getLayoutParams().width = (int)(mRatioWidth * 52);
            mBtnClose.getLayoutParams().height = (int)(mRatioWidth * 52);

            if (mDocumentType == TYPE_DOCUMENT_ETC) {
                mBtnSwitch.setVisibility(View.GONE);
                mBtnTakePicture.setVisibility(View.VISIBLE);
            }
        }
//    }

    /**
     * Progress Dialog를 입력된 값으로 표시.
     */
    public void progressDialog(Context context, boolean show, String mstring) {

        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
//            mDialog = null;
        }
        if (show && context != null) {
            SpannableString ssText = new SpannableString(mstring);
            ssText.setSpan(new RelativeSizeSpan(1f), 0, ssText.length(), 0);
            ssText.setSpan(new ForegroundColorSpan(Color.WHITE), 0, ssText.length(), 0);
            mDialog.setMessage(ssText);
            mDialog.show();
        }
    }
    /**
     * 인식 결과 전달
     */
    private void moveToResultActivity() {
        playSound(STOP_SOUND);
        playSound(STOP_SOUND);
        progressDialog(null, false, null);
        Intent intent = new Intent();
        intent.putStringArrayListExtra(DATA_RESULT_TEXT, mResultText);
        Bitmap e = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        byte[] encryptByteArray = null;
        int encryptLength = 600000;
        int encryptQuality = 80;

        if (mDocumentType == TYPE_ID_CARD) {

            mRRNRect.left /= 2;
            mRRNRect.right /= 2;
            mRRNRect.top /= 2;
            mRRNRect.bottom /= 2;
            intent.putExtra(DATA_RRN_RECT, mRRNRect);

            mLicenseNumberRect.left /= 2;
            mLicenseNumberRect.right /= 2;
            mLicenseNumberRect.top /= 2;
            mLicenseNumberRect.bottom /= 2;
            intent.putExtra(DATA_LICENSE_NUMBER_RECT, mLicenseNumberRect);

            mPhotoRect.left /= 2;
            mPhotoRect.right /= 2;
            mPhotoRect.top /= 2;
            mPhotoRect.bottom /= 2;
            intent.putExtra(DATA_PHOTO_RECT, mPhotoRect);
        }

        while(encryptLength > 500000) {
            byteArrayOutputStream.reset();
            if (mDocumentType == TYPE_DOCUMENT_A4 || mDocumentType == TYPE_DOCUMENT_ETC) {
                mCapturedImage.compress(Bitmap.CompressFormat.JPEG, encryptQuality, byteArrayOutputStream);
            } else {
                e = Bitmap.createScaledBitmap(mCapturedImage, mCapturedImage.getWidth() / 2, mCapturedImage.getHeight() / 2, false);
                e.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            }

            ImageController.recycleBitmap(e);

            encryptByteArray = byteArrayOutputStream.toByteArray();

            if(!isEmptyKey && (mDocumentType == TYPE_DOCUMENT_ETC)){
                Bitmap img = byteArrayToBitmap(encryptByteArray);

                Canvas c = new Canvas(img);
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.BLACK);
                //주민번호 영역 마스킹
                c.drawRect(mRRNRect, paint);
                //면허번호 영역 마스킹
                c.drawRect(mLicenseNumberRect, paint);
                //증명사진 영역 마스킹
                //paint.setStyle(Paint.Style.STROKE);
                //paint.setColor(Color.RED);
                //c.drawRect(photoRect, paint);
                encryptByteArray = bitmapToByteArray(img);
                c = null;
                if(!img.isRecycled())
                    img.recycle();

            }
            encryptQuality = encryptQuality - 10;
            encryptLength = encryptByteArray.length;
        }
        if (encryptByteArray.length < 100)
            return;

        intent.putExtra(DATA_ENCRYT_IMAGE_BYTE_ARRAY, encryptByteArray);
        intent.putExtra(DATA_BW_JUDGEMENT, isMonochrome);
        setResult(errorCode == 5?RETURN_OVERTIME:RETURN_OK, intent);
        isEndFlag = true;
        finish();
    }

    /**
     * 촬영된 이미지가 지워진 경우나 초점이 흐려 촬영에 실패한 경우의 프로세스
     *
     * @param errMessage 오류 문구
     */

    private void errorProcessing(int errMessage) {
        progressDialog(null, false, null);
        mBtnTakePicture.setEnabled(false);
        mBtnClose.setEnabled(false);
        //LayoutInflater li = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //View view = li.inflate(R.layout.layout_noti_focus, null, false);
        if(errMessage == R.string.error_focus) {
            mCameraMiddleTxt.setText(getText(R.string.error_focus));
            mIsTakePictureStart.set(false);
            mIsAutoFocusStart.set(false);
            mIsFindDocumentAreaRunning.set(false);
        }
        if (errMessage == R.string.error_retry)
            mCameraMiddleTxt.setText(getText(R.string.error_retry));

        if (errMessage == R.string.error_identy)
            mCameraMiddleTxt.setText(getText(R.string.error_identy));

        if (errMessage == R.string.error_recog_fail)
            mCameraMiddleTxt.setText(getText(R.string.error_recog_fail));

        if (errMessage == R.string.error_recog_fail_doc)
            mCameraMiddleTxt.setText(getText(R.string.error_recog_fail_doc));

        if (errMessage == R.string.error_recog_name)
            mCameraMiddleTxt.setText(getText(R.string.error_recog_name));

        if (errMessage == R.string.error_recog_rnn)
            mCameraMiddleTxt.setText(getText(R.string.error_recog_rnn));

        if (errMessage == R.string.error_recog_date)
            mCameraMiddleTxt.setText(getText(R.string.error_recog_date));

        if (errMessage == R.string.error_recog_licensenum)
            mCameraMiddleTxt.setText(getText(R.string.error_recog_licensenum));

        if (errMessage == R.string.error_recog_monochrome)
            mCameraMiddleTxt.setText(getText(R.string.error_recog_monochrome));

        if (errMessage == R.string.error_recog_3time)
            mCameraMiddleTxt.setText(getText(R.string.error_recog_3time));

        //if (err_message == R.string.error_recog_5time)
        //    mCameraMiddleTxt.setText(R.string.error_recog_5time);
        mCameraMiddleTxt.setVisibility(View.VISIBLE);
        restart(1500);
    }

    /**
     * 촬영을 다시 시작할 때 초기화 루틴
     *
     * @param delayMillis 초기화가 실행되는 지연 시간
     */
    private void restart(int delayMillis) {

        if (errorCode != 5)
            ImageController.recycleBitmap(mCapturedImage);

        if (mCameraView != null) {
            mCameraView.setVisibility(View.VISIBLE); // Camera 뷰 표시
            if (mDocumentType == TYPE_ID_CARD || mDocumentType == TYPE_DOCUMENT_A4)
                mBtnSwitch.setVisibility(View.VISIBLE);
            mBtnClose.setVisibility(View.VISIBLE);

            if(mFlagAutoManual == MANUAL_MODE)
                mBtnTakePicture.setVisibility(View.VISIBLE);
            else
                mBtnTakePicture.setVisibility(View.GONE);

            mFrame01.setVisibility(View.GONE);
            mFrame02.setVisibility(View.GONE);
            mFrame03.setVisibility(View.GONE);
            mFrame04.setVisibility(View.GONE);
            mBackGround01.setVisibility(View.VISIBLE);
            mBackGround02.setVisibility(View.VISIBLE);
        }

        mFindDocumentAreaHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (errorCode == 5) {
                    progressDialog(null, false, null);
                    moveToResultActivity();
                } else {
                    if (mImageCropView != null) {
                        mImageCropView.setCropMode(ImageCropView.CROP_MODE.NONE, null); // Crop 모드 해제
                        mImageCropView.setVisibility(View.GONE); // Crop 뷰 숨김
                        mImageCropView.destroyDrawingCache();
                    }

                    mCameraMiddleTxt.setVisibility(View.GONE);
                    playSound(NOTFIND_SOUND);
                      if (mFlagAutoManual == AUTO_MODE) {
                            mCameraTopTxt.setText(ssbAutoTop);
                            mCameraBottomTxt.setText(ssbAutoBot);
                       }else {
                            mCameraTopTxt.setText(ssbManualTop);
                            mCameraBottomTxt.setText(ssbManualBot);
                        }
                    mBtnCrop.setVisibility(View.GONE);

                    if (mCameraView != null) { // 촬영이 진행되면 카메라 프리뷰가 멈추므로 다시 시작하도록 설정
                        mCameraView.startPreview();
                        mCameraView.setPreviewCallback(mPreviewCallback);
                        mCameraView.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    }

                    mIsTakePictureStart.set(false);
                    mIsAutoFocusStart.set(false);
                    mIsFindDocumentAreaRunning.set(false);

                    mIsCropMode = false;
                    mIsFirstFrame = true;
                }
                mBtnTakePicture.setEnabled(true);
                mBtnClose.setEnabled(true);
            }
        }, delayMillis);
    }

    /**
     * 카메라 뷰 콜백
     */
    private final CameraView.CameraViewCallback mCameraViewCallback = new CameraView.CameraViewCallback() {
        @Override
        public void viewCreated() { // 카메라 뷰 생성
            if (mCameraView != null) {
                // 오토 포커싱 모드로 설정
                mCameraView.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                // 카메라 프리뷰를 받을 콜백 설정
                mCameraView.setPreviewCallback(mPreviewCallback);
            }
        }

        @Override
        public void viewChanged(int width, int height) { // 카메라 뷰 크기 변환
            if (mCameraView != null && mCameraView.getVisibility() == View.VISIBLE) {
                // 포커싱 영역 생성
                Canvas overlayCanvas = mCameraView.getOvelayCanvas();

                if (overlayCanvas != null) {
                    mCameraView.clearOvelayCanvas();
                    Camera.Size previewSize = mCameraView.getPreviewSize();
                    if (previewSize == null) {
                        return;
                    }

                    // 타입별 포커싱 영역 계산
                    if (mDocumentType == TYPE_DOCUMENT_LPR) { // 가로로 긴 직사각형

                        double ratioWH = previewSize.width / previewSize.height;

                        int LprLeft, LprRight, LprTop, LprBottom;

                        LprLeft = previewSize.width / 4;
                        LprTop = previewSize.height / 4;
                        LprRight = previewSize.width * 3 / 4;
                        LprBottom = previewSize.height * 3 / 4;

                        lprGuideRect.set(LprLeft, LprTop, LprRight, LprBottom);

                        mFocusRect = lprGuideRect;
                        mGuideRect = lprGuideRect;

                        overlayCanvas.drawARGB(200, 0, 0, 0);
                        overlayCanvas.drawRect(mGuideRect, mDocumentAreaPaint);

                    } else if(mDocumentType == TYPE_ID_CARD) {
                        int marginRatio, overRatio = 10;
                        float marginWidth, rectWidth, rectHeight, marginHeight;

                        if (mDocumentOrientation == ORIENTATION_PORTRAIT) {
                            marginRatio = 20;
                            marginWidth = (float) previewSize.height / marginRatio;
                            rectWidth = (float) previewSize.height - (marginWidth * 2);
                            rectHeight = rectWidth * ImageController.ID_CARD_ASPECT_RATIO;
                            marginHeight = (previewSize.width / 2) - (rectHeight / 2);

                            mGuideRect.set((int) marginWidth, (int) marginHeight, (int) (marginWidth + rectWidth), (int) (marginHeight + rectHeight));
                            mGuideRect_Large.set(0, (int) (marginHeight - rectHeight / overRatio), previewSize.height, (int) (marginHeight + rectHeight + rectHeight / overRatio));
                        } else {
                            marginRatio = 4;
                            marginWidth = (float) previewSize.width / marginRatio;
                            rectWidth = (float) previewSize.width - (marginWidth * 2);
                            rectHeight = rectWidth * ImageController.ID_CARD_ASPECT_RATIO;
                            marginHeight = (previewSize.height / 2) - (rectHeight / 2);

                            mGuideRect.set((int) marginWidth, (int) marginHeight, (int) (marginWidth + rectWidth), (int) (marginHeight + rectHeight));
                            mGuideRect_Large.set((int) (marginWidth - rectWidth / overRatio), (int) (marginHeight - rectHeight / overRatio), (int) (marginWidth + rectWidth + rectWidth / overRatio), (int) (marginHeight + rectHeight + rectHeight / overRatio));
                        }

                        mFocusRect = mGuideRect;

                        overlayCanvas.drawARGB(200, 0, 0, 0);
                        overlayCanvas.drawRect(mGuideRect, mDocumentAreaPaint);
                    } else {
                        mGuideRect.set(0, 0, previewSize.height, previewSize.width);
                        //mFocusRect = mGuideRect;

                        float marginHeight = (float) previewSize.width / 8;
                        float rectHeight = (float) previewSize.width - (marginHeight * 2);
                        float rectWidth = rectHeight / ImageController.DOCUMENT_A4_ASPECT_RATIO;
                        float marginWidth = (previewSize.height / 2) - (rectWidth / 2);
                        if (marginWidth < (float) previewSize.height / 10) {
                            marginWidth = (float) previewSize.height / 10;
                            rectWidth = (float) previewSize.height - (marginWidth * 2);
                            rectHeight = rectWidth * ImageController.DOCUMENT_A4_ASPECT_RATIO;
                            marginHeight = (previewSize.width / 2) - (rectHeight / 2);
                        }
                        mFocusRect.set((int) marginWidth, (int) marginHeight, (int) (marginWidth + rectWidth), (int) (marginHeight + rectHeight));
                    }
                }
            }
        }

        @Override
        public void viewDestroyed() { // 카메라 뷰 해제

        }
    };

    /**
     * 카메라에서 촬영이 이루어졌을 때 호출되는 콜백
     */
    private final CameraView.PictureCallback mPictureCallback = new CameraView.PictureCallback() {
        @Override
        public void onPictureTaken(Bitmap image, Camera camera) {
            if (image == null)
                return;
            progressDialog(mThisActivity, true, getString(R.string.image_processing)); // 프로그래스바 호출
            new ImageProcessingThread(image).start(); // 이미지 보정 작업 진행
        }
    };

    /**
     * 카메라에서 프리뷰를 전달받는 콜백 <br/>
     * 영상이 흐린지 판단하여 카메라가 초점을 잡도록 포커싱 실행 <br/>
     * 자동 촬영을 수행할지 여부 판단
     */
    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (!mIsFindDocumentAreaRunning.get()) {
                if (data != null) {
                    Camera.Size previewSize = mCameraView.getPreviewSize();
                    // 초점이 흐린지 여부를 수치로 반환(0~1), 1 이면 아주 흐림
                    // 입력 값 : 프리뷰 데이터, 프리뷰 가로 크기, 프리뷰 세로 크기,
                    mBlurValue = ImageController.getBlurValue(data, previewSize.width, previewSize.height);

                } else {
                    mBlurValue = 1.0;
                }

                if (mIsFirstFrame) { // 카메라가 켜지고 첫 프레임에서 포커싱 수행
                    mIsAutoFocusStart.set(true);
                    mFindDocumentAreaHandler.sendEmptyMessageDelayed(MSG_START_AUTO_FOCUS, 300);
                    mIsFirstFrame = false;
                } else {
                    // 1초 간격으로 초점이 흐린지 체크하여 포커싱 수행
                    long curTime = Calendar.getInstance().getTimeInMillis();
                    if (curTime - mLastFocusTime > 1000 && !mIsAutoFocusStart.get() && mBlurValue >= 0.6 && !mIsTakePictureStart.get()) {
                        mIsAutoFocusStart.set(true);
                        mFindDocumentAreaHandler.sendEmptyMessage(MSG_START_AUTO_FOCUS);
                    }
                }

                mIsFindDocumentAreaRunning.set(true);
                if (!mIsAutoFocusStart.get() && mBlurValue < 0.6 && data != null) {
                    Camera.Size size = camera.getParameters().getPreviewSize();
                    new FindDocumentAreaThread(data, size.width, size.height).start(); // 프리뷰에서 영역 검출 수행, 자동 촬영
                } else {
                    mSimilarCount = 0;
                    clearOverlayView();
                }
            }
        }
    };

    /**
     * 자동 촬영 판단 스레드
     */
    public class FindDocumentAreaThread extends Thread {
        private final byte[] mData;
        private int mWidth;
        private int mHeight;

        /**
         * @param data   카메라 프리뷰
         * @param width  카메라 프리뷰의 너비
         * @param height 카메라 프리뷰의 높이
         */
        public FindDocumentAreaThread(byte[] data, int width, int height) {
            if (data != null) {
                this.mData = Arrays.copyOf(data, data.length);
                mWidth = width;
                mHeight = height;
            } else {
                mData = null;
            }
        }

        @Override
        public void run() {
            final int SIMILAR_COUNT = 5; // 비슷한 영역이 3회 연속 검출되면 촬영 수행

            if (mData == null) {
                mSimilarCount = 0;
                clearOverlayView(); // 프리뷰에 그려진 영역 Clear
                return;
            }

            int degree = mCameraView.getCameraOrientation(mThisActivity); // 카메라 회전 각도
            if (mPreviewIdCardArea == null) {
                mPreviewIdCardArea = new int[ImageController.SIZE_DOC_AREA_ARRAY];
            }

            int type;
            int result;
            if (mDocumentType == TYPE_ID_CARD) {
                type = ImageController.TYPE_ID_CARD | ImageController.ORIENTATION_LANDSCAPE;
                result = ImageController.checkAreaSimilar(mData, type, mWidth, mHeight, degree, mPreviewIdCardArea, mGuideRect_Large, true);
            }else {
                type = ImageController.TYPE_DOCUMENT_A4 | ImageController.ORIENTATION_PORTRAIT;
                result = ImageController.checkAreaSimilar(mData, type, mWidth, mHeight, degree, mPreviewIdCardArea);
            }
            if (result == ImageController.FIND_AREA_FAIL) { // 영역 검출 실패시 초기화
                mSimilarCount = 0;
                clearOverlayView();
            } else {
                if (result == ImageController.FIND_AREA_SIMILAR) { // 유사한 영역
                    mSimilarCount++;
                } else { // 다른 영역
                    mSimilarCount = 0;
                }

                // 유사한 영역이 SIMILAR_COUNT 만큼 연속 검출되면 촬영 수행
                if (mSimilarCount > SIMILAR_COUNT && !mIsAutoFocusStart.get() && mFlagAutoManual == AUTO_MODE) {
                    mSimilarCount = 0;
                    runOnUiThread(() -> onCaptureBtnClick(null));
                    return;
                }

                // 찾은 영역을 카메라 프리뷰에 그리기 위해 핸들러 호출
                Bundle bundle = new Bundle();
                bundle.putIntArray(DATA_DOC_ARRAY, mPreviewIdCardArea);
                Message message = mFindDocumentAreaHandler.obtainMessage(MSG_OVERLAY_DRAW_AREA);
                message.setData(bundle);
                mFindDocumentAreaHandler.sendMessage(message);
            }
        }
    }

    /**
     * 카메라 프리뷰에 그려진 라인 제거를 위해 핸들러 호출
     */
    private void clearOverlayView() {
        Message message = mFindDocumentAreaHandler.obtainMessage(MSG_OVERLAY_CLEAR_AREA);
        mFindDocumentAreaHandler.sendMessage(message);

        if (mDocumentType == TYPE_ID_CARD || mDocumentType == TYPE_DOCUMENT_A4)
            playSound(NOTFIND_SOUND);
    }

    /**
     * 프리뷰에 표시되는 영역이나 메시지 변경을 위한 핸들러
     */
    private final Handler mFindDocumentAreaHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (mCameraView != null) {
                switch (msg.what) {
                    case MSG_REMOVE_NOTI_FOCUS: // 촬영 진행 중에 문구 숨김

                        break;
                    case MSG_START_AUTO_FOCUS: // 오토 포커싱 수행
                        mBtnCrop.setEnabled(true);
                        if (mIsTakePictureStart.get() && !mIsAutoFocusStart.get()) { // 촬영 진행
//                            if (mBlurValue > 0.6 || Double.isNaN(mBlurValue)) { // 초점이 흐리면 촬영하지 않고 문구 출력
//                                //errorProcessing(R.string.error_focus);
//                            } else {

                                    mCameraView.takePicture(mPictureCallback); // 촬영, 입력받은 콜백으로 촬영 결과 반환
                                //}

                        } else {

                                Bundle data = msg.getData();
                                float centerX = -1, centerY = -1;
                                if (data != null) {
                                    float[] event = data.getFloatArray(DATA_TOUCH_EVENT); // 터치에 의한 오토포커싱 값 확인
                                    if (event != null) {
                                        centerX = event[0];
                                        centerY = event[1];
                                    }
                                }

                                // 터치에 의한 오토포커싱이 아닌 경우
                                // 신분증 : 포커스영역 기준으로 중앙, 왼쪽, 오른쪽을 순환하며 포커싱 수행
                                // A4용지 : 포커스영역 기준으로 중앙, 위쪽, 아래쪽을 순환하며 포커싱 수행
                                int checked;
                                if (centerX < 0 || centerY < 0) {
                                    checked = mFocusRetryCount % 3;
                                    switch (checked) {
                                        case 0: // center
                                            centerX = mFocusRect.centerX();
                                            centerY = mFocusRect.centerY();
                                            mFocusRetryCount = 0;
                                            break;
                                        case 1:
                                            if (mDocumentType == TYPE_ID_CARD) { // left
                                                centerX = mFocusRect.left + mFocusRect.width() / 4;
                                                centerY = mFocusRect.centerY();
                                            } else { // top
                                                centerX = mFocusRect.centerX();
                                                centerY = mFocusRect.top + mFocusRect.height() / 4;
                                            }
                                            break;
                                        case 2:
                                            if (mDocumentType == TYPE_ID_CARD) {  // right
                                                centerX = mFocusRect.right - mFocusRect.width() / 4;
                                                centerY = mFocusRect.centerY();
                                            } else {  // bottom
                                                centerX = mFocusRect.centerX();
                                                centerY = mFocusRect.bottom - mFocusRect.width() / 4;
                                            }
                                            break;
                                        default:
                                            break;
                                    }

                                    Log.d("center1", centerX + " : " + centerY + " : " + checked);

                                    // mFocusRect 가 프리뷰크기 기준이므로 뷰크기에 맞게 보정
                                    PointF scaleValue = mCameraView.getOverlayCanvasScaleValue();
                                    centerX = centerX * scaleValue.x;
                                    centerY = centerY * scaleValue.y;
                                }

                                // 해당 좌표로 포커싱 수행
                                mCameraView.cancelAutoFocus();
                                Log.d("center2", centerX + " : " + centerY);
                                mCameraView.setFocusArea(centerX, centerY);
                                mCameraView.autoFocus(autoFocusCallback);
                        }
                        break;
                    case MSG_OVERLAY_DRAW_AREA: // 검출된 영역을 프리뷰에 그림
                        Bundle data = msg.getData();
                        if (data != null) {
                            int[] docArray = data.getIntArray(DATA_DOC_ARRAY);
                            if (docArray != null) {
                                Path path = new Path();
                                path.moveTo(docArray[0], docArray[1]);
                                path.lineTo(docArray[2], docArray[3]);
                                path.lineTo(docArray[4], docArray[5]);
                                path.lineTo(docArray[6], docArray[7]);
                                path.close();

                                Canvas overlayCanvas = mCameraView.getOvelayCanvas();
                                if (overlayCanvas != null) {
                                    mCameraView.clearOvelayCanvas();

                                    if (mDocumentType == TYPE_ID_CARD) {
                                        overlayCanvas.drawARGB(200, 0, 0, 0);
                                        overlayCanvas.drawRect(mGuideRect, mDocumentAreaPaint);
                                        overlayCanvas.drawRect(mGuideRect, mDocumentAreaBoarderPaint);
                                    }

                                    if (mDocumentType == TYPE_DOCUMENT_LPR) {
                                        overlayCanvas.drawARGB(200, 0, 0, 0);
                                        overlayCanvas.drawRect(mGuideRect, mDocumentAreaPaint);
                                    }

                                    if (mDocumentType == TYPE_ID_CARD || mDocumentType == TYPE_DOCUMENT_A4) {
                                        overlayCanvas.drawRect(mGuideRect, mDocumentAreaFillPaint);

                                        if (!misActionButton)
                                            playSound(FOUND_SOUND);
                                    }

                                    mCameraView.invalidateOvelay();
                                }
                            }
                        }

                        mIsFindDocumentAreaRunning.set(false);
                        break;
                    case MSG_OVERLAY_CLEAR_AREA: // 프리뷰에 그린 영역을 Clear
                        mCameraView.clearOvelayCanvas();

                        Canvas overlayCanvas = mCameraView.getOvelayCanvas();
                        if (overlayCanvas != null) {
                            mCameraView.clearOvelayCanvas();
                            if (mDocumentType == TYPE_ID_CARD) {
                                overlayCanvas.drawARGB(200, 0, 0, 0);
                                overlayCanvas.drawRect(mGuideRect, mDocumentAreaPaint);
                                overlayCanvas.drawRect(mGuideRect, mDocumentAreaBoarderPaint);
                            }

                            if (mDocumentType == TYPE_DOCUMENT_LPR) {
                                overlayCanvas.drawARGB(200, 0, 0, 0);
                                overlayCanvas.drawRect(mGuideRect, mDocumentAreaPaint);
                            }
                        }

                        mCameraView.invalidateOvelay();

                        mIsFindDocumentAreaRunning.set(false);
                        break;
                    default:
                        mCameraView.clearOvelayCanvas();
                        mCameraView.invalidateOvelay();
                        break;
                }
            }

            return false;
        }
    });

    /**
     * 각 스레드 처리 결과에 따라 호출되는 핸들러
     */
    private final Handler mImageProcessingHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_IMAGE_NEED_CROP: { // 영역검출이 실패하여 Crop 화면으로 변경
                    if (mCapturedImage == null || mCapturedImage.isRecycled()) {
                        errorProcessing(R.string.error_identy);
                        break;
                    }

                    playSound(STOP_SOUND);

                    mIsCropMode = true;
                    Bundle data = msg.getData();

                    Rect guideRect;
                    if (data == null) {
                        guideRect = new Rect(0, 0, mCapturedImage.getWidth() - 1,
                                mCapturedImage.getHeight() - 1);
                    } else {
                        Rect rect = data.getParcelable(DATA_GUIDE_RECT);
                        if (rect == null || rect.isEmpty()) {
                            if (mPreviewIdCardArea != null) {
                                guideRect = null;
                            } else {
                                guideRect = new Rect(0, 0, mCapturedImage.getWidth() - 1,
                                        mCapturedImage.getHeight() - 1);
                            }
                        } else {
                            guideRect = rect;
                        }
                    }

                    PointF[] quadranglePoints = new PointF[4];
                    if (guideRect == null) {
                        quadranglePoints[0] = new PointF(mPreviewIdCardArea[0], mPreviewIdCardArea[1]);
                        quadranglePoints[1] = new PointF(mPreviewIdCardArea[2], mPreviewIdCardArea[3]);
                        quadranglePoints[2] = new PointF(mPreviewIdCardArea[4], mPreviewIdCardArea[5]);
                        quadranglePoints[3] = new PointF(mPreviewIdCardArea[6], mPreviewIdCardArea[7]);
                    } else {
                        quadranglePoints[0] = new PointF(guideRect.left, guideRect.top);
                        quadranglePoints[1] = new PointF(guideRect.right, guideRect.top);
                        quadranglePoints[2] = new PointF(guideRect.right, guideRect.bottom);
                        quadranglePoints[3] = new PointF(guideRect.left, guideRect.bottom);
                    }

                    progressDialog(null, false, null);

                    mCameraView.setVisibility(View.GONE); // 카메라 뷰를 숨김

                    if (mDocumentType == TYPE_ID_CARD) {
                        //mCameraTop.setImageResource(R.drawable.text_id_top_crop);
                        mCameraTopTxt.setText("영역을 신분증에 맞게 조절 후 확인버튼을 눌러주세요");
                        //mCameraBottomTxt.setVisibility(View.GONE);
                    }
                    else {
                        //mCameraTop.setImageResource(R.drawable.text_doc_top_crop);
                        mCameraTopTxt.setText("영역을 문서에 맞게 조절 후 확인버튼을 눌러주세요");
                    }

//                    if (mDocumentType == TYPE_ID_CARD)
//                        mCameraBottomTxt.setVisibility(View.GONE);

                    mBtnTakePicture.setVisibility(View.GONE);
                    mBtnSwitch.setVisibility(View.GONE);

                    mFrame01.setVisibility(View.GONE);
                    mFrame02.setVisibility(View.GONE);
                    mFrame03.setVisibility(View.GONE);
                    mFrame04.setVisibility(View.GONE);
                    mBackGround01.setVisibility(View.GONE);
                    mBackGround02.setVisibility(View.GONE);
                    mBtnCrop.setVisibility(View.VISIBLE);
                    mBtnCrop.setEnabled(true);

                    // Crop뷰 설정
                    mImageCropView.setImageBitmap(mCapturedImage); // Crop 대상 이미지 입력
                    mImageCropView.setCropMode(ImageCropView.CROP_MODE.QUADRANGLE, quadranglePoints); // Crop 뷰의 모드 및 Crop 좌표 입력
                    mImageCropView.setVisibility(View.VISIBLE); // Crop 뷰를 보임
                    mImageCropView.setTouchable(true); // Crop 뷰 화면 터치 가능하도록 설정

                    mIsTakePictureStart.set(false);
                }
                break;
                case MSG_IMAGE_PROCESSING_END: // Perspective 수행 완료
                    if (mCapturedImage == null || mCapturedImage.isRecycled()) {
                        errorProcessing(R.string.error_identy);
                    } else {
                        //progressDialog(mThisActivity, true, getString(R.string.save_processing));
                        //new ImageSaveThread(mCaptruedImage, mDocumentType).start();
                        if(mDocumentType == TYPE_ID_CARD) {
                            startRecognition(mCapturedImage);
                        }else {
                            progressDialog(null, false, null);
                            moveToResultActivity(); // 결과 화면으로 이동
                        }
                    }
                    break;
                case MSG_IMAGE_SAVE_END: // 이미지 저장 완료
                case MSG_IMAGE_SAVE_FAIL: // 이미지 저장 실패
                    progressDialog(null, false, null);
                    moveToResultActivity(); // 결과 화면으로 이동
                    break;

                case MSG_RECOGNIZE_ERROR:
                     switch (errorCode){
                         case 0:
                             if(isFirstTime) {
                                 mCapturedImage = ImageController.rotateImage(mCapturedImage, 180, true);
                                 isFirstTime = false;
                                 startRecognition(mCapturedImage);
                             } else {
                                 progressDialog(mThisActivity, false, null);
                                 if (mDocumentType == TYPE_ID_CARD)
                                     errorProcessing(R.string.error_recog_fail);
                                 else
                                     errorProcessing(R.string.error_recog_fail_doc);
                                 isFirstTime = true;
                             }
                             break;
                         case 1:
                                 progressDialog(mThisActivity, false, null);
                                 errorProcessing(R.string.error_recog_name);
                                break;
                         case 2:
                             progressDialog(mThisActivity, false, null);
                             errorProcessing(R.string.error_recog_rnn);
                             break;
                         case 3:
                             progressDialog(mThisActivity, false, null);
                             errorProcessing(R.string.error_recog_date);
                             break;
                         case 4:

                             progressDialog(mThisActivity, false, null);
                             errorProcessing(R.string.error_recog_licensenum);
                             break;
                         case 5:
                             progressDialog(mThisActivity, false, null);
                             //2018.04.03 재촬영 횟수 수정 (5회 -> 3회)
                             errorProcessing(R.string.error_recog_3time);
                             break;
                         case 6:
                             progressDialog(mThisActivity, false, null);
                             errorProcessing(R.string.error_recog_monochrome);
                             break;
                     }
                    break;
                case MSG_RECOGNIZE_END:
                    moveToResultActivity();
                    break;
                default:
                    break;
            }

            return false;
        }
    });

    private void startRecognition(Bitmap origin) {
        progressDialog(mThisActivity, true, getString(R.string.recognize));


    }



    /**
     * OCR 결과 리스너 <br/>
     * 인식에 성공하면 인식결과 정보를 넘겨줌
     */

//    private boolean needFocusing() {
//        return Double.isNaN(mBlurValue) || mBlurValue > MIN_BLUR_VALUE;
//    }


    public class ImageProcessingThread extends Thread {
        private Bitmap mImage;

        /**
         * @param bitmap 카메라 촬영된 이미지
         */
        public ImageProcessingThread(Bitmap bitmap) {
            mImage = bitmap;
        }
        @Override
        public void run() {
            Rect resizeGuideRect;
            PointF[] quadPoints;
            isFirstTime = true;
            if (mIsCropMode) { // Crop 모드인 경우
                ImageController.recycleBitmap(mImage);
                mImage = mCapturedImage;
                resizeGuideRect = null;
                quadPoints = mImageCropView.getCroppedQuadPoints(); // 조정된 Crop 좌표 값을 받음
            } else {
                // 카메라 회전값에 맞게 이미지를 회전시킴
                int degree = mCameraView.getCameraOrientation(mThisActivity);
                mImage = ImageController.rotateImage(mImage, degree, true);
                if (mImage != null && !mImage.isRecycled()) {
                    //resizeGuideRect = new Rect(0, 0, mImage.getWidth(), mImage.getHeight());
                    Camera.Size previewSize = mCameraView.getPreviewSize();
                    Camera.Size pictureSize = mCameraView.getPictureSize();
                    float cameraPreviewPictureRatio = (float) pictureSize.width / previewSize.width;

                    if (mDocumentType == TYPE_ID_CARD)
                        resizeGuideRect = new Rect((int) (mGuideRect_Large.left * cameraPreviewPictureRatio), (int) (mGuideRect_Large.top * cameraPreviewPictureRatio), (int) (mGuideRect_Large.right * cameraPreviewPictureRatio), (int) (mGuideRect_Large.bottom * cameraPreviewPictureRatio));
                    else
                        resizeGuideRect = new Rect((int) (mGuideRect.left * cameraPreviewPictureRatio), (int) (mGuideRect.top * cameraPreviewPictureRatio), (int) (mGuideRect.right * cameraPreviewPictureRatio), (int) (mGuideRect.bottom * cameraPreviewPictureRatio));

                } else {
                    resizeGuideRect = null;
                }
                quadPoints = null;

            }

            Bitmap croppedImage = null;

            if (mImage != null && !mImage.isRecycled()) {
                int type = 0;
                if (mDocumentType == TYPE_ID_CARD)
                    type = mDocumentType | ORIENTATION_LANDSCAPE;
                if (mDocumentType == TYPE_DOCUMENT_A4)
                    type = mDocumentType | ORIENTATION_PORTRAIT;

                //int[] cardArea = ImageController.findArea(mImage, type, resizeGuideRect);

                // 입력된 이미지에서 신분증영역을 자동탐지하여 Crop
                if(mDocumentType == TYPE_ID_CARD)
                    croppedImage = ImageController.makeCroppedImage(mImage, type, resizeGuideRect, quadPoints, !mIsCropMode);
                else if(mDocumentType == TYPE_DOCUMENT_A4)
                    croppedImage = ImageController.makeCroppedImage(mImage, type, resizeGuideRect, quadPoints, !mIsCropMode);
                    //roppedImage = ImageController.perspectiveBitmap(mImage, type, cardArea, DOCUMENT_WIDTH_PORT, !mIsCropMode);
                else
                    croppedImage = mImage;
            }

            if (croppedImage == null && mImage != null && !mImage.isRecycled() && !mIsCropMode) { // 영역검출에 실패하여 Crop 모드 호출
                mCapturedImage = mImage;
                mImageProcessingHandler.sendEmptyMessage(MSG_RECOGNIZE_ERROR);
            } else { // Crop 된 이미지를 얻어 다음 단계 수행
                mCapturedImage = croppedImage;
                mImageProcessingHandler.sendEmptyMessage(MSG_IMAGE_PROCESSING_END);
            }
        }
    }
    @Override
    public void onBackPressed() {
        setResult(RETURN_CANCEL);
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mIsRestart = true;
        switch (resultCode) {
            case RETURN_OK :
                setResult(RETURN_OK);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                finish();
                break;

            case RETURN_OVERTIME :
                setResult(RETURN_OVERTIME);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                finish();
                break;

            case RETURN_OCR_FAIL :
                setResult(RETURN_OCR_FAIL);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                finish();
                break;

            case RETURN_CANCEL :
                break;

            case RETURN_NETWORK_ERROR :
                setResult(RETURN_NETWORK_ERROR);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                finish();
                break;

            case RETURN_END :
                setResult(RETURN_CANCEL);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                finish();
                break;
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        misActionButton = true;
        playSound(STOP_SOUND);
    }

    public Bitmap byteArrayToBitmap( byte[] mByteArray ) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        try{
            return BitmapFactory.decodeByteArray( mByteArray, 0, mByteArray.length, options);
        }catch(Exception e){
            return null;
        }
    }

    public byte[] bitmapToByteArray( Bitmap mBitmap ) {
        try(ByteArrayOutputStream stream = new ByteArrayOutputStream()){
            mBitmap.compress( Bitmap.CompressFormat.JPEG, 100, stream) ;
            return stream.toByteArray() ;
        }catch(Exception e){
            return null;
        }
    }
}
