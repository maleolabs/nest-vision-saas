package m2codes.perizinan_ocr_tool.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.application.service.impl.ExtractedTextQueryServiceImpl;
import m2codes.perizinan_ocr_tool.infrastructure.websocket.handler.OcrMessageHandler;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.OcrDataRequest;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OcrRequestEventListener {

    private final OcrMessageHandler handler;

    private final ExtractedTextQueryServiceImpl textQueryService;

    public OcrRequestEventListener(
            OcrMessageHandler handler,
            ExtractedTextQueryServiceImpl textQueryService
    ) {
        this.handler = handler;
        this.textQueryService = textQueryService;
    }

    @EventListener
    public void handleOcrResultEventListener(OcrRequestEvent event) throws Exception {
        OcrDataRequest request = event.getOcrRequest();
        var ocrResponse = textQueryService.findByIzinId(request.getIzinId());
        ObjectMapper mapper = new ObjectMapper();
        String dataJson = mapper.writeValueAsString(ocrResponse);
        handler.sendOcrResult(dataJson);
    }

}