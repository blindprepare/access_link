import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class InstructionMapperGenerator {
    public static void main(String[] args) throws IOException {
        generate();
    }

    public static Map<String, String> nameMapper = new HashMap<>();
    public static Map<String, String> actionTypeMapper = new HashMap<>();

    static {
        nameMapper.put("Alipay", "支付宝");
        nameMapper.put("Meituan", "美团");
        nameMapper.put("Amap", "高德");
        nameMapper.put("JD", "京东");
        nameMapper.put("QQMail", "QQ邮箱");
        nameMapper.put("QQMusic", "QQ音乐");

        actionTypeMapper.put("人为动作", "human");
        actionTypeMapper.put("点击动作", "click");
        actionTypeMapper.put("输入动作", "type");
        actionTypeMapper.put("筛选动作", "select");
    }

    public static void generate() throws IOException {
        FileWriter fileWriter = new FileWriter("src/databases/instructionMapper.json");
        JSONObject instructionMapper = new JSONObject();
        for (String key : nameMapper.keySet()) {
            String filePath = "src/databases/" + key + "/pages";
            File folder = new File(filePath);
            File[] files = folder.listFiles();
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
                JSONObject page = JSON.parseObject(json);
                String packageName = page.getString("package");
                String activityName = page.getString("page");
                String pageName = page.getString("pageName");
                JSONArray actionList = page.getJSONArray("actionList");
                for (int j = 0; j < actionList.size(); j++) {
                    JSONObject action = actionList.getJSONObject(j);
                    JSONObject instruction = new JSONObject();
                    String instructionKey = String.join("_", nameMapper.get(key), pageName, action.getString("action"), actionTypeMapper.get(action.getString("type")));
                    instruction.put("path", action.getString("path"));
                    instruction.put("page", activityName);
                    instruction.put("package", packageName);
                    instructionMapper.put(instructionKey, instruction);
                }
            }
        }
        fileWriter.write(instructionMapper.toJSONString());
        fileWriter.close();
    }
}
