package com.knowledge.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 查询某用户的会话列表：每个会话取最后一条消息（id 自增，MAX(id) 即最新）。
     * 按 (min(sender,receiver), max(sender,receiver)) 对会话去重。
     */
    @Select("SELECT m.* FROM message m JOIN (" +
            "SELECT MAX(id) AS last_id FROM message " +
            "WHERE deleted = 0 AND (sender_id = #{userId} OR receiver_id = #{userId}) " +
            "GROUP BY IF(sender_id < receiver_id, sender_id, receiver_id), " +
            "IF(sender_id < receiver_id, receiver_id, sender_id)" +
            ") t ON m.id = t.last_id ORDER BY m.id DESC")
    List<Message> selectLastMessages(@Param("userId") Long userId);

    /** 统计每个发送者发来且未读的消息数 */
    @Select("SELECT sender_id AS senderId, COUNT(*) AS cnt FROM message " +
            "WHERE deleted = 0 AND receiver_id = #{userId} AND is_read = 0 GROUP BY sender_id")
    List<Map<String, Object>> countUnreadBySender(@Param("userId") Long userId);

    /** 将某个会话中对方发来的消息全部标记为已读 */
    @Update("UPDATE message SET is_read = 1 " +
            "WHERE receiver_id = #{userId} AND sender_id = #{peerId} AND deleted = 0 AND is_read = 0")
    void markConversationRead(@Param("userId") Long userId, @Param("peerId") Long peerId);
}
