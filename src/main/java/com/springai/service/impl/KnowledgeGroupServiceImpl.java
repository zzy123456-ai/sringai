package com.springai.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.springai.entity.KnowledgeGroup;
import com.springai.service.KnowledgeGroupService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 知识分组管理实现。
 * 启动时从 knowledge-base/groups.json 加载已有分组，之后每次变更自动落盘。
 */
@Service
public class KnowledgeGroupServiceImpl implements KnowledgeGroupService {

    @Value("${rag.knowledge-base.path:knowledge-base}")
    private String basePath;

    private final Map<String, KnowledgeGroup> groupMap = new LinkedHashMap<>();
    private Path groupsFile;

    /** 启动时初始化：创建目录，加载已持久化的分组 */
    @PostConstruct
    public void init() throws IOException {
        Path baseDir = Paths.get(basePath);
        if (!Files.exists(baseDir)) {
            Files.createDirectories(baseDir);
        }
        groupsFile = baseDir.resolve("groups.json");
        if (Files.exists(groupsFile)) {
            loadGroups();
        }
    }

    @Override
    public KnowledgeGroup createGroup(String groupName, String description, String queryHint) {
        String groupId = UUID.randomUUID().toString().substring(0, 8);
        KnowledgeGroup group = new KnowledgeGroup(groupId, groupName, description, queryHint);
        groupMap.put(groupId, group);
        saveGroups();
        return group;
    }

    @Override
    public void deleteGroup(String groupId) {
        groupMap.remove(groupId);
        saveGroups();
    }

    @Override
    public List<KnowledgeGroup> listGroups() {
        return new ArrayList<>(groupMap.values());
    }

    @Override
    public KnowledgeGroup getGroup(String groupId) {
        KnowledgeGroup group = groupMap.get(groupId);
        if (group == null) {
            throw new IllegalArgumentException("分组不存在: " + groupId);
        }
        return group;
    }

    @Override
    public void addFileToGroup(String groupId, String filePath) {
        KnowledgeGroup group = getGroup(groupId);
        // 去重：同一文件不重复添加
        if (!group.getFilePaths().contains(filePath)) {
            group.getFilePaths().add(filePath);
            saveGroups();
        }
    }

    @Override
    public void removeFileFromGroup(String groupId, String filePath) {
        KnowledgeGroup group = getGroup(groupId);
        group.getFilePaths().remove(filePath);
        saveGroups();
    }

    @Override
    public void updateQueryHint(String groupId, String queryHint) {
        KnowledgeGroup group = getGroup(groupId);
        group.setQueryHint(queryHint);
        saveGroups();
    }

    /** 从 JSON 文件反序列化分组列表 */
    private void loadGroups() throws IOException {
        String json = Files.readString(groupsFile);
        JSONArray arr = JSON.parseArray(json);
        for (int i = 0; i < arr.size(); i++) {
            JSONObject obj = arr.getJSONObject(i);
            KnowledgeGroup group = new KnowledgeGroup();
            group.setGroupId(obj.getString("groupId"));
            group.setGroupName(obj.getString("groupName"));
            group.setDescription(obj.getString("description"));
            group.setQueryHint(obj.getString("queryHint"));
            JSONArray files = obj.getJSONArray("filePaths");
            if (files != null) {
                group.setFilePaths(files.toJavaList(String.class));
            }
            groupMap.put(group.getGroupId(), group);
        }
    }

    /** 将分组列表序列化为格式化的 JSON 写入文件 */
    private void saveGroups() {
        try {
            String json = JSON.toJSONString(new ArrayList<>(groupMap.values()), true);
            Files.writeString(groupsFile, json);
        } catch (IOException e) {
            throw new RuntimeException("保存分组配置失败", e);
        }
    }
}
