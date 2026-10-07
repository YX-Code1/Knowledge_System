package com.knowledge.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /** 原子加余额 */
    @Update("UPDATE sys_user SET balance = balance + #{delta} WHERE id = #{id}")
    int addBalance(@Param("id") Long id, @Param("delta") BigDecimal delta);

    /** 原子扣余额（余额不足时更新 0 行，防并发透支） */
    @Update("UPDATE sys_user SET balance = balance - #{delta} WHERE id = #{id} AND balance >= #{delta}")
    int deductBalanceIfEnough(@Param("id") Long id, @Param("delta") BigDecimal delta);
}
