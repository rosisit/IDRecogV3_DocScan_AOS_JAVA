package com.cardcam.jni;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.Serializable;

public class IDCardRcgn {
	// Used to load library on application startup.
	static {
		try {
			Log.d("com.cardcam", " idrcgn start");
			System.loadLibrary("idrcgn");
			//System.loadLibrary("LPRPro");  //LPRCar(일반버젼),LPRPro(프로버젼)*/
		}
		catch (UnsatisfiedLinkError e) {
			e.printStackTrace();
			Log.e("com.cardcam", "Error : failed libidrcgn.so load!", e);
		}
	}

	/**
	 * A native method that is implemented by the native library,
	 * which is packaged with this application.
	 */



	/*
	 * 인식 엔진 초기화.
	 * 인식 전 먼저 호출해줘야 엔진이 정상동작 한다.
	 *
	 */
	public final static native int initRcgn(Activity act);


	/*
	 *  Set External File Storage Directory
	 *  호출 전, 디렉토리는 생성되어 있어야 한다.
	 */
	public final  static native void SetExternalStorage(String path);


	/*
	 * 인식 결과 회신 class
	 */
	public static class IDs_Data implements Serializable {
		public int IDtype;				// 1=주민등록증, 2=운전면허증, 3=외국인등록증(옵션)
		public int MaskOpt;				// 결과 이미지 마스킹 옵션 :
		//     0,1=안함. 2=면허번호/주민번호 전체. 4=주민번호 전체. 8=주민번호 뒷자리. 16=주민번호 뒷자리 성별제외.
		//     256=면허번호 전체. 512=면허번호 가운데 6자리. 1024=면허번호 가운데 6자리 중 뒤 5자리.
		//     65536=발급일자 전체.
		public int DrvKindOpt;			// 1 =면허종별코드 인식, else=처리안함
		public byte[] IDstr;			// 주민등록번호
		public byte[] IDLicense;		// 운전면허번호
		public byte[] IDname;			// 이름
		public byte[] IDdate;			// 발급일
		public byte[] IDorgan;			// 발급기관
		public byte[] IDaddress;        // 주소
		public byte[] chkstr;			// 운전면허 checksum (면허증 작은사진 아래의 스트링)
		public byte[] IDcountry;        // 외국인등록증 국적
		public int DrvKindNum;			// 인식된 면허종별코드 개수. 즉, DrvKinds[]의 size
		public byte[] DrvKinds;			// 면허종별코드 (max= 9):
		//     11=1종 대형, 12=1종 보통, 13= 1종 소형,
		//     14=대형견인차(트레일러),  15=구난차(레커),  16=소형견인차,
		//     32=2종 보통,  33=2종 소형,  38=2종 원자
		public byte[] sDrvKinds;      // 종별구분을 리턴하기위한 구조체

		public int resWidth;			// 인식 결과 이미지의 width (만일, 결과 이미지를 못 만들었을 경우, 0 리턴)
		public int resHeight;			// 인식 결과 이미지의 height
		public int[] resImage;			// 인식 결과 이미지

		public int[] r_IDstr;			// 주민등록번호 좌표 [left, top, right, bottom]
		public int[] r_IDLicense;		// 운전면허 좌표
		public int[] r_IDname;			// 이름 좌표
		public int[] r_IDdate;			// 발급일 좌표
		public int[] r_Organ;			// 발급기관 좌표
		public int[] r_Address;			// 주소 좌표
		public int[] r_chkstr;			// checksum 좌표
		public int[] r_IDcountry;       // 외국인등록증 국적 좌표

		public int[] r_IDarea;			// ID영역 좌표
		public int[] r_Photo;			// Photo 좌표

		public int ColorCheckOpt;		// 컬러 혹은 흑백이미지 구분 요청여부 (1=구분요청, else=처리안함)
		public int isColor;				// 컬러 혹은 흑백 이미지 구분. 0=흑백, 1=컬러

		public long elapsed;			// 수행시간 (참고용)

