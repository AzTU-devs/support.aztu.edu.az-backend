package com.aztu.support.dto.dashboard;

import com.aztu.support.domain.enums.RoleName;

/**
 * Role-aware dashboard counters. Fields not relevant to the caller's role are
 * left null so the frontend can render exactly what applies.
 */
public record DashboardStatsResponse(
        RoleName role,
        // Personal
        Long myTickets,
        Long myOpenTickets,
        // Support / admin
        Long assignedToMe,
        Long totalTickets,
        Long openTickets,
        Long inProgressTickets,
        Long resolvedTickets,
        Long closedTickets,
        // Admin
        Long pendingApprovals,
        Long totalUsers,
        // Everyone
        Long unreadNotifications) {
}
