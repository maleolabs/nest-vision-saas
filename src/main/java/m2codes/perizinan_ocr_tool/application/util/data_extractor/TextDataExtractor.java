package m2codes.perizinan_ocr_tool.application.util.data_extractor;

public interface TextDataExtractor {

    String extract(String ocrText);

    boolean textExist(String ocrText);

}