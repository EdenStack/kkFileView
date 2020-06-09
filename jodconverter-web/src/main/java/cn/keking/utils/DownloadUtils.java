package cn.keking.utils;

import cn.keking.config.ConfigConstants;
import cn.keking.hutool.URLUtil;
import cn.keking.model.FileAttribute;
import cn.keking.model.ReturnResponse;
import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

/**
 * @author yudian-it
 */
@Component
public class DownloadUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadUtils.class);

    private String fileDir = ConfigConstants.getFileDir();

    private static volatile MinioClient minioClient;

    @Value("${MINIO_ENDPOINT:}")
    private String minioEndpoint;
    @Value("${MINIO_ACCESS_KEY:}")
    private String minioAccessKey;
    @Value("${MINIO_SECRET_KEY:}")
    private String minioSecretKey;

    /**
     * @param fileAttribute
     * @return
     */
    public ReturnResponse<String> downLoad(FileAttribute fileAttribute, String fileName) {
        String urlAddress = fileAttribute.getDecodedUrl();
        String type = fileAttribute.getSuffix();
        ReturnResponse<String> response = new ReturnResponse<>(0, "下载成功!!!", "");
        URL url;
        try {
            urlAddress = URLUtil.normalize(urlAddress, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        UUID uuid = UUID.randomUUID();
        if (null == fileName) {
            fileName = uuid + "." + type;
        } else { // 文件后缀不一致时，以type为准(针对simText【将类txt文件转为txt】)
            fileName = fileName.replace(fileName.substring(fileName.lastIndexOf(".") + 1), type);
        }
        String realPath = fileDir + fileName;
        File dirFile = new File(fileDir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        try {

            InputStream in = null;

            if (urlAddress.startsWith("minio://")) {
                try {
                    String[] split = urlAddress.split("/");
                    in = this.getMinioClient().getObject(split[2], split[3]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                url = new URL(urlAddress);
                URLConnection connection = url.openConnection();
                connection.setRequestProperty("User-Agent",
                        "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

                in = connection.getInputStream();
            }

            LOGGER.debug(realPath);

            FileOutputStream os = new FileOutputStream(realPath);
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = in.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
            os.close();
            in.close();

            response.setContent(realPath);
            // 同样针对类txt文件，如果成功msg包含的是转换后的文件名
            response.setMsg(fileName);

            // txt转换文件编码为utf8
            if ("txt".equals(type)) {
                convertTextPlainFileCharsetToUtf8(realPath);
            }
            return response;
        } catch (IOException e) {
            LOGGER.error("文件下载失败", e);
            response.setCode(1);
            response.setContent(null);
            if (e instanceof FileNotFoundException) {
                response.setMsg("文件不存在!!!");
            } else {
                response.setMsg(e.getMessage());
            }
            return response;
        }
    }

    /**
     * 转换文本文件编码为utf8
     * 探测源文件编码,探测到编码切不为utf8则进行转码
     *
     * @param filePath 文件路径
     */
    private static void convertTextPlainFileCharsetToUtf8(String filePath) throws IOException {
        File sourceFile = new File(filePath);
        if (sourceFile.exists() && sourceFile.isFile() && sourceFile.canRead()) {
            String encoding = null;
            try {
                FileCharsetDetector.Observer observer = FileCharsetDetector.guessFileEncoding(sourceFile);
                // 为准确探测到编码,不适用猜测的编码
                encoding = observer.isFound() ? observer.getEncoding() : null;
                // 为准确探测到编码,可以考虑使用GBK  大部分文件都是windows系统产生的
            } catch (IOException e) {
                // 编码探测失败,
                e.printStackTrace();
            }
            if (encoding != null && !"UTF-8".equals(encoding)) {
                // 不为utf8,进行转码
                File tmpUtf8File = new File(filePath + ".utf8");
                Writer writer = new OutputStreamWriter(new FileOutputStream(tmpUtf8File), "UTF-8");
                Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFile), encoding));
                char[] buf = new char[1024];
                int read;
                while ((read = reader.read(buf)) > 0) {
                    writer.write(buf, 0, read);
                }
                reader.close();
                writer.close();
                // 删除源文件
                sourceFile.delete();
                // 重命名
                tmpUtf8File.renameTo(sourceFile);
            }
        }
    }

    private MinioClient getMinioClient() throws InvalidPortException, InvalidEndpointException {
        if (minioClient == null) {
            synchronized (MinioClient.class) {
                if (minioClient == null) {
                    minioClient = new MinioClient(this.minioEndpoint,
                            this.minioAccessKey,
                            this.minioSecretKey);
                }
            }
        }
        return minioClient;
    }

}
