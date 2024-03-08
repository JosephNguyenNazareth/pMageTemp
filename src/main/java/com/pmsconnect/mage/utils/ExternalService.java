package com.pmsconnect.mage.utils;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ExternalService {
    public static String runCommand(File whereToRun, List<String> command) throws Exception {
//        System.out.println("Running in: " + whereToRun);
//        System.out.println("Command: " + command);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(whereToRun);
        Process process = builder.start();

        OutputStream outputStream = process.getOutputStream();
        InputStream inputStream = process.getInputStream();

        String result = getOutputFromCommand(inputStream);
        boolean isFinished = process.waitFor(30, TimeUnit.SECONDS);
        outputStream.flush();
        outputStream.close();

        if(!isFinished)
            process.destroyForcibly();

        return result;
    }


    public static String getOutputFromCommand(InputStream inputStream) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line);
            }
        }
        return content.toString();
    }
}
