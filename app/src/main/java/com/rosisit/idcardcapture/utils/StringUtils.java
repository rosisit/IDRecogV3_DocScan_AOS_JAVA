package com.rosisit.idcardcapture.utils;


import android.util.Base64;

import com.rosisit.idcardcapture.security.AES256Util;

import java.nio.charset.StandardCharsets;

public class StringUtils {
//    public String createReArrayString(String targetText, String separator, int arrayLength, int splitTextLength, String addText){
//        List<String> targetArray = Arrays.asList(targetText.split(separator));
//        if(targetArray.size() != arrayLength)
//            return targetText;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            return targetArray.stream().filter(item -> item.length() != splitTextLength).map(addText::concat).reduce("", String::concat);
//        }else{
//            int cnt =0;
//            while(cnt < splitTextLength){
//
//                targetArray.get(cnt).concat(addText);
//                cnt++;
//            }
//
//
//
//            return addText;
//        }
//    }

    public static String getArraysToString(int length, byte[] arrays) {
        StringBuilder builderText = new StringBuilder();
        int i = 0;
        while(i < length){
            String item = String.valueOf(arrays[i]);
            builderText.append(i != 0 ? ", " + item : item);
            i++;
        }
        return builderText.toString();
    }


    public static String convertByteArrayToUTF8(byte[] byteArray,String encryptKey){
        String encode = "UTF-8";
        boolean isEncrypt = encryptKey != null;
        try{
            String convertOrigin = new String(byteArray,"euc-kr");
            byte[] utf8StringBuffer = convertOrigin.getBytes(encode);
            return new String(isEncrypt? AES256Util.aesEncode(utf8StringBuffer,encryptKey): Base64.encode(utf8StringBuffer,0),encode);
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

    public static String convertStringEncrypt(String targetText,String encryptKey){
        String encode = "UTF-8";
        boolean isEncrypt = encryptKey != null;
        try{
            return new String(isEncrypt? AES256Util.aesEncode(targetText.getBytes(StandardCharsets.UTF_8),encryptKey):Base64.encode(targetText.getBytes(StandardCharsets.UTF_8),0),encode);
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

    public static String convertByteArrayToUTF8(byte[] byteArray){
        String encode = "UTF-8";
        try{
            String convertOrigin = new String(byteArray,"euc-kr");
            byte[] utf8StringBuffer = convertOrigin.getBytes(encode);
            return new String(utf8StringBuffer,encode).replace(" ","");
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }
    public static String convertByteArrayToUTF8(byte[] byteArray, String encryptKey, Boolean isReplace, String replaceText){
        String encode = "UTF-8";
        boolean isEncrypt = encryptKey != null;
        try{
            String convertOrigin = new String(byteArray,"euc-kr");
            if(isReplace && !replaceText.equals(""))
                convertOrigin = convertOrigin.replaceAll(replaceText,"");
            byte[] utf8StringBuffer = convertOrigin.getBytes(encode);
            return new String(isEncrypt? AES256Util.aesEncode(utf8StringBuffer,encryptKey):Base64.encode(utf8StringBuffer,0),encode);
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

    public static String convertStringToUTF8(String str, String encryptKey){
        String encode = "UTF-8";
        boolean isEncrypt = encryptKey != null;
        try{
            return new String(isEncrypt? AES256Util.aesEncode(str.getBytes(),encryptKey):Base64.encode(str.getBytes(),0),encode);
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }



}
