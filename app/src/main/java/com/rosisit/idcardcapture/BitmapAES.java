package com.rosisit.idcardcapture;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class BitmapAES {

	private final static String KeyString = "AESEncrypt";
	private static byte[] keybyte = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
	public BitmapAES() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * 암호화 하기
	 * @param clear
	 * @return
	 * @throws Exception
	 */
	public static byte[] encrypt( byte[] clear) throws Exception {
        //SecretKeySpec skeySpec = new SecretKeySpec(getKey(), "AES");
		SecretKeySpec skeySpec = new SecretKeySpec(keybyte, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(clear);
        return encrypted;
    }
	/**
	 * 암호화 해제
	 * @param encrypted
	 * @return
	 * @throws Exception
	 */
	public static byte[] decrypt( byte[] encrypted) throws Exception {
        //SecretKeySpec skeySpec = new SecretKeySpec(getKey(), "AES");
		SecretKeySpec skeySpec = new SecretKeySpec(keybyte, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }
    
	private static byte[] getKey (){

		try {
			byte[] keyStart = KeyString.getBytes();
			KeyGenerator kgen = KeyGenerator.getInstance("AES");
			SecureRandom sr;
			sr = SecureRandom.getInstance("SHA1PRNG");
			sr.setSeed(keyStart);
			kgen.init(128, sr); // 192 and 256 bits may not be available
			SecretKey skey = kgen.generateKey();
			return skey.getEncoded();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public static Bitmap byteTobitmap(byte[] data){
		Bitmap temp  = BitmapFactory.decodeByteArray(data, 0, data.length);
		return temp;
	}

	public static byte[] fileToByte(InputStream is) throws IOException {

		byte[] data = new byte[is.available()];
		is.read(data);
		return data;
	}
	/**
	 * 스트림 가져옴
	 * @param path
	 * @return
	 */
	public static InputStream getStream(String path){
		try {
			InputStream us = new FileInputStream(path);
			return us;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}


	/**
	 *
	 * 폴더에 파일로 쓰기
	 * @param writefile
	 * @param data
	 */

	public static void writeFile(File writefile, byte[] data){

		try {
			FileOutputStream fo = new FileOutputStream(writefile);
			fo.write(data);
			fo.flush();
			fo.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

