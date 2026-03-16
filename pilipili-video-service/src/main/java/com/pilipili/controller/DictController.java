package com.pilipili.controller;

import com.pilipili.entity.SysDictItem;
import com.pilipili.entity.out.Result;
import com.pilipili.service.SysDictService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公共字典查询控制器
 */
@RestController
@RequestMapping("/api/dict")
@Api(value = "字典查询", tags = "字典查询")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DictController {

    private final SysDictService sysDictService;

    @GetMapping("/{dictCode}")
    @ApiOperation("获取字典项列表")
    public Result<List<SysDictItem>> getDictItems(@PathVariable String dictCode) {
        return Result.build(sysDictService.getItemsByDictCode(dictCode, true));
    }
}
