package com.springai.service.impl;

import com.springai.mapper.CategoryMapper;
import com.springai.pojo.Category;
import com.springai.service.CategoryToolService;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryToolServiceImpl implements CategoryToolService {

    @Resource
    private CategoryMapper categoryMapper;

    @Tool(description = "查看系统中所有分类的 id 和名称。当用户询问有哪些分类、分类列表、分类数据时调用。")
    public String getAllCategories() {
        List<Category> categoryList = categoryMapper.getAllCategory();

        if (categoryList == null || categoryList.isEmpty()) {
            return "当前没有分类数据。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("当前所有分类如下：\n");

        for (int i = 0; i < categoryList.size(); i++) {
            Category category = categoryList.get(i);
            sb.append(i + 1)
                    .append(". 分类ID：").append(category.getCategoryId())
                    .append("，分类名称：").append(category.getCategoryName())
                    .append("\n");
        }

        return sb.toString();
    }
}
