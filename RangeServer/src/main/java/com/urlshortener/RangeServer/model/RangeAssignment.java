package com.urlshortener.RangeServer.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RangeAssignment {
    private long rangeStart;
    private long rangeEnd;
    private String assignedTo;
    private long assignedAtMs;
}