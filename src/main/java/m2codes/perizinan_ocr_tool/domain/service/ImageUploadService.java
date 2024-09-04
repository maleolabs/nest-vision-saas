package m2codes.perizinan_ocr_tool.domain.service;

import m2codes.perizinan_ocr_tool.domain.model.ImageUpload;
import m2codes.perizinan_ocr_tool.web.dto.request.ImageUploadRequest;

/**
 *
 * @author marij_mokoginta
 */
public interface ImageUploadService {

    ImageUpload save(ImageUploadRequest request);

}