package m2codes.perizinan_ocr_tool.infrastructure.websocket.handler;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class OcrMessageHandler extends TextWebSocketHandler {

    private WebSocketSession webSocketSession;

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session,@NonNull TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        this.webSocketSession = session;
    }

    public void sendOcrResult(String ocrResult) throws Exception {
        if (webSocketSession != null && webSocketSession.isOpen()) {
            webSocketSession.sendMessage(new TextMessage(ocrResult));
        }
    }
}