package com.rosisit.idcardcapture.utils;

@Deprecated
public class Definitions {

	public class Define {
		public static final boolean __DEBUG = false;			// 함수를 쓰는 것 보단 낫다. false 면 컴파일 시 빠지기 때문.
		public static final boolean __DRAW_CORNER = false;		// 함수를 쓰는 것 보단 낫다. false 면 컴파일 시 빠지기 때문.
		public static final boolean __DEBUG_CAMERA = false;		// 함수를 쓰는 것 보단 낫다. false 면 컴파일 시 빠지기 때문.
		public static final String TAG = "com.cardcam";         // basic log tag
	}

	public class Msg {
		public static final String MSG_FAILED_SERVER = "서버와의 접속에 실패했습니다. 잠시 후 다시 시도해주세요.";
		public static final String MSG_NO_LOGIN_ID = "ID가 존재하지 않습니다.";
		public static final String MSG_NO_LOGIN_PASSWORD = "패스워드가 잘 못 되었습니다.";
		public static final String MSG_NETWORK_FAILED = "네트워크 연결에 실패했습니다. 잠시 후 다시 시도해주세요.";
		public static final String MSG_VIDEO_RECORDING_ERROR = "녹화가 정상적으로 이루어지지 않았습니다. 문제가 계속 발생하면 앱을 완전히 종료했다가 다시 시작해 주세요.";
	}

	public class KeyDef {
		public static final String KEY_INPUTACTIVITY_DEFAULT = "inputactivitydefault";

		// between activities
		public static final String KEY_RECORDITEM_PARCELABLE = "keyofrecorditemparcelable";
		public static final String KEY_FROM_LISTACTIVITY = "keyfromlistactivity";
		public static final String KEY_FROM_INPUTFINALACTIVITY = "keyfrominputfinalactivity";
		public static final String KEY_PHOTOLIST = "keyphotolist";
		public static final String KEY_PHOTOLIST_INDEX = "keyphotolistindex";
		public static final String KEY_PHOTO_LAST_FILENAME = "keyphotolastfilename";
		// nib 07/10/2018 thumbnail
		public static final String KEY_PHOTO_THUMBNAIL_LIST = "keyphotothumbnaillist";
	}

	public class ValueDef {
		public static final String VALUE_PHOTO_MODE = "photomode";
	}

	public class DBVars {
		public static final String TABLE_NAME = "recorditem";
		public static final String _id = "_id";					// primary key. auto increment
		public static final String type = "type";				// integer. 1:photo, 2:video, 3:voice
		public static final String text = "text";				// STTed text
		public static final String timestamp = "timestamp";		// format : 'yyyy-MM-dd HH:mm:ss' (19 bytes)
		public static final String filename = "filename";		// video, voice file명 (format : filename_yyMMddHHmmssSSS.jpg|.mp4|.3gp)
		public static final String subfilename = "subfilename";	// video, photo의 경우, 음성녹음 파일명. (format : filename(ext포함)과동일_voiceRecord.wav)
		public static final String uploaded = "uploaded";		// 0 : not upload yet, 1 : uploaded already.
		public static final int TYPE_PHOTO = 1;
		public static final int TYPE_VIDEO = 2;
		public static final int TYPE_VOICE = 3;
	}

	public class FileVars {
		// Video_yyMMddHHmmssSSS.mp4
		public static final String DEF_VIDEO_FILENAME = "Video_%s.mp4";
		// Photo_yyMMddHHmmssSSS-0.jpg (1초 이내 촬영 클릭 시 한 그룹으로...)
		// 파일 이름 형태는 Photo_%s-%d.jpg 로서, %d를 같은 그룹의 사진 장 수로 사용할꺼다...
		// %d가 0이면 한 장, 1이면 두 장이다...
		public static final String DEF_PHOTO_FILENAME = "Photo_%s-%d.jpg";
		// Voice_yyMMddHHmmssSSS.wav
		public static final String DEF_VOICE_FILENAME = "Voice_%s.wav";

		// nib 07/10/2018 photoGrid의 속도를 위해 생성하는 thumbnail photo filename 접두사
		public static final String DEF_PHOTO_THUMBNAIL_PREFIX = "Thumbnail_";
	}

	////////////////////////////////////////////
	// common function part
	////////////////////////////////////////////

}
