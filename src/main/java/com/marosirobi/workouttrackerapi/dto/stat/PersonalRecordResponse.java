package com.marosirobi.workouttrackerapi.dto.stat;

import lombok.Builder;

@Builder
public record PersonalRecordResponse(Long exerciseId,
                                     Double maxWeight,
                                     String exerciseName) {
}
