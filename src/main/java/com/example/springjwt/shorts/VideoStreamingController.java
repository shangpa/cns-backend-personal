package com.example.springjwt.shorts;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.http.MediaTypeFactory;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
public class VideoStreamingController {

    // WebConfig와 동일 기준(프로젝트 루트/uploads)
    private static final Path UPLOAD_ROOT =
            Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath();

    /** 동영상 전용 스트리밍 (예: /uploads/videos/xxx.mp4) */
    @RequestMapping(path = "/uploads/videos/**", method = {RequestMethod.GET, RequestMethod.HEAD})
    public ResponseEntity<ResourceRegion> getVideo(HttpServletRequest request) {
        // e.g. /uploads/videos/abc.mp4  ->  videos/abc.mp4
        String uri = request.getRequestURI();
        String relative = uri.replaceFirst("^/uploads/", ""); // "videos/abc.mp4"

        // 실제 파일 경로
        Path filePath = UPLOAD_ROOT.resolve(relative).normalize();

        // ✅ 디렉터리 탈출 방지: uploads 밖이면 차단
        if (!filePath.startsWith(UPLOAD_ROOT)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        File file = filePath.toFile();
        if (!file.exists() || !file.isFile()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        FileSystemResource resource = new FileSystemResource(file);
        long contentLength;
        try {
            contentLength = resource.contentLength();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        ResourceRegion region;

        if (rangeHeader == null) {
            // Range 없으면 처음 1MB만 응답 (Exo가 이어서 range 요청)
            long chunk = Math.min(1 * 1024 * 1024L, contentLength);
            region = new ResourceRegion(resource, 0, chunk);
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .contentType(mediaType)
                    .body(region);
        } else {
            List<HttpRange> ranges = HttpRange.parseRanges(rangeHeader);
            if (ranges.isEmpty()) {
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).build();
            }
            HttpRange range = ranges.get(0);
            long start = range.getRangeStart(contentLength);
            long end   = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(1 * 1024 * 1024L, end - start + 1); // 1MB 청크

            region = new ResourceRegion(resource, start, rangeLength);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .contentType(mediaType)
                    .body(region);
        }
    }
}
