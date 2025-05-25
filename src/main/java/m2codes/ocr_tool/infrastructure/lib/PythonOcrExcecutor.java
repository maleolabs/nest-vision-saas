package m2codes.ocr_tool.infrastructure.lib;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

@Component
public class PythonOcrExcecutor {

    @Value("${ocr.python.path:python3}")
    private String pythonPath;

    @Value("${ocr.script.path}")
    private String scriptPath;

    public String runOcrScript(String imagePath) throws IOException, InterruptedException {
        String scriptPath = Paths.get(this.scriptPath).toAbsolutePath().toString();

        ProcessBuilder builder = new ProcessBuilder(pythonPath, scriptPath, imagePath);
        builder.redirectErrorStream(true);

        Process process = builder.start();

        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            output = result.toString();
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python script exited with code " + exitCode + ". Output: \n" + output);
        }

        return output.trim();
    }

}
