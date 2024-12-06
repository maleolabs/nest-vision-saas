package m2codes.perizinan_ocr_tool.infrastructure.filemanager.service;

import m2codes.perizinan_ocr_tool.domain.service.FileRetriever;
import m2codes.perizinan_ocr_tool.infrastructure.filemanager.config.FileProperties;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service("localFileRetriever")
public class LocalFileRetriever implements FileRetriever {

    private final FileProperties fileProperties;

    public LocalFileRetriever(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
    }

    @Override
    public InputStream retrieveFile(String fileName) throws Exception {
        Path filePath = Paths.get(fileProperties.getUploadDir(), fileName);
        if (!Files.exists(filePath)) {
            throw  new IllegalArgumentException("File not found: " + filePath);
        }
        return new FileInputStream(filePath.toFile());
    }

}