package com.knowledge.base.service;

import com.knowledge.base.entity.Tag;
import com.knowledge.base.vo.TagVO;

import java.util.List;

public interface TagService {
    List<TagVO> getAllTags();
    TagVO createTag(Tag tag);
    TagVO updateTag(Long id, Tag tag);
    void deleteTag(Long id);
}
