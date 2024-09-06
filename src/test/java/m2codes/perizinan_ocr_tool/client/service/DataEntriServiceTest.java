package m2codes.perizinan_ocr_tool.client.service;

import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.service.DataEntriService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 *
 * @author marij_mokoginta
 */
@SpringBootTest
public class DataEntriServiceTest {

    @Autowired
    private DataEntriService dataEntriService;

    @Test
    public void getByJenisPerizinanIdTest() {
        dataEntriService.getByJenisPerizinanId(167L).block().forEach(dataEntri -> {
            System.out.println("Data Entri: " + dataEntri.getNama());
        });
    }

}