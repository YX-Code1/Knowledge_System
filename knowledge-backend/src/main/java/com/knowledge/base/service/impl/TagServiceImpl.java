package com.knowledge.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.base.common.BusinessException;
import com.knowledge.base.dto.TagRequest;
import com.knowledge.base.entity.ArticleTag;
import com.knowledge.base.entity.Tag;
import com.knowledge.base.mapper.ArticleTagMapper;
import com.knowledge.base.mapper.TagMapper;
import com.knowledge.base.service.TagService;
import com.knowledge.base.vo.TagVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Override
    public List<TagVO> getAllTags() {
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId));

        // 每个标签被多少篇已发布文章使用
        Map<Long, Long> counts = new HashMap<>();
        for (Map<String, Object> row : tagMapper.countArticlesByTag()) {
            counts.put(toLong(row.get("tagId")), toLong(row.get("cnt")));
        }
        // collect 得到可变列表，后续 sort 需要（Stream.toList() 返回不可变列表）
        List<TagVO> vos = tags.stream().map(t -> {
            TagVO vo = TagVO.from(t);
            vo.setArticleCount(counts.getOrDefault(t.getId(), 0L));
            return vo;
        }).collect(Collectors.toList());

        // 按使用量降序（使用量相同按 id 升序，保持稳定）
        vos.sort(Comparator.comparingLong((TagVO t) -> -t.getArticleCount()).thenComparingLong(TagVO::getId));
        return vos;
    }

    private long toLong(Object v) {
        return v == null ? 0L : ((Number) v).longValue();
    }

    @Override
    public TagVO createTag(TagRequest request) {
        String name = checkName(request.getName());
        checkDuplicate(name, null);
        Tag t = new Tag();
        t.setName(name);
        tagMapper.insert(t);
        return TagVO.from(t);
    }

    @Override
    public TagVO updateTag(Long id, TagRequest request) {
        String name = checkName(request.getName());
        Tag exist = tagMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("标签不存在");
        }
        checkDuplicate(name, id);
        exist.setName(name);
        tagMapper.updateById(exist);
        return TagVO.from(exist);
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        Tag exist = tagMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("标签不存在");
        }
        // 先清理文章与标签的关联，再删除标签
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, id));
        tagMapper.deleteById(id);
    }

    private String checkName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("标签名称不能为空");
        }
        String trimmed = name.trim();
        if (trimmed.length() > 20) {
            throw new BusinessException("标签名称不能超过20个字符");
        }
        return trimmed;
    }

    private void checkDuplicate(String name, Long excludeId) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<Tag>().eq(Tag::getName, name);
        if (excludeId != null) {
            wrapper.ne(Tag::getId, excludeId);
        }
        if (tagMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("标签已存在");
        }
    }
}
