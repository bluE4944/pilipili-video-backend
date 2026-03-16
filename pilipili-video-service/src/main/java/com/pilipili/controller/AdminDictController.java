package com.pilipili.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.SysDict;
import com.pilipili.entity.SysDictItem;
import com.pilipili.entity.User;
import com.pilipili.entity.in.SysDictCondition;
import com.pilipili.entity.in.SysDictItemCondition;
import com.pilipili.entity.out.Result;
import com.pilipili.exception.BusinessException;
import com.pilipili.service.SysDictService;
import com.pilipili.utils.Status;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/dict")
@Api(value = "字典管理", tags = "管理端-字典管理")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AdminDictController {

    private final SysDictService sysDictService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new BusinessException(Status.UNAUTHORIZED, "用户未登录");
    }

    @GetMapping("/page")
    @ApiOperation("分页查询字典")
    public Result<Page<SysDict>> getDictPage(
            @ModelAttribute SysDictCondition condition,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<SysDict> page = sysDictService.getDictPage(condition, pageNum, pageSize);
        return Result.build(page);
    }

    @GetMapping("/{dictId}")
    @ApiOperation("获取字典详情")
    public Result<SysDict> getDictById(@PathVariable Long dictId) {
        SysDict dict = sysDictService.getDictById(dictId);
        return Result.build(dict);
    }

    @PostMapping
    @ApiOperation("创建字典")
    public Result<SysDict> createDict(@RequestBody SysDictCondition condition) {
        SysDict dict = sysDictService.createDict(condition, getCurrentUser());
        return Result.build(dict);
    }

    @PutMapping("/{dictId}")
    @ApiOperation("更新字典")
    public Result<SysDict> updateDict(
            @PathVariable Long dictId,
            @RequestBody SysDictCondition condition) {
        SysDict dict = sysDictService.updateDict(dictId, condition, getCurrentUser());
        return Result.build(dict);
    }

    @DeleteMapping("/{dictId}")
    @ApiOperation("删除字典")
    public Result<?> deleteDict(@PathVariable Long dictId) {
        sysDictService.deleteDict(dictId);
        return Result.build();
    }

    @DeleteMapping("/batch")
    @ApiOperation("批量删除字典")
    public Result<?> deleteDictBatch(@RequestBody List<Long> dictIds) {
        if (dictIds == null || dictIds.isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "字典ID不能为空");
        }
        sysDictService.deleteDictBatch(dictIds);
        return Result.build();
    }

    @GetMapping("/{dictCode}/items")
    @ApiOperation("获取字典项列表")
    public Result<List<SysDictItem>> getDictItems(@PathVariable String dictCode) {
        List<SysDictItem> items = sysDictService.getItemsByDictCode(dictCode, false);
        return Result.build(items);
    }

    @PostMapping("/{dictCode}/items")
    @ApiOperation("创建字典项")
    public Result<SysDictItem> createItem(
            @PathVariable String dictCode,
            @RequestBody SysDictItemCondition condition) {
        SysDictItem item = sysDictService.createItem(dictCode, condition, getCurrentUser());
        return Result.build(item);
    }

    @PutMapping("/items/{itemId}")
    @ApiOperation("更新字典项")
    public Result<SysDictItem> updateItem(
            @PathVariable Long itemId,
            @RequestBody SysDictItemCondition condition) {
        SysDictItem item = sysDictService.updateItem(itemId, condition, getCurrentUser());
        return Result.build(item);
    }

    @DeleteMapping("/items/{itemId}")
    @ApiOperation("删除字典项")
    public Result<?> deleteItem(@PathVariable Long itemId) {
        sysDictService.deleteItem(itemId);
        return Result.build();
    }

    @DeleteMapping("/items/batch")
    @ApiOperation("批量删除字典项")
    public Result<?> deleteItemsBatch(@RequestBody List<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "字典项ID不能为空");
        }
        sysDictService.deleteItemsBatch(itemIds);
        return Result.build();
    }
}
