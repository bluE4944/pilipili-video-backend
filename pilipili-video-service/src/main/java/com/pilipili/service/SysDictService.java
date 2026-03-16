package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.SysDict;
import com.pilipili.entity.SysDictItem;
import com.pilipili.entity.User;
import com.pilipili.entity.in.SysDictCondition;
import com.pilipili.entity.in.SysDictItemCondition;
import com.pilipili.exception.BusinessException;
import com.pilipili.repository.SysDictItemRepository;
import com.pilipili.repository.SysDictRepository;
import com.pilipili.utils.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据字典服务
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SysDictService {

    private final SysDictRepository sysDictRepository;
    private final SysDictItemRepository sysDictItemRepository;

    public Page<SysDict> getDictPage(SysDictCondition condition, Integer pageNum, Integer pageSize) {
        Page<SysDict> page = new Page<>(pageNum, pageSize);
        QueryWrapper<SysDict> wrapper = new QueryWrapper<>();
        if (condition != null) {
            if (notBlank(condition.getDictCode())) {
                wrapper.like("dict_code", condition.getDictCode());
            }
            if (notBlank(condition.getDictName())) {
                wrapper.like("dict_name", condition.getDictName());
            }
            if (condition.getEnabled() != null) {
                wrapper.eq("enabled", condition.getEnabled());
            }
        }
        wrapper.orderByAsc("sort_order").orderByDesc("create_time");
        return sysDictRepository.page(page, wrapper);
    }

    public SysDict getDictById(Long dictId) {
        if (dictId == null) {
            throw new BusinessException(Status.PARAM_ERROR, "字典ID不能为空");
        }
        SysDict dict = sysDictRepository.getById(dictId);
        if (dict == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "字典不存在");
        }
        return dict;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysDict createDict(SysDictCondition condition, User admin) {
        if (condition == null || !notBlank(condition.getDictCode()) || !notBlank(condition.getDictName())) {
            throw new BusinessException(Status.PARAM_ERROR, "字典编码和名称不能为空");
        }
        if (existsDictCode(condition.getDictCode(), null)) {
            throw new BusinessException(Status.BUSINESS_ERROR, "字典编码已存在");
        }
        SysDict dict = new SysDict();
        dict.setDictCode(condition.getDictCode());
        dict.setDictName(condition.getDictName());
        dict.setDescription(condition.getDescription());
        dict.setEnabled(condition.getEnabled() != null ? condition.getEnabled() : 1);
        dict.setSortOrder(condition.getSortOrder() != null ? condition.getSortOrder() : 0);
        fillCreateInfo(dict, admin);
        sysDictRepository.save(dict);
        return dict;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysDict updateDict(Long dictId, SysDictCondition condition, User admin) {
        SysDict existing = getDictById(dictId);
        if (condition == null) {
            return existing;
        }
        if (notBlank(condition.getDictCode()) && !condition.getDictCode().equals(existing.getDictCode())) {
            throw new BusinessException(Status.PARAM_ERROR, "不支持修改字典编码");
        }
        if (notBlank(condition.getDictName())) {
            existing.setDictName(condition.getDictName());
        }
        if (condition.getDescription() != null) {
            existing.setDescription(condition.getDescription());
        }
        if (condition.getEnabled() != null) {
            existing.setEnabled(condition.getEnabled());
        }
        if (condition.getSortOrder() != null) {
            existing.setSortOrder(condition.getSortOrder());
        }
        fillUpdateInfo(existing, admin);
        sysDictRepository.updateById(existing);
        return existing;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDict(Long dictId) {
        SysDict dict = getDictById(dictId);
        removeItemsByDictCode(dict.getDictCode());
        sysDictRepository.removeById(dictId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDictBatch(List<Long> dictIds) {
        if (dictIds == null || dictIds.isEmpty()) {
            return;
        }
        List<SysDict> dicts = sysDictRepository.listByIds(dictIds);
        if (dicts != null && !dicts.isEmpty()) {
            List<String> codes = dicts.stream()
                    .map(SysDict::getDictCode)
                    .filter(this::notBlank)
                    .collect(Collectors.toList());
            if (!codes.isEmpty()) {
                QueryWrapper<SysDictItem> wrapper = new QueryWrapper<>();
                wrapper.in("dict_code", codes);
                sysDictItemRepository.remove(wrapper);
            }
        }
        sysDictRepository.removeByIds(dictIds);
    }

    public List<SysDictItem> getItemsByDictCode(String dictCode, boolean onlyEnabled) {
        if (!notBlank(dictCode)) {
            throw new BusinessException(Status.PARAM_ERROR, "字典编码不能为空");
        }
        SysDict dict = findByCode(dictCode);
        if (dict == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "字典不存在");
        }
        if (onlyEnabled && dict.getEnabled() != null && dict.getEnabled() == 0) {
            return Collections.emptyList();
        }
        QueryWrapper<SysDictItem> wrapper = new QueryWrapper<>();
        wrapper.eq("dict_code", dictCode);
        if (onlyEnabled) {
            wrapper.eq("enabled", 1);
        }
        wrapper.orderByAsc("sort_order").orderByAsc("id");
        return sysDictItemRepository.list(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public SysDictItem createItem(String dictCode, SysDictItemCondition condition, User admin) {
        if (!notBlank(dictCode)) {
            throw new BusinessException(Status.PARAM_ERROR, "字典编码不能为空");
        }
        if (condition == null || !notBlank(condition.getItemValue()) || !notBlank(condition.getItemLabel())) {
            throw new BusinessException(Status.PARAM_ERROR, "字典项值和名称不能为空");
        }
        SysDict dict = findByCode(dictCode);
        if (dict == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "字典不存在");
        }
        if (existsItem(dictCode, condition.getItemValue(), null)) {
            throw new BusinessException(Status.BUSINESS_ERROR, "字典项值已存在");
        }
        SysDictItem item = new SysDictItem();
        item.setDictCode(dictCode);
        item.setItemValue(condition.getItemValue());
        item.setItemLabel(condition.getItemLabel());
        item.setSortOrder(condition.getSortOrder() != null ? condition.getSortOrder() : 0);
        item.setEnabled(condition.getEnabled() != null ? condition.getEnabled() : 1);
        item.setRemark(condition.getRemark());
        fillCreateInfo(item, admin);
        sysDictItemRepository.save(item);
        return item;
    }

    @Transactional(rollbackFor = Exception.class)
    public SysDictItem updateItem(Long itemId, SysDictItemCondition condition, User admin) {
        if (itemId == null) {
            throw new BusinessException(Status.PARAM_ERROR, "字典项ID不能为空");
        }
        SysDictItem existing = sysDictItemRepository.getById(itemId);
        if (existing == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "字典项不存在");
        }
        if (condition != null) {
            if (notBlank(condition.getItemValue())
                    && !condition.getItemValue().equals(existing.getItemValue())) {
                if (existsItem(existing.getDictCode(), condition.getItemValue(), existing.getId())) {
                    throw new BusinessException(Status.BUSINESS_ERROR, "字典项值已存在");
                }
                existing.setItemValue(condition.getItemValue());
            }
            if (notBlank(condition.getItemLabel())) {
                existing.setItemLabel(condition.getItemLabel());
            }
            if (condition.getSortOrder() != null) {
                existing.setSortOrder(condition.getSortOrder());
            }
            if (condition.getEnabled() != null) {
                existing.setEnabled(condition.getEnabled());
            }
            if (condition.getRemark() != null) {
                existing.setRemark(condition.getRemark());
            }
        }
        fillUpdateInfo(existing, admin);
        sysDictItemRepository.updateById(existing);
        return existing;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long itemId) {
        if (itemId == null) {
            throw new BusinessException(Status.PARAM_ERROR, "字典项ID不能为空");
        }
        SysDictItem existing = sysDictItemRepository.getById(itemId);
        if (existing == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "字典项不存在");
        }
        sysDictItemRepository.removeById(itemId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteItemsBatch(List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }
        sysDictItemRepository.removeByIds(itemIds);
    }

    private void removeItemsByDictCode(String dictCode) {
        if (!notBlank(dictCode)) {
            return;
        }
        QueryWrapper<SysDictItem> wrapper = new QueryWrapper<>();
        wrapper.eq("dict_code", dictCode);
        sysDictItemRepository.remove(wrapper);
    }

    private SysDict findByCode(String dictCode) {
        QueryWrapper<SysDict> wrapper = new QueryWrapper<>();
        wrapper.eq("dict_code", dictCode);
        return sysDictRepository.getOne(wrapper);
    }

    private boolean existsDictCode(String dictCode, Long ignoreId) {
        SysDict dict = findByCode(dictCode);
        if (dict == null) {
            return false;
        }
        if (ignoreId == null) {
            return true;
        }
        return !ignoreId.equals(dict.getId());
    }

    private boolean existsItem(String dictCode, String itemValue, Long ignoreId) {
        if (!notBlank(dictCode) || !notBlank(itemValue)) {
            return false;
        }
        QueryWrapper<SysDictItem> wrapper = new QueryWrapper<>();
        wrapper.eq("dict_code", dictCode);
        wrapper.eq("item_value", itemValue);
        SysDictItem found = sysDictItemRepository.getOne(wrapper);
        if (found == null) {
            return false;
        }
        if (ignoreId == null) {
            return true;
        }
        return !ignoreId.equals(found.getId());
    }

    private void fillCreateInfo(SysDict dict, User admin) {
        Date now = new Date();
        if (admin != null) {
            dict.setCreateId(admin.getId());
            dict.setCreateName(admin.getUsername());
            dict.setUpdateId(admin.getId());
            dict.setUpdateName(admin.getUsername());
        }
        dict.setCreateTime(now);
        dict.setUpdateTime(now);
        dict.setLogicDel(0);
    }

    private void fillUpdateInfo(SysDict dict, User admin) {
        Date now = new Date();
        if (admin != null) {
            dict.setUpdateId(admin.getId());
            dict.setUpdateName(admin.getUsername());
        }
        dict.setUpdateTime(now);
    }

    private void fillCreateInfo(SysDictItem item, User admin) {
        Date now = new Date();
        if (admin != null) {
            item.setCreateId(admin.getId());
            item.setCreateName(admin.getUsername());
            item.setUpdateId(admin.getId());
            item.setUpdateName(admin.getUsername());
        }
        item.setCreateTime(now);
        item.setUpdateTime(now);
        item.setLogicDel(0);
    }

    private void fillUpdateInfo(SysDictItem item, User admin) {
        Date now = new Date();
        if (admin != null) {
            item.setUpdateId(admin.getId());
            item.setUpdateName(admin.getUsername());
        }
        item.setUpdateTime(now);
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
