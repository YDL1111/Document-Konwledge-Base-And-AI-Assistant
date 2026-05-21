package com.docbase.admin.config;


import com.docbase.admin.DocBaseAdminApplication;
import com.docbase.common.config.DocBaseConfig;
import com.docbase.common.constant.Constants.UploadSubDir;
import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = DocBaseAdminApplication.class)
@RunWith(SpringRunner.class)
public class DocBaseConfigTest {

    @Autowired
    private DocBaseConfig config;

    @Test
    public void testConfig() {
        String fileBaseDir = "D:\\agileboot\\profile";

        Assertions.assertEquals("DocBase", config.getName());
        Assertions.assertEquals("1.8.0", config.getVersion());
        Assertions.assertEquals("2022", config.getCopyrightYear());
        Assertions.assertFalse(config.isDemoEnabled());
        Assertions.assertEquals(fileBaseDir, DocBaseConfig.getFileBaseDir());
        Assertions.assertFalse(DocBaseConfig.isAddressEnabled());
        Assertions.assertEquals("math", DocBaseConfig.getCaptchaType());
        Assertions.assertEquals("math", DocBaseConfig.getCaptchaType());
        Assertions.assertEquals(fileBaseDir + "\\import",
            DocBaseConfig.getFileBaseDir() + File.separator + UploadSubDir.IMPORT_PATH);
        Assertions.assertEquals(fileBaseDir + "\\avatar",
            DocBaseConfig.getFileBaseDir() + File.separator + UploadSubDir.AVATAR_PATH);
        Assertions.assertEquals(fileBaseDir + "\\download",
            DocBaseConfig.getFileBaseDir() + File.separator + UploadSubDir.DOWNLOAD_PATH);
        Assertions.assertEquals(fileBaseDir + "\\upload",
            DocBaseConfig.getFileBaseDir() + File.separator + UploadSubDir.UPLOAD_PATH);
    }

}
