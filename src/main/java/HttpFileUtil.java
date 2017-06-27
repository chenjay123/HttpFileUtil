import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created by chenduo on 2017/6/21.
 * HTTP 上传者文件（或者流），下载文件（或者流）：
 * POST请求一般为传统的application/x-www-form-urlencoded表单，
 * 另一个经常用到的是上传文件用的表单，这种表单的类型为 multipart/form-data;HttpFileUtil采用
 * multipart/form-data 表单上传文件
 * <p>
 * 下载文件支持： get、post 下载文件（或者流）
 */
public class HttpFileUtil {
    private HttpFileUtil() {
    }

    private static final Logger logger = LoggerFactory.getLogger(HttpFileUtil.class);


    /**
     * HTTP 上传文件
     *
     * @param destUrl
     * @param textParts
     * @param fileParts
     * @return
     */
    public static String uploadFile(String destUrl, Map<String, String> textParts, Map<String, File> fileParts) {
        String reStr = null;
        if (destUrl == null || fileParts == null) {
            return null;
        }
        HashMap<String, StreamFileType> streamParts = new HashMap<>();
        Iterator<Map.Entry<String, File>> fileIter = fileParts.entrySet().iterator();
        ArrayList<FileInputStream> fisList = new ArrayList<>();
        try {
            while (fileIter.hasNext()) {
                Map.Entry<String, File> entry = fileIter.next();
                String fileName = entry.getKey();
                File file = entry.getValue();
                FileInputStream inPutStream = new FileInputStream(file);
                fisList.add(inPutStream);
                StreamFileType streamFile = new StreamFileType(inPutStream, file.getName());
                streamParts.put(fileName, streamFile);
            }
            reStr = uploadStream(destUrl, textParts, streamParts);
        } catch (FileNotFoundException e) {
            logger.error("上传文件异常：{}| {}", e, e.getMessage());
        } finally {
            // 关闭文件流。
            for (FileInputStream fis : fisList) {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        logger.error("上传文件异常：{}|{}", e, e.getMessage());
                    }
                }
            }
        }
        return reStr;
    }

    /**
     * HTTP上传二进制流
     *
     * @param destUrl
     * @param textParts
     * @param streamParts
     * @return
     */

    public static String uploadStream(String destUrl, Map<String, String> textParts, Map<String, StreamFileType> streamParts) {
        int timeOut = 3000;
        String reStr = null;
        if (destUrl == null || streamParts == null) {
            return null;
        }
        //1、创建HttpClient
        CloseableHttpClient client = HttpClients.createDefault();
        try {
            HttpPost post = new HttpPost(destUrl);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            //2、设置 multipart/form-data text表单
            if (textParts != null) {
                Iterator<Map.Entry<String, String>> iter = textParts.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, String> entry = iter.next();
                    String paramName = entry.getKey();
                    String paramValue = entry.getValue();
                    builder.addTextBody(paramName, paramValue, ContentType.DEFAULT_TEXT);
                }
            }

            //3、设置 multipart/form-data 文件流表单
            Iterator<Map.Entry<String, StreamFileType>> fileIter = streamParts.entrySet().iterator();
            while (fileIter.hasNext()) {
                Map.Entry<String, StreamFileType> entry = fileIter.next();
                String name = entry.getKey();
                StreamFileType inputStreamFile = entry.getValue();
                builder.addBinaryBody(name, inputStreamFile.getInStream(), ContentType.DEFAULT_BINARY, inputStreamFile.getFileName());
            }
            HttpEntity reqEntity = builder.build();
            // 4、设置POST请求实体
            post.setEntity(reqEntity);

            // 5、设置超时时间
            RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(timeOut)
                    .setConnectTimeout(timeOut).setSocketTimeout(timeOut).build();
            post.setConfig(requestConfig);
            // 6、得到response
            HttpResponse response = client.execute(post);
            // 7、得到 http status code
            int resStatu = response.getStatusLine().getStatusCode();

            if (resStatu == HttpStatus.SC_OK) {
                // 8、get result data
                HttpEntity entity = response.getEntity();
                reStr = EntityUtils.toString(entity);
                logger.info("{} : resStatu is {}", destUrl, resStatu);
            } else {
                logger.error("{} : resStatu is {}", destUrl, resStatu);
                throw new Exception("resStatu is" + resStatu);
            }
        } catch (Exception e) {
            logger.error("uploadFile异常 :{}", e);

        } finally {
            try {
                // 9、关闭HttpClient
                client.close();
            } catch (Exception e) {
                logger.error("uploadFile|关闭HttpClient :{}", e);
            }
        }
        return reStr;
    }


    /**
     * HTTP 下载流
     *
     * @param url
     * @param params
     * @param outPut
     * @return
     */
    public static String getDownloadStream(String url, Map<String, String> params, OutputStream outPut) {
        String fileType = null;
        InputStream in = null;
        if (url == null || outPut == null) {
            return null;
        }
        // 生成一个httpclient对象
        CloseableHttpClient client = HttpClients.createDefault();
        try {
            HttpGet httpget = new HttpGet(url);
            if (params != null) {
                Iterator<Map.Entry<String, String>> iter = params.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, String> entry = iter.next();
                    String paramName = entry.getKey();
                    String paramValue = entry.getValue();
                    httpget.addHeader(paramName, paramValue);
                }
            }

            HttpResponse response = client.execute(httpget);
            HttpEntity entity = response.getEntity();
            in = entity.getContent();
            Header[] typeHeadrer = response.getHeaders("Content-Type");
            String contentTypes = typeHeadrer[0].getValue();
            String temp = contentTypes.toUpperCase();
            if (temp.indexOf("HTML") == -1 && temp.indexOf("TEXT") == -1 && temp.indexOf("JSON") == -1) {
                int len = -1;
                byte[] tmp = new byte[1024];
                while ((len = in.read(tmp)) != -1) {
                    outPut.write(tmp, 0, len);
                }
                outPut.flush();
                fileType = contentTypes;
            }
        } catch (Exception e) {
            logger.error("getDownloadStream异常 :{}", e);
        } finally {
            // 关闭文件流。
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                logger.error("getDownloadStream fout关闭异常 :{}", e);
            }
            // 关闭客户端
            try {
                if (client != null) {

                    client.close();
                }
            } catch (IOException e) {
                logger.error("downloadFile client 关闭异常 :{}", e);
            }
        }
        return fileType;
    }


    /**
     * HTTP POST下载流
     *
     * @param url
     * @param params
     * @param outPut
     * @return
     */
    public static String postDownloadStream(String url, Map<String, String> params, OutputStream outPut) {
        String fileType = null;
        InputStream in = null;
        if (url == null || outPut == null) {
            return null;
        }
        // 生成一个httpclient对象
        CloseableHttpClient client = HttpClients.createDefault();
        try {
            HttpPost post = new HttpPost(url);
            EntityBuilder builder = EntityBuilder.create();
            if (params != null) {
                Iterator<Map.Entry<String, String>> iter = params.entrySet().iterator();
                List pairs = new ArrayList<NameValuePair>();
                while (iter.hasNext()) {
                    Map.Entry<String, String> entry = iter.next();
                    String paramName = entry.getKey();
                    String paramValue = entry.getValue();
                    NameValuePair pair = new BasicNameValuePair(paramName, paramValue);
                    pairs.add(pair);
                }
                builder.setParameters(pairs);
            }
            HttpEntity reqEntity = builder.build();
            // 设置POST请求实体
            post.setEntity(reqEntity);
            HttpResponse response = client.execute(post);
            HttpEntity entity = response.getEntity();
            in = entity.getContent();
            Header[] typeHeadrer = response.getHeaders("Content-Type");
            String contentTypes = typeHeadrer[0].getValue();
            String temp = contentTypes.toUpperCase();
            if (temp.indexOf("HTML") == -1 && temp.indexOf("TEXT") == -1 && temp.indexOf("JSON") == -1) {
                // 不为文本类型时，就写流
                int len = -1;
                byte[] tmp = new byte[1024];
                while ((len = in.read(tmp)) != -1) {
                    outPut.write(tmp, 0, len);
                }
                outPut.flush();
                fileType = contentTypes;
            }

        } catch (Exception e) {
            logger.error("postDownloadStream 异常 :{}", e);
        } finally {
            // 关闭文件流。
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                logger.error("postDownloadStream |fout关闭异常 :{}", e);
            }
            // 关闭客户端
            try {
                if (client != null) {

                    client.close();
                }
            } catch (IOException e) {
                logger.error("downloadFile client 关闭异常 :{}", e);
            }
        }
        // 返回文件类型
        return fileType;
    }

    /**
     * HTTP POST下载文件
     *
     * @param url
     * @param params
     * @param destFileName
     * @return
     */
    public static int postDownloadFile(String url, Map<String, String> params, String destFileName) {
        int rte = -1;
        String fileType = null;
        if (url == null || destFileName == null) {
            return -2;
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        fileType = postDownloadStream(url, params, bos);
        if (fileType != null) {
            String[] arrs = fileType.split("/");
            StringBuilder buf = new StringBuilder(destFileName);
            if (arrs[1] != null) {
                buf.append(".").append(arrs[1]);
            }
            File file = new File(buf.toString());
            try (FileOutputStream fout = new FileOutputStream(file)) {
                fout.write(bos.toByteArray());
                rte = 0;
            } catch (Exception e) {
                logger.error("downloadFile异常 :{}", e);
            }
            try {
                bos.close();
            } catch (IOException e) {
                logger.error("postDownloadFile 异常 :{}", e);
            }
        }
        return rte;
    }


    /**
     * HTTP 下载文件
     *
     * @param url
     * @param params
     * @param destFileName
     * @return
     */
    public static int getDownloadFile(String url, Map<String, String> params, String destFileName) {
        String fileType = null;
        int ret = -1;
        if (url == null || destFileName == null) {
            return -2;
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        fileType = getDownloadStream(url, params, bos);
        if (fileType != null) {
            String[] arrs = fileType.split("/");
            StringBuilder buf = new StringBuilder(destFileName);
            if (arrs[1] != null) {
                buf.append(".").append(arrs[1]);
            }
            File file = new File(buf.toString());
            try (FileOutputStream fout = new FileOutputStream(file);) {
                fout.write(bos.toByteArray());
                ret = 0;
            } catch (IOException e) {
                logger.error("getDownloadFile 下载异常{}", e);
            }
        }
        try {
            bos.close();
        } catch (Exception e) {
            logger.error("downloadFile异常 :{}", e);
        }
        return ret;
    }


    public static class StreamFileType {
        private InputStream inStream;
        private String FileName;

        public StreamFileType(InputStream inStream, String fileName) {
            this.inStream = inStream;
            this.FileName = fileName;
        }

        public InputStream getInStream() {
            return inStream;
        }

        public void setInStream(InputStream inStream) {
            this.inStream = inStream;
        }

        public String getFileName() {
            return FileName;
        }

        public void setFileName(String fileName) {
            FileName = fileName;
        }
    }

}

