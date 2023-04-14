package com.pilipili.controller;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理控制器
 * @author Li
 * @version 1.0
 * @ClassName ManageController
 * @Description
 * @since 2023/3/29 12:11
 */
@Slf4j
@RestController
@Api(value = "管理控制器",tags = "管理控制器")
@RequestMapping("/manage")
public class ManageController {

}
