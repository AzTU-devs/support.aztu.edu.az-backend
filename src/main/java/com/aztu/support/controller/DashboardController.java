package com.aztu.support.controller;

import com.aztu.support.dto.dashboard.DashboardStatsResponse;
import com.aztu.support.security.AppUserPrincipal;
import com.aztu.support.security.Authorities;
import com.aztu.support.service.TicketService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize(Authorities.AUTHENTICATED)
public class DashboardController {

    private final TicketService ticketService;

    public DashboardController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public DashboardStatsResponse stats(@AuthenticationPrincipal AppUserPrincipal principal) {
        return ticketService.dashboardStats(principal.getId(), principal.roleName());
    }
}
