package com.knowledge.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.base.entity.Comment;
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
    public CommentVO createComment(Comment comment) {
        commentMapper.insert(comment);
        articleMapper.updateCommentCount(comment.getArticleId(), 1);
        return CommentVO.from(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment != null) {
            commentMapper.deleteById(id);
            articleMapper.updateCommentCount(comment.getArticleId(), -1);
        }
    }
}
