package com.gpch.grpc.fileservice;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.context.LocalRunningGrpcPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {"grpc.port=0"})
public class FileServiceApplicationTests {

    @LocalRunningGrpcPort
    private int port;

    private FileServiceClient fileServiceClient;

    @Before
    public void setup(){
        fileServiceClient = new FileServiceClient("localhost", port);
    }

    @Test
    public void downloadFile() {

        try {
            String property = "java.io.tmpdir";
            String tmpDir = System.getProperty(property);

            String fileName = "icon.png";
            ByteArrayOutputStream imageOutputStream = fileServiceClient.downloadFie(fileName);
            byte[] bytes = imageOutputStream.toByteArray();

            ImageIcon imageIcon = new ImageIcon(bytes);
            Image image = imageIcon.getImage();
            int width = 912;
            int height = 513;
            BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            Graphics g = bi.getGraphics();
            g.drawImage(image, 0, 0, null);
            File tmpFile = new File(tmpDir + fileName);
            ImageIO.write(bi, "png", tmpFile);
            log.info("File has been downloaded --> " + tmpFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }


}
