package com.mupceet.rxlearning.apps;

import android.support.annotation.Nullable;

import java.util.List;

public class ApplicationsList {

    private static ApplicationsList ourInstance = new ApplicationsList();

    private List<AppInfo> mList;

    private ApplicationsList() {
    }

    public static ApplicationsList getInstance() {
        return ourInstance;
    }

    public @Nullable List<AppInfo> getList() {
        return mList;
    }

    public void setList(List<AppInfo> list) {
        mList = list;
    }
}
