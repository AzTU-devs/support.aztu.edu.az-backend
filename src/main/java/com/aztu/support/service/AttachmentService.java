package com.aztu.support.service;

import com.aztu.support.config.AppProperties;
import com.aztu.support.domain.Attachment;
import com.aztu.support.domain.Ticket;
import com.aztu.support.domain.User;
import com.aztu.support.domain.enums.RoleName;
import com.aztu.support.dto.ticket.AttachmentResponse;
import com.aztu.support.exception.ApiException;
import com.aztu.support.repository.AttachmentRepository;
import com.aztu.support.repository.UserRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final TicketService ticketService;
    private final Path uploadRoot;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             UserRepository userRepository,
                             TicketService ticketService,
                             AppProperties props) {
        this.attachmentRepository = attachmentRepository;
        this.userRepository = userRepository;
        this.ticketService = ticketService;
        this.uploadRoot = Paths.get(props.getStorage().getUploadDir()).toAbsolutePath().normalize();
    }

    @Transactional
    public AttachmentResponse upload(Long userId, RoleName role, Long ticketId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("No file was provided.");
        }
        Ticket ticket = ticketService.loadTicket(ticketId);
        ticketService.requireView(ticket, userId, role);
        User user = loadUser(userId);

        String original = sanitize(file.getOriginalFilename());
        String stored = UUID.randomUUID() + extensionOf(original);
        try {
            Files.createDirectories(uploadRoot);
            Path target = uploadRoot.resolve(stored).normalize();
            if (!target.startsWith(uploadRoot)) {
                throw ApiException.badRequest("Invalid file name.");
            }
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store attachment", e);
        }

        Attachment attachment = new Attachment();
        attachment.setTicket(ticket);
        attachment.setUploadedBy(user);
        attachment.setOriginalFilename(original);
        attachment.setStoredFilename(stored);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        return AttachmentResponse.from(attachmentRepository.save(attachment));
    }

    @Transactional(readOnly = true)
    public DownloadedFile download(Long userId, RoleName role, Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> ApiException.notFound("Attachment not found."));
        ticketService.requireView(attachment.getTicket(), userId, role);
        try {
            Path path = uploadRoot.resolve(attachment.getStoredFilename()).normalize();
            if (!path.startsWith(uploadRoot) || !Files.exists(path)) {
                throw ApiException.notFound("Attachment file is missing.");
            }
            Resource resource = new ByteArrayResource(Files.readAllBytes(path));
            return new DownloadedFile(resource, attachment.getOriginalFilename(),
                    attachment.getContentType());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read attachment", e);
        }
    }

    private User loadUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("User not found."));
    }

    private String sanitize(String filename) {
        if (filename == null || filename.isBlank()) {
            return "attachment";
        }
        return Paths.get(filename).getFileName().toString();
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    /** Resource + metadata returned to the download controller. */
    public record DownloadedFile(Resource resource, String filename, String contentType) {
    }
}
