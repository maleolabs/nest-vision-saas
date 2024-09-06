package m2codes.perizinan_ocr_tool.application.service;

import m2codes.perizinan_ocr_tool.interfaces.dto.response.ExtractedTextResponse;
import m2codes.perizinan_ocr_tool.interfaces.dto.response.WebResponse;

import java.util.List;

public interface ExtractedTextQueryService {

    WebResponse<ExtractedTextResponse> findByTextkey();

    WebResponse<List<ExtractedTextResponse>> findByIzinId();

}
