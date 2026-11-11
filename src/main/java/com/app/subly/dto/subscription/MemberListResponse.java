package com.app.subly.dto.subscription;

import java.util.List;

public record MemberListResponse(List<SubscriptionMemberResponse> members, boolean full) {}
