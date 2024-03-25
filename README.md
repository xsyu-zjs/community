# 登录、退出

## 访问登录页面

点击顶部区域链接，打开登录页面

## 登录

验证账号、密码、验证码

成功时，生成登录凭证，发送给客户端

失败时，跳转回登录页

## 退出

将登录凭证修改为失效状态

跳转至网站首页

# 显示登录信息

## 拦截器

定义拦截器，实现HandlerInterceptor

配置拦截器，指定拦截、排除的路径

## 拦截器应用

请求开始时，查询登录用户

在本次请求，持有用户数据

在模板视图，显示用户数据

在请求结束时，清理用户数据

# 7.账号设置

## 上传文件

请求：必须是post请求

表单：enctype=“multipart/form-data”

Spring MVC：通过MultipartFile处理上传文件

## 开发步骤

访问账号设置页面

上传头像

获取头像

# 8.检查登录状态

## 使用拦截器

在方法前标注自定义注解

拦截所有请求，只处理带有注解的方法

## 自定义注解

常用的元注解：

@Target声明自定义的注解写在哪个位置

@Retention声明有效时间

@Document文档是否携带

@Inherited继承

如何读取注解：

```java
Method.getDeclaredAnnotations()
Method.getAnnotation(Class<T> annotionClass)
```

# 过滤敏感词

## 前缀树（Trie、字典树、查找树）

查找效率高、消耗内存大

用于字符串检索、词频统计、字符串排序等

## 敏感词过滤器

定义前缀树

根据敏感词，初始化前缀树

编写过滤敏感词的方法

# 发布帖子

## AJAX

Asynchronous JavaScript and XML

异步的 Javascript 与 XML 不是技术 而是名词

可以将增量更新展现到页面上，不需要刷新

虽然X代表XML，但目前的JSON使用比XML更加普遍

## 实践

采用AJAX，实现发布帖子的功能

# 点赞

点赞：对帖子、评论点赞、第一次点赞，第二次取消

首页点赞数量：统计首页点赞数量

详情页点赞数量：统计点赞数量、显示点赞状态

要考虑性能问题

```java
@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    // 点赞
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);

                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);

                operations.multi();

                if (isMember) {
                    operations.opsForSet().remove(entityLikeKey, userId);
                    operations.opsForValue().decrement(userLikeKey);
                } else {
                    operations.opsForSet().add(entityLikeKey, userId);
                    operations.opsForValue().increment(userLikeKey);
                }

                return operations.exec();
            }
        });
    }

    // 查询某实体点赞的数量
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    // 查询某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    // 查询某个用户获得的赞
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count.intValue();
    }

}

```

## 我收到的赞

![image-20240311134731634](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20240311134731634.png)

## 关注、取消关注





# kafka实现系统通知功能（被评论、点赞）

## Spring整合kafka

![image-20240309165247847](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20240309165247847.png)

## 启动kafka和elasticsearch

进入kafka目录下

启动zookeeper

```
bin\windows\zookeeper-server-start.bat config\zookeeper.properties
```

启动kafka

```
bin\windows\kafka-server-start.bat config\server.properties
```

启动elasticsearch

```
elasticsearch
```

## 发送系统通知

![image-20240310193953507](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20240310193953507.png)



Event是一个消息对象

评论后，查询是评论帖子还是评论评论，set好对应的event然后丢到消息队列中kafkatemplate.send(topic,data)

## 显示系统通知

![image-20240310204419361](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20240310204419361.png)

访问`/notice/detail/{topic}`后设置为已读，即将status从0设置为1

```java
    public int readMessage(List<Integer> ids) {
        return messageMapper.updateStatus(ids, 1);
    }
```

用拦截器处理最上方未读消息的数量

```java
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
            int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
            modelAndView.addObject("allUnreadCount", letterUnreadCount + noticeUnreadCount);
        }
```



# 任务执行和调度

![image-20240309192359982](C:\Users\Lenovo\AppData\Roaming\Typora\typora-user-images\image-20240309192359982.png)

quartz将参数存储在服务器中

