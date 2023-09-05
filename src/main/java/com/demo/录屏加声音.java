package com.demo;

import java.util.Scanner;

/**
 * Hello world!
 */
public class 录屏加声音 {
    public static void main(String[] args) {
        Record recorder = new Record(true);
        recorder.start();
        while (true) {
            System.out.println("你要停止吗？请输入(stop)，程序会停止。。");
            Scanner sc = new Scanner(System.in);
            if (sc.next().equalsIgnoreCase("stop")) {
                recorder.stop();
                break;
            }
        }
    }
}
