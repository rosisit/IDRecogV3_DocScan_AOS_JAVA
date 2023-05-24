package com.rosisit.idcardcapture.struct;

import android.os.Build;
import android.util.Log;

import com.cardcam.jni.IDCardRcgn.IDs_Data;
import com.rosisit.idcardcapture.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class ImageRecognition {
    public static final String TYPE_DRIVER_LICENSE = "자동차운전면허증";
    public static final String TYPE_ID_CARD = "주민등록증";
    public static final String TYPE_ALIEN_CARD = "외국인등록증";
    public static final String TYPE_RECOGNITION_FAIL = "인식실패";

    private ArrayList<String> mResultString;
    private final String mEncryptKey;
    private final IDs_Data idsData;

    public ImageRecognition(IDs_Data idsData,String encryptKey) {
        mResultString = new ArrayList<>(9);
        for(int i=0;i<9;i++)
            mResultString.add(i,"");
        this.idsData = idsData;
        this.mEncryptKey = encryptKey;
    }

    public ArrayList<String> getResultRecognized() {
        int type = idsData.IDtype;
        boolean checkReg = type >0 && type <4;
        String typeString = TYPE_RECOGNITION_FAIL;
        if(!checkReg){
            mResultString.set(EssentialFieldData.TYPE.ordinal(),typeString);
            return mResultString;
        }

        String userName = StringUtils.convertByteArrayToUTF8(idsData.IDname,mEncryptKey);
        String organization = StringUtils.convertByteArrayToUTF8(idsData.IDorgan,mEncryptKey);
//        String regNumber =StringUtils.convertByteArrayToUTF8(idsData.IDstr,mEncryptKey,true,"[^\\d]");
        String regNumber =StringUtils.convertByteArrayToUTF8(idsData.IDstr,mEncryptKey,true,"[^\\d]");
        String issueData = StringUtils.convertByteArrayToUTF8(idsData.IDdate);

        List<String> targetArray = Arrays.asList(issueData.split("\\."));
        for (int i=0;i<targetArray.size();i++) {
            String item = targetArray.get(i);
            if (0 != i){
                if (item.length() < 2)
                    targetArray.set(i, "0".concat(item));
                }else{
                if (item.length() < 4)
                    targetArray.set(i, "20".concat(item));
                }
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            issueData = targetArray.stream().map(String::valueOf).collect(Collectors.joining());
        }else{
            StringBuilder sb = new StringBuilder();
            for(String s : targetArray){
                sb.append(s);
            }
            issueData = sb.toString();
            Log.d("issueData",issueData);

        }
        issueData = StringUtils.convertStringEncrypt(issueData,mEncryptKey);

        mResultString.set(EssentialFieldData.NAME.ordinal(), userName);
        mResultString.set(EssentialFieldData.ORGANIZATION.ordinal(), organization);
        mResultString.set(EssentialFieldData.REGISTRATION_NUMBER.ordinal(), regNumber);
        mResultString.set(EssentialFieldData.ISSUE_DATE.ordinal(), issueData);

        switch(type){
            case 1:
                typeString = TYPE_ID_CARD;
                break;
            case 2:
                typeString = TYPE_DRIVER_LICENSE;
//                String licenseNumber = StringUtils.convertByteArrayToUTF8(idsData.IDLicense,mEncryptKey,true,"[^0-9ㄱ-힣]");
                String licenseNumber = StringUtils.convertByteArrayToUTF8(idsData.IDLicense,mEncryptKey);
                String chkAcfn = StringUtils.convertByteArrayToUTF8(idsData.chkstr,mEncryptKey);
                String licType= StringUtils.convertByteArrayToUTF8(idsData.sDrvKinds,mEncryptKey);
                mResultString.set(EssentialFieldData.LICENSE_NUMBER.ordinal(),licenseNumber);
                mResultString.set(EssentialFieldData.ACFN.ordinal(),chkAcfn);
                mResultString.set(EssentialFieldData.LICENSE_TYPE.ordinal(), licType);

                break;
            case 3:
                typeString = TYPE_ALIEN_CARD;
                String nation = StringUtils.convertByteArrayToUTF8(idsData.IDcountry,mEncryptKey);
                mResultString.set(EssentialFieldData.NATION.ordinal(),nation);
                mResultString.set(EssentialFieldData.ORGANIZATION.ordinal(), "");
                break;
        }

        typeString = StringUtils.convertStringToUTF8(typeString,mEncryptKey);
        mResultString.set(EssentialFieldData.TYPE.ordinal(),typeString);
        return mResultString;
    }



    public enum EssentialFieldData {
        TYPE,
        NAME,
        REGISTRATION_NUMBER,
        ISSUE_DATE,
        LICENSE_NUMBER,
        ORGANIZATION,
        NATION,
        ACFN,
        LICENSE_TYPE
    }
}