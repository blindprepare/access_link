package cn.edu.fudan.vd.accessibility.entity;

import java.util.List;

public class PageInfo {
    private String pageName;
    private String page;
    private String packageName;
    private List<Operation> actionList;

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public List<Operation> getActionList() {
        return actionList;
    }

    public void setActionList(List<Operation> actionList) {
        this.actionList = actionList;
    }
}
