package ar.edu.utn.frc.tup.piii.dtos.cards;

public record CardSearchRequest(
        String query,
        String supertype,
        String setCode,
        String stage,
        Integer page,
        Integer size
) {
}
