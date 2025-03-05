package m2codes.perizinan_ocr_tool.domain.seeder;

import m2codes.perizinan_ocr_tool.domain.model.AccountType;
import m2codes.perizinan_ocr_tool.domain.repository.AccountTypeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountTypeSeeder {

    @Value("${account.personal.daily-limit}")
    private int personalDailyLimit;

    @Value("${account.organization.daily-limit}")
    private int organizationDailyLimit;

    @Bean
    CommandLineRunner seedAccountTypes(AccountTypeRepository accountTypeRepository) {
        return args -> {
          if (accountTypeRepository.count() == 0) {
              accountTypeRepository.save(AccountType.builder()
                      .name("Organization")
                      .icon("https://cdn-icons-png.flaticon.com/128/3067/3067280.png")
                      .description("Akun untuk bisnis dan institusi dengan batasan penggunaan lebih tinggi.")
                      .dailyLimit(organizationDailyLimit)
                      .build());

              accountTypeRepository.save(AccountType.builder()
                      .name("Personal")
                      .icon("https://cdn-icons-png.flaticon.com/128/3135/3135715.png")
                      .description("Akun individu untuk penggunaan pribadi dengan batasan yang lebih fleksibel.")
                      .dailyLimit(personalDailyLimit)
                      .build());
          }
        };
    }

}