package com.rosisit.idcardcapture.image;

import android.graphics.Rect;

/***
 * 이미지 마스킹 처리를 위한 구조체
 */
public class MaskPosition {
    //마스킹할 신분증 종류
    //1:주민등록증
    //2:운전면허증
    private int type;

    //라이브러리에서 감지한 영역별 좌표값
    //int[0]: left
    //int[1]: top
    //int[2]: right
    //int[3]: bottom
    private int[] rect;

    //int[] rect 으로 실제 이미지에 접근하기 위한 객체
    private Rect convertRect;

    //좌표의 시작점 left
    private int startPos;

    //좌표의 끝점 right
    private int endPos;

    //마스팅 될 영역에 대한 설명을 기술할 객체 (= 디버깅용)
    private String description;

    /***
     *
     * @param type
     * @param rect
     * @param startPos
     * @param endPos
     * @param description
     */

    public MaskPosition(int type, int[] rect, int startPos, int endPos, String description) {
        this.type = type;
        this.convertRect = setRectConvert(rect);
        this.startPos = setPercentRange(startPos);
        this.endPos = setPercentRange(endPos);
        this.description = description;
    }


    public MaskPosition(int type, Rect rect, int startPos, int endPos, String description) {
        this.type = type;
        this.convertRect = rect;
        this.startPos = setPercentRange(startPos);
        this.endPos = setPercentRange(endPos);
        this.description = description;
    }

    public MaskPosition(int type, int[] rect, int startPos, int endPos) {
        this.type = type;
        this.convertRect = setRectConvert(rect);
        this.startPos = setPercentRange(startPos);
        this.endPos = setPercentRange(endPos);
        this.description = "";
    }

    private int setPercentRange(int range) {
        if (range > 100)
            return 100;
        if (range < 0)
            return 0;
        return range;
    }

    private Rect setRectConvert(int[] rectArray) {
        return ImageUtils.convertIntArrayToRect(rectArray);
    }

    public int getType() {
        return type;
    }

    public Rect getRect() {
        return convertRect;
    }


    public int getStartPos() {
        return startPos;
    }


    public int getEndPos() {
        return endPos;
    }


    public String getDescription() {
        return description;
    }

}
