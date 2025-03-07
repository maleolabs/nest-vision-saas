package m2codes.ocr_tool.domain.service;

import java.io.InputStream;

public interface FileRetriever {

    InputStream retrieveFile(String identifier) throws Exception;

}