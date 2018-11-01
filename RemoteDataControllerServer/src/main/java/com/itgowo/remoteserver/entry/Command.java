package com.itgowo.remoteserver.entry;

import com.alibaba.fastjson.JSON;

import java.util.List;

public class Command {
    /**
     * 操作动作
     */
    private String action;
    /**
     * 数据库名
     */
    private String database;
    private String spFileName;

    /**
     * 数据表明
     */
    private String tableName;
    /**
     * 携带的数据
     */
    private String data;
    /**
     *
     */
    private List<RowDataCommand> RowDataCommands;
    /**
     * 非必须
     */
    private Integer pageIndex;
    /**
     * 非必须
     */
    private Integer pageSize;
    private String clientId;
    private String remoteUrl;

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public Command setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
        return this;
    }

    public String getClientId() {
        return clientId;
    }
    public String toJson(){
        return JSON.toJSONString(this);
    }
    public Command setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getSpFileName() {
        return spFileName;
    }

    public Command setSpFileName(String mSpFileName) {
        spFileName = mSpFileName;
        return this;
    }

    public Integer getPageIndex() {
        return pageIndex;
    }

    public Command setPageIndex(Integer mPageIndex) {
        pageIndex = mPageIndex;
        return this;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Command setPageSize(Integer mPageSize) {
        pageSize = mPageSize;
        return this;
    }

    public String getTableName() {
        return tableName;
    }

    public Command setTableName(String mTableName) {
        tableName = mTableName;
        return this;
    }

    public List<RowDataCommand> getRowDataCommands() {
        return RowDataCommands;
    }

    public Command setRowDataCommands(List<RowDataCommand> mRowDataCommands) {
        RowDataCommands = mRowDataCommands;
        return this;
    }

    public String getAction() {
        return action;
    }

    public Command setAction(String mAction) {
        action = mAction;
        return this;
    }

    public String getDatabase() {
        return database;
    }

    public Command setDatabase(String mDatabase) {
        database = mDatabase;
        return this;
    }

    public String getData() {
        return data;
    }

    public Command setData(String mData) {
        data = mData;
        return this;
    }

    public static class RowDataCommand {
        public String title;
        public boolean isPrimary;
        public String dataType;
        public String value;

        public String getTitle() {
            return title;
        }

        public RowDataCommand setTitle(String mTitle) {
            title = mTitle;
            return this;
        }

        public boolean isPrimary() {
            return isPrimary;
        }

        public RowDataCommand setPrimary(boolean mPrimary) {
            isPrimary = mPrimary;
            return this;
        }

        public String getDataType() {
            return dataType;
        }

        public RowDataCommand setDataType(String mDataType) {
            dataType = mDataType;
            return this;
        }

        public String getValue() {
            return value;
        }

        public RowDataCommand setValue(String mValue) {
            value = mValue;
            return this;
        }
    }
}
