package com.knowledge.base.controller;

import com.knowledge.base.common.Result;
import com.knowledge.base.dto.CommentRequest;
import com.knowledge.base.entity.User;
import com.knowledge.base.service.CommentService;
import com.knowledge.base.vo.CommentVO;
import jakarta.validation.Valid;
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
    public Result<CommentVO> createComment(@Valid @RequestBody CommentRequest request,
                                         @AuthenticationPrincipal User user) {
        return Result.success(commentService.createComment(request, user.getId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteComment(@PathVariable Long id,
                                      @AuthenticationPrincipal User user) {
        commentService.deleteComment(id, user);
        return Result.success();
    }
}
