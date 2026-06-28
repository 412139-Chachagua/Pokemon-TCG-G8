package ar.edu.utn.frc.tup.piii.dtos.matches;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchSummaryResponse(
        String id,
        String winnerName,
        String loserName,
        int totalTurns,
        Instant createdAt,
        Long durationSeconds,
        String finishReason
) {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yy").withZone(ZoneId.of("UTC"));
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.of("UTC"));

    public String formattedDate() {
        return createdAt != null ? DATE_FMT.format(createdAt) : "";
    }

    public String formattedTime() {
        return createdAt != null ? TIME_FMT.format(createdAt) : "";
    }

    public String formattedDuration() {
        if (durationSeconds == null) return "-";
        long m = durationSeconds / 60;
        long s = durationSeconds % 60;
        return m + "m " + s + "s";
    }
}
