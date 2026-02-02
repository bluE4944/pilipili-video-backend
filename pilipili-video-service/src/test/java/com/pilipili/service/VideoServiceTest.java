package com.pilipili.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pilipili.entity.User;
import com.pilipili.entity.Video;
import com.pilipili.entity.in.VideoCondition;
import com.pilipili.exception.BusinessException;
import com.pilipili.repository.VideoRepository;
import com.pilipili.utils.Status;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VideoServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private VideoService videoService;

    @Test
    public void uploadVideo_throwsWhenVideoFormatInvalid() {
        MultipartFile videoFile = new MockMultipartFile(
                "video", "video.txt", "text/plain", new byte[]{1, 2, 3}
        );
        Video video = new Video();
        User user = buildUser(1L, "u1", "user");

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> videoService.uploadVideo(videoFile, null, video, user)
        );

        assertEquals(Status.FILE_FORMAT_ERROR, ex.getStatus());
    }

    @Test
    public void uploadVideo_throwsWhenVideoTooLarge() {
        MultipartFile videoFile = org.mockito.Mockito.mock(MultipartFile.class);
        when(videoFile.getOriginalFilename()).thenReturn("video.mp4");
        when(videoFile.getSize()).thenReturn(500L * 1024 * 1024 + 1);

        Video video = new Video();
        User user = buildUser(1L, "u1", "user");

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> videoService.uploadVideo(videoFile, null, video, user)
        );

        assertEquals(Status.FILE_SIZE_ERROR, ex.getStatus());
    }

    @Test
    public void uploadVideo_throwsWhenCoverFormatInvalid() {
        MultipartFile videoFile = new MockMultipartFile(
                "video", "video.mp4", "video/mp4", new byte[]{1, 2}
        );
        MultipartFile coverFile = new MockMultipartFile(
                "cover", "cover.txt", "text/plain", new byte[]{1}
        );
        Video video = new Video();
        User user = buildUser(1L, "u1", "user");

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> videoService.uploadVideo(videoFile, coverFile, video, user)
        );

        assertEquals(Status.FILE_FORMAT_ERROR, ex.getStatus());
    }

    @Test
    public void uploadVideo_success() {
        MultipartFile videoFile = new MockMultipartFile(
                "video", "video.mp4", "video/mp4", new byte[]{1, 2, 3, 4}
        );
        MultipartFile coverFile = new MockMultipartFile(
                "cover", "cover.jpg", "image/jpeg", new byte[]{9}
        );
        when(storageService.uploadFile(videoFile, "videos")).thenReturn("http://cdn/video.mp4");
        when(storageService.uploadFile(coverFile, "covers")).thenReturn("http://cdn/cover.jpg");
        when(videoRepository.save(any(Video.class))).thenReturn(true);

        Video video = new Video();
        User user = buildUser(9L, "liam", "user");

        Video result = videoService.uploadVideo(videoFile, coverFile, video, user);

        assertSame(video, result);
        assertEquals("http://cdn/video.mp4", video.getVideoUrl());
        assertEquals("http://cdn/cover.jpg", video.getCoverUrl());
        assertEquals(Long.valueOf(videoFile.getSize()), video.getFileSize());
        assertEquals("mp4", video.getFormat());
        assertEquals(Long.valueOf(9L), video.getUserId());
        assertEquals("liam", video.getUserName());
        assertEquals(Integer.valueOf(0), video.getStatus());
        assertEquals(Long.valueOf(0L), video.getPlayCount());
        assertEquals(Long.valueOf(0L), video.getLikeCount());
        assertEquals(Long.valueOf(0L), video.getCollectCount());
        assertEquals(Long.valueOf(0L), video.getCommentCount());
        assertEquals(Long.valueOf(9L), video.getCreateId());
        assertEquals("liam", video.getCreateName());
        assertNotNull(video.getCreateTime());
        assertEquals(Integer.valueOf(0), video.getLogicDel());
        verify(videoRepository).save(video);
    }

    @Test
    public void uploadVideoChunk_delegatesToStorageService() {
        MultipartFile chunk = new MockMultipartFile(
                "chunk", "chunk.part", "application/octet-stream", new byte[]{1}
        );
        when(storageService.uploadChunk(chunk, "folder", 1, 2, "upload-1"))
                .thenReturn("ok");

        String result = videoService.uploadVideoChunk(chunk, "folder", 1, 2, "upload-1");

        assertEquals("ok", result);
        verify(storageService).uploadChunk(chunk, "folder", 1, 2, "upload-1");
    }

    @Test
    public void completeVideoUpload_success() {
        when(storageService.completeChunkUpload("folder", "upload-1", "video.mp4"))
                .thenReturn("http://cdn/video.mp4");
        when(videoRepository.save(any(Video.class))).thenReturn(true);

        Video video = new Video();
        User user = buildUser(7L, "alice", "user");

        Video result = videoService.completeVideoUpload("folder", "upload-1", "video.mp4", video, user);

        assertSame(video, result);
        assertEquals("http://cdn/video.mp4", video.getVideoUrl());
        assertEquals(Long.valueOf(7L), video.getUserId());
        assertEquals("alice", video.getUserName());
        assertEquals(Integer.valueOf(0), video.getStatus());
        assertEquals(Long.valueOf(0L), video.getPlayCount());
        assertEquals(Long.valueOf(0L), video.getLikeCount());
        assertEquals(Long.valueOf(0L), video.getCollectCount());
        assertEquals(Long.valueOf(0L), video.getCommentCount());
        assertEquals(Long.valueOf(7L), video.getCreateId());
        assertEquals("alice", video.getCreateName());
        assertNotNull(video.getCreateTime());
        assertEquals(Integer.valueOf(0), video.getLogicDel());
        verify(videoRepository).save(video);
    }

    @Test
    public void updateVideo_throwsWhenNotFound() {
        Video input = new Video();
        input.setId(10L);
        when(videoRepository.getById(10L)).thenReturn(null);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> videoService.updateVideo(input, buildUser(1L, "u1", "user"))
        );

        assertEquals(Status.DATA_NOT_FOUNT, ex.getStatus());
    }

    @Test
    public void updateVideo_throwsWhenForbidden() {
        Video existing = new Video();
        existing.setUserId(1L);
        when(videoRepository.getById(10L)).thenReturn(existing);

        Video input = new Video();
        input.setId(10L);
        User user = buildUser(2L, "u2", "user");

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> videoService.updateVideo(input, user)
        );

        assertEquals(Status.FORBIDDEN, ex.getStatus());
    }

    @Test
    public void updateVideo_successWhenOwner() {
        Video existing = new Video();
        existing.setUserId(1L);
        when(videoRepository.getById(10L)).thenReturn(existing);
        when(videoRepository.updateById(any(Video.class))).thenReturn(true);

        Video input = new Video();
        input.setId(10L);
        User user = buildUser(1L, "owner", "user");

        Video result = videoService.updateVideo(input, user);

        assertSame(input, result);
        assertEquals(Long.valueOf(1L), input.getUpdateId());
        assertEquals("owner", input.getUpdateName());
        assertNotNull(input.getUpdateTime());
        verify(videoRepository).updateById(input);
    }

    @Test
    public void deleteVideo_throwsWhenNotFound() {
        when(videoRepository.getById(10L)).thenReturn(null);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> videoService.deleteVideo(10L, buildUser(1L, "u1", "user"))
        );

        assertEquals(Status.DATA_NOT_FOUNT, ex.getStatus());
    }

    @Test
    public void deleteVideo_throwsWhenForbidden() {
        Video video = new Video();
        video.setUserId(1L);
        when(videoRepository.getById(10L)).thenReturn(video);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> videoService.deleteVideo(10L, buildUser(2L, "u2", "user"))
        );

        assertEquals(Status.FORBIDDEN, ex.getStatus());
    }

    @Test
    public void deleteVideo_successDeletesFiles() {
        Video video = new Video();
        video.setUserId(1L);
        video.setVideoUrl("http://cdn/video.mp4");
        video.setCoverUrl("http://cdn/cover.jpg");
        when(videoRepository.getById(10L)).thenReturn(video);
        when(storageService.deleteFile(any(String.class))).thenReturn(true);

        videoService.deleteVideo(10L, buildUser(1L, "u1", "user"));

        verify(videoRepository).removeById(10L);
        verify(storageService).deleteFile("http://cdn/video.mp4");
        verify(storageService).deleteFile("http://cdn/cover.jpg");
    }

    @Test
    public void getVideoPage_delegatesToRepository() {
        VideoCondition condition = new VideoCondition();
        condition.setTitle("t");
        condition.setCategoryId(1L);
        condition.setUserId(2L);
        condition.setStatus(0);
        condition.setTags("tag");

        Page<Video> expected = new Page<>();
        when(videoRepository.page(any(Page.class), any(QueryWrapper.class))).thenReturn(expected);

        Page<Video> result = videoService.getVideoPage(condition, 2, 20);

        assertSame(expected, result);

        ArgumentCaptor<Page> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(videoRepository).page(pageCaptor.capture(), any(QueryWrapper.class));
        Page captured = pageCaptor.getValue();
        assertEquals(2L, captured.getCurrent());
        assertEquals(20L, captured.getSize());
    }

    @Test
    public void getVideoById_throwsWhenNotFound() {
        when(videoRepository.getById(10L)).thenReturn(null);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> videoService.getVideoById(10L)
        );

        assertEquals(Status.DATA_NOT_FOUNT, ex.getStatus());
    }

    @Test
    public void getVideoById_returnsVideo() {
        Video video = new Video();
        when(videoRepository.getById(10L)).thenReturn(video);

        Video result = videoService.getVideoById(10L);

        assertSame(video, result);
    }

    @Test
    public void updateVideoStatus_throwsWhenNotFound() {
        when(videoRepository.getById(10L)).thenReturn(null);

        BusinessException ex = assertThrows(
                BusinessException.class,
                () -> videoService.updateVideoStatus(10L, 1, "ok", buildUser(1L, "u1", "admin"))
        );

        assertEquals(Status.DATA_NOT_FOUNT, ex.getStatus());
    }

    @Test
    public void updateVideoStatus_success() {
        Video video = new Video();
        video.setId(10L);
        when(videoRepository.getById(10L)).thenReturn(video);
        when(videoRepository.updateById(any(Video.class))).thenReturn(true);

        User user = buildUser(3L, "auditor", "admin");
        videoService.updateVideoStatus(10L, 2, "down", user);

        assertEquals(Integer.valueOf(2), video.getStatus());
        assertEquals("down", video.getAuditRemark());
        assertEquals(Long.valueOf(3L), video.getUpdateId());
        assertEquals("auditor", video.getUpdateName());
        assertNotNull(video.getUpdateTime());
        verify(videoRepository).updateById(video);
    }

    @Test
    public void incrementPlayCount_updatesWhenVideoExists() {
        Video video = new Video();
        video.setPlayCount(5L);
        when(videoRepository.getById(10L)).thenReturn(video);
        when(videoRepository.updateById(any(Video.class))).thenReturn(true);

        videoService.incrementPlayCount(10L);

        assertEquals(Long.valueOf(6L), video.getPlayCount());
        verify(videoRepository).updateById(video);
    }

    @Test
    public void incrementPlayCount_noUpdateWhenMissing() {
        when(videoRepository.getById(10L)).thenReturn(null);

        videoService.incrementPlayCount(10L);

        verify(videoRepository, never()).updateById(any(Video.class));
    }

    @Test
    public void updateVideo_successWhenAdmin() {
        Video existing = new Video();
        existing.setUserId(1L);
        when(videoRepository.getById(10L)).thenReturn(existing);
        when(videoRepository.updateById(any(Video.class))).thenReturn(true);

        Video input = new Video();
        input.setId(10L);
        User admin = buildUser(99L, "admin", "admin");

        Video result = videoService.updateVideo(input, admin);

        assertSame(input, result);
        assertEquals(Long.valueOf(99L), input.getUpdateId());
        assertEquals("admin", input.getUpdateName());
        assertNotNull(input.getUpdateTime());
        verify(videoRepository).updateById(input);
    }

    @Test
    public void deleteVideo_successWithoutCover() {
        Video video = new Video();
        video.setUserId(1L);
        video.setVideoUrl("http://cdn/video.mp4");
        video.setCoverUrl(null);
        when(videoRepository.getById(10L)).thenReturn(video);
        when(storageService.deleteFile(any(String.class))).thenReturn(true);

        videoService.deleteVideo(10L, buildUser(1L, "u1", "user"));

        verify(videoRepository).removeById(10L);
        verify(storageService, times(1)).deleteFile("http://cdn/video.mp4");
    }

    private User buildUser(Long id, String username, String role) {
        User user = new User();
        user.setId(id);
        user.setUserName(username);
        user.setRole(role);
        user.setCreateTime(new Date());
        return user;
    }
}
