package m2codes.perizinan_ocr_tool.infrastructure.filemanager.service;

import m2codes.perizinan_ocr_tool.infrastructure.filemanager.config.FileProperties;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileManagerService {

    private final LocalFileUploader localFileUploader;
    private final LocalFileRetriever localFileRetriever;
    private final ExternalFileUploader externalFileUploader;
    private final FileProperties fileProperties;

    public FileManagerService(
            LocalFileUploader localFileUploader,
            LocalFileRetriever localFileRetriever,
            ExternalFileUploader externalFileUploader,
            FileProperties fileProperties
    ) {
        this.localFileUploader = localFileUploader;
        this.localFileRetriever = localFileRetriever;
        this.externalFileUploader = externalFileUploader;
        this.fileProperties = fileProperties;
    }

    public String uploadFile(MultipartFile file) throws Exception {
        if ("local".equalsIgnoreCase(fileProperties.getUploadMethod())) {
            return localFileUploader.uploadFile(file);
        } else if ("external".equalsIgnoreCase(fileProperties.getUploadMethod())) {
            return externalFileUploader.uploadFile(file);
        } else {
            throw new IllegalArgumentException("Unsupported upload method : " + fileProperties.getUploadMethod());
        }
    }

    public InputStream retrieveFile(String identifier) throws Exception {
        if ("local".equalsIgnoreCase(fileProperties.getUploadMethod())) {
            return localFileRetriever.retrieveFile(identifier);
        } else if ("external".equalsIgnoreCase(fileProperties.getUploadMethod())) {
            return null;
        } else {
            throw new IllegalArgumentException("Unsupported upload method : " + fileProperties.getUploadMethod());
        }
    }

    public File createTempFileFromInputStream(InputStream inputStream, String identifier) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        String tempFileName = System.nanoTime() + "." + FilenameUtils.getExtension(identifier);
        Path tempFilePath = Paths.get(tempDir, tempFileName);
        File tempFile = tempFilePath.toFile();

        if (!tempFile.exists()) {
            tempFile.createNewFile();
        }

        try (OutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[1024];
            int byteRead;
            while ((byteRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, byteRead);
            }
        }

        return tempFile;
    }

}
