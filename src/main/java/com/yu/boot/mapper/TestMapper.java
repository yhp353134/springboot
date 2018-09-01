package com.yu.boot.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Select;


/**
 * 接口数据
 * */
public interface TestMapper {

    
    /**
     * 下面是两种方式查询数据，第一种不需要xml  
     * 第二个方法需要xml文件
     */
    
    
    @Select("SELECT DEALER_ID,DEALER_CODE,DEALER_FULL_NAME  FROM BASE_DEALER")
    public List<Map<String,Object>> selectDealer();
    
    
    public List<Map<String,Object>> selectDealerList();
}
