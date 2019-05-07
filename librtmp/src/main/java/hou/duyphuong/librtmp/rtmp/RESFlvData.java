package hou.duyphuong.librtmp.rtmp;

public class RESFlvData {

    // video size
//    public static final int VIDEO_WIDTH = 1280;
//    public static final int VIDEO_HEIGHT = 720;
//    public static final int VIDEO_BITRATE = 2500000; // 500Kbps
//    public static final int FPS = 30;
//    public static final int AAC_SAMPLE_RATE = 44100;
//    public static final int AAC_BITRATE = 1024 * 1024;

    public final static int FLV_RTMP_PACKET_TYPE_VIDEO = 9;
    public final static int FLV_RTMP_PACKET_TYPE_AUDIO = 8;
    public final static int FLV_RTMP_PACKET_TYPE_INFO = 18;
    public final static int NALU_TYPE_IDR = 5;

    public boolean droppable;

    public int dts;//解码时间戳 decode time stamp ?

    public byte[] byteBuffer; //数据 - byte array **

    public int size; //字节长度

    public int flvTagType; //视频和音频的分类 video/audio type

    public int videoFrameType;

    public boolean isKeyframe() {
        return videoFrameType == NALU_TYPE_IDR;
    }

}