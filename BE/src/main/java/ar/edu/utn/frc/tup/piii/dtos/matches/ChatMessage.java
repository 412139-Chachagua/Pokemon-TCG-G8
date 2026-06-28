package ar.edu.utn.frc.tup.piii.dtos.matches;

public record ChatMessage(
        String senderId,
        String senderName,
        String content,
        long timestamp
) {
}
