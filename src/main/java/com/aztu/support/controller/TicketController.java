package com.aztu.support.controller;

import com.aztu.support.domain.enums.TicketPriority;
import com.aztu.support.domain.enums.TicketStatus;
import com.aztu.support.dto.common.PageResponse;
import com.aztu.support.dto.ticket.AttachmentResponse;
import com.aztu.support.dto.ticket.CommentRequest;
import com.aztu.support.dto.ticket.CommentResponse;
import com.aztu.support.dto.ticket.CreateTicketRequest;
import com.aztu.support.dto.ticket.TicketDetailResponse;
import com.aztu.support.dto.ticket.TicketSummaryResponse;
import com.aztu.support.dto.ticket.UpdateTicketStatusRequest;
import com.aztu.support.dto.user.UserSummaryResponse;
import com.aztu.support.security.AppUserPrincipal;
import com.aztu.support.security.Authorities;
import com.aztu.support.service.AttachmentService;
import com.aztu.support.service.TicketService;
import com.aztu.support.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tickets")
@PreAuthorize(Authorities.AUTHENTICATED)
public class TicketController {

    private final TicketService ticketService;
    private final AttachmentService attachmentService;
    private final UserService userService;

    public TicketController(TicketService ticketService, AttachmentService attachmentService,
                            UserService userService) {
        this.ticketService = ticketService;
        this.attachmentService = attachmentService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<TicketDetailResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
                                                       @Valid @RequestBody CreateTicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.create(principal.getId(), request));
    }

    @GetMapping
    public PageResponse<TicketSummaryResponse> list(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long platformId,
            @RequestParam(required = false) Long assignedToId,
            @RequestParam(required = false) Boolean mine,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        return PageResponse.from(
                ticketService.list(principal.getId(), principal.roleName(), status, priority,
                        platformId, assignedToId, mine, q, pageable),
                TicketSummaryResponse::from);
    }

    @GetMapping("/{id}")
    public TicketDetailResponse get(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @PathVariable Long id) {
        return ticketService.getDetail(principal.getId(), principal.roleName(), id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(Authorities.SUPPORT)
    public TicketDetailResponse updateStatus(@AuthenticationPrincipal AppUserPrincipal principal,
                                             @PathVariable Long id,
                                             @Valid @RequestBody UpdateTicketStatusRequest request) {
        return ticketService.updateStatus(principal.getId(), principal.roleName(), id, request);
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize(Authorities.SUPPORT)
    public TicketDetailResponse assign(@AuthenticationPrincipal AppUserPrincipal principal,
                                       @PathVariable Long id,
                                       @RequestBody com.aztu.support.dto.ticket.AssignTicketRequest request) {
        return ticketService.assign(principal.getId(), principal.roleName(), id, request.assigneeId());
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(@AuthenticationPrincipal AppUserPrincipal principal,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.addComment(principal.getId(), principal.roleName(), id, request));
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(attachmentService.upload(principal.getId(), principal.roleName(), id, file));
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long attachmentId) {
        AttachmentService.DownloadedFile file =
                attachmentService.download(principal.getId(), principal.roleName(), attachmentId);
        MediaType mediaType = file.contentType() != null
                ? MediaType.parseMediaType(file.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.filename() + "\"")
                .body(file.resource());
    }

    /** Active support staff a ticket can be assigned to. */
    @GetMapping("/assignable-agents")
    @PreAuthorize(Authorities.SUPPORT)
    public List<UserSummaryResponse> assignableAgents() {
        return userService.supportAgents();
    }
}
