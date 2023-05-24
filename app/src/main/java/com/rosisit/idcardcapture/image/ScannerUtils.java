package com.rosisit.idcardcapture.image;

import android.content.Context;
import android.graphics.Bitmap;

import com.rosisit.idcardcapture.storage.StorageController;
import com.rosisit.idcardcapture.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;


@Deprecated
public class ScannerUtils {

     // nib 02/07/2019 step scan
    private ArrayList<String> scanFiles = null;
    private boolean isStepScan = false;
    private int stepIndex = 0;
    // nib 02/27/2019 added AutoScan function
    private boolean isAutoScan = false;
    private Bitmap createdBitmap;

    private final Context parentContext;

    public ScannerUtils(Context parentContext){
        this.parentContext =parentContext;
    }


    private void stepScan() {
        StorageController storage = new StorageController(parentContext);
        if (scanFiles == null) {

            String imageDir = storage.getSampleImageDir();
            File folder = new File(imageDir);
            String[] files = folder.list();
            if (files == null || files.length == 0) {
                Utils.showNoticeDialog(parentContext, "알림", "app/IDRcgnSample 폴더에 샘플이미지가 없습니다.");
                isAutoScan = false;
                isStepScan = false;
                return;
            }
            // nib 03/01/2019 차후 테스트 결과 비교를 위해 sorting 처리..
            Arrays.sort(files);

            scanFiles = new ArrayList<>();
            scanFiles.addAll(Arrays.asList(files));
            stepIndex = 0;
        }
        if (scanFiles.size() == 0) return;
        if (!isStepScan && !isAutoScan) return;

        if (stepIndex > scanFiles.size() - 1) {
            stepIndex = 0;   // rotation
            // nib 02/28/2019 auto scan mode 면 한 번 만 돌도록...
            if (isAutoScan) {
                isAutoScan = false;
                if (createdBitmap != null && !createdBitmap.isRecycled()) {
                    //Log.d(TAG, "StepScan : recycle bitmap @@@");
                    createdBitmap.recycle();
                }
                createdBitmap = null;
                return;
            }
        }

        String fileName = scanFiles.get(stepIndex);
        stepIndex++;                                            // get ready to the next image
        fileName = storage.getSampleImageDir() + File.separator + fileName;

        ImageUtils.recyclesBitmap(createdBitmap);
        createdBitmap = null;

        /*if (mHandler != null) {
            Message msg = Message.obtain(mHandler);
            msg.what = HANDLER_MSG_CHECK_FILE_EXISTANCE;
            msg.obj = filename;
            stFileCheck = System.currentTimeMillis();
            mHandler.sendMessage(msg);
        }*/
    }
}
