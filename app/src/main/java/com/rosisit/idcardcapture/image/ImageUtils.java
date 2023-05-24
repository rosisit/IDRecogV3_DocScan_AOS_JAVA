package com.rosisit.idcardcapture.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/***
 * ImageCrop 추가
 * @since 2022/08/03 로시스
 * 이미지 처리 관련 클래스
 * @since 2022/07/28 로시스
 */
public class ImageUtils {
    /***
     * OCR 된 신분증에 증명 사진만 원본으로 추출 (증명사진 인식 모듈을 태우기 위한)
     * @param width 생성될 이미지 Width
     * @param height 생성될 이미지 Width
     * @param targetImg Crop 될 대상 이미지
     * @param pos Crop 된 이미지
     * @return Crop 이미지 byte array
     */
    public static byte[] getFaceBitmap(int width, int height, int[] targetImg, int[] pos){
        Bitmap createdBitmap = null;
        byte[] mImageByteArray = new byte[0];
        try {
            createdBitmap = ImageUtils.createBitmap(width, height, targetImg);
            Bitmap faceImg = createCropImage(createdBitmap, pos);
            mImageByteArray = bitmapToByteArray(faceImg);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            recyclesBitmap(createdBitmap);
        }
        return mImageByteArray;
    }

    /***
     * Intent data return 일정 용량을 초과하면 (3.5MB) 전달 거절되는 이슈로 이미지를 절반 크기로 줄임
     * @param width 리사이즈 Width
     * @param height 리사이즈 Height
     * @param img 리사이징할 이미지
     * @return 리사이즈된 이미지
     * @since 2022/08/24 로시스 이재훈
     */
    public static byte[] resizedBitmap(int width, int height, int[] img){
        Bitmap createdBitmap;
        Bitmap resizedBitmap = null;
        byte[] mImageByteArray = null;
        try {
            //putExtra 용량문제로 이미지를 1/2 Size 로 리사이징함
            createdBitmap = ImageUtils.createBitmap(width, height, img);
            resizedBitmap = Bitmap.createScaledBitmap(createdBitmap, createdBitmap.getWidth() , createdBitmap.getHeight() , false);

            if (resizedBitmap != null && !resizedBitmap.isRecycled())
                mImageByteArray = bitmapToByteArray2(resizedBitmap);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            recyclesBitmap(resizedBitmap);
        }
        return mImageByteArray;
    }

