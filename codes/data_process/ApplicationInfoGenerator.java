import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ApplicationInfoGenerator {
    public static void main(String[] args) throws IOException {
        generate();
    }

    public static Map<String, String> nameMapper = new HashMap<>();

    static {
        nameMapper.put("Alipay", "支付宝");
        nameMapper.put("Meituan", "美团");
        nameMapper.put("Amap", "高德");
        nameMapper.put("JD", "京东");
        nameMapper.put("QQMail", "QQ邮箱");
        nameMapper.put("QQMusic", "QQ音乐");
    }

    public static void generate() throws IOException {
        for (String key : nameMapper.keySet()) {
            String filePath = "src/databases/" + key + "/pages";
            File folder = new File(filePath);
            File[] files = folder.listFiles();
            String outputPath = "src/databases/" + key + "/" + nameMapper.get(key) + "应用信息.json";
            FileWriter fileWriter = new FileWriter(outputPath);
            fileWriter.write("{\n" +
                    "  \"pages\": [");
            for (File file : files) {
                System.out.println(file.getName());
                StringBuilder sb = new StringBuilder();
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                br.close();
                fr.close();
                String json = sb.toString();
                fileWriter.write(json);
                fileWriter.write(",\n");
            }
            fileWriter.write("  ]\n" +
                    "}");
            fileWriter.close();
        }
    }
}

