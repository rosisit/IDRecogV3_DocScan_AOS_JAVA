package com.rosisit.idcardcapture.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.MediaActionSound;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class Utils {


	public static <T> void RestartApp(Activity context, Class<T> targetClass) {
		Intent i = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.finish();
		context.startActivity(i);
	}

	public static int splitInHalf(int target){
		return target > 0?target / 2:target;
	}

	// nib 01/10/2019 get captured-image angle when image captured.
	// ref. getScreenDirection() of mTransMe.java in IPPcarnum project
	public static int getCapturedImageAngle(Activity activity) {
		if (activity == null) return 0;
		WindowManager manager = (WindowManager)activity.getSystemService(Context.WINDOW_SERVICE);
		Display display = manager.getDefaultDisplay();
		int rotation = display.getRotation();
		switch (rotation) {
			case Surface.ROTATION_0:
				return 90;
			case Surface.ROTATION_180:
				return 270;
			case Surface.ROTATION_270:
				return 180;
			case Surface.ROTATION_90:
			default:
				return 0;
		}
	}

	// nib 01/11/2019 get orientation from Exif of image file
	// ref. GetExifOrientation() of mTransMe.java in IPPcarnum project
	public static int getExifOrientation(String filepath) {
		int degree = 0;
		ExifInterface exif = null;
		try {
			exif = new ExifInterface(filepath);
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
			if (orientation != -1) {
				switch(orientation) {
					case ExifInterface.ORIENTATION_ROTATE_90:
						degree = 90;
						break;
					case ExifInterface.ORIENTATION_ROTATE_180:
						degree = 180;
						break;
					case ExifInterface.ORIENTATION_ROTATE_270:
						degree = 270;
						break;
				}
			}
			return degree;
		}catch (Exception e) {
			Log.e("Exception", "Error : ExifInterface in getExifOrientation", e);
			return -1;
		}
	}

	// Debug mode 확인을 한번만 처리하기 위해서 추가.
    private class RUN_MODE {
        static final int MODE_DEFAULT = 1090;
        static final int MODE_DEBUG = 1091;
        static final int MODE_ELSE = 1092;
    }


    // nib 11/02/2018 The activity argument should be an Activity not a Context, because of getExternalFilesDir()
	// if use "Context", when Runtime, it will cause of [NPE: Attempt to invoke virtual method 'java.io.File android.content.Context.getExternalFilesDir(java.lang.String)' on a null object reference].
	public static String getFilesDir(Context activity) {
		boolean ret = true;
//		if (activity == null) return null;

		// nib 05/31/2018 함수별 storage path
		/*
		//
		// 아래하면, 내부메모리 /files 폴더가 얻어진다... /files 폴더가 없으면 새로 생성한다.
		Log.d(TAG, "getFilesDir.getAbsolutePath                ="+activity.getFilesDir().getAbsolutePath());
		// -> /data/user/0/com.cardcam.idcardrcgn/files
		Log.d(TAG, "getFilesDir.getPath                        ="+activity.getFilesDir().getPath());
		// -> /data/user/0/com.cardcam.idcardrcgn/files

		// 아래하면, 외부메모리 /files 폴더가 얻어진다... /files 폴더가 없으면 새로 생성한다.
		Log.d(TAG, "getExternalFilesDir.getAbsolutePath        ="+activity.getExternalFilesDir(null).getAbsolutePath());
		// -> /storage/emulated/0/Android/data/com.cardcam.idcardrcgn/files
		Log.d(TAG, "getExternalFilesDir.getPath                ="+activity.getExternalFilesDir(null).getPath());
		// -> /storage/emulated/0/Android/data/com.cardcam.idcardrcgn/files

		// 아래하면, 외부메모리 root 폴더가 얻어진다..
		Log.d(TAG, "getExternalStorageDirectory.getAbsolutePath="+Environment.getExternalStorageDirectory().getAbsolutePath());
		// -> /storage/emulated/0
		Log.d(TAG, "getExternalStorageDirectory.getPath        ="+Environment.getExternalStorageDirectory().getPath());
		// -> /storage/emulated/0

		// 아래하면, 내부메모리 app 폴더가 얻어진다..
		Log.d(TAG, "getApplicationInfo().dataDir               ="+activity.getApplication().getApplicationInfo().dataDir);
		// -> /data/user/0/com.cardcam.idcardrcgn

		// 아래하면, 외부메모리 app 폴더가 얻어진다..
		// 하지만, side-effect로 /files 폴더가 생성된다..
		Log.d(TAG, "getExternalFilesDir.getParent()            ="+activity.getExternalFilesDir(null).getParent());
		// -> /storage/emulated/0/Android/data/com.cardcam.idcardrcgn
		// 하지만 이것은, 만일 안드로이드 폴더명 정책이 바뀌면 문제가 발생할 수 있다.
		Log.d(TAG, "get External app dir                       ="+Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + activity.getApplicationContext().getPackageName());
		// -> /storage/emulated/0/Android/data/com.cardcam.idcardrcgn

		//
		*/

		String strPath;
		String ext = Environment.getExternalStorageState();
		if (ext.equals(Environment.MEDIA_MOUNTED))
			strPath = activity.getExternalFilesDir(null).getAbsolutePath();
		else
			strPath = activity.getFilesDir().getAbsolutePath();

		// FilesDir()을 이용하면 자신의 앱영역에 /files 폴더를 자동으로 잡아준다.
		// 이외 다른 폴더명으로 생성하는 경우는 아래 처럼...
		//File file = new File(strPath + File.separator + "myfoldername");
		File file = new File(strPath);

		// 없는 폴더를 생성하기 위해 mkdirs()를 했을 때, 새로 생성할 때 만 true를 리턴하고, 생성 실패거나 폴더가 이미 있으면 false를 리턴한다.
		// 그러므로, 괜한 mkdirs()를 호출하기 전에 폴더가 있는지 없는지 먼저 검사...
		// isDirectory()는 폴더가 있으면 true, 파일이거나 폴더가 없으면 false를 리턴한다.
		if (!file.isDirectory()) {
			ret = file.mkdirs();
		}

		if (!ret) strPath = null;

		return strPath;
	}

	public static String getFilePath(Context activity, String fileName) throws NullPointerException {
		try {
			return getFilesDir(activity) + File.separator + fileName;
		}
		catch (NullPointerException e) {
			return "";
		}
	}

	// nib 11/07/2018 get file list from the folder
	public static File[] getFileListAsFile(Activity activity) {
		File folder = getFolder(activity);
		return folder.exists()? folder.listFiles():null;
	}

	public static String[] getFileListAsString(Activity activity) {
		File folder = getFolder(activity);
		return folder.exists()? folder.list():null;
	}

	private static File getFolder(Activity activity){
		return new File(getFilesDir(activity));
	}
	// -------------------------------------------

	// nib 02/08/2019 get external root dir
	// input : subdir with '/' -> ex. "/myFolder"
	public static String getWorkingDirOnExternalRoot(String subPath) {
		String workDir = Environment.getExternalStorageDirectory().getAbsolutePath();   // /storage/emulated/0
		if (subPath != null && subPath.length() > 1)
			workDir += subPath;

		File file = new File(workDir);
		if (!file.isDirectory())
			file.mkdirs();
		return workDir;
	}

	public static String getAppPath(Activity activity) {


		// 아래하면, 내부메모리 app 폴더가 얻어진다.. : /data/user/0/com.cardcam.idcardrcgn
		//strPath = activity.getApplication().getApplicationInfo().dataDir;

		// 만일, 외부메모리의 app 폴더를 얻으려면...
		// Environment.MEDIA_MOUNTED로 외부메모리가 있는지 확인 후... 다음을 사용할 수는 있지만...
		// 각각의 문제가 조금 있다. (위의 GetFilesDir()의 코멘트 부분 참조)
		//strPath = activity.getExternalFilesDir(null).getParent();
		// 혹은...
		// Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + activity.getApplicationContext().getPackageName();

		return activity.getExternalFilesDir(null).getParent();
	}

	public static String getCurrentDateTime(String strFormat) {
		Calendar calendar = Calendar.getInstance(Locale.KOREA);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(strFormat, Locale.KOREA);
		return simpleDateFormat.format(calendar.getTime());
	}

	// nib 06/30/2018 date calculation with Date class
	public static String getNewDateTime(Date date, String pattern, int hours, int minutes, int seconds) {
		String strNewDate;
		SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.KOREA);

		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.HOUR, hours);
		c.add(Calendar.MINUTE, minutes);
		c.add(Calendar.SECOND, seconds);

		strNewDate = formatter.format(c.getTime());		// now the new date in String
		return strNewDate;
	}

	// nib 06/30/2018 date calculation with date String
	public static String getNewDateTime(String strDate, String pattern, int hours, int minutes, int seconds) {
		SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.KOREA);
		Calendar c = Calendar.getInstance();
		Date date;
		try {
			date = formatter.parse(strDate);
			if(date != null) {
				c.setTime(date);
				c.add(Calendar.HOUR, hours);
				c.add(Calendar.MINUTE, minutes);
				c.add(Calendar.SECOND, seconds);
				return formatter.format(c.getTime());
			}
		} catch (ParseException e) {
			Log.e("Exception", e.getMessage());
		}
		return "";
	}

	// https://stackoverflow.com/questions/3394765/how-to-check-available-space-on-android-device-on-sd-card
	public static float megabytesAvailable(File f) {
		StatFs stat = new StatFs(f.getPath());
		long bytesAvailable =  stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
		return bytesAvailable / (1024.f * 1024.f);
	}

	public static long bytesAvailable(File f) {
		StatFs stat = new StatFs(f.getPath());
		long bytesAvailable = 0;
		bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
		return bytesAvailable;
	}

	// taking photo : shutter sound
	// https://stackoverflow.com/questions/10891742/android-takepicture-not-making-sound
	public static void takePhotoSound(Context context, boolean useMAS) {

		if (useMAS) {
			// 구글은 camera class setPreviewCallback 설명에서 MediaActionSound를 사용하라고 권고하지만,
			// 소리가 넘 크당... ㅋ
			MediaActionSound sound = new MediaActionSound();
			sound.play(MediaActionSound.SHUTTER_CLICK);
		}
		else {
			AudioManager mgr = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
			//mgr.playSoundEffect(AudioManager.FLAG_PLAY_SOUND);
			mgr.playSoundEffect(AudioManager.FX_KEY_CLICK);        // used in QT
		}
	}

	// taking photo : landscape image to portrait
	// nib 07/05/2018 preview에서 takePicture()한 이미지나 PreviewCallback에서 그냥 save하면 이미지가 landscape으로 저장된다..
	// https://stackoverflow.com/questions/15808719/controlling-the-camera-to-take-pictures-in-portrait-doesnt-rotate-the-final-ima
	// 여기에 보면, setParameters()를 통해 카메라 orientation을 돌리는 방법이 있지만... (나도 이게 근본적이라 생각하지만...)
	// 어떤 device에서는 안된다는 말이 많음...
	// 두번째.. 질문자가 답을 올린 방법은 이미지 저장시 이미지를 돌리는 방법인데 이건 모두에서 다되는 모양....
	// 그래서 이것으로 택함...
	public static void saveLandscapeJpgToPortrait(byte[] data, String filepath) throws FileNotFoundException, IOException {

//		// nib 07/08/2018 picture orientation
//		FileOutputStream os = new FileOutputStream(filepath);
//		os.write(data);
//		os.close();

		Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length);
		FileOutputStream os = new FileOutputStream(filepath);
		ExifInterface exif = new ExifInterface(filepath);
		String exifOrientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
		if (exifOrientation.equalsIgnoreCase("6")) {
			realImage = rotate(realImage, 90);
		}
		else if (exifOrientation.equalsIgnoreCase("8")) {
			realImage = rotate(realImage, 270);
		}
		else if (exifOrientation.equalsIgnoreCase("3")) {
			realImage = rotate(realImage, 180);
		}
		else if (exifOrientation.equalsIgnoreCase("0")) {
			realImage = rotate(realImage, 90);
		}

		boolean bo = realImage.compress(Bitmap.CompressFormat.JPEG, 100, os);
		os.close();

		// free bitmap
		if (realImage != null) {
			realImage.recycle();
			realImage = null;
		}
	}

	private static Bitmap rotate(Bitmap bitmap, int degree) {
		int w = bitmap.getWidth();
		int h = bitmap.getHeight();
		Matrix mtx = new Matrix();
		mtx.setRotate(degree);
		return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
	}

	public static Bitmap rotateBitmapData(byte[] data, int degree) {
		try {
			return rotate(BitmapFactory.decodeByteArray(data, 0, data.length), degree);
		}catch (Exception e){
			Log.e("Exception", e.getMessage());
			return null;
		}
	}

	// nib 07/11/2018
	// orientation degree를 얻기위해 구글 Camera class reference 페이지의 setDisplayOrientation() snippet code 를 사용함.
	// reference by https://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
	//
	/**
	 * @param activity
	 * @param cameraId Camera.CameraInfo.CAMERA_FACING_FRONT,
	 *                 Camera.CameraInfo.CAMERA_FACING_BACK
	 * @param camera   Camera Orientation
	 */
	public static int seekCameraDisplayAngle(Activity activity, int cameraId, Camera camera) {
		if (activity == null) return 0;
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = rotation * 90;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
			//result = (info.orientation + degrees) % 360;
			return (360 - (info.orientation + degrees) % 360) % 360;  // compensate the mirror
		 else  // back-facing
			return (info.orientation - degrees + 360) % 360;
	}


	public static void showNoticeDialog(Context context, String title, String msg) {
		AlertDialog.Builder confirmDlg = new AlertDialog.Builder(context);
		confirmDlg.setTitle(title)
				.setMessage(msg)
				.setCancelable(false)
				.setPositiveButton("확인", new AlertDialog.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
		AlertDialog alert = confirmDlg.create();
		alert.getButton(DialogInterface.BUTTON_POSITIVE);//.setContentDescription(msg);
		alert.show();
	}


	public static byte[] toBytesLittleEndian(int i) {
		byte[] result = new byte[4];
		result[0] = (byte)(i & 0xFF); i >>= 8;
		result[1] = (byte)(i & 0xFF); i >>= 8;
		result[2] = (byte)(i & 0xFF); i >>= 8;
		result[3] = (byte)(i & 0xFF);
		return result;
	}

}
