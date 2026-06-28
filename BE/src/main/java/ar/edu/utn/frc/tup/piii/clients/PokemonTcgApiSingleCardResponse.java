package ar.edu.utn.frc.tup.piii.clients;

import ar.edu.utn.frc.tup.piii.dtos.cards.PokemonTcgApiCardDto;

public class PokemonTcgApiSingleCardResponse {
    private PokemonTcgApiCardDto data;

    public PokemonTcgApiCardDto getData() {
        return data;
    }

    public void setData(PokemonTcgApiCardDto data) {
        this.data = data;
    }
}
