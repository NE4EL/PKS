package ru.mirea.isp.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.isp.dto.ImportDtos.ImportMode;
import ru.mirea.isp.dto.ImportDtos.ImportPreview;
import ru.mirea.isp.dto.ImportDtos.ImportResult;
import ru.mirea.isp.dto.PageResponse;
import ru.mirea.isp.dto.ConnectionRequestDtos.BulkRequest;
import ru.mirea.isp.dto.ConnectionRequestDtos.BulkResult;
import ru.mirea.isp.dto.ConnectionRequestDtos.ConnectionRequestRequest;
import ru.mirea.isp.dto.ConnectionRequestDtos.ConnectionRequestResponse;
import ru.mirea.isp.service.CsvConnectionRequestService;
import ru.mirea.isp.service.ConnectionRequestService;
import ru.mirea.isp.web.PageableFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

/** REST-контур управления заявками. */
@RestController
@RequestMapping("/api/requests")
@SecurityRequirement(name = "bearerAuth")
public class ConnectionRequestController {
    private final ConnectionRequestService requests;
    private final CsvConnectionRequestService csv;
    private final PageableFactory pageableFactory;

    public ConnectionRequestController(ConnectionRequestService requests, CsvConnectionRequestService csv, PageableFactory pageableFactory) {
        this.requests = requests;
        this.csv = csv;
        this.pageableFactory = pageableFactory;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','OPERATOR','ADMIN')")
    public PageResponse<ConnectionRequestResponse> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {
        return PageResponse.from(requests.search(q, pageableFactory.requests(page, size, sort)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','OPERATOR','ADMIN')")
    public ConnectionRequestResponse get(@PathVariable UUID id) {
        return requests.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ResponseEntity<ConnectionRequestResponse> create(@Valid @RequestBody ConnectionRequestRequest request) {
        return ResponseEntity.status(201).body(requests.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ConnectionRequestResponse update(@PathVariable UUID id, @Valid @RequestBody ConnectionRequestRequest request) {
        return requests.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ConnectionRequestResponse archive(@PathVariable UUID id) {
        return requests.archive(id);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ConnectionRequestResponse restore(@PathVariable UUID id) {
        return requests.restore(id);
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> permanentDelete(
            @PathVariable UUID id,
            @RequestHeader(name = "X-Confirm-Delete", required = false) String confirmation) {
        requests.permanentDelete(id, confirmation);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public BulkResult bulk(@Valid @RequestBody BulkRequest request) {
        return requests.bulk(request);
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ImportPreview previewImport(@RequestPart("file") MultipartFile file,
                                       @RequestParam(defaultValue = "CREATE_ONLY") ImportMode mode) {
        return csv.preview(file, mode);
    }

    @PostMapping(value = "/import/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public ImportResult applyImport(@RequestPart("file") MultipartFile file,
                                    @RequestParam(defaultValue = "CREATE_ONLY") ImportMode mode) {
        return csv.apply(file, mode);
    }

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    @PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
    public void export(@RequestParam(defaultValue = "") String q, HttpServletResponse response) throws IOException {
        String filename = "requests-" + LocalDate.now() + ".csv";
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" +
                URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"));
        csv.export(q, response.getOutputStream());
    }
}
