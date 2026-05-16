package com.springai.service;

import com.springai.entity.KnowledgeGroup;

import java.util.List;

/**
 * 知识分组管理服务。
 * 负责分组的增删查改，以及分组内文件的管理。
 * 分组配置持久化到 knowledge-base/groups.json。
 */
public interface KnowledgeGroupService {
    /** 创建分组，自动生成 groupId */
    KnowledgeGroup createGroup(String groupName, String description, String queryHint);
    /** 删除分组 */
    void deleteGroup(String groupId);
    /** 查看所有分组 */
    List<KnowledgeGroup> listGroups();
    /** 查看单个分组详情 */
    KnowledgeGroup getGroup(String groupId);
    /** 向分组添加文件 */
    void addFileToGroup(String groupId, String filePath);
    /** 从分组移除文件 */
    void removeFileFromGroup(String groupId, String filePath);
    /** 更新分组的查询目标提示 */
    void updateQueryHint(String groupId, String queryHint);
}
