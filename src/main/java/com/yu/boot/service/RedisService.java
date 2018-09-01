package com.yu.boot.service;

import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.yu.boot.common.BaseService;

@Service  
public class RedisService extends BaseService{

    @Autowired
    private RedisTemplate redisTemplate;
    
    private static String redisCode = "utf-8";
    
    /**
     * 获取redis value (String)
     */
    //@Cacheable(value = "reportcache", keyGenerator = "wiselyKeyGenerator") 如果需要缓存到内存里面 就需要加上这句话
    public Object get(final String key) {
        return redisTemplate.execute(new RedisCallback() {
            public String doInRedis(RedisConnection connection) throws DataAccessException {
                try {
                    return new String(connection.get(key.getBytes()), redisCode);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return "";
            }
        });
   }
    
    /**
     * 通过key删除 这里可以写无限个key
     */
    public Object del(final String... keys) {
        return redisTemplate.execute(new RedisCallback() {
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                long result = 0;
                for (int i = 0; i < keys.length; i++) {
                    result = connection.del(keys[i].getBytes());
                }
                return result;
            }
        });
    }
    
    
    /**
     * 添加key value (字节)(序列化)
     * liveTime 存活时间
     */
    public void set(final byte[] key, final byte[] value, final long liveTime) {
        redisTemplate.execute(new RedisCallback() {
            public Long doInRedis(RedisConnection connection) throws DataAccessException {
                connection.set(key, value);
                if (liveTime > 0) {
                    connection.expire(key, liveTime);
                }
                return 1L;
            }
        });
    }
    
    /**
     * 检查key是否已经存在
     */
    public Object exists(final String key) {
        return redisTemplate.execute(new RedisCallback() {
            public Boolean doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.exists(key.getBytes());
            }
        });
    } 

}
