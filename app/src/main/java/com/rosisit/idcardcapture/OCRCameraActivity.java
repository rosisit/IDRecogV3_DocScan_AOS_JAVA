package com.rosisit.idcardcapture;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cardcam.jni.IDCardRcgn;
import com.rosisit.idcardcapture.camera.CameraPreview;
import com.rosisit.idcardcapture.camera.CameraView;
import com.rosisit.idcardcapture.image.ImageUtils;
import com.rosisit.idcardcapture.screen.ScreenController;
import com.rosisit.idcardcapture.security.AES256Util;
import com.rosisit.idcardcapture.storage.StorageController;
import com.rosisit.idcardcapture.struct.ImageRecognition;
import com.rosisit.idcardcapture.utils.StringUtils;
import com.rosisit.idcardcapture.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class OCRCameraActivity extends Activity {

    /***
     * Event Handler
     */
    public Handler mHandler;

    //OCR 시작
    private final int HANDLER_MSG_RECOGNIZING_WORK_START = 1000;

    //OCR 종료
    private final int HANDLER_MSG_RECOGNIZING_WORK_END = 1001;

    //자동 촬영시 피사체가 영역에 들어왔을때 촬영 이벤트를 위한
    private final int HANDLER_MSG_AUTODETECT = 1005;


    /***
     * OCR 결과 상태
     */

    //OCR 성공
    public static final int RETURN_OK = 1;

    //OCR 중지
    public final static int RETURN_CANCEL = 2;

    //OCR 중지
    public final static int RETURN_NO_OCR = 3;

    //촬영횟수 초과
    public final static int RETURN_OVERTIME = 9;

    //OCR 실패
    public final static int RETURN_OCR_FAIL = 11;


    /***
     * OCR 결과값
     */

    //OCR 결과 필수값
    public static final String DATA_RESULT_STRUCT = "resultstruct";

    //OCR 결과 필수값
    public static final String DATA_RESULT_TEXT = "resultstrings";

    //OCR 이미지
    public static final String DATA_IMAGE_BYTEARRAY = "imagebytearray";

    //OCR 이미지에서 Crop 한 얼굴 이미지
    public static final String DATA_FACE_IMAGE_BYTE_ARRAY = "facebytearray";

    //OCR 이미지에서 Crop 한 얼굴 이미지를 BASE64로 인코딩
    public static final String DATA_FACE_IMAGE_BASE64 = "facebase64";

    //OCR 이미지의 흑백 여부
    public static final String DATA_COLOR_JUDGEMENT = "iscolor";

    //OCR 주민등록번호 영역
    public static final String DATA_ID_NUMBER_RECT = "idnumberrect";

    //OCR 운전면허등록번호 영역
    public static final String DATA_DRV_NUMBER_RECT = "drvnumberrect";

    //OCR 인물 이미지 영역
    public static final String DATA_USER_PHOTO_RECT = "userphotorect";

    //Orientation (추후 가능할 수 있도록 유지. 현재는 기본값)
    public static final String DATA_DOCUMENT_ORIENTATION = "documentorientation";

    public static final String JUST_TAKE_A_PICTURE = "justtakeapicture";




    /***
     * Intent Properties
     */
    //암호화 키
    public static final String DATA_ENCRYPT_KEY = "encryptkey";

    //암호화를 위한 IV
    public static final String DATA_ENCRYPT_IV = "encryptiv";

    //이미지 마스크 래핑 유무
    public static final String DATA_MASKING_PROCEED = "maskoption";

    //신분증이 아닌 이미지를 촬영할 때 제한 횟수
    public static final String SHUTTER_COUNT = "shootcount";

    //프리뷰 영역에 신분증이 들어왔을때, n초 후에 촬영 시작 (자동 촬영시 사용)
    public static final String SHUTTER_DELAY = "shootdelay";

    //프리뷰에서 자동 촬영일 때 화면에 출력할 메시지
    public static final String DATA_TITLE_MESSAGE_AUTO = "titlemessageauto";

    //프리뷰에서 수동 촬영일 때 화면에 출력할 메시지
    public static final String DATA_TITLE_MESSAGE_MANUAL = "titlemessagemanual";

    //프리뷰 영역에 출력할 메시지
    public static final String DATA_TITLE_MESSAGE_GUIDELINE = "titlemessageguideline";

    //자동 모드 우선
    public static final String AUTO_MODE = "automode";

    //자동/수동 변경 토글 보이기/숨기기
    public static final String SHUTTER_SWITCH = "shutterswitch";

    //프리뷰의 알파값 제거
    public static final String REMOVE_BACKGROUND_ALPHA = "previewfullscreen";

    //신분증 인식간 소리 재생
    public static final String DOCUMENT_DETECTION_SOUND = "detectsound";

    //과거 지역명이 표기된 운전면허증 앞 두자리를 숫자로 변환
    public static final String DRV_REGIONAL_CONVERSION="drvregionalconversion";

    public static final String PIC_HIGHLIGHT="pichighlight";

    //자동/수동 토글 테마 변경
    public static final String SHUTTER_SWITCH_THEME = "shutterswitchtheme";

    public static final String DATA_ALERT_MESSAGE = "alertmessage";

    public static final String DATA_MONOCHECK = "datamonocheck";


    /***
     * preview area
     */
    public View cameraWindow;
    public Rect cameraWindowPosition;
    private SurfaceView surfaceView;

    public FrameLayout onDrawView;
    private CameraView viewDraw;
    private CameraPreview mCameraPreview;
    private ImageButton mShutter;
    private TextView engineVersionText;
    private boolean isAutoDetectMode;
    private boolean isForgeryEnable;


    /***
     * 전역 변수
     */

    //Modal 객체
    private ProgressDialog mDialog;

    //처리 상태를 화면에 보여주는 Modal 객체
    private ProgressDialog waitingDialog;

    //OCR 결과를 암호화하기 위한 Key
    private String mEncryptKey;

    //암호화 여부
    private boolean isEncrypt;

    //재촬영 한계 수치
    private int defineShutterCnt;

    //재촬영 횟수
    private int shutterCount;

    //촬영 딜레이
    private int shutterDelay;

    //자동/수동 토글 보이기/숨기기 여부
    private boolean isShowShutterSwitch;

    //마스킹 옵션
    private int mMaskOption;

    //마스킹 옵션에 따른 적용 여부
    private boolean isMasking;

    //신분증 인식간 소리 재생 여부
    private boolean isDetectSound;

    //지역명 - 숫자...로 시작하는 과거 운전면허증을 현재 형태 (숫자 2자리 - 숫자...)로 변경 여부
    private boolean isConversionOldDrvNum;

    //증명 사진 하이라이트 표시 여부
    private boolean isPicHighlight;

    //신분증 촬영 후 OCR 진행여부
    private boolean isNotOcr;

    //CameraView 중복 진입 방지 객체
    public int onDrawScreening;

    //CameraView Clear
    public boolean isCanvasClear;

    //자동 촬영 유무
    private boolean isAutoMode;



    /***
     * 촬영시 소리 재생을 위한 객체
     */
    //Player
    private SoundPool soundPool;

    //신분증을 찾았을때 나는 소리
    private int soundBeepFound;

    //신분증을 찾지 못했을때 나는 소리
    private int soundBeepNotFind;
    private int streamFound;
    private int streamNotFind;
    private List<Integer> streams;
    private boolean mFoundFlag;
    private boolean mNotFindFlag;

    //OCR 결과 객체
    private IDCardRcgn.IDs_Data idRcgnResult;

    /***
     * Screen Controller
     */
    private ScreenController screen;

    //수치화된 프리뷰 포커스
    private int focusValue;

    private boolean monochromeCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int ret = IDCardRcgn.initRcgn(OCRCameraActivity.this);
        if (ret != 0) {
            String msg = (ret == -1 ?getString(R.string.error_license) :getString(R.string.error_unknown))
                    .concat("\n").concat(getString(R.string.error_finally_not_recog).concat(getString(R.string.error_ocr_fail_question_manager)));
            Utils.showNoticeDialog(OCRCameraActivity.this, getString(R.string.error_title), msg);
            return;
        }
        //재촬영 횟수 초기화
        shutterCount = 0;
        onDrawScreening = 0;
        isCanvasClear = false;
        cameraWindowPosition = new Rect(0, 0, 0, 0);
        mHandler = new ActivityHandler();


        //소리 재생 관련 객체 초기화
        streams = new ArrayList<>();
        mFoundFlag = false;
        mNotFindFlag = false;


        //영역 인식 소리 재생 여부
        isDetectSound = getIntent().getBooleanExtra(DOCUMENT_DETECTION_SOUND, false);

        monochromeCheck = getIntent().getBooleanExtra(DATA_MONOCHECK, true);


        //재촬영 횟수
        defineShutterCnt = getIntent().getIntExtra(SHUTTER_COUNT, 3);

        //신분증 인식 후 촬영간 지연 시간
        shutterDelay =  getIntent().getIntExtra(SHUTTER_DELAY, 0);

        //촬영 버튼 활성 여부
        isShowShutterSwitch = getIntent().getBooleanExtra(SHUTTER_SWITCH, true);

        //구 운전면허증 지역명 -> 숫자로 변환 여부
        isConversionOldDrvNum = getIntent().getBooleanExtra(DRV_REGIONAL_CONVERSION, false);

        //신분증 내 증명사진 하이라이트 여부
        isPicHighlight = getIntent().getBooleanExtra(PIC_HIGHLIGHT, false);

        //신분증 OCR 진행 안할지?
        //true: 촬영 -> 이미지 보정 -> 이미지 리턴
        //false: 촬영 -> OCR-> 이미지 보정 -> 이미지 및 OCR 결과 리턴
        isNotOcr = getIntent().getBooleanExtra(JUST_TAKE_A_PICTURE, false);

        //세로 화면 고성
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        StorageController storage = new StorageController(this);
        screen = new ScreenController(this);

        setContentView(R.layout.activity_camera);

        //촬영간 여러 알림을 위한 다이얼로그 객체
        mDialog = new ProgressDialog(this, R.style.mDialog);
        initViews();
        initSound();

        //암호화 키
        String intentKey = getIntent().getStringExtra(DATA_ENCRYPT_KEY);

        //IV
        String intentIv = getIntent().getStringExtra(DATA_ENCRYPT_IV);

        String mEncryptIV;
        try {
            mEncryptKey = intentKey != null ? intentKey : "";
            mEncryptIV = intentIv != null ? intentIv : "";
            int keyLength = mEncryptKey.length();
            int ivLength = mEncryptIV.length();
            isEncrypt = !(keyLength != ivLength || keyLength < 16);
            if (isEncrypt)
                AES256Util.setIVValue(mEncryptIV);
        } catch (Exception e) {
            isEncrypt = false;
            Log.e("Exception", e.getMessage());
        }

        //Raw 데이터 경로
        //확인 필요 -> 이미지는 메모리에만 적재하기 때문에 왜 임시 경로가 필요한지 확인 필요
        String rawDataPath = storage.createWorkDir("IDRcgn");
        IDCardRcgn.SetExternalStorage(rawDataPath);



        //마스킹 옵션
        mMaskOption = getIntent().getIntExtra(DATA_MASKING_PROCEED, 1);

        //마스킹 진행 여부
        isMasking = mMaskOption > 1;

        String engineText;
        try{
            engineText = IDCardRcgn.GetDescription();
        }catch(Exception e){
            engineText = "";
        }
        //OCR 엔진 버전 출력
        engineVersionText.setText(engineText);
    }

    private void initViews() {
        getAppScreenInfo();

        try {
            onDrawView = (FrameLayout) findViewById(R.id.ondraw_view);
            viewDraw = new CameraView(this);
            onDrawView.addView(viewDraw);

            mShutter = (ImageButton) findViewById(R.id.shutter_button);
            CheckBox chkSelectAutodetect = (CheckBox) findViewById(R.id.chk_select_autodetect);
            isAutoMode = getIntent().getBooleanExtra(AUTO_MODE, true);
            int mTheme = getIntent().getIntExtra(SHUTTER_SWITCH_THEME, 1);

            if (mTheme != 0) {
//            switch (mTheme) {
//                case 1:
//                    chk_select_autodetect.setButtonDrawable(R.drawable.auto_check_wh);
//                    break;
//                case 2:
//                    chk_select_autodetect.setButtonDrawable(R.drawable.auto_check_blue);
//                    break;
//            }
                chkSelectAutodetect.setButtonDrawable(mTheme == 1 ? R.drawable.auto_check_wh : R.drawable.auto_check);
                chkSelectAutodetect.setScaleY(0.85f);
                chkSelectAutodetect.setScaleX(0.85f);
                RelativeLayout.LayoutParams chkLayout = (RelativeLayout.LayoutParams) chkSelectAutodetect.getLayoutParams();

                chkLayout.topMargin = 60;
                chkLayout.leftMargin = 20;
                chkSelectAutodetect.setLayoutParams(chkLayout);
            }


            //촬영 버튼 Visible 여부
            if (isShowShutterSwitch) {
                //수동 모드일때
//            if (!isAutoMode) {
//                chkSelectAutodetect.setChecked(false);
//            } else {
//                //자동모드일때
                chkSelectAutodetect.setChecked(isAutoMode);
                //}
            } else {
                //촬영버튼 숨기기
                chkSelectAutodetect.setVisibility(View.INVISIBLE);
            }


            //촬영 버튼 클릭 이벤트 리스너
            mShutter.setOnClickListener(v -> {
                if (v.getId() == R.id.shutter_button) {// take picture
                    mCameraPreview.takePicture();
                }
            });

            //자동/수동 토글 클릭 이벤트 리스너
            chkSelectAutodetect.setOnClickListener(v -> {
                boolean checked = ((CheckBox) v).isChecked();
                //자동 모드
                if (checked) {
                    isAutoMode = true;
                    mShutter.setVisibility(View.INVISIBLE);
                } else {
                    //수동모드
                    isAutoMode = false;
                    mShutter.setVisibility(View.VISIBLE);
                }

                adjustLayout(false);
                setToPreviewMode(isAutoMode);
            });

            findViewById(R.id.btn_close).setOnClickListener(v -> {
                if (v.getId() == R.id.btn_close) {
                    onBackPressed();
                }
            });
            engineVersionText = findViewById(R.id.engine_version_text);
            adjustLayout(true);
        }catch(Exception e){
            Log.e("Init Error", e.getMessage());
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = true;

        if (requestCode != 100)
            return;
        if (grantResults.length > 0) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    granted = false;
                    break;
                }
            }
        } else {
            // if grantResult length is 0, permission denied.
            granted = false;
        }
        if (!granted) {
            Toast.makeText(OCRCameraActivity.this, getString(R.string.guide_permission_granted), Toast.LENGTH_LONG).show();
            OCRCameraActivity.this.finish();        // 앱 종료
        } else {
            // 카메라 활성 허용 후 앱 restart
            Utils.RestartApp(OCRCameraActivity.this, OCRCameraActivity.class);
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        createPreview();
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        playSound(0);
        setAutoDetection(isAutoDetectMode);
    }


    @Override
    protected void onPause() {
        //OCR 중 기다리는 시간에 Back 에서 실행될 기능
        int HANDLER_MSG_AWAITING_TIMER = 1003;
        mHandler.removeMessages(HANDLER_MSG_AWAITING_TIMER);
        playSound(2);
        setAutoDetection(false);
        super.onPause();
    }

    @Override
    protected void onStop() {
        playSound(2);
        cameraWindow.getViewTreeObserver().removeOnGlobalLayoutListener(mOnGlobalLayoutListener);

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        playSound(2);
        setAutoDetection(false);
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        setResult(RETURN_CANCEL, new Intent());
        finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /***
     * OCR 데이터 유효성 검토
     * @return 에러메시지
     */
    private String verificationData() {

        //OCR을 하지 않을땐 Empty로 return
        if(isNotOcr)
            return "";

        if(monochromeCheck && idRcgnResult.isColor == 0)
            return "촬영된 신분증이 복사본(흑백)입니다. 신분증을 확인하시고 재촬영해 주십시오.";

        //OCR로 파악된 신분증 종류
        int idType = idRcgnResult.IDtype;

        //에러 메시지를 담을 리스트 객체
        List<String> errorList = new ArrayList<>();

        try {
            //공통 항목 (=주민등록증)
            //OCR: 이름
            String name = new String(idRcgnResult.IDname, "euc-kr");

            //OCR: 발급일자 - 숫자가 아닌 문자 제거
            String issuedData = new String(idRcgnResult.IDdate, "euc-kr").replaceAll(getString(R.string.regex_number), "");

            //OCR: 발급기관
            String issuedBy = new String(idRcgnResult.IDorgan, "euc-kr");

            //OCR: 주민등록번호 - 숫자가 아닌 문자 제거
            String idNumber = new String(idRcgnResult.IDstr, "euc-kr").replaceAll(getString(R.string.regex_number), "");


            //======이름, 발급일, 발급 기관, 주민등록번호 유효성 검토======
            //이름이 2자 미만일때
            checkData(errorList, name, 2, "성함", false);

            //발급일이 5자리 미만일때
            checkData(errorList, issuedData, 5, "발급일", false);

            //발급기관명이 3자리 미만일때
            checkData(errorList, issuedBy, 3, "발급기관", false);

            //주민등록번호가 13자리가 아닌때
            checkData(errorList, idNumber, 13, "주민등록번호", true);

            //신분증 종류별 분기
            switch (idType) {

                //운전면허등록증
                case 2:

                    //OCR: 운전면허등록번호 - 숫자 혹은 한글이 아닌 문자 제거
                    String licenseNumber = new String(idRcgnResult.IDLicense, "euc-kr").replaceAll(getString(R.string.regex_number_korean), "");

                    //OCR: 암호화 일련번호
                    String pwdSerialNumber = new String(idRcgnResult.chkstr, "euc-kr");

                    //OCR: 운전면허 종별 구분
                    String licenseArray = StringUtils.getArraysToString(idRcgnResult.DrvKindNum, idRcgnResult.DrvKinds).trim();

                    //======운전면허등록번호, 암호화 일련번호, 종별 구분 유효성 검토======

                    //운전면허등록번호가 12자리가 아닐때
                    checkData(errorList, licenseNumber, 12, "운전면허번호", true);

                    //종별 구분이 1자리 미만 일때
                    checkData(errorList, licenseArray, 1, "종별 구분", false);

                    //암호화 일련 번호가 6자리가 아닐때
                    checkData(errorList, pwdSerialNumber, 6, "암호 일련 번호", true);
                    break;

                //외국인 신분증, 외국국적동포 국내거소신고증
                case 3:

                    //OCR: 국가명
                    String country = new String(idRcgnResult.IDcountry, "euc-kr");

                    //======국가명 유효성 검토======

                    //국가명이 1자 이하일때
                    checkData(errorList, country, 2, "국가", false);
                    break;
            }
        } catch (Exception e) {
            //Log.i("RosisException", e.getMessage());
            return getString(R.string.error_ocr_fail_add_retry);
        }

        //필수 OCR 항목이 유효성 검사에서 문제가 없을때 Empty return
        if (errorList.size() == 0)
            return "";

        //에러 메시지를 담기위한 빌더
        StringBuilder errorMsgBuilder = new StringBuilder();

        for (int i = 0; i < errorList.size(); i++) {
            String errorMsg = errorList.get(i);
            //첫번째 에러 메시지가 아닐 경우에만 콤마 + 에러 메시지
            errorMsgBuilder.append(i != 0 ? ", " + errorMsg : errorMsg);
        }

        //전체 에러 메시지
        String errorFullMsg = errorMsgBuilder.toString();

        //에러 메시지가 20자를 초과할때
        if (errorFullMsg.length() > 20) {
            //에러 메시지를 20자리까지 추출
            String tempErrorSplitMsg = errorFullMsg.substring(0, 19);

            //추출된 20자리 에러 메시지에서 마지막 콤마 위치를 가져옴
            int lastIndex = tempErrorSplitMsg.lastIndexOf(",");

            //마지막 콤마전의 에러 메시지를 가져온 후 "~등의"로 메시지를 축약 시킴
            errorFullMsg = tempErrorSplitMsg.substring(0, lastIndex).concat(getString(R.string.error_ocr_fail_items_header));
        }

        //전체 에러 메시지의 맺음말 추가하여 Return
        return errorFullMsg.concat(getString(R.string.error_ocr_fail_items_footer));
    }

    /***
     * Activity Handler Class
     * 이벤트 발생등으로 메시지가 전달될때 핸들러에서 메시지에 해당하는 이벤트를 실행함
     * inner private class 로 느슨하게 연결
     */
    @SuppressLint("HandlerLeak")
    private class ActivityHandler extends Handler {
        public ActivityHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            switch (msg.what) {

                //OCR 시작 ~ 진행 중
                case HANDLER_MSG_RECOGNIZING_WORK_START:
                    Bitmap mBitmap = (Bitmap) msg.obj;
                    showWaitingDialog(getString(R.string.wait_ocr));
                    new Thread(new RecognizingThread(mBitmap)).start();
                    break;

                //OCR 종료 ~ 결과 Return
                case HANDLER_MSG_RECOGNIZING_WORK_END:
                    hideWaitingDialog();

                    //엔진 라이선스 체크
                    int rcgnStatus = msg.arg1;

                    //rcgnStatus == -1 비인가 라이선스로 OCR 작동 X
                    if (rcgnStatus == -1) {
                        Utils.showNoticeDialog(OCRCameraActivity.this, getString(R.string.error_title),
                                    getString(R.string.error_ocr_fail)
                                            .concat(getString(R.string.error_ocr_fail_question_manager))
                                            .concat(getString(R.string.error_ocr_fail_error_code)
                                                    .concat(String.valueOf(rcgnStatus))));
                        setToPreviewMode(true);
                        saddleNum = 0;
                        ippPreviewDet = 0;
                        cornerPtNum = 0;
                        idCheckStatus = 0;
                        setBase = 0;
                        break;
                    }

                    //이미지 생성 여부
                    //이미지가 없거나, 가로 길이 0이나 세로 길이가 0일 때 -> 이미지가 생성되지 않음
                    boolean isNullImage = idRcgnResult.resWidth <= 0 || idRcgnResult.resHeight <= 0 || idRcgnResult.resImage == null;

                    //이미지 생성에 실패했을때
                    if (isNullImage) {
                        mHandler.removeMessages(HANDLER_MSG_RECOGNIZING_WORK_END);

                        //지정된 촬영 횟수보다 촬영 시도 횟수가 많을시
                        //라이브러리 종료
                        if (shutterCount > defineShutterCnt) {
                            reachLimitShootCount(getString(R.string.error_overtime_header) + shutterCount + getString(R.string.error_overtime_footer));
                        } else {
                            //신분증 인식 실패 Alert
                            showErrorRestart(getString(R.string.error_ocr_fail_add_retry));
                        }
                    } else {
                            //OCR return 값에 대해 유효성 검토 실행
                            String verificationResultText = verificationData();

                            //유효성 검토를 통과 하지 못한 영역별 Text 가 n개 이상일때
                            if(verificationResultText.equals("")) {
                                moveToResultActivity();
                            }else {
                                idRcgnResult = null;
                                showErrorRestart(verificationResultText);
                            }

//
//                            if (!verificationResultText.equals("")){
//                                //결과 객체를 비워주고
//                                idRcgnResult = null;
//
//                                //유효성 검사간 통과 하지 못한 항목을 Alert
//                                showErrorRestart(verificationResultText);
//                            }
//
//                            //OCR 데이터 콜백 준지
//                            moveToResultActivity();
                    }
                    break;

                //자동 촬영간 신분증 인식시
                case HANDLER_MSG_AUTODETECT:
                    try {

                        //사용자 혹은 기본값으로 지정된 시간만큼 촬영 지연
                        Thread.sleep(shutterDelay);
                    }catch (Exception e){
                        Log.e("Exception",e.getMessage());
                    }

                    //사진 촬영
                    mCameraPreview.takePicture();
                    break;
            }
        }
    }

    /***
     * 진행 사항 dialog
     * @param context dialog 를 띄울 대상
     * @param show dialog show 여부
     * @param msg 출력 할 메시지
     */
    public void progressDialog(Context context, boolean show, String msg) {
        //이미 dialog 가 띄워져 있을 시 해당 Dialog 죽임
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();

        //dialog 를 그려 줄 대상(부모)이 있을때
        if (show && context != null) {
            SpannableString ssText = new SpannableString(msg);
            ssText.setSpan(new RelativeSizeSpan(1f), 0, ssText.length(), 0);
            ssText.setSpan(new ForegroundColorSpan(Color.WHITE), 0, ssText.length(), 0);

            //메시지를 set -> show
            mDialog.setMessage(ssText);
            mDialog.show();
        }
    }

    /***
     * OCR 데이터에 대한 유효성 검토 (길이로 체크)
     * @param errorList 에러 메시지를 담는 List
     * @param checkString OCR 결과 데이터
     * @param length 유효성 검토를 위한 데이터 길이
     * @param addMessage 유효성 검토를 통과 하지 못할때 errorList 에 넣어줄 메시지
     * @param isEquals true - checkString length 와 length 가 일치, false - checkString length 보다 length 작을 때
     */
    private void checkData(List<String> errorList, String checkString, int length, String addMessage, boolean isEquals) {
        try {
            //isEquals true - OCR 결과 데이터 length 와 length 가 일치
            //         false - OCR 결과 데이터 length 보다 length 작을 때,
            //or OCR 결과 데이터가 empty 일때
            if ((isEquals ? checkString.length() != length : checkString.length() < length) || checkString.equals(""))
                errorList.add(addMessage);
        } catch (Exception e) {
            errorList.add(addMessage);
        }
    }

    /***
     * OCR 데이터 콜백 준비
     */
    private void moveToResultActivity() {
        //콜백 준비가 OCR 데이터 결과 객체가 비어있을땐 빛반사로 인해 신분증이 비정상 촬영되었을 가능성이 높음
        if (idRcgnResult == null) {
            Utils.showNoticeDialog(OCRCameraActivity.this, getString(R.string.error_title), getString(R.string.error_ocr_fail_cuz_light));
            return;
        }

        //해당 라이브러리를 호출한 Activity 에 결과 값 전달
        Intent intent = new Intent();

        //암호화 여부
        intent.putExtra("IS_DATA_ENCRYPT", isEncrypt);

        //OCR Type 이게 뭐였지????
        intent.putExtra("OCR_TYPE",0);

        //신분증 이미지를 담을 객체 선언
        byte[] mImageByteArray;
        try {
            //신분증 이미지 리사이즈
            mImageByteArray = ImageUtils.resizedBitmap(idRcgnResult.resWidth, idRcgnResult.resHeight, idRcgnResult.resImage);

            //신분증 이미지내 증명사진 영역
            int[] facePos = idRcgnResult.r_Photo;

            //증명사진에 하이라이트 설정시
            if(isPicHighlight)
                //신분증 이미지, 증명사진에 선을 그려줌
                mImageByteArray =  ImageUtils.createImageFocusLine(mImageByteArray,facePos);

            //신분증 이미지 (암호화 여부 판단하여 암호화, 평문 데이터 전달)
//            intent.putExtra(DATA_IMAGE_BYTEARRAY, isEncrypt ? AES256Util.aesEncode(mImageByteArray, mEncryptKey) : mImageByteArray);
            intent.putExtra(DATA_IMAGE_BYTEARRAY, mImageByteArray);

            //신분증 이미지 방향 -> 의미없음
            intent.putExtra(DATA_DOCUMENT_ORIENTATION, 256);

            //단순 촬영만 진행 시(OCR X), 신분증 이미지만 Return 후 라이브러리 종료
            if(isNotOcr){
                setResult(RETURN_NO_OCR, intent);
                finish();
            }

            //신분증 내 증명사진 Crop
            //한국 인식 산업 모듈(특장점 추출)을 태우기 위해선 증명사진에 대해 일정 용량이 확보가 되어야 함 (Resize X)
            byte[] faceImage = ImageUtils.getFaceBitmap(idRcgnResult.resWidth, idRcgnResult.resHeight,idRcgnResult.resImage, facePos);

            intent.putExtra(DATA_FACE_IMAGE_BYTE_ARRAY, faceImage);
            //intent.putExtra(DATA_FACE_IMAGE_BASE64, Base64.encodeToString(faceBytes, 0));

            //메모리 덤프를 피하기 위해 OCR 결과 객체내 신분증 이미지를 소멸시킴
            idRcgnResult.resImage = null;
            faceImage = null;
            mImageByteArray = null;

            //OCR 결과를 담을 객체 선언 ArrayList 로만 전달 가능
            ArrayList<String> mResultText;



//            //신분증내 인식된 영역을 resize 된 이미지에 맞춰 재설정
//            //타깃이 4인 이유는 영역이 left, top, right, bottom 으로 int[4]로 고정되어 있기 때문
//            for (int i = 0; i < 4; i++) {
//
//                //주민등록번호 영역
//                idRcgnResult.r_IDstr[i] = Utils.splitInHalf(idRcgnResult.r_IDstr[i]);
//
//                //운전면허번호 영역
//                idRcgnResult.r_IDLicense[i] = Utils.splitInHalf(idRcgnResult.r_IDLicense[i]);
//
//                //이름 영역
//                idRcgnResult.r_IDname[i] = Utils.splitInHalf(idRcgnResult.r_IDname[i]);
//
//                //발급 일자 영역
//                idRcgnResult.r_IDdate[i] = Utils.splitInHalf(idRcgnResult.r_IDdate[i]);
//
//                //발급 기관 영역
//                idRcgnResult.r_Organ[i] = Utils.splitInHalf(idRcgnResult.r_Organ[i]);
//
//                //주소 영역
//                idRcgnResult.r_Address[i] = Utils.splitInHalf(idRcgnResult.r_Address[i]);
//
//                //암호화 일련 번호 영역 (운전면허증)
//                idRcgnResult.r_chkstr[i] = Utils.splitInHalf(idRcgnResult.r_chkstr[i]);
//
//                //국가명 영역 (외국인 신분증)
//                idRcgnResult.r_IDcountry[i] = Utils.splitInHalf(idRcgnResult.r_IDcountry[i]);
//
//                //뭔지 모르겠음
//                idRcgnResult.r_IDarea[i] = Utils.splitInHalf(idRcgnResult.r_IDarea[i]);
//
//                //증명 사진 영역
//                idRcgnResult.r_Photo[i] = Utils.splitInHalf(idRcgnResult.r_Photo[i]);
//            }


            //신분증 인식 결과가 운전면허증일때
            //인식 영역이 많기 때문에 추가되어야 할 부분이 많음
            if(idRcgnResult.IDtype == 2){
                //운전면허 종별 구분
                //1개 이상으로 구성되어 있을 수 도 있어, Arrays -> String 으로 변환 후 byte 로 변환
                idRcgnResult.sDrvKinds = StringUtils.getArraysToString(idRcgnResult.DrvKindNum, idRcgnResult.DrvKinds).getBytes();

                //구형 운전면허에 존재하는 지역명을 숫자로 변환 할 시
                if(isConversionOldDrvNum){

                    //운전면허번호를 String으로 변환
                    String licenseNumber = StringUtils.convertByteArrayToUTF8(idRcgnResult.IDLicense);

                    //지역명 추출
                    //ex) 서울, 부산, 경북, 전남, 제주 등
                    String licenseNumberLocal = licenseNumber.substring(0, 2);

                    //11(서울)을 기본값으로
                    String replaceLocal = "11";

                    //지역명에 따라 숫자로 변환
                    switch (licenseNumberLocal) {
                        case "부산":
                            replaceLocal = "12";
                            break;
                        case "경기":
                            replaceLocal = "13";
                            break;
                        case "강원":
                            replaceLocal = "14";
                            break;
                        case "충북":
                            replaceLocal = "15";
                            break;
                        case "충남":
                            replaceLocal = "16";
                            break;
                        case "전북":
                            replaceLocal = "17";
                            break;
                        case "전남":
                            replaceLocal = "18";
                            break;
                        case "경북":
                            replaceLocal = "19";
                            break;
                        case "경남":
                            replaceLocal = "20";
                            break;
                        case "제주":
                            replaceLocal = "21";
                            break;
                        case "대구":
                            replaceLocal = "22";
                            break;
                        case "인천":
                            replaceLocal = "23";
                            break;
                        case "광주":
                            replaceLocal = "24";
                            break;
                        case "대전":
                            replaceLocal = "25";
                            break;
                        case "울산":
                            replaceLocal = "26";
                            break;
                    }
                    //지역명을 숫자로 치환
                    licenseNumber = licenseNumber.replaceAll(licenseNumberLocal,replaceLocal);

                    //운전면허증 객체에 다시 활당
                    idRcgnResult.IDLicense = licenseNumber.getBytes("EUC-KR");
                }
            }
            //OCR 데이터 결과 원시 데이터를 List 로 변환
            //List index 별 삽입될 데이터가 고정되어 있음
            mResultText = new ImageRecognition(idRcgnResult, mEncryptKey).getResultRecognized();

            //OCR 실패 시
            //이 조건문을 탈 가능성은 현저히 낮음
            //외부 요건 (메모리 누수 등)으로 발생될 수 있음
            if (mResultText.get(ImageRecognition.EssentialFieldData.TYPE.ordinal()).equals(ImageRecognition.TYPE_RECOGNITION_FAIL)) {
                intent.putExtra(DATA_ALERT_MESSAGE, getString(R.string.error_ocr_fail));
                intent.putExtra("MSG_RESULT_FAIL", 10006);
                setResult(RETURN_OCR_FAIL, intent);
                finish();
            }

            //인식된 신분증 흑백 여부 0일때 흑백, 1일때 컬러
            intent.putExtra(DATA_COLOR_JUDGEMENT, idRcgnResult.isColor == 0);

            //주민등록번호 영역 - 사용자 편의를 위해 제공됨
            intent.putExtra(DATA_ID_NUMBER_RECT, new Rect(idRcgnResult.r_IDstr[0], idRcgnResult.r_IDstr[1], idRcgnResult.r_IDstr[2], idRcgnResult.r_IDstr[3]));

            //증명사진 영역 - 사용자 편의를 위해 제공됨
            intent.putExtra(DATA_USER_PHOTO_RECT, new Rect(idRcgnResult.r_Photo[0], idRcgnResult.r_Photo[1], idRcgnResult.r_Photo[2], idRcgnResult.r_Photo[3]));

            //운전 면허 등록 번호 영역 - 사용자 편의를 위해 제공됨
            intent.putExtra(DATA_DRV_NUMBER_RECT, idRcgnResult.IDtype == 2 ? new Rect(idRcgnResult.r_IDLicense[0], idRcgnResult.r_IDLicense[1], idRcgnResult.r_IDLicense[2], idRcgnResult.r_IDLicense[3]) : new Rect(0, 0, 0, 0));

            //정제된 OCR 결과 데이터 List 삽입
            intent.putStringArrayListExtra(DATA_RESULT_TEXT, mResultText);

            //OCR 결과, 원시 데이터 또한 삽입
            intent.putExtra(DATA_RESULT_STRUCT, idRcgnResult);
        } catch (Exception ex) {
            ex.printStackTrace();
            intent.putExtra(DATA_ALERT_MESSAGE, getString(R.string.error_ocr_fail));
            intent.putExtra("MSG_RESULT_FAIL", 10006);
            idRcgnResult = null;
            setResult(RETURN_OCR_FAIL, intent);
            finish();
        }
        idRcgnResult = null;
        setResult(RETURN_OK, intent);
        finish();
    }


    /***
     * initSound
     * 신분증 영역 접근시 소리 출력을 위한 객체 초기화
     */
    private void initSound() {
        soundPool = new SoundPool(5, 3, 0);
        soundBeepFound = soundPool.load(this.getApplicationContext(), R.raw.camera_beep_found, 1);
        soundBeepNotFind = soundPool.load(this.getApplicationContext(), R.raw.camera_beep_notfind, 1);
    }

    /***
     * 신분증 영역 접근시 소리 출력
     * @param mType 영역별 소리 지정
     */
    private void playSound(int mType) {
        //소리 출력 설정이 되어 있지 않았다면 실행하지 않음
        if (!isDetectSound)
            return;

        if (mType == 0 && !mNotFindFlag) {
            streamNotFind = soundPool.play(soundBeepNotFind, 1.0F, 1.0F, 0, -1, 1.0F);
            soundPool.stop(streamFound);
            mNotFindFlag = true;
            mFoundFlag = false;
            streams.add(streamNotFind);
        } else if (mType == 1 && !mFoundFlag) {
            streamFound = soundPool.play(soundBeepFound, 1.0F, 1.0F, 0, -1, 2.0F);
            soundPool.stop(streamNotFind);
            mFoundFlag = true;
            mNotFindFlag = false;
            streams.add(streamFound);
        } else if (mType == 2) {
            for (int i = 0; i < streams.size(); ++i)
                soundPool.stop(streams.get(i));
            streams.clear();
            mNotFindFlag = false;
            mFoundFlag = false;
        }

    }

    /***
     * 레이 아웃 조정
     * @param isInit 라이브러리 init 여부
     * camera window 도 신분증 비율에 맞추고 좀더 보이도록 height 를 조금 더 늘려준다.
     * shutter 버튼은 layout_below camera_window 로, 여기서 따로 위치 지정할 필요 없다.
     */
    private void adjustLayout(boolean isInit) {
        cameraWindow = findViewById(R.id.camera_window);
        if (isInit) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) cameraWindow.getLayoutParams();
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = dpToPixel(20);
            params.width = width;
            params.height = height;
            cameraWindow.setLayoutParams(params);
        }


        // camera_window 의 absolute coordinates 를 얻기 위해 ViewTreeObserver 를 사용한다.
        ViewTreeObserver observer = cameraWindow.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(mOnGlobalLayoutListener);

        isForgeryEnable = false;
        isAutoDetectMode = isAutoMode; //false;
        mShutter.setVisibility(isAutoMode ? View.GONE : View.VISIBLE);

        setToPreviewMode(true);
    }

    public ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            cameraWindow.getGlobalVisibleRect(cameraWindowPosition);
            cameraWindow.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }
    };

    /***
     * 일정 시간 동안 에러 메시지를 출력, 이후 다시 촬영화면으로 돌아가기 위한 기능
     * @param msg 에러메시지
     */
    public void showErrorRestart(String msg) {
        //에러 메시지 Dialog
        showWaitingDialog(msg);

        new Thread(() -> {
            try {
                //2초간 지연
                Thread.sleep(2000);

                //이벤트 핸들러에 작업 종료가 되지 않았음 알림
                mHandler.removeMessages(HANDLER_MSG_RECOGNIZING_WORK_END);

                //촬영횟수 가산
                //n번 촬영 후에도 신분증을 인식하지 못할 경우 라이브러리 종료
                shutterCount++;

                //프리뷰 재시작
                mCameraPreview.previewStart();

                //에러 메시지 출력간 background clear 해제
                isCanvasClear = false;

                //에러 Dialog 로 숨기기
                hideWaitingDialog();

                //신분증 인식 재시작
                setAutoDetection(true);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /***
     * 지정된 촬영 횟수를 초과했을때 실행
     * @param msg 지정된 촬영 횟수를 초과했을때 사용자에게 보여줄 메시지
     */
    public void reachLimitShootCount(String msg) {
        //지정된 촬영 횟수를 초과하였음을 사용자에 알림
        showWaitingDialog(msg);

        new Thread(() -> {
            try {
                //혹시 남아있을 결과 객체에 가비지 제거를 위해 null 로 초기화
                idRcgnResult = null;

                //2.5초간 에러 메시지 dialog 출력
                Thread.sleep(2500);

                //에러 메시지 dialog 닫기
                hideWaitingDialog();

                //지정된 촬영 횟수가 초과 되었음 알리고 라이브러리 종료
                setResult(RETURN_OVERTIME, new Intent());
                finish();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /***
     * 카메라 프리뷰 실행
     * @param isPreviewMode
     */
    public void setToPreviewMode(boolean isPreviewMode) {
        //카메라가 실행되어 있지 않을때
        if (isPreviewMode) {
            if (mCameraPreview != null && !mCameraPreview.previewing) {
                mCameraPreview.previewStart();
            }
            isCanvasClear = false;

            //자동 영역 감지 및 촬영 시작
            //자동 촬영으로 설정되어 있을때만 실행됨
            setAutoDetection(true);
        } else {
            //카메라가 실행 중일때
            if (mCameraPreview != null && !mCameraPreview.previewing) {
                //프리뷰 중단
                mCameraPreview.previewStop();
            }
            isCanvasClear = true;
            viewDraw.invalidate();
        }
        noForgery = false;

        //moreLighting = false;
        startedNeedLightLoop = false;
    }

    /***
     * 프리뷰 Init
     */
    private void createPreview() {
        if (mCameraPreview != null) return;
        if (surfaceView == null)
            surfaceView = findViewById(R.id.preview_view);
        mCameraPreview = new CameraPreview(OCRCameraActivity.this, surfaceView);
        mCameraPreview.delegate = mCameraPreviewDelegate;
    }


    //////////////////////////////////////////////
    // recognize functions
    //////////////////////////////////////////////
    // nib 02/24/2019 thread로 변경..

    private class RecognizingThread implements Runnable {
        private final Bitmap mBitmap;
        private int[] imgRGBA = null;
        private int width;
        private int height;

        public RecognizingThread(Bitmap bitmap) {
            this.mBitmap = bitmap;
        }

        @Override
        public void run() {
            recognizeImage(mBitmap);

            if (imgRGBA == null) {
                Message recogMsg = Message.obtain(mHandler);
                recogMsg.what = HANDLER_MSG_RECOGNIZING_WORK_END;
                recogMsg.arg1 = -1;
                mHandler.sendMessageDelayed(recogMsg, 200);
                return;
            }
            idRcgnResult = new IDCardRcgn.IDs_Data();

            // 결과 이미지 마스킹 옵션 :
            //     0,1=안함. 2=면허번호/주민번호 전체. 4=주민번호 전체. 8=주민번호 뒷자리. 16=주민번호 뒷자리 성별제외.
            //     256=면허번호 전체. 512=면허번호 가운데 6자리. 1024=면허번호 가운데 6자리 중 뒤 5자리.
            //     65536=발급일자 전체.
            //			idRcgnResult.MaskOpt = 8 + 1024 + 65536;

            idRcgnResult.MaskOpt = mMaskOption < 2 || !isMasking ? 1 : mMaskOption;
            idRcgnResult.ColorCheckOpt = 1;
            idRcgnResult.DrvKindOpt = 1;

            // nib 02/26/2019 thread 간의 timing 조정을 위해 delay를 준다.
            Message msg = Message.obtain(mHandler);
            msg.what = HANDLER_MSG_RECOGNIZING_WORK_END;
            msg.arg1 = IDCardRcgn.idrcgnImageProc(imgRGBA, width, height, idRcgnResult);
            mHandler.sendMessageDelayed(msg, 200);
        }

        private void recognizeImage(Bitmap bitmap) {
            if (bitmap == null)
                return;
            width = bitmap.getWidth();
            height = bitmap.getHeight();
            imgRGBA = new int[width * height];
            bitmap.getPixels(imgRGBA, 0, width, 0, 0, width, height);
        }
    }

    private void setAutoDetection(boolean start) {
        if (mCameraPreview == null) createPreview();
        if (start) {
            mCameraPreview.startBurstPhotos();
            //moreLighting = false;
            startedNeedLightLoop = false;
        } else {
            mCameraPreview.stopBurstPhotos();
        }
        progressDialog(null, false, getString(R.string.error_ocr_fail_add_retry));
    }

    // nib 02/08/2019 added progress dialog


    private void showWaitingDialog(String msg) {
        if (waitingDialog == null) {
            waitingDialog = new ProgressDialog(this);
            waitingDialog.setMessage(msg);

            waitingDialog.setIndeterminate(true);
            waitingDialog.setCancelable(false);
        } else if (waitingDialog.isShowing()) {
            waitingDialog.hide();
        }
        waitingDialog.show();
    }

    // hide()제거. dismiss()에서 hide도 함.
    private void hideWaitingDialog() {
        if (waitingDialog != null) {
            if (waitingDialog.isShowing()) {
                waitingDialog.dismiss();
            }
        }
        waitingDialog = null;
    }


    /////////////////////////////////////////////////////////////////////
    // check forgery and auto capture
    /////////////////////////////////////////////////////////////////////

    public int cornerPtNum = 0;
    private int[] cornerPt = new int[8];

    public int ippPreviewDet = 0;
    public int[] rCrop = {0, 0, 0, 0};

    public int cameraWidth = 0;
    public int cameraHeight = 0;
    public Point screenSize = new Point(1600, 1200);
    private int screenDirection = 90;

    public int idCheckStatus = 0;
    public int darkMode = 0;
    public int setBase = 0;
    public int saddleNum = 0;
    public int unvalidChk = 0;
    public long tfStatus = 0;
    private boolean startedNeedLightLoop = false;

    private void setTFStatus(int[] rCrop, int[] corner) {

        if (tfStatus == 100) {
            idCheckStatus += 100; //check OK
            if (idCheckStatus > 1000) idCheckStatus = 1000;
        }

        if (tfStatus > 0 && ippPreviewDet > 0) { // DisplayCornerMark
            int maxGap;
            int ht;
            ht = rCrop[3] - rCrop[1];

            int thrHeight = (int) (ht / 1.5f);
            unvalidChk = 0;
            if (setBase == 0) {
                maxGap = ScreenController.getMaxGap(rCrop, corner);
                if (maxGap < thrHeight) {
                    setBase = 1;
                }
            } else if (setBase == 1) {
                setBase = 2;
            }
        } else if (tfStatus <= 0 && idCheckStatus < 200) {
            unvalidChk++;
            if (unvalidChk >= 3) {
                idCheckStatus = 0;
                setBase = 0;
            }
        }
    }


    // nib 11/03/2018 CameraPreviewDelegate 구현 함수
    CameraPreview.CameraPreviewDelegate mCameraPreviewDelegate = new CameraPreview.CameraPreviewDelegate() {
        @Override
        public void takePictureDone(Bitmap mBitmap) {
            playSound(2);
            if (mBitmap == null) return;
            if (mHandler != null) {
                Message recordMsg = Message.obtain(mHandler);
                recordMsg.what = HANDLER_MSG_RECOGNIZING_WORK_START;
                recordMsg.obj = mBitmap;
                mHandler.sendMessage(recordMsg);
            }
        }

        @Override
        public boolean previewProcInThread(byte[] yuv420sp, int width, int height, int previewAngle, boolean isFinal) {
            playSound(2);
            playSound(0);
            int iret = 0;
            if (width < height || yuv420sp == null)
                return false;

            cameraWidth = width;
            cameraHeight = height;

            focusValue = IDCardRcgn.GetFocusValueYuv420(yuv420sp, width, height, width / 2, height / 2, 0);

            if (focusValue >= 1000) {
                int sizeRat = 2;  //
                int[] corner = new int[8];

                if (rCrop[0] > 0) {
                    if (isForgeryEnable && idCheckStatus < 200) {
                        saddleNum = 0;
                        iret = IDCardRcgn.PreviewIDChkPort(yuv420sp, width, height, screenDirection, sizeRat, rCrop, corner, darkMode, setBase, 0);
                        tfStatus = iret;
                        setTFStatus(rCrop, corner);
                        if (setBase > 0 && !startedNeedLightLoop) {
                            if (mHandler != null) {
                                startedNeedLightLoop = true;
                                // moreLighting = false;
                            }
                        } else if (ippPreviewDet < 4) {
                            //moreLighting = false;
                            startedNeedLightLoop = false;
                        }
                    } else {
                        iret = IDCardRcgn.PreviewIDChkPort(yuv420sp, width, height, screenDirection, sizeRat, rCrop, corner, darkMode, setBase, 0);
                        if (iret > 0 && ippPreviewDet > 0) { // DisplayCornerMark
                            int maxgap, ht;

                            ht = rCrop[3] - rCrop[1];

                            int thr_ht = ht / 4;

                            maxgap = ScreenController.getMaxGap(rCrop, corner);
                            if (maxgap < thr_ht) {
                                saddleNum++;
                                if (saddleNum == 3) {
                                    /*if (chk_AutoDetect.isChecked()) {*/
                                    if (isAutoDetectMode) {
                                        saddleNum = 0;
                                        ippPreviewDet = 0;
                                        cornerPtNum = 0;
                                        idCheckStatus = 0;
                                        setBase = 0;
                                        mHandler.sendEmptyMessageDelayed(HANDLER_MSG_AUTODETECT, 200);

                                        return true;
                                    }
                                }
                            } else saddleNum = 0;
                        } else {
                            saddleNum = 0;
                            if (iret == 0 && idCheckStatus >= 200 && isForgeryEnable && isAutoDetectMode) {
                                idCheckStatus = 0;
                                setBase = 0;
                            }
                        }
                    }
                }

                if (iret > 0 && corner[0] > 0) {
                    ippPreviewDet++;
                    cornerPtNum = 4;
                    System.arraycopy(corner, 0, cornerPt, 0, 8);
                } else if (iret == 0) {
                    if (ippPreviewDet > 3) ippPreviewDet = 3;
                    else ippPreviewDet = 0;
                    cornerPtNum = 0;
                }
            } else {
                ippPreviewDet = 0;
                cornerPtNum = 0;
                unvalidChk++;
                if ((unvalidChk >= 3 && idCheckStatus < 200) || (isForgeryEnable && isAutoDetectMode)) {
                    idCheckStatus = 0;
                    setBase = 0;
                }
            }

            if (isAutoDetectMode || isForgeryEnable) {
                // redraw
                isCanvasClear = true;
                runOnUiThread(() -> viewDraw.invalidate());
            } else {
                ippPreviewDet = cornerPtNum = 0;
            }

            return false;
        }

    };

    public int dpToPixel(float dp) {
        float dpiScale = getResources().getDisplayMetrics().density;
        return (int) (dp * dpiScale);
    }

    private void getAppScreenInfo() {
        //실행할때 한번만 실행됨
        screenSize.x = getResources().getDisplayMetrics().widthPixels;
        screenSize.y = getResources().getDisplayMetrics().heightPixels;
        screenDirection = ScreenController.getScreenDirection((WindowManager) getSystemService(Context.WINDOW_SERVICE));
    }


    public boolean noForgery = false;


    public void displayIpp(final Canvas canvas, final Paint paint) {
        runOnUiThread(() -> {
            if (cameraWidth == 0 || cameraHeight == 0)
                return;

            int baseWidth;
            int baseHeight;
            Paint outerLinePaint;

            float xRat;
            float yRat;
            int lineWhite = 0xFFFFFFFF;
            int textSize = screen.dpToPixel(12);    // 16
            int posLevel = screen.dpToPixel(18);    // 18
            Rect r = new Rect(0, 0, 0, 0);
            Rect r2 = new Rect(0, 0, 0, 0);
            Rect r3 = new Rect(0, 0, 0, 0);
            int screenWidth = screenSize.x;
            int screenHeight = screenSize.y;


            //
            // NOTE :
            //	galaxy note 10의 경우, surface view size 와 screen size 의 비율이 맞지 않으므로,
            //	(CameraPreview initCameraParameter 함수 참조)
            //	screen size 보다는 surface view size 로 계산을 해야 한다.
            //
//            if (false) {
//                baseWidth = screenWidth;
//                baseHeight = screenHeight;
//            } else {
                if (mCameraPreview == null) return;
                baseWidth = mCameraPreview.surfaceSize.x;
                baseHeight = mCameraPreview.surfaceSize.y;
                if (baseWidth == 0 || baseHeight == 0) return;
            //}
            int width = cameraWidth;
            int height = cameraHeight;

            if ((baseWidth > baseHeight && cameraWidth < cameraHeight) || (baseWidth < baseHeight && cameraWidth > cameraHeight)) {
                width = cameraHeight;
                height = cameraWidth;
            }

            xRat = (float) baseWidth / (float) (width);
            yRat = (float) baseHeight / (float) (height);

            int statusBarHeight = 0;
            float adjSBarRat = (float) baseHeight / (float) (baseHeight - statusBarHeight);
            yRat = yRat / adjSBarRat;

            //width = cameraWindowPosition.width();
            height = cameraWindowPosition.height();

            int margin_hor = dpToPixel(4);    // 4dp
            rCrop[0] = cameraWindowPosition.left + margin_hor;
            rCrop[2] = cameraWindowPosition.right - margin_hor;
            int heightSize = (rCrop[2] - rCrop[0]) * 54 / 86;
            //int heightSize = (rCrop[2] - rCrop[0]) * 54 / 172;

            rCrop[1] = cameraWindowPosition.top + (height - heightSize) / 2;
            rCrop[3] = rCrop[1] + heightSize;

            // mapping screen coordinate to preview's
            rCrop[0] = (int) ((float) rCrop[0] / xRat);
            rCrop[1] = (int) ((float) rCrop[1] / yRat);
            rCrop[2] = (int) ((float) rCrop[2] / xRat);
            rCrop[3] = (int) ((float) rCrop[3] / yRat);



            //r 뷰파인더 영역
            r.left = (int) ((float) rCrop[0] * xRat);
            r.top = (int) ((float) rCrop[1] * yRat);
            r.right = (int) ((float) rCrop[2] * xRat);
            r.bottom = (int) ((float) rCrop[3] * yRat);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);

            /*
             * fullscreen 추가
             */
            boolean isFullScreen = getIntent().getBooleanExtra(REMOVE_BACKGROUND_ALPHA, false);
            paint.setAlpha(isFullScreen ? 0 : 100);


            //r2 외곽선 밖에 부분
            r2.left = 0;
            r2.top = 0;
            r2.right = baseWidth;
            r2.bottom = r.top;
            canvas.drawRect(r2, paint);

            r2.left = 0;
            r2.top = r.bottom;
            r2.right = baseWidth;
            r2.bottom = baseHeight;

            canvas.drawRect(r2, paint);
            r2.left = 0;
            r2.top = r.top;
            r2.right = r.left;
            r2.bottom = r.bottom;
            canvas.drawRect(r2, paint);

            r2.left = r.right + 2;
            r2.top = r.top;
            r2.right = baseWidth;
            r2.bottom = r.bottom;
            canvas.drawRect(r2, paint);


            // outer rect.
            outerLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            outerLinePaint.setColor(lineWhite);
            outerLinePaint.setAlpha(250);
            outerLinePaint.setStrokeWidth(8);
            outerLinePaint.setStyle(Paint.Style.STROKE);
            outerLinePaint.setStrokeJoin(Paint.Join.ROUND);
            outerLinePaint.setStrokeCap(Paint.Cap.ROUND);
            outerLinePaint.setPathEffect(new CornerPathEffect(10));
            outerLinePaint.setAntiAlias(true);

            // remove outer rect. box
            //canvas.drawRect(r, paint2);

            int[] innerCrop = {0, 0, 0, 0};
            int innerHeight = r.bottom - r.top;
            int innerWidth = r.right - r.left;

            innerCrop[1] = innerHeight / 20;    // outer rect 보다 안쪽으로 높이의 1/20
            innerCrop[3] = innerHeight - innerCrop[1];

            int innerWidthLength = (innerCrop[3] - innerCrop[1]) * 86 / 54;
            innerCrop[0] = (innerWidth - innerWidthLength) / 2;
            innerCrop[2] = innerWidth - innerCrop[0];

            //r3 inner Margin
            r3.left = r.left + innerCrop[0]; // (int) ((float) innerCrop[0] * xRat);
            r3.top = r.top + innerCrop[1]; //(int) ((float) innerCrop[1] * yRat);
            r3.right = r.right - innerCrop[0]; //(int) ((float) innerCrop[0] * xRat);
            r3.bottom = r.bottom - innerCrop[1]; //(int) ((float) innerCrop[1] * yRat);

            outerLinePaint.setColor(lineWhite);
            canvas.drawRect(r3, outerLinePaint);
            paint.setColor(lineWhite);

            String guideText = isAutoMode ? getIntent().getStringExtra(DATA_TITLE_MESSAGE_AUTO) : getIntent().getStringExtra(DATA_TITLE_MESSAGE_MANUAL);

            String innerGuideText = getIntent().getStringExtra(DATA_TITLE_MESSAGE_GUIDELINE);

            if (innerGuideText == null)
                innerGuideText = getString(R.string.guide_fit_idcard);
            String level = guideText != null ? guideText : getString(R.string.guide_fit_idcard).concat(getString(R.string.guide_fit_idcard_add_increase_recognize));
            // 화면 중앙에 directives 를 띄운다.
            if (ippPreviewDet < 4 && idCheckStatus < 200) {
                paint.setTextSize(dpToPixel(14));    // 18
                ScreenController.setTypeface(paint, 1);    // normal
                paint.setStyle(Paint.Style.FILL);
//                if (isForgeryEnable) {
//                    canvas.drawText("신분증을 가이드 안으로 맞춘 후,", r3.left + screen.dpToPixel(10), (int) ((float) (r3.top + r3.bottom) / 2 - screen.dpToPixel(16)), paint);
//                    canvas.drawText("흰종이나 어두운 배경에서 홀로그램이 나타나도록", r3.left + screen.dpToPixel(10), (int) ((float) (r3.top + r3.bottom) / 2 + screen.dpToPixel(6)), paint);
//                    canvas.drawText("신분증을 앞뒤로 천천히 기울여 주세요.", r3.left + screen.dpToPixel(10), (int) ((float) (r3.top + r3.bottom) / 2 + screen.dpToPixel(28)), paint);
//                } else {
                if (focusValue < 1000)
                    innerGuideText = innerGuideText.concat("\n").concat(getString(R.string.guide_focus_on_screen_center));

                else {
                    mCameraPreview.setModeContinue();
                    isCanvasClear = true;
                }

                int x = r3.left + screen.dpToPixel(10);
                int y = (r3.top + r3.bottom) / 2;
                for (String line : innerGuideText.split("\n")) {
                    canvas.drawText(line, x, y, paint);
                    y += paint.descent() - paint.ascent();
                }
                //}
            }

            //홀로그램 체크 해당 부분은 추후 도입

//        if (isForgeryEnable) {
//            level = "신분증 진위여부를 확인중입니다.";
//            if (idCheckStatus >= 200) {
//                level = "정품(100%) 입니다. (재 인증하려면 화면을 터치해 주세요.)";
//
//                moreLighting = false;
//                startedNeedLightLoop = false;
//
//                if (!isAutoDetectMode) {
//                    paint.setTextSize(screen.dpToPixel(24));    // 48
//                    ScreenController.setTypeface(paint, 2);    // bold
//                    paint.setStyle(Paint.Style.FILL);
//                    paint.setColor(Color.YELLOW);
//                    paint.setAlpha(128);
//                    canvas.drawText("인증완료", (int) ((float) (r3.left + r3.right) / 2 - screen.dpToPixel(48)), (int) ((float) (r3.top + r3.bottom) / 2 + 30), paint);
//                } else {
//                    paint.setTextSize(textSize);
//                    ScreenController.setTypeface(paint, 2);    // bold
//                    paint.setStyle(Paint.Style.FILL);
//                    paint.setColor(Color.GREEN);
//                    canvas.drawText("정상신분증 인증되었습니다. 화면을 터치하거나 촬영버튼을 눌러주세요.", r3.left, r3.bottom + screen.dpToPixel(14), paint);
//                    paint.setColor(Color.YELLOW);
//                    paint.setTextSize(screen.dpToPixel(14)); // 18
//                    canvas.drawText("빛 반사 없이 촬영해 주세요.", r3.left, r3.top - screen.dpToPixel(10), paint);
//                    // 가이드 박스 내 문구..
//                    paint.setTextSize(screen.dpToPixel(20));    // 40
//                    ScreenController.setTypeface(paint, 2);    // bold
//                    paint.setStyle(Paint.Style.FILL);
//                    paint.setColor(Color.YELLOW);
//                    paint.setAlpha(128);
//                    canvas.drawText("인증완료", (int) ((float) (r3.left + r3.right) / 2 - screen.dpToPixel(40)), (int) ((float) (r3.top + r3.bottom) / 2 - screen.dpToPixel(11)), paint);
//                    paint.setTextSize(screen.dpToPixel(12));    // 20
//                    paint.setColor(Color.WHITE);
//                    ScreenController.setTypeface(paint, 2);    // bold
//                    paint.setAlpha(128);
//                    if (!noForgery) {
//                        noForgery = true;
//                        ippPreviewDet = 0;
//                    }
//
//                    setAutoDetection(true);
//
//                    return;
//                }
//            } else if (ippPreviewDet >= 4) {
//                level = "진위를 검사중입니다. 신분증을 앞뒤로 천천히 기울여 주세요.";
//            } else {
//                noForgery = false;
//            }
//        } else {
//            noForgery = false;
//        }
//
//        if (moreLighting) {
//            paint.setTextSize(screen.dpToPixel(12)); // 18
//            ScreenController.setTypeface(paint, 2);    // bold
//            paint.setStyle(Paint.Style.FILL);
//            paint.setColor(Color.YELLOW);
//            canvas.drawText("홀로그램이 나타나게 해주세요.", r3.left + screen.dpToPixel(10), r3.top + screen.dpToPixel(14), paint);
//        }
            paint.setTextSize(textSize);
            paint.setColor(Color.WHITE);
            ScreenController.setTypeface(paint, 1);    // normal
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText(level, r3.left, r3.top - posLevel, paint);
        });
    }
}



