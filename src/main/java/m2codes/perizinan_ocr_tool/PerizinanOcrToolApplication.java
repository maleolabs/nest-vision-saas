package m2codes.perizinan_ocr_tool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PerizinanOcrToolApplication {

	public static void main(String[] args) {
		SpringApplication.run(PerizinanOcrToolApplication.class, args);
	}

}