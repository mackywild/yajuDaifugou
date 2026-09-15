package com.example.daifugo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 大富豪Webアプリケーションの起動クラス。
 */
@SpringBootApplication
@EnableScheduling
public class DaifugoApplication {

    /**
     * Spring Bootアプリケーションを起動する。
     *
     * @param args 起動引数
     */
    public static void main(String[] args) {
        SpringApplication.run(DaifugoApplication.class, args);
    }
}
