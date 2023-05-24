package com.rosisit.idcardcapture.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.view.View;

import com.rosisit.idcardcapture.OCRCameraActivity;
import com.rosisit.idcardcapture.screen.ScreenController;

public final class CameraView extends View {
	private Path path;
	private final Paint paint2;
	private Paint paint;
	private Context context;
	private OCRCameraActivity parent;

	public CameraView(Context context) {
		super(context);
		this.parent = (OCRCameraActivity) context;
		path = new Path();
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setDither(true);
		paint.setColor(0xffff0000);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		paint.setStrokeCap(Paint.Cap.ROUND);
		paint.setStrokeWidth(3);
		paint.setTextSize(32);
		paint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
		ScreenController.setTypeface(paint2, 1);
		paint2.setTextSize(24);
	}

	//
	//	앱 실행 중 화면에 뭐인가 그려주려면,
	//	parent에서,
	//	isCanvasClear=false; viewdraw.invalidate();
	//	를 해주면 된다.
	//

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// nib 02/23/2019 CameraView onDraw() 시 화면 UI를 다시 그려줄 때 중복 진입 방지.
		if (parent.onDrawScreening > 0)
			return;

		// nib 02/23/2019 CameraView onDraw() 시 화면 UI를 다시 그려줄 때 중복 진입 방지.
		parent.onDrawScreening = 1;

		if (parent.isCanvasClear)
			canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

		// nib 01/09/2020 show box : porting from CcIPP
		parent.displayIpp(canvas, paint2);

		/*// just make sure cameraWindowPosition is correct.
		if (Definitions.Define.__DEBUG_CAMERA) canvas.drawRect(parent.cameraWindowPosition, paint);*/

		// nib 02/23/2019 CameraView onDraw() 시 화면 UI를 다시 그려줄 때 중복 진입 방지.
		parent.onDrawScreening = 0;
	}

	@Override
	public void invalidate() {
		super.invalidate();
	}
}
