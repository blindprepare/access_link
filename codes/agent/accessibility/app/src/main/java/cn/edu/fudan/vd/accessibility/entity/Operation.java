package cn.edu.fudan.vd.accessibility.entity;

import cn.edu.fudan.vd.accessibility.enumeration.OperationType;

public class Operation {

    private String path;
    private String page;
    private String pageName;
    private OperationType type;
    private String remark;
    private String nextPageName;
    private String explain;

    public Operation(String path, String page, OperationType type, String remark) {
        this.path = path;
        this.page = page;
        this.type = type;
        this.remark = remark;
    }


    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public OperationType getType() {
        return type;
    }

    public void setType(OperationType type) {
        this.type = type;
    }


    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getNextPageName() {
        return nextPageName;
    }

    public void setNextPageName(String nextPageName) {
        this.nextPageName = nextPageName;
    }

    public String getExplain() {
        return explain;
    }

    public void setExplain(String explain) {
        this.explain = explain;
    }
}
