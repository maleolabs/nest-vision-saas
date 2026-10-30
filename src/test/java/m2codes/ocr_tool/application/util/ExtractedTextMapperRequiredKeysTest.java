package m2codes.ocr_tool.application.util;

import m2codes.ocr_tool.application.dto.ExtractedTextDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regresi: filterParsedDataByRequiredKeys memakai ListIterator.set() pada
 * parsedData yang bisa immutable (List.of) -> UnsupportedOperationException.
 * SIM `nomor` -> rename fuzzy ke `nik` butuh jalur ini utuh.
 */
class ExtractedTextMapperRequiredKeysTest {

    private final ExtractedTextMapper mapper = new ExtractedTextMapper(null);

    @Test
    void immutableParsedDataCanBeRenamed() {
        List<ExtractedTextDto> parsed = List.of(
                ExtractedTextDto.builder().textKey("nomor").textValue("3171234567890123").build(),
                ExtractedTextDto.builder().textKey("nama").textValue("BUDI").build());

        List<ExtractedTextDto> out = mapper.filterParsedDataByRequiredKeys(parsed, List.of("nik", "nama"));

        // "nomor" levenshtein ke "nama"=3, ke "nik"=4 -> fuzzy ambil "nama";
        // bagian ini hanya membuktikan list immutable tidak melempar UOE.
        assertEquals("nama", out.get(0).getTextKey(), "nomor->nama dist 3 menang atas nomor->nik dist 4");
        assertEquals("3171234567890123", out.get(0).getTextValue());
        assertEquals("nama", out.get(1).getTextKey());
    }

    @Test
    void exactKeyMatchPreservedOnImmutableList() {
        List<ExtractedTextDto> parsed = List.of(
                ExtractedTextDto.builder().textKey("nik").textValue("7501070410020002").build());

        List<ExtractedTextDto> out = mapper.filterParsedDataByRequiredKeys(parsed, List.of("NIK", "nama"));

        assertEquals("NIK", out.get(0).getTextKey(), "casing dipulihkan dari requiredKeys");
        assertEquals("7501070410020002", out.get(0).getTextValue());
    }

    @Test
    void nullRequiredKeysReturnsParsedUnchanged() {
        List<ExtractedTextDto> parsed = List.of(ExtractedTextDto.builder().textKey("nik").textValue("1").build());
        assertEquals(parsed, mapper.filterParsedDataByRequiredKeys(parsed, null));
        assertEquals(parsed, mapper.filterParsedDataByRequiredKeys(parsed, List.of()));
    }
}
