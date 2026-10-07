package com.knowledge.base.controller;

import com.knowledge.base.common.Result;
import com.knowledge.base.entity.Tag;
import com.knowledge.base.service.TagService;
import com.knowledge.base.vo.TagVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
public class TagController {

    @Autowired
    private TagService tagService;

    @GetMapping
    public Result<List<TagVO>> getAllTags() {
        return Result.success(tagService.getAllTags());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<TagVO> createTag(@RequestBody Tag tag) {
        try {
            TagVO t = tagService.createTag(tag);
            return Result.success(t);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<TagVO> updateTag(@PathVariable Long id, @RequestBody Tag tag) {
        try {
            TagVO t = tagService.updateTag(id, tag);
            return Result.success(t);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public Result<Void> deleteTag(@PathVariable Long id) {
        try {
            tagService.deleteTag(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
