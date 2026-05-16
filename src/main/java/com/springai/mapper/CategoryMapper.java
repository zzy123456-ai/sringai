package com.springai.mapper;

import com.springai.pojo.Category;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Insert("insert into category values (#{categoryId},#{categoryName},#{sortNum},now(),now())")
    int insert(Category record);

    @Select("select MAX(sort_num) from category")
    int getMaxSortNum();

    @Select("select category_id,category_name from category")
    List<Category> getAllCategory();


}
