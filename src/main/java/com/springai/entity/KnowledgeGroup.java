package com.springai.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识分组实体，对应一个知识库分组。
 * 一个分组包含多个文档文件，共享同一个查询目标（queryHint）。
 */
public class KnowledgeGroup {
    private String groupId;              // 分组唯一标识（UUID 前 8 位）
    private String groupName;            // 分组名称，如"部队信息"
    private String description;          // 分组描述
    private String queryHint;            // 查询目标，提示 AI 该分组的知识范围
    private List<String> filePaths = new ArrayList<>();  // 分组内文件的绝对路径列表

    public KnowledgeGroup() {}

    public KnowledgeGroup(String groupId, String groupName, String description, String queryHint) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.description = description;
        this.queryHint = queryHint;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getQueryHint() {
        return queryHint;
    }

    public void setQueryHint(String queryHint) {
        this.queryHint = queryHint;
    }

    public List<String> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(List<String> filePaths) {
        this.filePaths = filePaths;
    }
}
