package m2codes.perizinan_ocr_tool.infrastucture.integration.perizinan.service.impl;

import m2codes.perizinan_ocr_tool.infrastructure.integration.perizinan.endpoint.DataEntriEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 *
 * @author marij_mokoginta
 */
@SpringBootTest
public class DataEntriEndpointTest {

    @Autowired
    private DataEntriEndpoint dataEntriEndpoint;

    @Test
    public void getByJenisPerizinanIdTest() {
        dataEntriEndpoint.getByJenisPerizinanId(167L).block().forEach(dataEntri -> {
            System.out.println("Data Entri: " + dataEntri.getNama());
        });
    }

}