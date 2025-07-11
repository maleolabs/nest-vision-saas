package m2codes.ocr_tool.infrastructure.lib;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
public class LibraryLoader {

    public static void loadNativeLibrary() {
        String libFileName = "libtesseract.so";
        try {
            Path tempDir = Files.createTempDirectory("tesseract-directory");
            InputStream in = LibraryLoader.class.getResourceAsStream("/native/" + libFileName);
            Path tempFile = tempDir.resolve(libFileName);
            assert in != null;
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);

            System.load(tempFile.toAbsolutePath().toString());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

}
