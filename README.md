# PbLive
An Android library which help you connect to rtmp server

<b>Input</b>: rtmp address, video data, audio data.

<hr>
+ Module "librtmp" is the library
+ Module app is sample ScreenRecord that demonstrate how to use librtmp.
+ Thanks to Eterrao with <a href="https://github.com/eterrao/ScreenRecorder">ScreenRecorder</a> 
and LakeinChina with <a href="https://github.com/lakeinchina/librestreaming">Librestreaming</a>.
<br><hr>
+ ScreenRecorder class collects video data from screen
+ RESAudioClient class collects audio data from micro
<br>
&#8594; in your video/audio class, you'll want to implement RESFlvDataCollecter.collect(video/audioData, type) for video/audio prepare.

<hr>
<b>Streaming</b><br>

```java
        RESFlvDataCollecter collecter = new RESFlvDataCollecter() {
            @Override
            public void collect(RESFlvData flvData, int type) {
                rtmpSender.feed(flvData,type);
            }
        };
```
+ RESFlvDataCollecter is interface that help collect data from sources like video, audio. Call this function and <br>
push audio, video buffer as following:
```java
        mVideoRecorder = new ScreenRecorder(collecter, resConfig.getHeightScreen(), resConfig.getWidthScreen(), resConfig.getBitRate(), mScreenDensity, mediaProjection);
        mVideoRecorder.start();
        // audio
        audioClient = new RESAudioClient(coreParameters);
        if (audioClient != null) {
            audioClient.start(collecter);
        }
        // this line will start stream
        rtmpSender.start(rtmpAddr);
```
+ Note: both mVieoRecorder and audioClient need param is "collecter" - collect data from these two sources to rtmpSender
