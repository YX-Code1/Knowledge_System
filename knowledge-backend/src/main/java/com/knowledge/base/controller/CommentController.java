package com.knowledge.base.controller;

import com.knowledge.base.common.Result;
import com.knowledge.base.entity.Comment;
import com.knowledge.base.entity.User;
import com.knowledge.base.service.CommentService;
import com.knowledge.base.vo.CommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping("/article/{articleId}")
    public Result<List<CommentVO>> getComments(@PathVariable Long articleId) {
        return Result.success(commentService.getCommentsByArticleId(articleId));
    }

    @PostMapping
    public Result<CommentVO> createComment(@RequestBody Comment comment,
                                         @AuthenticationPrincipal User user) {
        try {
            comment.setUserId(user.getId());
            CommentVO c = commentService.createComment(comment);
            return Result.success(c);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        try {
            commentService.deleteComment(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
