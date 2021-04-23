package com.yann.asmplugin;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class FileUtils {

    public static void getAllFiles(File file, List<File> totalFiles) {
        if (!file.isDirectory()) {
            totalFiles.add(file);
            return;
        }

        File[] files = file.listFiles();
        if (files != null) {
            for(File temp: files) {
                getAllFiles(temp, totalFiles);
            }
        }
    }

    public static byte[] getBytes(File file) throws FileNotFoundException {
        FileInputStream is  = new FileInputStream(file);
        ByteArrayOutputStream answer = new ByteArrayOutputStream();
        byte[] byteBuffer = new byte[8192];

        int nbByteRead;
        try {
            while((nbByteRead = is.read(byteBuffer)) != -1) {
                answer.write(byteBuffer, 0, nbByteRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeWithWarning(is);
        }

        return answer.toByteArray();
    }

    public static void closeWithWarning(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException var2) {
            }
        }

    }
}
