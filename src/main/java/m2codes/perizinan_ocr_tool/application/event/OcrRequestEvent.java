package m2codes.perizinan_ocr_tool.application.event;

import m2codes.perizinan_ocr_tool.interfaces.dto.request.OcrDataRequest;
import org.springframework.context.ApplicationEvent;

public class OcrRequestEvent extends ApplicationEvent {

    private final OcrDataRequest request;

    public OcrRequestEvent(Object source, OcrDataRequest request) {
        super(source);
        this.request = request;
    }

    public OcrDataRequest getOcrRequest() {
        return request;
    }

}