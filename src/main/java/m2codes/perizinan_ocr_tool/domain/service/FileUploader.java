package m2codes.perizinan_ocr_tool.domain.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploader {

    String uploadFile(MultipartFile file) throws Exception;

}