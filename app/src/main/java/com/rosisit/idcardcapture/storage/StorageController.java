package com.rosisit.idcardcapture.storage;

import android.app.Activity;
import android.content.Context;

import com.rosisit.idcardcapture.utils.Utils;

import java.io.File;

public class StorageController {

    private Activity activity;
    private Context context;

    public StorageController(Activity activity) {
        this.activity = activity;
    }
    public StorageController(Context context) {
        this.context = context;
    }

    public String createWorkDir(String workingDir) {
        File dir = new File(getRootPath() + File.separator + workingDir + "/rawdata");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            for (File file : files)
                file.delete();
        } else {
            dir.mkdirs();
        }
        getSampleImageDir();

        return dir.getAbsolutePath();
    }
    public String getSampleImageDir() {
        File imageDir = new File(getRootPath() + "/IDRcgnSample");
        if (!imageDir.isDirectory())
            imageDir.mkdirs();

        return imageDir.getAbsolutePath();
    }
    private String getRootPath(){
        return Utils.getAppPath(activity);
    }
}
