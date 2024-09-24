package results;

import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.text.MessageFormat;

public class ResultAnalyze {
    public static void main(String[] args) throws IOException {
        analyze();
    }

    private static void analyze() throws IOException {
        File folder = new File("src/results/data");
        FileWriter fileWriter = new FileWriter("src/results/result.json");
        File[] files = folder.listFiles();
        int totalTaskInFolder = 0;
        int totalCorrectTaskInFolder = 0;
        int totalTypeTaskInFolder = 0;
        int totalCorrectTypeTaskInFolder = 0;
        int totalInstructionInFolder = 0;
        int totalCorrectInstructionInFolder = 0;
        int totalTypeInstructionInFolder = 0;
        int totalCorrectTypeInstructionInFolder = 0;
        int totalSelectInstructionInFolder = 0;
        int totalCorrectSelectInstructionInFolder = 0;
        int totalClickInstructionInFolder = 0;
        int totalCorrectClickInstructionInFolder = 0;
        int totalHumanInstructionInFolder = 0;
        int totalCorrectHumanInstructionInFolder = 0;
        for (File file : files) {
            StringBuilder sb = new StringBuilder();
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line;
            boolean startAnalyze = false;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Total tasks:")) {
                    startAnalyze = true;
                }
                if (startAnalyze) {
                    break;
                }
            }
            String[] tasksInfo = line.substring("Total tasks:".length()).split("/");
            int correctTask = Integer.parseInt(tasksInfo[0]);
            int totalTask = Integer.parseInt(tasksInfo[1]);
            totalTaskInFolder += totalTask;
            totalCorrectTaskInFolder += correctTask;
            String accuracyInfo = br.readLine();
            String[] typeTaskInfo = br.readLine().substring("Total type tasks:".length()).split("/");
            int correctTypeTask = Integer.parseInt(typeTaskInfo[0]);
            int totalTypeTask = Integer.parseInt(typeTaskInfo[1]);
            totalTypeTaskInFolder += totalTypeTask;
            totalCorrectTypeTaskInFolder += correctTypeTask;
            String typeTaskAcc = br.readLine();
            String[] totalCostInfo = br.readLine().substring("Total Cost:".length()).split("/");
            double totalInstructionAvgCost = Double.parseDouble(totalCostInfo[0]);
            double totalInstructionCost = Double.parseDouble(totalCostInfo[1]);
            double totalInstructionAvgCostDeviTaskNum = Double.parseDouble(totalCostInfo[2]);
            double totalInstructionCostDeviTaskNum = Double.parseDouble(totalCostInfo[3]);
            String[] correctCostInfo = br.readLine().substring("Total Correct Cost:".length()).split("/");
            double correctInstructionAvgCost = Double.parseDouble(correctCostInfo[0]);
            double correctInstructionCost = Double.parseDouble(correctCostInfo[1]);
            double correctInstructionAvgCostDeviCorrectTaskNum = Double.parseDouble(correctCostInfo[2]);
            double correctInstructionCostDeviCorrectTaskNum = Double.parseDouble(correctCostInfo[3]);
            String[] wrongCostInfo = br.readLine().substring("Total Wrong Cost:".length()).split("/");
            double wrongInstructionAvgCost = Double.parseDouble(wrongCostInfo[0]);
            double wrongInstructionCost = Double.parseDouble(wrongCostInfo[1]);
            double wrongInstructionAvgCostDeviCorrectTaskNum = Double.parseDouble(wrongCostInfo[2]);
            double wrongInstructionCostDeviCorrectTaskNum = Double.parseDouble(wrongCostInfo[3]);
            String totalInstructInfoStr = br.readLine();
            String[] totalInstructInfo = totalInstructInfoStr.substring("Total instruct:".length(), totalInstructInfoStr.indexOf(";")).split("/");
            int totalCorrectInst = Integer.parseInt(totalInstructInfo[0]);
            int totalInst = Integer.parseInt(totalInstructInfo[1]);
            totalInstructionInFolder += totalInst;
            totalCorrectInstructionInFolder += totalCorrectInst;
            String typeInstructionInfoStr = br.readLine();
            String[] typeInstructInfo = typeInstructionInfoStr.substring("type instruct:".length(), typeInstructionInfoStr.indexOf(";")).split("/");
            int typeCorrectInst = Integer.parseInt(typeInstructInfo[0]);
            int typeInst = Integer.parseInt(typeInstructInfo[1]);
            totalTypeInstructionInFolder += typeInst;
            totalCorrectTypeInstructionInFolder += typeCorrectInst;
            String selectInstructionInfoStr = br.readLine();
            String[] selectInstructInfo = selectInstructionInfoStr.substring("select instruct:".length(), selectInstructionInfoStr.indexOf(";")).split("/");
            int selectCorrectInst = Integer.parseInt(selectInstructInfo[0]);
            int selectInst = Integer.parseInt(typeInstructInfo[1]);
            totalSelectInstructionInFolder += selectInst;
            totalCorrectSelectInstructionInFolder += selectCorrectInst;
            String clickInstructionInfoStr = br.readLine();
            String[] clickInstructInfo = clickInstructionInfoStr.substring("click instruct:".length(), clickInstructionInfoStr.indexOf(";")).split("/");
            int clickCorrectInst = Integer.parseInt(clickInstructInfo[0]);
            int clickInst = Integer.parseInt(clickInstructInfo[1]);
            totalClickInstructionInFolder += clickInst;
            totalCorrectClickInstructionInFolder += clickCorrectInst;
            String humanInstructionInfoStr = br.readLine();
            String[] humanInstructInfo = humanInstructionInfoStr.substring("human instruct:".length(), humanInstructionInfoStr.indexOf(";")).split("/");
            int humanCorrectInst = Integer.parseInt(humanInstructInfo[0]);
            int humanInst = Integer.parseInt(humanInstructInfo[1]);
            totalHumanInstructionInFolder += humanInst;
            totalCorrectHumanInstructionInFolder += humanCorrectInst;
            br.close();
            fr.close();
        }
        double accuracy;
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("任务总数", totalTaskInFolder);
        jsonObject.put("任务正确总数", totalCorrectTaskInFolder);
        accuracy = ((double) totalCorrectTaskInFolder) / ((double) totalTaskInFolder);
        jsonObject.put("任务完成率", accuracy);
        jsonObject.put("输入任务总数", totalTypeTaskInFolder);
        jsonObject.put("输入任务正确总数", totalCorrectTypeTaskInFolder);
        accuracy = ((double) totalCorrectTypeTaskInFolder) / ((double) totalTypeTaskInFolder);
        jsonObject.put("输入任务完成率", accuracy);
        jsonObject.put("指令总数", totalInstructionInFolder);
        jsonObject.put("指令正确总数", totalCorrectInstructionInFolder);
        accuracy = ((double) totalCorrectInstructionInFolder) / ((double) totalInstructionInFolder);
        jsonObject.put("指令精度", accuracy);
        jsonObject.put("输入指令总数", totalTypeInstructionInFolder);
        jsonObject.put("输入指令正确总数", totalCorrectTypeInstructionInFolder);
        accuracy = ((double) totalCorrectTypeInstructionInFolder) / ((double) totalTypeInstructionInFolder);
        jsonObject.put("输入指令精度", accuracy);
        jsonObject.put("筛选指令总数", totalSelectInstructionInFolder);
        jsonObject.put("筛选指令正确总数", totalCorrectSelectInstructionInFolder);
        accuracy = ((double) totalCorrectSelectInstructionInFolder) / ((double) totalSelectInstructionInFolder);
        jsonObject.put("筛选指令精度", accuracy);
        jsonObject.put("点击指令总数", totalClickInstructionInFolder);
        jsonObject.put("点击指令正确总数", totalCorrectClickInstructionInFolder);
        accuracy = ((double) totalCorrectClickInstructionInFolder) / ((double) totalClickInstructionInFolder);
        jsonObject.put("点击指令精度", accuracy);
        jsonObject.put("人为指令总数", totalHumanInstructionInFolder);
        jsonObject.put("人为指令正确总数", totalCorrectHumanInstructionInFolder);
        accuracy = ((double) totalCorrectHumanInstructionInFolder) / ((double) totalHumanInstructionInFolder);
        jsonObject.put("人为指令精度", accuracy);
        fileWriter.write(jsonObject.toJSONString());
        fileWriter.close();
        System.out.println("---------");
        System.out.println(MessageFormat.format("任务总数:{0}", totalTaskInFolder));
        System.out.println(MessageFormat.format("任务正确总数:{0}", totalCorrectTaskInFolder));
        accuracy = ((double) totalCorrectTaskInFolder) / ((double) totalTaskInFolder);
        System.out.println(MessageFormat.format("任务完成率:{0}", accuracy));
        System.out.println("---------");
        System.out.println(MessageFormat.format("输入任务总数:{0}", totalTypeTaskInFolder));
        System.out.println(MessageFormat.format("输入任务正确总数:{0}", totalCorrectTypeTaskInFolder));
        accuracy = ((double) totalCorrectTypeTaskInFolder) / ((double) totalTypeTaskInFolder);
        System.out.println(MessageFormat.format("输入任务完成率:{0}", accuracy));
        System.out.println("---------");
        System.out.println(MessageFormat.format("指令总数:{0}", totalInstructionInFolder));
        System.out.println(MessageFormat.format("指令正确总数:{0}", totalCorrectInstructionInFolder));
        accuracy = ((double) totalCorrectInstructionInFolder) / ((double) totalInstructionInFolder);
        System.out.println(MessageFormat.format("指令精度:{0}", accuracy));
        System.out.println("---------");
        System.out.println(MessageFormat.format("输入指令总数:{0}", totalTypeInstructionInFolder));
        System.out.println(MessageFormat.format("输入指令正确总数:{0}", totalCorrectTypeInstructionInFolder));
        accuracy = ((double) totalCorrectTypeInstructionInFolder) / ((double) totalTypeInstructionInFolder);
        System.out.println(MessageFormat.format("输入指令精度:{0}", accuracy));
        System.out.println("---------");
        System.out.println(MessageFormat.format("筛选指令总数:{0}", totalSelectInstructionInFolder));
        System.out.println(MessageFormat.format("筛选指令正确总数:{0}", totalCorrectSelectInstructionInFolder));
        accuracy = ((double) totalCorrectSelectInstructionInFolder) / ((double) totalSelectInstructionInFolder);
        System.out.println(MessageFormat.format("筛选指令精度:{0}", accuracy));
        System.out.println("---------");
        System.out.println(MessageFormat.format("点击指令总数:{0}", totalClickInstructionInFolder));
        System.out.println(MessageFormat.format("点击指令正确总数:{0}", totalCorrectClickInstructionInFolder));
        accuracy = ((double) totalCorrectClickInstructionInFolder) / ((double) totalClickInstructionInFolder);
        System.out.println(MessageFormat.format("点击指令精度:{0}", accuracy));
        System.out.println("---------");
        System.out.println(MessageFormat.format("人为指令总数:{0}", totalHumanInstructionInFolder));
        System.out.println(MessageFormat.format("人为指令正确总数:{0}", totalCorrectHumanInstructionInFolder));
        accuracy = ((double) totalCorrectHumanInstructionInFolder) / ((double) totalHumanInstructionInFolder);
        System.out.println(MessageFormat.format("人为指令精度:{0}", accuracy));
    }
}