		// 모든 array size는 엔진의 IDs_Data와 동일함.
		public IDs_Data() {
			this.IDtype = 0;
			this.MaskOpt = 0;
			this.DrvKindOpt = 0;

			this.IDstr = new byte[20];
			this.IDLicense = new byte[20];
			this.IDname = new byte[40];
			this.IDdate = new byte[20];
			this.IDorgan = new byte[40];
			this.IDaddress = new byte[120];
			this.chkstr = new byte[20];
			this.IDcountry = new byte[40];

			this.DrvKindNum = 0;
			this.DrvKinds = new byte[20];

			// 결과 이미지는 항상 1200 X 760 이고, color depth는 32bit 이다.
			this.resWidth = 1200;
			this.resHeight = 760;
			this.resImage = new int[this.resWidth * this.resHeight];

			// 좌표 array 순서 : left, top, right, bottom
			this.r_IDstr = new int[4];
			this.r_IDLicense = new int[4];
			this.r_IDname = new int[4];
			this.r_IDdate = new int[4];
			this.r_Organ = new int[4];
			this.r_Address = new int[4];
			this.r_chkstr = new int[4];
			this.r_IDcountry = new int[4];

			this.r_IDarea = new int[4];
			this.r_Photo = new int[4];

			this.ColorCheckOpt = 0;
			this.isColor = 0;

			this.elapsed = 0;
		}
	}

	/*
	 *  Input
	 *   imgRGBA	: 이미지 raw 데이타. 32bit depth.
	 *   width	: 이미지 width
	 *   height	: 이미지 height
	 *   idinf	: 결과를 리턴받을 구조체
	 *
	 *  Output
	 *   idinf	: 인식 결과.

	 *  return	: 0=인식실패, 1=주민등록증, 2=운전면허증
	 *
	 */
	public final static native int idrcgnImageProc(int[]imgRGBA, int width, int height, IDs_Data idinf);

	/*
	 * Input : 없음
	 * Output : 없음
	 * Return :
	 *    0 - 이미지 인식 대기중.
	 *    1 - 이미지 인식 진행중.
	 */
	public final static native int isBusyidrcgn();

	/*
	 * 인식 모듈 버전 확인
	 *
	 * return : 버전 스트링
	 *          if demo, "2.0.1.13 Trial (2021/01/01~2021/12/31)"
	 *          if commercial, "2.0.1.13"
	 */
	public final static native String GetDescription();

	/* calculates the focus value for the region of  the center (mx, my)
	 *  input : yuvdata : yuv420sp data from camera preview
	 *  		mx,my : Center coordinates, mx = width/2, my = height/2
	 *
	 *  return : focus value
	 *
	 */
	public final static native int GetFocusValueYuv420(byte[] yuvdata, int width, int height, int mx, int my, int mode);

	/*
	Input
	  yuvdata : yuv420sp data from camera preview
	  width : preview image's width
	  height: preview image's height
	  ScreenDir : preview direction.
	  SizeRat : default=2 (Not Used , Reserved Value )
	  rCrop[] : crop area (RECT) left-up [0,1] right-down [2,3]
	  corner[] : array to get detected ID card area. array size 8.
	  photoMode: 0= Normal(default), 1=사진부착 판별
	  setBase: 0 = get Corner Points
			   1 = set basic ID card image to check and compare next ID-image
			   2 = check ID card whether forgery or not.
	  debugmode(internal use): 0=no debug(default), 1=debug mode
	Output
	  corner[] : corner Point [0,1]left-up [2,3]right-up [4,5]right-dn [6,7]left-dn

	  return : if setbase == 0, 0 =no corner, 1 =corner detect,
			   if setbase == 1, 2=set base ID (=success), else fail (0 or -1= Invalid ID (much different ID)).
			   if setbase == 2, check ID card forgery.
			                    0 or -1 = Invalid
			                    8 = not decide yet. keep on checking next image.
								100= real ID card
	Legend : ID = ID card image
	*/
	public final  static native int PreviewIDChk(byte[] yuvdata, int width, int height, int ScreenDir, int SizeRat, int rCrop[], int corner[], int photoMode, int setBase, int debugmode);

	// portrait mode에서, PreviewIDChkPort( )를 사용한다.
	public final  static native int PreviewIDChkPort(byte[] yuvdata, int width, int height, int ScreenDir, int SizeRat, int rCrop[], int corner[], int photoMode, int setBase, int debugmode);

	/*
	정품 인증 완료 후, 다른 신분증으로 인식했는지 검증.
	신분증 인식 함수 idrcgnImageProc()를 실행 후 호출.
	Input : none
	return : 0=정품 확인한 신분증 아님!
	         100=정품 확인한 신분증이 맞다.
	 */
	public final static native int IsSameTFCD(int photoMode);


	/*
	컬러 이미지와 흑백 이미지 구분 함수
	Input : Bitmap class 이미지
	return : 0=흑백 이미지
	         1=컬러 이미지
	 */
	public final static native int CheckColorBitmap(Bitmap bitmap);
}
