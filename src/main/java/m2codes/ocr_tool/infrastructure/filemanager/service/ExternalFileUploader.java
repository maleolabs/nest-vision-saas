package m2codes.ocr_tool.infrastructure.filemanager.service;

import m2codes.ocr_tool.domain.service.FileUploader;
import m2codes.ocr_tool.infrastructure.filemanager.config.FileProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service("externalFileUploader")
public class ExternalFileUploader implements FileUploader {

    private final FileProperties fileProperties;

    public ExternalFileUploader(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
    }

    @Override
    public String uploadFile(MultipartFile file) throws Exception {
        return "";
    }

}