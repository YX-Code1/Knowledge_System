package com.knowledge.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.base.common.BusinessException;
import com.knowledge.base.dto.CommentRequest;
import com.knowledge.base.entity.Comment;
import com.knowledge.base.entity.User;
import com.knowledge.base.mapper.ArticleMapper;
import com.knowledge.base.mapper.CommentMapper;
import com.knowledge.base.service.CommentService;
import com.knowledge.base.vo.CommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public List<CommentVO> getCommentsByArticleId(Long articleId) {
        return commentMapper.selectList(
                        new LambdaQueryWrapper<Comment>()
                                .eq(Comment::getArticleId, articleId)
                                .orderByAsc(Comment::getCreateTime))
                .stream().map(CommentVO::from).toList();
    }

    @Override
    @Transactional
    public CommentVO createComment(CommentRequest request, Long userId) {
        Comment comment = new Comment();
        comment.setArticleId(request.getArticleId());
        comment.setUserId(userId);
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        commentMapper.insert(comment);
        articleMapper.updateCommentCount(comment.getArticleId(), 1);
        return CommentVO.from(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long id, User currentUser) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        // 归属校验：仅评论作者或管理员可删除，防止越权删除他人评论
        boolean isAdmin = "ADMIN".equals(currentUser.getRole()) || "SUPER_ADMIN".equals(currentUser.getRole());
        if (!comment.getUserId().equals(currentUser.getId()) && !isAdmin) {
            throw new BusinessException("只能删除自己的评论");
        }
        // 只有真正删除了行才回写计数，避免并发重复删除导致评论数漂移
        if (commentMapper.deleteById(id) > 0) {
            articleMapper.updateCommentCount(comment.getArticleId(), -1);
        }
    }
}
