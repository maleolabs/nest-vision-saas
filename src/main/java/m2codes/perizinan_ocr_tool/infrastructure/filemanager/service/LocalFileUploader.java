package m2codes.perizinan_ocr_tool.infrastructure.filemanager.service;

import lombok.extern.slf4j.Slf4j;
import m2codes.perizinan_ocr_tool.domain.service.FileUploader;
import m2codes.perizinan_ocr_tool.infrastructure.filemanager.config.FileProperties;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service("localFileUploader")
public class LocalFileUploader implements FileUploader {

    private final FileProperties fileProperties;

    public LocalFileUploader(FileProperties fileProperties) {
        this.fileProperties = fileProperties;
    }

    /**
     *
     * @param file
     * @return fileName
     * @throws IOException
     */
    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(fileProperties.getUploadDir());
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String fileName = UUID.randomUUID() + "." + FilenameUtils.getExtension(file.getOriginalFilename());
        Path filePath = uploadPath.resolve(fileName);
        file.transferTo(filePath);

        return fileName;
    }

}