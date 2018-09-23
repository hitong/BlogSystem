package com.duan.blogos.service.impl.blogger;

import com.duan.blogos.service.config.preference.DbProperties;
import com.duan.blogos.service.config.preference.DefaultProperties;
import com.duan.blogos.service.dao.blog.BlogDao;
import com.duan.blogos.service.dao.blog.BlogLabelDao;
import com.duan.blogos.service.dto.blog.BlogLabelDTO;
import com.duan.blogos.service.entity.blog.Blog;
import com.duan.blogos.service.entity.blog.BlogLabel;
import com.duan.blogos.service.exception.CodeMessage;
import com.duan.blogos.service.exception.ResultUtil;
import com.duan.blogos.service.manager.DataFillingManager;
import com.duan.blogos.service.restful.ResultModel;
import com.duan.blogos.service.service.blogger.BloggerLabelService;
import com.duan.common.util.ArrayUtils;
import com.duan.common.util.CollectionUtils;
import com.duan.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created on 2017/12/19.
 *
 * @author DuanJiaNing
 */
@Service
public class BloggerLabelServiceImpl implements BloggerLabelService {

    @Autowired
    private BlogLabelDao labelDao;

    @Autowired
    private BlogDao blogDao;

    @Autowired
    private DbProperties dbProperties;

    @Autowired
    private DefaultProperties defaultProperties;

    @Autowired
    private DataFillingManager dataFillingManager;

    @Override
    public int insertLabel(int bloggerId, String title) {

        BlogLabel label = new BlogLabel();
        label.setBloggerId(bloggerId);
        label.setTitle(title);
        int effect = labelDao.insert(label);
        if (effect <= 0) return -1;

        return label.getId();
    }

    @Override
    public boolean updateLabel(int labelId, int bloggerId, String newTitle) {

        //检查标签存在及标签创建者是否为当前博主
        BlogLabel label = labelDao.getLabel(labelId);
        if (label == null || label.getBloggerId() != bloggerId) return false;

        BlogLabel la = new BlogLabel();
        la.setTitle(newTitle);
        la.setId(labelId);
        int effect = labelDao.update(la);
        if (effect <= 0) return false;

        return true;
    }

    @Override
    public boolean deleteLabel(int bloggerId, int labelId) {

        //检查标签存在及标签创建者是否为当前博主
        BlogLabel label = labelDao.getLabel(labelId);
        if (label == null || label.getBloggerId() != bloggerId) return false;

        // 删除数据库记录
        int effect = labelDao.delete(labelId);
        if (effect <= 0) return false;

        // 将所有拥有该标签的博文修改（j将标签移除）
        List<Blog> blogs = blogDao.listAllLabelByBloggerId(bloggerId);
        String ch = dbProperties.getStringFiledSplitCharacterForNumber();
        for (Blog blog : blogs) {
            int[] lids = StringUtils.intStringDistinctToArray(blog.getLabelIds(), ch);
            if (CollectionUtils.isEmpty(lids)) continue;

            if (CollectionUtils.intArrayContain(lids, labelId)) {
                int[] ids = ArrayUtils.removeFromArray(lids, labelId);
                blog.setLabelIds(StringUtils.intArrayToString(ids, ch));
                if (blogDao.update(blog) <= 0)
                    throw ResultUtil.failException(CodeMessage.COMMON_UNKNOWN_ERROR, new SQLException());
            }
        }

        return true;
    }

    @Override
    public ResultModel<List<BlogLabelDTO>> listLabel(int offset, int rows) {

        List<BlogLabel> result = labelDao.listLabel(offset, rows);
        if (CollectionUtils.isEmpty(result)) return null;

        List<BlogLabelDTO> dtos = result.stream().map(dataFillingManager::blogLabel2DTO).collect(Collectors.toList());
        return new ResultModel<>(dtos);
    }

    @Override
    public BlogLabelDTO getLabel(int labelId) {
        BlogLabel label = labelDao.getLabel(labelId);
        return dataFillingManager.blogLabel2DTO(label);
    }

    @Override
    public ResultModel<List<BlogLabelDTO>> listLabelByBlogger(int bloggerId, int offset, int rows) {
        offset = offset < 0 ? 0 : offset;
        rows = rows < 0 ? defaultProperties.getLabelCount() : rows;

        List<BlogLabel> result = labelDao.listLabelByBloggerId(bloggerId, offset, rows);
        if (CollectionUtils.isEmpty(result)) return null;

        List<BlogLabelDTO> dtos = result.stream().map(dataFillingManager::blogLabel2DTO).collect(Collectors.toList());
        return new ResultModel<>(dtos);
    }
}