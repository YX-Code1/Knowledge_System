package com.knowledge.base.service;

import com.knowledge.base.dto.TagRequest;
import com.knowledge.base.vo.TagVO;

import java.util.List;

public interface TagService {
    List<TagVO> getAllTags();
    TagVO createTag(TagRequest request);
    TagVO updateTag(Long id, TagRequest request);
    void deleteTag(Long id);
}
