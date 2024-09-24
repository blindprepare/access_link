package cn.edu.fudan.vd.accessibility.enumeration;

public enum OperationType {
    CLICK("点击动作"), TYPE("输入动作"), SELECT("筛选动作"), HUMAN("人为动作"), UNKNOWN("未知动作");

    OperationType(String explain) {
        this.explain = explain;
    }

    public final String explain;

    public static OperationType getOperationType(String explain) {
        switch (explain) {
            case "点击动作":
                return CLICK;
            case "输入动作":
                return TYPE;
            case "筛选动作":
                return SELECT;
            case "人为动作":
                return HUMAN;
            default:
                return UNKNOWN;
        }
    }

}
