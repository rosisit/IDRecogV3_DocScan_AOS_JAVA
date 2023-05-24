package com.rosisit.idcardcapture.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Deprecated
public class ASEUtils {

/*    private static byte[] ENCRYPT_KEY = {
            '1', '2', '3', '4', '5', '6', '7', '8',
            '1', '2', '3', '4', '5', '6', '7', '8',
            '1', '2', '3', '4', '5', '6', '7', '8',
            '1', '2', '3', '4', '5', '6', '7', '8'
    };*/

    private static byte[] AES256_IV = {
            '1', '2', '3', 'a', '5', 'd', 'z', 'e',
            '1', '4', 'c', '2', 'b', '8', '7', 'f'};

    public static byte[] getAES256IV(){
        return AES256_IV;
    }


//    public static String covertByteArrayToString(){
//        return new String(ENCRYPT_KEY,StandardCharsets.UTF_8);
//    }

    private static byte[] covertByteArray(String key){
        return compareLength(key.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] compareLength(byte[] byteArray){
        if(byteArray.length == AES256_IV.length)
            return byteArray;

        byte[] convertByteArray = new byte[AES256_IV.length];;
        int cnt = 0;
        if(byteArray.length > AES256_IV.length){
            while(cnt<AES256_IV.length){
                convertByteArray[cnt] = byteArray[cnt];
                cnt++;
            }
        }/*else{
            convertByteArray = new byte[byteArray.length];
            while(cnt<convertByteArray.length){
                convertByteArray[cnt] = AES256_IV[cnt];
                cnt++;
            }
            AES256_IV = convertByteArray;
        }*/
        return convertByteArray;

    }
//    public static byte[] encByKey(byte[] value){
//        try {
//
//            SecretKeySpec secretKeySpec = new SecretKeySpec(ENCRYPT_KEY, "AES");
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(AES256_IV));
//            byte[] encValue = cipher.doFinal(value);
//            return encValue;
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return null;
//
//    }

    private static String encByKey(String key, String value){
        SecretKeySpec secretKeySpec = new SecretKeySpec(covertByteArray(key), "AES");
        Cipher cipher = null;
        byte[] encValue = null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(1, secretKeySpec, new IvParameterSpec(AES256_IV));
            encValue = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return encValue != null?new String(encValue):"";
    }

    public static byte[] encByKey(String encText,byte[] byteArry){
        SecretKeySpec secretKeySpec = new SecretKeySpec(covertByteArray(encText), "AES");
        Cipher cipher = null;
        byte[] encValue = null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(1, secretKeySpec, new IvParameterSpec(AES256_IV));
            encValue = cipher.doFinal(byteArry);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return encValue;
    }

    private String decByKey(String key, String encText){
        SecretKeySpec secretKeySpec =  new SecretKeySpec(covertByteArray(key), "AES");
        Cipher cipher = null;
        byte[] decValue =null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(2, secretKeySpec, new IvParameterSpec(AES256_IV));
            decValue = cipher.doFinal(encText.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e){
            e.printStackTrace();
        }
        return decValue != null?new String(decValue):"";
    }

    public static byte[] decByKey(String key, byte[] byteArry){
        SecretKeySpec secretKeySpec =  new SecretKeySpec(covertByteArray(key), "AES");
        Cipher cipher = null;
        byte[] decValue =null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(AES256_IV));
            decValue = cipher.doFinal(byteArry);
        } catch (Exception e){
            e.printStackTrace();
        }
        return decValue;
    }

   /* public static byte[] decByKey(byte[] byteArry, String encText){
        SecretKeySpec secretKeySpec =  new SecretKeySpec(byteArry, "AES");
        Cipher cipher = null;
        byte[] decValue =null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(2, secretKeySpec, new IvParameterSpec(AES256_IV));
            decValue = cipher.doFinal(encText.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e){
            e.printStackTrace();
        }
        return decValue;
    }
    public static byte[] decByKey(byte[] byteArry, byte[] encText){
        SecretKeySpec secretKeySpec =  new SecretKeySpec(byteArry, "AES");
        Cipher cipher = null;
        byte[] decValue =null;
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(2, secretKeySpec, new IvParameterSpec(AES256_IV));
            decValue = cipher.doFinal(encText);
        } catch (Exception e){
            e.printStackTrace();
        }
        return decValue;
    }*/





   /* public static byte[] decrypt(byte[] byteData, String key) {
        return byteData != null && key != null ? decrypt(byteData, key) : byteData;
    }


    public static String encrypt(String text, String key) {
        return text != null && key != null ? encByKey(text, key) : text;
    }

    public static String decrypt(String text, String key) {
        return text != null && key != null ? decrypt(text, key) : text;
    }

    public static byte[] getEncryptImage(Bitmap origin, String key) {
        byte[] encryptByteArray = null;
        if (origin != null && !origin.isRecycled()) {
            try {
                Bitmap resized = Bitmap.createScaledBitmap(origin, origin.getWidth() / 2, origin.getHeight() / 2, false);
                if (resized != null && !resized.isRecycled()) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    resized.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                    byte[] byteArray = byteArrayOutputStream.toByteArray();

                    try {
                        encryptByteArray = encrypt(byteArray, key);
                    } catch (Exception var7) {
                        encryptByteArray = null;
                    }

                    resized.recycle();
                }
            } catch (Exception var8) {
                encryptByteArray = null;
            }
        }

        return encryptByteArray;
    }*/

}



