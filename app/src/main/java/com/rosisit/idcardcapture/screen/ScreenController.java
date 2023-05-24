package com.rosisit.idcardcapture.screen;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;



public class ScreenController {
    private Context context;

    public ScreenController(Context context){
        this.context =context;
    }
    // get physical screen size.
    // If the navigation bar exists, the size will be a whole screen size including navigation bar area.
    public static Point getRealScreenSize(Context context) {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();

        Point size = new Point();
        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(size);
        } else if (Build.VERSION.SDK_INT >= 14) {
            try {
                size.x = (Integer) Display.class.getMethod("getRawWidth").invoke(display);
                size.y = (Integer) Display.class.getMethod("getRawHeight").invoke(display);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Log.d("====size===","x:" +size.x + " y: "+size.y);
        return size;
    }


    public static int getMaxGap(int rCrop[], int corner[]) {
        int i, k, x, y, idx, dx, dy, maxgap, min = 10000;
        int[] rc = new int[4];
        for (i = 0; i < 4; i++)
            rc[i] = rCrop[i];

        rc[3] = rc[3] - rc[1];
        rc[1] = 0;
        idx = 0;
        for (i = 0; i < 4; i++) {
            dx = corner[i * 2] - rc[0];
            if (dx < 0) dx = -dx;
            dy = corner[i * 2 + 1] - rc[1];
            if (dy < 0) dy = -dy;
            if (dx + dy < min) {
                min = dx + dy;
                idx = i;
            }
        }

        x = rc[0];
        y = rc[1];
        maxgap = 0;
        for (k = 0; k < 4; k++) {
            i = (k + idx) % 4;
            if (k == 0) {
                x = rc[0];
                y = rc[1];
            } else if (k == 1) {
                x = rc[2];
                y = rc[1];
            } else if (k == 2) {
                x = rc[2];
                y = rc[3];
            } else if (k == 3) {
                x = rc[0];
                y = rc[3];
            }

            dx = corner[i * 2] - x;
            if (dx < 0) dx = -dx;
            dy = corner[i * 2 + 1] - y;
            if (dy < 0) dy = -dy;
            if (dx + dy > maxgap) {
                maxgap = dx + dy;
            }
        }

        return maxgap / 2;
    }

    // get App screen info.
    // If the navigation bar exists, the ScreenSize would be the size without navigation bar area.
    public static int getScreenDirection(WindowManager manager)   //captured_angle when image captured
    {
        Display display = manager.getDefaultDisplay();
        int rotation = display.getRotation();
        if (rotation == Surface.ROTATION_0)
            return 90;
        else if (rotation == Surface.ROTATION_90)
            return 0;
        else if (rotation == Surface.ROTATION_180)
            return 270;
        else if (rotation == Surface.ROTATION_270)
            return 180;
        return 0;
    }

    /**
     * calculate dp to pixel
     *
     * @param dp
     * @return int pixel
     */

    public int dpToPixel(float dp) {
        if (context == null)
            return 0;
        float dpiScale = context.getResources().getDisplayMetrics().density;
        return (int)((dp * dpiScale) + 0.5);
    }

    public static void setTypeface(Paint paint, int which) {
        Typeface type = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC);
        switch (which) {
            case 1:
                type = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
                break;
            case 2:
                type = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
                break;
            case 3:
                type = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC);
                break;
        }
        paint.setTypeface(type);
    }

//    public static int getScreenOrientation(Activity activity, boolean rotarianDegree) {
//        if (activity == null) return (rotarianDegree ? 0 : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//
//        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
//        DisplayMetrics dm = new DisplayMetrics();
//        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
//        int width = dm.widthPixels;
//        int height = dm.heightPixels;
//        int orientation;
//        int degree;
//
//        // if the device's natural orientation is portrait:
//        if ((rotation == Surface.ROTATION_0
//                || rotation == Surface.ROTATION_180) && height > width ||
//                (rotation == Surface.ROTATION_90
//                        || rotation == Surface.ROTATION_270) && width > height) {
//            switch(rotation) {
//                case Surface.ROTATION_90:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
//                    degree = 90;
//                    break;
//                case Surface.ROTATION_180:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
//                    degree = 180;
//                    break;
//                case Surface.ROTATION_270:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
//                    degree = 270;
//                    break;
//                default:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
//                    degree = 0;
//                    break;
//            }
//        }
//        // if the device's natural orientation is landscape or if the device
//        // is square:
//        else {
//            switch(rotation) {
//                case Surface.ROTATION_90:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
//                    degree = 0;
//                    break;
//                case Surface.ROTATION_180:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
//                    degree = 270;
//                    break;
//                case Surface.ROTATION_270:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
//                    degree = 180;
//                    break;
//                default:
//                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
//                    degree = 90;
//                    break;
//            }
//        }
//        return rotarianDegree ? degree : orientation;
//    }


}
