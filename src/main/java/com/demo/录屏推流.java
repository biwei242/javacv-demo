package com.demo;


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
        ProcessBuilder pb = new ProcessBuilder(ffmpeg,
                "-re",
                "-y",
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
                "d:/vod/cmd_record.mp4"
        );
        //pb.redirectErrorStream(true);
        pb.redirectInput(ProcessBuilder.Redirect.INHERIT);  // 将标准输入流重定向到当前进程的标准输入流

        MyThread myThread = new MyThread(pb);
        myThread.start();
        Thread.sleep(15000);
//        OutputStream outputStream = myThread.getProcess().getOutputStream();
//        outputStream.write("q".getBytes());
//        outputStream.flush();
//        Robot robot = new Robot();
//        robot.keyPress(KeyEvent.VK_Q);
//        robot.keyRelease(KeyEvent.VK_Q);
        //Thread.sleep(2);
        //myThread.getProcess().destroy();
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
