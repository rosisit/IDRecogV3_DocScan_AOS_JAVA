package com.rosisit.idcardcapture.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import com.rosisit.idcardcapture.OCRCameraActivity;
import com.rosisit.idcardcapture.utils.Definitions;
import com.rosisit.idcardcapture.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;


/***
 * 갤럭시 S 시리즈가 Camera2, CameraX 를 지원하지 않기 때문에 Legacy 인 Camera 를 써야함
 * Camera API 는 LollyPop 에서 Deprecated
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {
	public Camera mCamera;
	private final Point mCaptureSize = new Point(1600, 1200);
	private final Point mPreviewSize = new Point(0, 0);
	private final boolean forceAutoFocus = false;

	private Context mContext;
	private SurfaceView mSurfaceView;
	private SurfaceHolder mSurfaceHolder;
	private OCRCameraActivity parent;
	public boolean previewing = false;
	public Point surfaceSize = new Point(0, 0);
	private float mDist = 0.0f;

	private int previewAngle;
	private int maxNumFocusAreas;
	private int maxNumMeteringAreas;


	private BurstPhotosThread mBurstPhotosThread;


	public CameraPreview(Context context){
		super(context);
	}

	@SuppressLint("ClickableViewAccessibility")
	public CameraPreview(Context context, SurfaceView surfaceView) {
		super(context);
		mContext = context;
		mSurfaceView = surfaceView;
		parent = (OCRCameraActivity) context;
		mSurfaceView.getHolder().addCallback(CameraPreview.this);
		mSurfaceView.setOnTouchListener(this);
		mSurfaceView.setClickable(true);
		mSurfaceView.setFocusable(true);
	}

	public CameraPreviewDelegate delegate = null;

	public interface CameraPreviewDelegate {

		void takePictureDone(Bitmap mBitmap);
		boolean previewProcInThread(byte[] data, int width, int height, int previewAngle, boolean isFinal); // nib 12/27/2019 auto detect
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mSurfaceHolder = holder;
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		cameraStart();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (mCamera == null) return;
		surfaceSize.x = width;
		surfaceSize.y = height;
		previewStop();
		initCameraParameters();
		requestLayout();
		if (mSurfaceHolder != null) {
			try {
				mCamera.setPreviewDisplay(mSurfaceHolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		previewStart();
		if (mBurstPhotosThread != null) {
			resumeBurstPhotos();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		cameraStop();
		mSurfaceHolder = null;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction() & MotionEvent.ACTION_MASK;
		int pointCnt = event.getPointerCount();
		boolean resultFlag = true;
		if (mCamera != null && previewing) {
			switch (pointCnt) {
				case 1:
					if (action == MotionEvent.ACTION_DOWN) {
						if (parent.idCheckStatus >= 200) {
							parent.saddleNum = 0;
							parent.ippPreviewDet = 0;
							parent.cornerPtNum = 0;
							parent.idCheckStatus = 0;
							parent.setBase = 0;
							takePicture();
						} else {
							mCamera.cancelAutoFocus();
							if (setFocusArea(event.getX(), event.getY())) {
								mCamera.autoFocus(null);
							}
						}
						parent.idCheckStatus = 0;
						resultFlag = true;
					}
					break;
				case 2:
					Camera.Parameters params = mCamera.getParameters();
					if (action == MotionEvent.ACTION_POINTER_DOWN) {
						mDist = getFingerSpacing(event);
					} else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
						handleZoom(event, params);
					}
					break;
				default:
					resultFlag = false;
			}
		}
		return resultFlag;
	}

	private void handleZoom(MotionEvent event, Camera.Parameters params) {
		int maxZoom = params.getMaxZoom();
		int zoom = params.getZoom();
		float newDist = getFingerSpacing(event);
		if (newDist > mDist) {
			//zoom in
			if (zoom < maxZoom)
				zoom++;
		} else if (newDist < mDist) {
			//zoom out
			if (zoom > 0)
				zoom--;
		}
		mDist = newDist;
		params.setZoom(zoom);
		mCamera.setParameters(params);
	}

	private float getFingerSpacing(MotionEvent event) {
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	// 화면 rotation, 화면 사이즈 변경시 호출...
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
		int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
		setMeasuredDimension(width, height);
	}

	public boolean cameraStart() {
		try {
			openCamera();
			setKeepScreenOn(true);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public void cameraStop() {
		setKeepScreenOn(false);
		closeCamera();
	}
	public void previewStart() {
		if (mCamera != null) {
			initFocusing();
			mCamera.startPreview();
			previewing = true;
		}
	}

	public void previewStop() {
		if (mCamera != null) {
			mCamera.stopPreview();
			previewing = false;
		}
	}

	public void takePicture() {
		try {
			mCamera.takePicture(shutterCallback, null, jpegCallback);
		} catch (Exception e) {
			Log.e("Exception", e.getMessage());
		}
	}

	private Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			int angle = Utils.seekCameraDisplayAngle((Activity) mContext, getCameraId(), mCamera);
			Bitmap bitmap = Utils.rotateBitmapData(data, angle);

			try {
//				if (!parent._LANDSCAPE) {
				if (parent.cameraWindowPosition.bottom > 0) {
					int bw = bitmap.getWidth();
					int bh = bitmap.getHeight();
					Rect rt = new Rect(parent.cameraWindowPosition);
					float xRat = (float) bw / (float) surfaceSize.x;
					float yRat = (float) bh / (float) surfaceSize.y;

					Bitmap cropped = Bitmap.createBitmap(bitmap, (int) (rt.left * xRat), (int) (rt.top * yRat), (int) (rt.width() * xRat), (int) (rt.height() * yRat));
					bitmap.recycle();
					bitmap = cropped;
				}
				//}
			} catch (Exception e) {
				Log.e("exception", e.getMessage());
			} finally {
				if (delegate != null) {
					delegate.takePictureDone(bitmap);
				}
			}

		}
	};

	// shutter sound
	// https://stackoverflow.com/questions/10891742/android-takepicture-not-making-sound
	private final Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
		@Override
		public void onShutter() {
			Utils.takePhotoSound(mContext, false);
		}
	};
	// nib 07/05/2018 PreviewCallback 은 언제 어느때라도 set/remove 할 수 있다.
	//                (구글 camera class setPreviewCallback 참조)
	public final void startBurstPhotos() {
		if (mBurstPhotosThread != null)
			stopBurstPhotos();
		mBurstPhotosThread = new BurstPhotosThread();
		mBurstPhotosThread.start();
		resumeBurstPhotos();
	}

	public final void stopBurstPhotos() {
		if (mCamera != null)
			mCamera.setOneShotPreviewCallback(null);

		if (mBurstPhotosThread != null) {
			mBurstPhotosThread.stopThread();
			// wait until stop running.
			try {
				mBurstPhotosThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			mBurstPhotosThread = null;
		}
	}

	private void resumeBurstPhotos() {
		if (mCamera != null)
			mCamera.setOneShotPreviewCallback(mOneShotPreviewCallback);

	}

	private Camera.PreviewCallback mOneShotPreviewCallback = (data, camera) -> {
		// data 는...
		// Camera.Parameters.setPreviewFormat(int)으로 preview format 을 지정하지 않았다면,
		// 디폴트는 YCbCr_420_SP (NV21) format 이다.
		// 현재, NV21 format...
		//
		try {
			Size size = camera.getParameters().getPreviewSize();
			if (mBurstPhotosThread != null)
				mBurstPhotosThread.addPhoto(data, size.width, size.height);
		} catch (RuntimeException e) {
			if (mCamera == null) {
				cameraStart();
				previewStop();
				initCameraParameters();
				previewStart();
			}
		}
	};

	private class BurstPhotosThread extends Thread {
		ArrayBlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(1);
		int width, height;

		public void stopThread() {
			addPhoto(new byte[]{0}, -1, -1);
		}

		public void addPhoto(byte[] data, int width, int height) {
			if (queue.remainingCapacity() > 0)
				queue.add(data);
			this.width = width;
			this.height = height;
		}
		@Override
		public void run() {
			try {
				while (true) {
					byte[] data = queue.take();
					if (data.length == 1)
						return;
//                        if (delegate != null) {
//                        }


					if (delegate != null) {
						boolean ret = delegate.previewProcInThread(data, width, height, previewAngle, false);
						// nib 01/01/2020 bug fix : did not restart auto detect after auto detect recognizing.
						if (ret) {
							mCamera.setOneShotPreviewCallback(null);
							mBurstPhotosThread = null;
							return;
						}
					}
					resumeBurstPhotos();                // wait next preview image
				}
			} catch (Exception e) {
				Log.e(Definitions.Define.TAG, "Exception : BurstPhotosThread error", e);
			}
		}
	}

	private void openCamera() {
		int cameraId;

		cameraId = getCameraId();
		if (mCamera == null) {
			// 최신 모델 중 back camera가 3개 이상있는 모델은,
			// 카메라 각각에대해 setParameters()로 parameter를 세팅하는 허용값이 있다.
			// 특정 cameraId로 camera.open(cameraId); 후 setParameters() 시,
			// 만일, 해당 camera는 세팅할 값을 허용하지 않는 카메라라면 RuntimeException이 발생한다.
			try {
				mCamera = Camera.open();

			} catch (Exception e1) {
				try {
					mCamera = Camera.open(cameraId);
				} catch (Exception e2) {
					e2.printStackTrace();
					mCamera = null;
				}
			}
			if (mCamera != null) {
				Camera.Parameters params = mCamera.getParameters();
				maxNumFocusAreas = params.getMaxNumFocusAreas();
				maxNumMeteringAreas = params.getMaxNumMeteringAreas();
			}
		}
	}

	private void closeCamera() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
		previewing = false;
	}

	public int getCameraId() {
		int cameraId = CameraInfo.CAMERA_FACING_BACK;
		int numberOfCameras = Camera.getNumberOfCameras();
		CameraInfo cameraInfo = new CameraInfo();
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);
			if (cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				// 후방카메라가 여러개 일 때, 첫번째 것 선택..
				cameraId = i;
				break;
			}
		}
		return cameraId;
	}


	private void initCameraParameters() {
		if (mCamera == null) return;
		Camera.Parameters params = mCamera.getParameters();
		Point previewRef = new Point(surfaceSize.x, surfaceSize.y);
		if (previewRef.x < previewRef.y) {
			int x = previewRef.x;
			previewRef.x = previewRef.y;
			previewRef.y = x;
		}
		getPreviewSize(mCamera, mPreviewSize, previewRef);

		// preview의 가로/세로 비율에 맞추어 surfaceview의 width, height를 재설정해주어야 한다.
		// preview와 surfaceview의 가로/세로 비율이 맞지않으면, 단말기 회전 시 화면이 찌그러지는 현상 발생.
		// 특히, galaxy note 10에서...
		//
		// saucerSize : surfaceView가 놓여있는 mother view의 size..
		//              예) 전체 스크린 위에 match_parent로 놓여있다면 ScreenSize,
		//                  특정 View위에 놓여있다면, aView.width & aView.height..
		Point saucerSize = new Point(parent.screenSize.x, parent.screenSize.y);
		surfaceSize = adjustSurfaceViewArea(mSurfaceView, mPreviewSize, saucerSize);

		// 여기까지 왔을 땐, mCaptureSize와 mPreviewSize는 null 일 수 없다...
		parent.cameraWidth = parent.screenSize.x;
		parent.cameraHeight = parent.screenSize.y;


		// set image capture size to be sent to idrcgn module
		// ref. CameraConfig.java setDesiredCameraParameters in QT
		Point captureRef = new Point(0, 0);
		Point refSaucer = new Point(surfaceSize.x, surfaceSize.y);    // screen size를 사용할 수도 있다.
		if (refSaucer.x < refSaucer.y) {
			// portrait
			int x = refSaucer.x;
			refSaucer.x = refSaucer.y;
			refSaucer.y = x;
		}

		captureRef.x = refSaucer.x * 20 / 10;
		captureRef.y = refSaucer.y * 20 / 10;
		if (captureRef.x >= 2048) {
			float ratx;
			ratx = 2048.0f / refSaucer.x;
			captureRef.x = 2048;
			captureRef.y = (int) ((float) refSaucer.y * ratx);
		}
		captureRef.x = 2048;
		getCaptureSize(mCamera, mCaptureSize, captureRef);
		params.setPreviewSize(mPreviewSize.x, mPreviewSize.y);
		params.setPictureSize(mCaptureSize.x, mCaptureSize.y);

		params.setPictureFormat(ImageFormat.JPEG);
		params.setPreviewFormat(ImageFormat.NV21);        // YCrCb (default for Camera class)

		params.setZoom(0); // x1.5


		// parameter 변경/세팅 후 setParameters()를 해줘야 한다..

		mCamera.setParameters(params);
		// set camera orientation
		int cameraId = getCameraId();
		int angle = Utils.seekCameraDisplayAngle((Activity) mContext, cameraId, mCamera);
		previewAngle = angle;
		mCamera.setDisplayOrientation(angle);
		//세로보기는 0으로 고정시킴
		//mCamera.setDisplayOrientation(90);
	}

	private void getCaptureSize(Camera mCamera, Point desiredRes, Point refRes) {
		if (mCamera == null)
			return;
		int wd;
		int ht;
		int d1;
		int d;
		int dMax = 8000000;
		float sa;
		float sa2;
		float sb;

		Camera.Parameters parameters = mCamera.getParameters();
		List<Size> pvSizes = parameters.getSupportedPictureSizes();
		sa = (float) refRes.y / (float) refRes.x;
		sa2 = sa * 0.95f;

		for (Size x : pvSizes) {  //2560x1920 , 2048x1536 2304x1296  1280x960 1536x864
			wd = x.width;
			ht = x.height;
			if (x.width < x.height) {
				ht = x.width;
				wd = x.height;
			}
			sb = (float) ht / (float) wd;
			//if(sb<sa2) continue;

			d1 = wd - refRes.x;
			d = ht - refRes.y;
			d = d * d + d1 * d1;
			d = (int) ((float) d * sa / sb);
			if (d < dMax) {
				dMax = d;
				desiredRes.x = wd;
				desiredRes.y = ht;
			}
		}
	}

	private void getPreviewSize(Camera mCamera, Point desiredRes, Point refRes) {
		if (mCamera == null) return;
		Camera.Parameters parameters = mCamera.getParameters();
		List<Size> pvSizes = parameters.getSupportedPreviewSizes();
		int width;
		int height;
		int d1;
		int d;
		int dMax = 8000000;
		if (refRes.x < refRes.y) {
			width = refRes.x;
			refRes.x = refRes.y;
			refRes.y = width;
		}
		for (Size x : pvSizes) {
			width = x.width;
			height = x.height;
			if (x.width <= x.height) {
				height = x.width;
				width = x.height;
			}
			if (width > 2048)
				continue;
			d1 = width - refRes.x;
			d = height - refRes.y;
			d = d * d + d1 * d1;
			if (d >= 0 && d < dMax) {
				dMax = d;
				desiredRes.x = width;
				desiredRes.y = height;
			}
		}

	}


	/**
	 * void adjustSurfaceViewArea
	 * <p>
	 * Preview의 가로/세로 resolution 비율에따라 surfaceView의 가로/세로 pixel을
	 * parent view size에 맞추어 layout을 재설정.
	 * <p>
	 * 특기사항: preview의 가로/세로 비율과 surfaceView의 가로/세로 비율이 맞지 않으면
	 * 카메라 회전시 상이 찌그러지는 현상이 발생한다.
	 *
	 * @param surfaceView           : SurfaceView. surfaceView object.
	 * @param previewSize           : Point. preview의 가로/세로 pixel 수.
	 * @param surfaceViewSaucerSize : Point. View group 중 surfaceView가 놓여있는 view의 가로/세로 pixel 수.
	 * @return new surfaceView size : Point.
	 */

	private Point adjustSurfaceViewArea(SurfaceView surfaceView, Point previewSize, Point surfaceViewSaucerSize) {
		int dw;
		int dy;
		float rat;
		int marginStart = 0, marginTop = 0;


		//Log.d("프리뷰 사이즈",previewSize.x  + " : " + previewSize.y);

		// get ratio
		//
		rat = (float) previewSize.x / (float) previewSize.y;
		// if portrait design, that is, preview angle is 90 or 270 degrees, reverse the ratio.

		//세로 보기 모드
		if (surfaceViewSaucerSize.x <= surfaceViewSaucerSize.y) {
			dy = (int) ((float) surfaceViewSaucerSize.x * rat);
			dw = surfaceViewSaucerSize.x;
			if (dy > surfaceViewSaucerSize.y) {
				dy = surfaceViewSaucerSize.y;
				dw = (int) ((float) surfaceViewSaucerSize.y / rat);
				marginStart = (surfaceViewSaucerSize.x - dw) / 2;
			}
		} else {

			dw = (int) ((float) surfaceViewSaucerSize.y * rat);
			dy = surfaceViewSaucerSize.y;
			if (dw > surfaceViewSaucerSize.x) {
				dw = surfaceViewSaucerSize.x;
				dy = (int) ((float) surfaceViewSaucerSize.x / rat);
				marginTop = (surfaceViewSaucerSize.y - dy) / 2;
			}
		}

		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) surfaceView.getLayoutParams();

		if (lp.width != dw || lp.height != dy) {

			lp.width = dw;
			lp.height = dy;
			surfaceView.setLayoutParams(lp);

			float prex = surfaceView.getX();
			float prey = surfaceView.getY();
			surfaceView.setX(marginStart + prex);
			surfaceView.setY(marginTop + prey);

			lp = (RelativeLayout.LayoutParams) parent.onDrawView.getLayoutParams();
			lp.width = dw;
			lp.height = dy;
			parent.onDrawView.setLayoutParams(lp);
			prex = parent.onDrawView.getX();
			prey = parent.onDrawView.getY();
			parent.onDrawView.setX(marginStart + prex);
			parent.onDrawView.setY(marginTop + prey);
			parent.onDrawView.setRight(marginStart);

			lp = (RelativeLayout.LayoutParams) parent.cameraWindow.getLayoutParams();
			lp.width = dw;
			float id_wd = 8.54f;
			float id_ht = 5.5f; //5.39f;
			lp.height = (int) (lp.width * id_ht / id_wd) + parent.dpToPixel(20);
			parent.cameraWindow.setLayoutParams(lp);

			ViewTreeObserver observer = parent.cameraWindow.getViewTreeObserver();
			observer.addOnGlobalLayoutListener(parent.mOnGlobalLayoutListener);
		}
		return new Point(dw, dy);
	}
	public void setModeContinue() {
		initFocusing();
	}
	private void initFocusing() {
		if (mCamera == null) return;
		Camera.Parameters params = mCamera.getParameters();
		String mode = getFocusModeProperties(params);
		params.setFocusMode(mode);
		mCamera.setParameters(params);
	}

	private String getFocusModeProperties(Camera.Parameters params) {
		if (forceAutoFocus) return Camera.Parameters.FOCUS_MODE_AUTO;
		List<String> values = params.getSupportedFocusModes();
		if (isSupported(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE, values)) {
			return Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
		} else if (isSupported(Camera.Parameters.FOCUS_MODE_AUTO, values)) {
			return Camera.Parameters.FOCUS_MODE_AUTO;
		}
		return values.get(0);
	}

	private boolean isSupported(String value, List<String> supported) {
		return (supported != null && supported.contains(value));
	}

	public boolean setFocusArea(float centerX, float centerY) {
		if (mCamera == null || centerX < 0.0F || centerY < 0.0F)
			return false;

		Rect dst = new Rect();
		float[] p = new float[]{centerX, centerY};
		int focusAreaSize = 150;

		dst.left = (int) p[0] - focusAreaSize;
		dst.right = (int) p[0] + focusAreaSize;
		dst.top = (int) p[1] - focusAreaSize;
		dst.bottom = (int) p[1] + focusAreaSize;
		int width = dst.width();
		int height = dst.height();

		if (dst.left < -1000) {
			dst.left = -1000;
			dst.right = dst.left + width;
		} else if (dst.right > 1000) {
			dst.right = 1000;
			dst.left = dst.right - width;
		}
		if (dst.top < -1000) {
			dst.top = -1000;
			dst.bottom = dst.top + height;
		} else if (dst.bottom > 1000) {
			dst.bottom = 1000;
			dst.top = dst.bottom - height;
		}

		ArrayList<Area> arrayList = new ArrayList<>();
		arrayList.add(new Area(dst, 1000));
		return this.setArea(arrayList);

	}

	private boolean setArea(List<Area> list) {
		if (mCamera == null || list == null)
			return false;
		Camera.Parameters parameters = mCamera.getParameters();
		parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		if (maxNumFocusAreas > 0)
			parameters.setFocusAreas(list);
		if (maxNumMeteringAreas > 0)
			parameters.setMeteringAreas(list);
		mCamera.setParameters(parameters);
		return true;

	}


}

