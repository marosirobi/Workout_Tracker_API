package com.marosirobi.workouttrackerapi.dto.stat;

import lombok.Builder;

@Builder
public record BestEffortResponse(Integer distanceMeters,
                                 Double durationSeconds,
                                 Double startOffsetMeters,
                                 Integer rank) {
}
