package com.yu.boot.controller;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.yu.boot.model.Student;
import com.yu.boot.service.RedisService;
import com.yu.boot.service.TestService;

@Controller
@RequestMapping(value = "/test/")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private TestService testService;

    //redis
    @Autowired
    private RedisService redisService;
    
    //国际化
    @Autowired
    private MessageSource messageSource;
    
    @RequestMapping("list")
    @ResponseBody
    public List<Student> getStus() {
        logger.info("从数据库读取Student集合");
        return this.testService.getList();
    }

    @RequestMapping("dealer1")
    @ResponseBody
    public List<Map<String, Object>> selectDealer() {
        return this.testService.selectDealer();
    }

    /****
     *ehcache缓存的方式：测试第二次和第一次相隔多少毫秒
     * 在service层添加注解，cacheName是ehcache里面配置的name
     * @Cacheable(value = "cacheName")
        public String selectCaches() {
        }
        因为有两种缓存机制，所以只能开一种
     * */
    /*@RequestMapping("ehcache")
    @ResponseBody
    public String selectDealerList() {
        Long startTime = System.currentTimeMillis();
        String selectDealerList = this.testService.selectCaches();
        System.out.println("耗时:" + (System.currentTimeMillis() - startTime));
        return  "耗时:" +(System.currentTimeMillis() - startTime)+"毫秒，数据为<br/>："+selectDealerList;
    }*/

    @RequestMapping("redis1")
    @ResponseBody
    public String getRedisName() {
        String a = this.redisService.get("hai").toString();
        return a;
    }

    @RequestMapping("redis2")
    @ResponseBody
    public String setRedisName() {
        Object b = this.redisService.del("pf1", "pf2");
        return b.toString();
    }

    @RequestMapping("demo")
    public String yu(HttpServletRequest request,ModelMap map,String lang) {
        //国际化的配置
        Locale locale = LocaleContextHolder.getLocale();
        map.addAttribute("title", messageSource.getMessage("title", null, locale));
        map.addAttribute("host", "主机地址");
        map.addAttribute("html", "This is an &lt;em&gt;HTML&lt;/em&gt; text. &lt;b&gt;Enjoy yourself!&lt;/b&gt;");
        map.addAttribute("name", "张三");
        map.addAttribute("age", "3");
        map.addAttribute("num", 4);
        map.addAttribute("gendar", "女");
        map.addAttribute("dealerList", this.testService.selectDealerList());
        map.addAttribute("ceshiDate", "2017-07-09 12:09:45");
        return "test/demo";
    }
    
    /**
     * 表单提交
     * */
    @RequestMapping("forms")
    @ResponseBody
    public String submitForm(HttpServletRequest rq) {
       String name= rq.getParameter("name");
       String age =  rq.getParameter("age");
        return name+age;
    }

}
