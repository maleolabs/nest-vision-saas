package m2codes.perizinan_ocr_tool.application.event;

import m2codes.perizinan_ocr_tool.interfaces.dto.request.OcrDataRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class OcrRequestEventPublisher {

    private final ApplicationEventPublisher publisher;

    public OcrRequestEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publish(OcrDataRequest request) {
        OcrRequestEvent event = new OcrRequestEvent(this, request);
        publisher.publishEvent(event);
    }

}