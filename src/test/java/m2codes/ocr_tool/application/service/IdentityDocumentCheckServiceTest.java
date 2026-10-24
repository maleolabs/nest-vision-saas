package m2codes.ocr_tool.application.service.impl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdentityDocumentCheckServiceTest {
    @Test void classifyUsesDocumentLabelsNotNIK() {
        assertEquals("KTP", IdentityDocumentCheckServiceImpl.classify("NIK 1234567890123456 KARTU TANDA PENDUDUK"));
        assertEquals("SIM", IdentityDocumentCheckServiceImpl.classify("SURAT IZIN MENGEMUDI"));
        assertNull(IdentityDocumentCheckServiceImpl.classify("NIK 1234567890123456"));
    }
}
