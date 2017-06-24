import java.io.File;
import java.util.HashMap;

/**
 *
 * 文件上传下载测试
 */
public class FileUtilTest {

    public int postFileTest() {

        String filePath = "E:\\test.jpg";
        String upLoadURI = "http://XXXXXXXXXXXXXXXXXXXX/file/put";
        File file = new File(filePath);

        String param1 = "1";
        String param2 = "2";
        String param3 = "3";

        HashMap param = new HashMap();
        param.put("param1", param1);
        param.put("param2", param2);
        param.put("param3", param3);

        HashMap paramFile = new HashMap();
        paramFile.put("file", new File(filePath));

        String result = HttpFileUtil.uploadFile(upLoadURI, param, paramFile);
        System.out.print(result);

        return 0;
    }


    public int getFileTest() {

        String param1 = "1";
        String param2 = "2";
        String param3 = "3";

        HashMap param = new HashMap();
        param.put("param1", param1);
        param.put("param2", param2);
        param.put("param3", param3);
        String url = "http://xxx/test/.jpg";
        //POST 方式下载
        // int  result = HttpFileUtil.postDownloadFile(url , param ,"E:\\test2.jpg");
        //GET方式下载
        int result = HttpFileUtil.getDownloadFile("https://timgsa.baidu.com/timg?image&quality=80&size=b10000_10000&sec=1498285613&di=256aef583974374465b6d5c541af99ac&src=http://image48.360doc.com/DownloadImg/2011/12/2310/20235268_46.jpg", null, "E:\\test2.jpg");

        return result;
    }

    public static void main(String[] agrs) {
        FileUtilTest test = new FileUtilTest();
        //test.postFileTest();
        test.getFileTest();

    }
}
