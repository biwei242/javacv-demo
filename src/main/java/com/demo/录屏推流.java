package com.demo;

import expect4j.Expect4j;
import expect4j.matches.EofMatch;
import lombok.Data;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bytedeco.javacpp.Loader;


/**
 * @author baiqi
 * @description
 * @creattime 2023/8/29 15:14
 */
public class 录屏推流 {


    public static void desktopRecord() throws Exception {

        String ffmpeg = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);
        ProcessBuilder pb = new ProcessBuilder(ffmpeg, "-re",
                "-f", "gdigrab",
                "-framerate", "25",
                "-video_size", "1920x1080",
                "-i", "desktop",
                "-f", "dshow",
                "-i", "audio=\"virtual-audio-capturer\"",
                "-c:v", "libx264",
                "-c:a", "aac",
                "-preset","ultrafast",
                "-strict", "2",
                "-f", "flv",
                "rtmp://推流地址"
        );

        MyThread myThread = new MyThread(pb);
        myThread.start();
    }

    @Getter
    static  class MyThread extends Thread {
        private Process process;
        private ProcessBuilder processBuilder;

        public MyThread(ProcessBuilder processBuilder){
            this.processBuilder = processBuilder;
        }

        @SneakyThrows
        @Override
        public void run() {
            process = processBuilder.inheritIO().start();
            process.waitFor();
        }
    }


    public static void main(String[] args) throws Exception{
        desktopRecord();
    }
}
