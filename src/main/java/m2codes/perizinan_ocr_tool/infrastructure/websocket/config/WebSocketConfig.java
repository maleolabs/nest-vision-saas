package m2codes.perizinan_ocr_tool.infrastructure.websocket.config;

import m2codes.perizinan_ocr_tool.infrastructure.websocket.handler.OcrMessageHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final OcrMessageHandler handler;

    public WebSocketConfig(OcrMessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ocr-ws").setAllowedOrigins("*");
    }

}