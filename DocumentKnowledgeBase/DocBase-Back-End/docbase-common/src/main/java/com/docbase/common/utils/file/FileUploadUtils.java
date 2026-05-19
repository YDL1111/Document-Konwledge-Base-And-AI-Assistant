package com.docbase.common.utils.file;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.docbase.common.config.DocBaseConfig;
import com.docbase.common.constant.Constants;
import com.docbase.common.exception.ApiException;
import com.docbase.common.exception.error.ErrorCode;
import com.docbase.common.exception.error.ErrorCode.Business;
import com.docbase.common.exception.error.ErrorCode.Internal;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

public class FileUploadUtils {

    public static final long MAX_FILE_SIZE = 50 * Constants.MB;
    public static final int MAX_FILE_NAME_LENGTH = 127;

    private static final String[] ALLOWED_EXTENSIONS = {
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rar", "zip", "pdf"
    };

    private FileUploadUtils() {
    }

    public static String upload(String subDir, MultipartFile file) {
        try {
            return upload(subDir, file, ALLOWED_EXTENSIONS);
        } catch (Exception e) {
            throw new ApiException(Business.UPLOAD_FILE_FAILED, e.getMessage());
        }
    }

    public static String upload(String subDir, MultipartFile file, String[] allowedExtension)
        throws IOException {
        isAllowedUpload(file, allowedExtension);
        String fileName = generateFilename(file);
        saveFileToLocal(file, subDir, fileName);
        return getRelativeFileUrl(subDir, fileName);
    }

    static void saveFileToLocal(MultipartFile file, String subDir, String fileName) throws IOException {
        if (StrUtil.isEmpty(subDir) || StrUtil.isEmpty(fileName)) {
            throw new ApiException(Internal.INVALID_PARAMETER, "subDir or fileName");
        }

        File destination = new File(getFileAbsolutePath(subDir, fileName));
        if (!destination.exists() && !destination.getParentFile().exists()) {
            destination.getParentFile().mkdirs();
        }
        file.transferTo(destination);
    }

    static String getRelativeFileUrl(String subDir, String fileName) {
        return StrUtil.format("/{}/{}/{}", Constants.RESOURCE_PREFIX, subDir, fileName);
    }

    static void isAllowedUpload(MultipartFile file, String[] allowedExtension) {
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        int fileNameLength = originalFilename.length();
        if (fileNameLength > MAX_FILE_NAME_LENGTH) {
            throw new ApiException(ErrorCode.Business.UPLOAD_FILE_NAME_EXCEED_MAX_LENGTH, MAX_FILE_NAME_LENGTH);
        }
        if (StrUtil.containsAny(originalFilename, "..", "/", "\\")) {
            throw new ApiException(ErrorCode.Business.UPLOAD_FILE_TYPE_NOT_ALLOWED, "非法文件名");
        }

        long size = file.getSize();
        if (size > MAX_FILE_SIZE) {
            throw new ApiException(ErrorCode.Business.UPLOAD_FILE_SIZE_EXCEED_MAX_SIZE, MAX_FILE_SIZE / Constants.MB);
        }

        String extension = getFileExtension(file);
        if (!isExtensionAllowed(extension, allowedExtension)) {
            throw new ApiException(
                ErrorCode.Business.UPLOAD_FILE_TYPE_NOT_ALLOWED,
                StrUtil.join(",", (Object[]) allowedExtension)
            );
        }
    }

    public static boolean isAllowDownload(String resource) {
        if (StrUtil.isBlank(resource)) {
            return false;
        }
        Path normalizedPath = Paths.get(resource).normalize();
        String normalized = normalizedPath.toString().replace("\\", "/");
        if (normalized.startsWith("../") || normalized.contains("/../") || normalized.startsWith("/")) {
            return false;
        }
        return StrUtil.containsAnyIgnoreCase(FileNameUtil.getSuffix(normalized), ALLOWED_EXTENSIONS);
    }

    static boolean isExtensionAllowed(String extension, String[] allowedExtension) {
        if (allowedExtension == null || allowedExtension.length == 0) {
            return true;
        }
        return StrUtil.containsAnyIgnoreCase(extension, allowedExtension);
    }

    static String getFileExtension(MultipartFile file) {
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (StrUtil.isEmpty(extension)) {
            MimeType mimeType = MimeTypeUtils.parseMimeType(Objects.requireNonNull(file.getContentType()));
            extension = mimeType.getSubtype();
        }
        return extension;
    }

    static String generateFilename(MultipartFile file) {
        return StrUtil.format(
            "{}_{}.{}",
            DateUtil.format(DateUtil.date(), DatePattern.PURE_DATETIME_PATTERN),
            IdUtil.simpleUUID(),
            getFileExtension(file)
        );
    }

    public static HttpHeaders getDownloadHeader(String fileName) {
        String randomFileName = System.currentTimeMillis() + "_" + fileName;
        String fileNameUrlEncoded = URLUtil.encode(randomFileName, CharsetUtil.CHARSET_UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Disposition", String.format("attachment;filename=%s", fileNameUrlEncoded));
        return headers;
    }

    public static String getFileAbsolutePath(String subDir, String fileName) {
        return DocBaseConfig.getFileBaseDir() + File.separator + subDir + File.separator + fileName;
    }
}
