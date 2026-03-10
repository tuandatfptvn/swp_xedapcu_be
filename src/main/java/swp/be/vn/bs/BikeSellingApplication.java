package swp.be.vn.bs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BikeSellingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BikeSellingApplication.class, args);
    }

}
