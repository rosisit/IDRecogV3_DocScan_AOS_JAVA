package com.rosisit.idcardcapture.security;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES256Util {

    private static byte[] ivBytes;
    public static void setIVValue(String iv){
        ivBytes = iv.getBytes();
    }
    public static byte[] aesEncode(byte[] str, String key) {
        if(ivBytes == null || key == null)
            return str;
        try {
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            SecretKeySpec newKey = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                newKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            }
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec);
            return Base64.encode(cipher.doFinal(str), 0);
        }catch(Exception e ){
            e.printStackTrace();
            return str;
        }
    }

    public static byte[] aesDecode(byte[] str, String key) {
        if(ivBytes == null || key == null)
            return str;
       try {
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
           SecretKeySpec newKey = null;
           if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
               newKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
           }
           Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, newKey, ivSpec);
            return cipher.doFinal(Base64.decode(str, 0));
        }catch(Exception e){
            e.printStackTrace();
            return str;
        }
    }
}