    /***
     * byte[]을 Bitmap 으로 변환
     * @param mByteArray Image byte Array 데이터
     * @return 변환된 이미지
     */
    public static Bitmap byteArrayToBitmap(byte[] mByteArray) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        try {
            return BitmapFactory.decodeByteArray(mByteArray, 0, mByteArray.length, options);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /***
     * Bitmap 을 byte[] 으로 변환
     * @param mBitmap Image 데이터
     * @return 변환된 바이트배열
     */
    public static byte[] bitmapToByteArray2(Bitmap mBitmap ) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream() ;
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream) ;
        try {
            return stream.toByteArray();
        }catch(Exception e){
            return null;
        }
    }

    /***
     * Bitmap 을 byte[] 으로 변환
     * @param mBitmap Image 데이터
     * @return 변환된 바이트배열
     */
    public static byte[] bitmapToByteArray(Bitmap mBitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream() ;
        mBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream) ;
        try {
            return stream.toByteArray();
        }catch(Exception e){
            return null;
        }
    }

    /***
     * 이미지 마스킹 후처리 프로세싱 병렬처리
     * @param idType 라이브러리를 통해 인식 된 신분증 종류 (1 주민등록증, 2 운전면허증)
     * @param imgByteToBitmap 신분증별 마스킹 할 대상과 범위을 지정할 List
     * @param maskingPosList 신분증별 마스킹 할 대상과 범위을 지정할 List
     * @return 마스킹된 이미지
     * @since 2022/07/28 로시스 이재훈
     */
    public static Bitmap makeMaskPic(final int idType, final Bitmap imgByteToBitmap, List<MaskPosition> maskingPosList) {
        Bitmap maskImg;
        Future<Bitmap> future;
        try {
            future = Executors.newSingleThreadExecutor().submit(
                    () -> setMasking(idType, imgByteToBitmap, maskingPosList)
            );
            maskImg = future.get();
            return maskImg;
        } catch (Exception e) {
            return null;
        }
    }


    /***
     * Bitmap 생성
     * @param width 생성할 bitmap 가로 길이
     * @param height 생성할 bitmap 세로 길이
     * @param pixel Pixel
     * @return 생성된 Bitmap
     * @since 2022/08/12 로시스 이재훈
     */
    public static Bitmap createBitmap(int width, int height, int[] pixel) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixel, 0, width, 0, 0, width, height);
        return bitmap;
    }

    /***
     * Image Crop
     * @param imgByteToBitmap 원본 이미지
     * @param cropPos Crop 포지션
     * @return crop 된 이미지 바이트 배열
     * @since 2022/08/03 로시스 이재훈
     */
    public static byte[] createCropImage(byte[] imgByteToBitmap, int[] cropPos) {
        try {
            Bitmap idImg = byteArrayToBitmap(imgByteToBitmap);
            Bitmap faceImg = createCropImage(idImg, cropPos);
            assert faceImg != null;
            return bitmapToByteArray(faceImg);
        }catch(Exception e){
            return null;
        }
    }
    /***
     * Image Crop
     * @param imgByteToBitmap 원본 이미지
     * @param cropPos Crop 포지션
     * @return crop 된 이미지
     * @since 2022/08/03 로시스 이재훈
     */
    public static Bitmap createCropImage(Bitmap imgByteToBitmap, int[] cropPos) {
        if (imgByteToBitmap == null)
            return null;
        ByteArrayOutputStream cropOutputStream = new ByteArrayOutputStream();
        Bitmap faceImg;
        try {
            faceImg = Bitmap.createBitmap(imgByteToBitmap, cropPos[0], cropPos[1], cropPos[2] - cropPos[0], cropPos[3] - cropPos[1]);
            faceImg.compress(Bitmap.CompressFormat.JPEG, 100, cropOutputStream);
        }finally {
            try {
                cropOutputStream.flush();
                cropOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return faceImg;
    }


    /***
     * Image Crop
     * @param originBitmapByteArray line 을 그려줄 이미지 바이트
     * @param focusPos line 을 그려줄 좌표
     * @return focus line 이 그려진 이미지 바이트
     * @since 2022/11/28 로시스 이재훈
     */
    public static byte[] createImageFocusLine(byte[] originBitmapByteArray, int[] focusPos) {
        if (originBitmapByteArray == null)
            return null;

        Bitmap originBitmap = byteArrayToBitmap(originBitmapByteArray);
        Canvas c = new Canvas(originBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        c.drawRect(new Rect( focusPos[0], focusPos[1], focusPos[2], focusPos[3]), paint);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        assert originBitmap != null;
        originBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
        }



    /***
     * 이미지 마스킹 후처리 프로세싱
     * @param idType 라이브러리를 통해 인식 된 신분증 종류 (1 주민등록증, 2 운전면허증)
     * @param maskingPosList 신분증별 마스킹 할 대상과 범위을 지정할 List
     * @param imgByteToBitmap 인식된 신분증 이미지
     * @return 마스킹된 이미지
     * @since 2022/07/28 로시스 이재훈
     */
    private static Bitmap setMasking(int idType, Bitmap imgByteToBitmap, List<MaskPosition> maskingPosList) {
        if (imgByteToBitmap == null || maskingPosList.isEmpty())
            return null;
        Canvas c = new Canvas(imgByteToBitmap);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        for(MaskPosition item :  maskingPosList){

            int maskType = item.getType();
            //인식된 신분증이 사용자가 지정한 종류와 같은 것만 마스킹 처리
            if (maskType == idType) {
                //
                Rect rect = item.getRect();
                //사용자가 지정한 마스킹 시작점
                int startPos = item.getStartPos();

                //사용자가 지정한 마스킹 끝점
                int endPos = item.getEndPos();

                //감지된 실제 특정 영역 ex) 이름, 주민번호, 운전면허번호 등
                //감지된 실제 특정 영역 시작점 - 0미만은 0으로 고정
                int startRect = rect.left;

                //감지된 실제 특정 영역 끝점 - 100초과시 100으로 고정
                int endRect = rect.right;

                //끝점과 시작점의 거리를 백분율화
                int distanceDiv = (endRect - startRect) / 100;

                //시작점이 0이 아닐시
                if (startPos != 0)
                    rect.left = distanceDiv * startPos + startRect;

                //끝점이 100이 아닐시
                if (endPos != 100)
                    rect.right = endRect - distanceDiv * (100 - endPos);
                c.drawRect(rect, paint);
            }
        }
        return imgByteToBitmap;
    }

    public static void recyclesBitmap(Bitmap targetBitmap){
        if(targetBitmap != null && !targetBitmap.isRecycled())
            targetBitmap.recycle();
    }

    public static Rect convertIntArrayToRect(int[] rectArray){
        return rectArray != null?new Rect(rectArray[0], rectArray[1], rectArray[2], rectArray[3]):null;
    }


}