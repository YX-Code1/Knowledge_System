package com.knowledge.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /** 将某用户的全部通知标记为已读（手写 SQL 需自带逻辑删除条件） */
    @Update("UPDATE notification SET is_read = 1 " +
            "WHERE user_id = #{userId} AND deleted = 0 AND is_read = 0")
    void markAllRead(@Param("userId") Long userId);
}
