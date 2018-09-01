package com.yu.boot.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yu.boot.common.BaseService;
import com.yu.boot.mapper.TestMapper;
import com.yu.boot.model.Student;

@Service
@Transactional
public class TestService extends BaseService{

    //这种注入类直接在javal里面写sql
    @Autowired
    private JdbcTemplate jdbcTemplate;

    //sql在其他地方 有可能是xml里面
    @Autowired
    private TestMapper testMapper;

    public List<Student> getList() {
        String sql = "SELECT DEALER_ID,DEALER_CODE,DEALER_FULL_NAME  FROM BASE_DEALER";
        return (List<Student>) jdbcTemplate.query(sql, new RowMapper<Student>() {
            public Student mapRow(ResultSet rs, int rowNum) throws SQLException {
                Student stu = new Student();
                stu.setId(rs.getLong("DEALER_ID"));
                stu.setName(rs.getString("DEALER_CODE"));
                stu.setSumScore(rs.getString("DEALER_FULL_NAME"));
                return stu;
            }
        });
    }

    @Transactional(rollbackFor = {IllegalArgumentException.class})
    public List<Map<String, Object>> selectDealer() {
        return this.testMapper.selectDealer();
    }

    
    public List<Map<String, Object>> selectDealerList() {
        return this.testMapper.selectDealerList();
    }
    
    
    /****
     *ehcache缓存的方式：测试第二次和第一次相隔多少毫秒
     * 在service层添加注解，cacheName是ehcache里面配置的name
     * @Cacheable(value = "cacheName")
        public String selectCaches() {
        }
     * */
    /*@Cacheable(value = "cacheName")
    public String selectCaches() {
        List<Map<String, Object>> selectDealerList = this.testMapper.selectDealerList();
        if (selectDealerList.size()>0) {
            return selectDealerList.toString();
        } else {
            return "缓存没有数据";
        }
    }*/

}
