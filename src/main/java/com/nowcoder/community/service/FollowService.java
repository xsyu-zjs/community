package com.nowcoder.community.service;

import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;



    public void follow(int userId, int entityType, int entityId) {

        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                operations.multi();

                operations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());

                return operations.exec();
            }
        });
    }

    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                operations.multi();

                operations.opsForZSet().remove(followeeKey, entityId);
                operations.opsForZSet().remove(followerKey, userId);

                return operations.exec();
            }
        });
    }
    /*
    这段代码定义了一个名为unfollow的方法，其目的是从Redis中移除特定的关注关系。
    这个方法接受三个整数参数：userId（用户的ID）、entityType（实体的类型）和entityId（实体的ID）。
    使用RedisTemplate执行命令:
    redisTemplate.execute(new SessionCallback() {
    这里使用RedisTemplate的execute方法来执行一个原生的Redis命令。SessionCallback是一个接口，允许你在一个Redis会话中执行多个操作。
    生成Redis键:
    String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
    String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
    这两行代码调用RedisKeyUtil的静态方法来生成两个Redis键。一个键代表被关注者的关注者列表（followeeKey），另一个键代表关注者的关注列表（followerKey）。
    开始事务:operations.multi();
    使用multi方法开始一个Redis事务。
    这意味着后续的操作会被放入一个队列中，直到调用exec方法时才会被实际执行。
    从两个有序集合中移除元素:
    operations.opsForZSet().remove(followeeKey, entityId);
    operations.opsForZSet().remove(followerKey, userId);
    这两行代码从两个有序集合（ZSet）中移除元素。
    第一个命令从followeeKey（被关注者的关注者列表）中移除entityId，第二个命令从followerKey（关注者的关注列表）中移除userId。
    执行事务:return operations.exec();
    使用exec方法执行前面在事务中排队的所有操作。这个方法返回一个包含所有操作结果的列表。
    结束SessionCallback:
    结束SessionCallback的定义。
    总的来说，这个unfollow方法的作用是：从Redis中移除一个用户（由userId标识）对一个特定类型实体（由entityType标识）的关注（由entityId标识）。
    这通过从两个有序集合中移除对应的元素来实现，这两个有序集合分别代表被关注者的关注者列表和关注者的关注列表。
     */
    // 查询关注的实体的数量
    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    // 查询实体的粉丝的数量
    public long findFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    // 查询当前用户是否已关注该实体
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    // 查询某用户关注的人
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);

        if (targetIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }

        return list;
    }

    // 查询某用户的粉丝
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit) {
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);

        if (targetIds == null) {
            return null;
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }

        return list;
    }

}
