package com.pilipili.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.User;
import com.pilipili.entity.VideoCollection;
import com.pilipili.entity.VideoEpisode;
import com.pilipili.entity.in.BatchCollectionUpdateRequest;
import com.pilipili.entity.in.EpisodeUpdateRequest;
import com.pilipili.entity.out.Result;
import com.pilipili.exception.BusinessException;
import com.pilipili.repository.VideoCollectionRepository;
import com.pilipili.repository.VideoEpisodeRepository;
import com.pilipili.service.StorageService;
import com.pilipili.utils.FileUploadUtil;
import com.pilipili.utils.Status;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;

/**
 * 管理员视频合集管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/collections")
@Api(value = "管理员合集管理", tags = "管理员-合集管理")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AdminVideoCollectionController {

    private final VideoCollectionRepository videoCollectionRepository;
    private final VideoEpisodeRepository videoEpisodeRepository;
    private final StorageService storageService;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new BusinessException(Status.UNAUTHORIZED, "用户未登录");
    }

    @GetMapping("/page")
    @ApiOperation("分页查询合集")
    public Result<Page<VideoCollection>> getCollectionPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer collectionType,
            @RequestParam(required = false) Integer enabled) {
        Page<VideoCollection> page = new Page<>(pageNum, pageSize);
        QueryWrapper<VideoCollection> wrapper = new QueryWrapper<>();
        if (title != null && !title.trim().isEmpty()) {
            wrapper.like("title", title);
        }
        if (collectionType != null) {
            wrapper.eq("collection_type", collectionType);
        }
        if (enabled != null) {
            wrapper.eq("enabled", enabled);
        }
        wrapper.orderByDesc("create_time");
        Page<VideoCollection> result = videoCollectionRepository.page(page, wrapper);
        if (result.getRecords() != null) {
            for (VideoCollection collection : result.getRecords()) {
                normalizeCollectionCoverUrl(collection);
            }
        }
        return Result.build(result);
    }

    @GetMapping("/{collectionId}")
    @ApiOperation("获取合集详情")
    public Result<VideoCollection> getCollectionById(@PathVariable Long collectionId) {
        VideoCollection collection = videoCollectionRepository.getById(collectionId);
        if (collection == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "合集不存在");
        }
        normalizeCollectionCoverUrl(collection);
        return Result.build(collection);
    }

    @PutMapping("/{collectionId}")
    @ApiOperation("更新合集信息")
    public Result<VideoCollection> updateCollection(
            @PathVariable Long collectionId,
            @RequestBody VideoCollection collection) {
        VideoCollection existing = videoCollectionRepository.getById(collectionId);
        if (existing == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "合集不存在");
        }
        if (collection != null) {
            if (notBlank(collection.getTitle())) {
                existing.setTitle(collection.getTitle());
            }
            if (notBlank(collection.getDescription())) {
                existing.setDescription(collection.getDescription());
            }
            if (notBlank(collection.getCoverUrl())) {
                existing.setCoverUrl(collection.getCoverUrl());
            }
            if (notBlank(collection.getSourceFolderPath())) {
                existing.setSourceFolderPath(collection.getSourceFolderPath());
            }
            if (collection.getVideoCount() != null) {
                existing.setVideoCount(collection.getVideoCount());
            }
            if (collection.getCollectionType() != null) {
                existing.setCollectionType(collection.getCollectionType());
            }
            if (collection.getEnabled() != null) {
                existing.setEnabled(collection.getEnabled());
            }
        }
        User admin = getCurrentUser();
        existing.setUpdateId(admin.getId());
        existing.setUpdateName(admin.getUsername());
        existing.setUpdateTime(new Date());
        videoCollectionRepository.updateById(existing);
        normalizeCollectionCoverUrl(existing);
        return Result.build(existing);
    }

    @PostMapping("/{collectionId}/cover")
    @ApiOperation("上传合集封面")
    public Result<VideoCollection> uploadCollectionCover(
            @PathVariable Long collectionId,
            @RequestParam("coverFile") MultipartFile coverFile) {
        if (coverFile == null || coverFile.isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "封面文件不能为空");
        }
        if (!FileUploadUtil.isValidImageFormat(coverFile.getOriginalFilename())) {
            throw new BusinessException(Status.FILE_FORMAT_ERROR, "不支持的封面格式");
        }
        VideoCollection collection = videoCollectionRepository.getById(collectionId);
        if (collection == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "合集不存在");
        }
        String coverUrl = storageService.uploadFile(coverFile, "covers");
        User admin = getCurrentUser();
        collection.setCoverUrl(coverUrl);
        collection.setUpdateId(admin.getId());
        collection.setUpdateName(admin.getUsername());
        collection.setUpdateTime(new Date());
        videoCollectionRepository.updateById(collection);
        normalizeCollectionCoverUrl(collection);
        return Result.build(collection);
    }

    @DeleteMapping("/{collectionId}")
    @ApiOperation("删除合集")
    public Result<?> deleteCollection(@PathVariable Long collectionId) {
        VideoCollection collection = videoCollectionRepository.getById(collectionId);
        if (collection == null) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "合集不存在");
        }
        videoCollectionRepository.removeById(collectionId);
        QueryWrapper<VideoEpisode> wrapper = new QueryWrapper<>();
        wrapper.eq("collection_id", collectionId);
        videoEpisodeRepository.remove(wrapper);
        return Result.build();
    }

    @DeleteMapping("/batch")
    @ApiOperation("批量删除合集")
    public Result<?> deleteCollections(@RequestBody java.util.List<Long> collectionIds) {
        if (collectionIds == null || collectionIds.isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "合集ID不能为空");
        }
        videoCollectionRepository.removeByIds(collectionIds);
        QueryWrapper<VideoEpisode> wrapper = new QueryWrapper<>();
        wrapper.in("collection_id", collectionIds);
        videoEpisodeRepository.remove(wrapper);
        return Result.build();
    }

    @PutMapping("/batch/enabled")
    @ApiOperation("批量更新合集启用状态")
    public Result<?> updateCollectionEnabledBatch(
            @RequestBody java.util.List<Long> collectionIds,
            @RequestParam Integer enabled) {
        if (collectionIds == null || collectionIds.isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "合集ID不能为空");
        }
        if (enabled == null) {
            throw new BusinessException(Status.PARAM_ERROR, "启用状态不能为空");
        }
        User admin = getCurrentUser();
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<VideoCollection> wrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        wrapper.in("id", collectionIds)
                .set("enabled", enabled)
                .set("update_id", admin.getId())
                .set("update_name", admin.getUsername())
                .set("update_time", new Date());
        videoCollectionRepository.update(wrapper);
        return Result.build();
    }

    @PutMapping("/batch/fields")
    @ApiOperation("批量更新合集标题/描述")
    public Result<?> updateCollectionFieldsBatch(@RequestBody BatchCollectionUpdateRequest request) {
        if (request == null || request.getCollectionIds() == null || request.getCollectionIds().isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "合集ID不能为空");
        }
        boolean hasUpdate = false;
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<VideoCollection> wrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        wrapper.in("id", request.getCollectionIds());
        if (notBlank(request.getTitle())) {
            wrapper.set("title", request.getTitle());
            hasUpdate = true;
        }
        if (request.getDescription() != null) {
            wrapper.set("description", request.getDescription());
            hasUpdate = true;
        }
        if (!hasUpdate) {
            throw new BusinessException(Status.PARAM_ERROR, "至少提供一个更新字段");
        }
        User admin = getCurrentUser();
        wrapper.set("update_id", admin.getId())
                .set("update_name", admin.getUsername())
                .set("update_time", new Date());
        videoCollectionRepository.update(wrapper);
        return Result.build();
    }

    @GetMapping("/{collectionId}/episodes")
    @ApiOperation("获取合集分集列表")
    public Result<List<VideoEpisode>> getCollectionEpisodes(@PathVariable Long collectionId) {
        QueryWrapper<VideoEpisode> wrapper = new QueryWrapper<>();
        wrapper.eq("collection_id", collectionId);
        wrapper.orderByAsc("sort_order", "episode_number");
        return Result.build(videoEpisodeRepository.list(wrapper));
    }

    @DeleteMapping("/{collectionId}/episodes/{episodeId}")
    @ApiOperation("移除单个分集")
    public Result<?> deleteEpisode(
            @PathVariable Long collectionId,
            @PathVariable Long episodeId) {
        VideoEpisode episode = videoEpisodeRepository.getById(episodeId);
        if (episode == null || !collectionId.equals(episode.getCollectionId())) {
            throw new BusinessException(Status.DATA_NOT_FOUNT, "分集不存在");
        }
        videoEpisodeRepository.removeById(episodeId);
        refreshCollectionVideoCount(collectionId);
        return Result.build();
    }

    @DeleteMapping("/{collectionId}/episodes/batch")
    @ApiOperation("批量移除分集")
    public Result<?> deleteEpisodesBatch(
            @PathVariable Long collectionId,
            @RequestBody List<Long> episodeIds) {
        if (episodeIds == null || episodeIds.isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "分集ID不能为空");
        }
        QueryWrapper<VideoEpisode> wrapper = new QueryWrapper<>();
        wrapper.eq("collection_id", collectionId);
        wrapper.in("id", episodeIds);
        videoEpisodeRepository.remove(wrapper);
        refreshCollectionVideoCount(collectionId);
        return Result.build();
    }

    @PutMapping("/{collectionId}/episodes/sort")
    @ApiOperation("批量更新分集排序")
    public Result<?> updateEpisodeSort(
            @PathVariable Long collectionId,
            @RequestBody List<EpisodeUpdateRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "排序数据不能为空");
        }
        User admin = getCurrentUser();
        for (EpisodeUpdateRequest item : items) {
            if (item == null || item.getId() == null || item.getSortOrder() == null) {
                continue;
            }
            VideoEpisode episode = videoEpisodeRepository.getById(item.getId());
            if (episode == null || !collectionId.equals(episode.getCollectionId())) {
                continue;
            }
            episode.setSortOrder(item.getSortOrder());
            episode.setUpdateId(admin.getId());
            episode.setUpdateName(admin.getUsername());
            episode.setUpdateTime(new Date());
            videoEpisodeRepository.updateById(episode);
        }
        return Result.build();
    }

    @PutMapping("/{collectionId}/episodes/batch")
    @ApiOperation("批量调整分集信息")
    public Result<?> updateEpisodesBatch(
            @PathVariable Long collectionId,
            @RequestBody List<EpisodeUpdateRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(Status.PARAM_ERROR, "分集数据不能为空");
        }
        User admin = getCurrentUser();
        for (EpisodeUpdateRequest item : items) {
            if (item == null || item.getId() == null) {
                continue;
            }
            VideoEpisode episode = videoEpisodeRepository.getById(item.getId());
            if (episode == null || !collectionId.equals(episode.getCollectionId())) {
                continue;
            }
            if (item.getEpisodeNumber() != null) {
                episode.setEpisodeNumber(item.getEpisodeNumber());
            }
            if (item.getEpisodeName() != null) {
                episode.setEpisodeName(item.getEpisodeName());
            }
            if (item.getSortOrder() != null) {
                episode.setSortOrder(item.getSortOrder());
            }
            episode.setUpdateId(admin.getId());
            episode.setUpdateName(admin.getUsername());
            episode.setUpdateTime(new Date());
            videoEpisodeRepository.updateById(episode);
        }
        return Result.build();
    }

    private void normalizeCollectionCoverUrl(VideoCollection collection) {
        if (collection == null || collection.getId() == null) {
            return;
        }
        String coverUrl = collection.getCoverUrl();
        if (coverUrl == null || coverUrl.isEmpty()) {
            collection.setCoverUrl("/api/cover/collection/" + collection.getId());
            return;
        }
        if (isRemoteUrl(coverUrl)) {
            return;
        }
        collection.setCoverUrl("/api/cover/collection/" + collection.getId());
    }

    private boolean isRemoteUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void refreshCollectionVideoCount(Long collectionId) {
        if (collectionId == null) {
            return;
        }
        QueryWrapper<VideoEpisode> wrapper = new QueryWrapper<>();
        wrapper.eq("collection_id", collectionId);
        long count = videoEpisodeRepository.count(wrapper);
        VideoCollection collection = videoCollectionRepository.getById(collectionId);
        if (collection == null) {
            return;
        }
        collection.setVideoCount((int) count);
        User admin = getCurrentUser();
        collection.setUpdateId(admin.getId());
        collection.setUpdateName(admin.getUsername());
        collection.setUpdateTime(new Date());
        videoCollectionRepository.updateById(collection);
    }
}
