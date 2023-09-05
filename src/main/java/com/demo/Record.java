package com.demo;

import lombok.Getter;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import javax.sound.sampled.*;
import java.awt.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author baiqi
 * @description
 * @creattime 2023/9/4 20:43
 */
@Getter
public class Record {
    private int frameRate = 25;
    private boolean isStop = false;
    //线程池 screenTimer

    private FFmpegFrameRecorder recorder;
    private FFmpegFrameGrabber grabber;
    private TargetDataLine line;
    private AudioFormat audioFormat;
    private DataLine.Info dataLineInfo;
    private boolean isHaveDevice = true;
    //线程池 exec
    private ScheduledThreadPoolExecutor videoExe;
    private ScheduledThreadPoolExecutor soundExe;

    private boolean hasDevice;
    private boolean isActive = false;


    public Record(boolean isHaveDevice) {
        this.hasDevice = isHaveDevice;

        grabber = new FFmpegFrameGrabber("desktop");
        grabber.setFormat("gdigrab");
        grabber.setFrameRate(frameRate);
        // 捕获指定区域，不设置则为全屏
        grabber.setImageHeight(Toolkit.getDefaultToolkit().getScreenSize().height);
        grabber.setImageWidth(Toolkit.getDefaultToolkit().getScreenSize().width);
        //grabber.setOption("offset_x", "200");
        //grabber.setOption("offset_y", "200");//必须设置了大小才能指定区域起点，参数可参考 FFmpeg 入参
        try {
            grabber.start();
        } catch (FFmpegFrameGrabber.Exception e) {
            e.printStackTrace();
        }


        recorder = new FFmpegFrameRecorder("d:/vod/api02.mp4", Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264); // 13
        recorder.setFormat("mp4");
        // recorder.setFormat("mov,mp4,m4a,3gp,3g2,mj2,h264,ogg,MPEG4");
        recorder.setSampleRate(44100);
        recorder.setFrameRate(frameRate);
        recorder.setVideoQuality(0);
        recorder.setVideoOption("crf", "23");
        // 2000 kb/s, 720P视频的合理比特率范围
        recorder.setVideoBitrate(3000000);
        /**
         * 权衡quality(视频质量)和encode speed(编码速度) values(值)： ultrafast(终极快),superfast(超级快),
         * veryfast(非常快), faster(很快), fast(快), medium(中等), slow(慢), slower(很慢),
         * veryslow(非常慢)
         * ultrafast(终极快)提供最少的压缩（低编码器CPU）和最大的视频流大小；而veryslow(非常慢)提供最佳的压缩（高编码器CPU）的同时降低视频流的大小
         * 参考：https://trac.ffmpeg.org/wiki/Encode/H.264 官方原文参考：-preset ultrafast as the
         * name implies provides for the fastest possible encoding. If some tradeoff
         * between quality and encode speed, go for the speed. This might be needed if
         * you are going to be transcoding multiple streams on one machine.
         */
        recorder.setVideoOption("preset", "slow");
        recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P); // yuv420p
        recorder.setAudioChannels(2);
        recorder.setAudioOption("crf", "0");
        // Highest quality
        recorder.setAudioQuality(0);
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        try {
            recorder.start();
        } catch (FFmpegFrameRecorder.Exception e) {
            e.printStackTrace();
        }


    }

    public void start() {
        if (isActive) {
            return;
        }
        isActive = true;
        screenCapture();
        soundCapture();
    }

    public void stop() {
        if (null != videoExe) {
            videoExe.shutdownNow();
        }
        if (null != soundExe) {
            soundExe.shutdownNow();
        }
        try {
            recorder.stop();
            grabber.stop();

            recorder.release();
            grabber.release();


            if (null != line) {
                line.stop();
                line.close();
            }
            dataLineInfo = null;
            audioFormat = null;

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void screenCapture() {
        // 录屏
        videoExe = new ScheduledThreadPoolExecutor(1);
        videoExe.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (isStop) {
                    return;
                }
                try {
                    // 获取屏幕捕捉的一帧
                    Frame frame = grabber.grabFrame();
                    // 将这帧放到录制
                    recorder.record(frame);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, 1000 / frameRate, TimeUnit.MILLISECONDS);
    }

    /**
     * 可路系统声音+ 麦克风声音
     */
    private void soundCapture() {
        if (!hasDevice) {
            return;
        }
        audioFormat = new AudioFormat(44100.0F, 16, 2, true, false);
        dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
        try {
            line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
        } catch (LineUnavailableException e1) {
            System.out.println("#################");
        }
        try {
            line.open(audioFormat);
        } catch (LineUnavailableException e1) {
            e1.printStackTrace();
        }
        line.start();

        final int sampleRate = (int) audioFormat.getSampleRate();
        final int numChannels = audioFormat.getChannels();

        int audioBufferSize = sampleRate * numChannels;
        final byte[] audioBytes = new byte[audioBufferSize];

        soundExe = new ScheduledThreadPoolExecutor(1);
        soundExe.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    int nBytesRead = line.read(audioBytes, 0, line.available());
                    int nSamplesRead = nBytesRead / 2;
                    short[] samples = new short[nSamplesRead];

                    // Let's wrap our short[] into a ShortBuffer and
                    // pass it to recordSamples
                    ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
                    ShortBuffer sBuff = ShortBuffer.wrap(samples, 0, nSamplesRead);

                    // recorder is instance of
                    // org.bytedeco.javacv.FFmpegFrameRecorder
                    recorder.recordSamples(sampleRate, numChannels, sBuff);
                    // System.gc();
                } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, (int) (1000 / frameRate), TimeUnit.MILLISECONDS);
    }


}
