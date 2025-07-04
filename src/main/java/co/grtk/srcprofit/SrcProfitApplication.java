package co.grtk.srcprofit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SrcProfitApplication {

    public static void main(String[] args) {
        SpringApplication.run(SrcProfitApplication.class, args);
    }

}
