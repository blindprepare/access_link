package cn.edu.fudan.vd.accessibility.entity;

public class LLMInstruction {
    public String path;
    public String activity;
    public String instruction;
    public String type;
    public String remark;
    public String packageName;

    public LLMInstruction(String path, String activity, String type, String remark, String instruction, String packageName) {
        this.path = path;
        this.activity = activity;
        this.type = type;
        this.remark = remark;
        this.instruction = instruction;
        this.packageName = packageName;
    }

}
