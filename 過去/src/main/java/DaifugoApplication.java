

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 大富豪オンラインゲームの起動クラス。
 *
 * Spring Bootアプリケーションを起動し、
 * Webサーバーおよび各種Springコンポーネントを初期化する。
 */
@SpringBootApplication
public class DaifugoApplication {

    /**
     * アプリケーションを起動する。
     *
     * @param args 起動時引数
     */
    public static void main(String[] args) {

        SpringApplication.run(
                DaifugoApplication.class,
                args);
    }
}