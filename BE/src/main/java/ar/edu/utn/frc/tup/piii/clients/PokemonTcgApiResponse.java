package ar.edu.utn.frc.tup.piii.clients;

import java.util.List;

import ar.edu.utn.frc.tup.piii.dtos.cards.PokemonTcgApiCardDto;

public class PokemonTcgApiResponse {
    private List<PokemonTcgApiCardDto> data;
    private int totalCount;

    public List<PokemonTcgApiCardDto> getData() {
        return data;
    }

    public void setData(List<PokemonTcgApiCardDto> data) {
        this.data = data;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
