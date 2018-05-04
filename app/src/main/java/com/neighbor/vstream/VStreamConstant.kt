package com.neighbor.vstream

object VStreamConstant {

    val CODEC_VIDEO_H264_IDR = 1
    val CODEC_VIDEO_H264_NON_IDR = 2
    val CODEC_AUDIO_AAC = 101

    val VIDEO_RESOLUTION_FHD = 1
    val VIDEO_RESOLUTION_UHD = 2
    val VIDEO_RESOLUTION_QHD = 3
    val VIDEO_RESOLUTION_HD = 4
    val VIDEO_RESOLUTION_VGA = 5
    val VIDEO_RESOLUTION_SD_OR_D1 = 6
    val VIDEO_RESOLUTION_PIP = 90

    val AUDIO_CHANNEL_MONO = 1
    val AUDIO_CHANNEL_STEREO = 2

    enum class FHD(val value: Int) {
        ID(1),
        WIDTH(1920),
        HEIGHT(1080),
        BIT_RATE(8000000),
        FRAME_RATE(60)
    }

    enum class UHD(val value: Int) {
        ID(2),
        WIDTH(3840),
        HEIGHT(2160),
        BIT_RATE(35000000),
        FRAME_RATE(144)
    }

    enum class QHD(val value: Int){
        ID(3),
        WIDTH(960),
        HEIGHT(540),
        BIT_RATE(3000000),
        FRAME_RATE(30)
    }

    enum class HD(val value: Int){
        ID(4),
        WIDTH(1280),
        HEIGHT(720),
        BIT_RATE(5000000),
        FRAME_RATE(30)
    }

    enum class VGA(val value: Int){
        ID(5),
        WIDTH(640),
        HEIGHT(480),
        BIT_RATE(2500000),
        FRAME_RATE(30)
    }

    enum class SD(val value: Int){
        ID(6),
        WIDTH(720),
        HEIGHT(480),
        BIT_RATE(2500000),
        FRAME_RATE(30)
    }
}