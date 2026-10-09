package com.knowledge.base.service;

import com.knowledge.base.dto.CommentRequest;
import com.knowledge.base.entity.User;
import com.knowledge.base.vo.CommentVO;

import java.util.List;

public interface CommentService {
    List<CommentVO> getCommentsByArticleId(Long articleId);
    CommentVO createComment(CommentRequest request, Long userId);
    void deleteComment(Long id, User currentUser);
}
