package ar.edu.utn.frc.tup.piii.configs;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import static org.junit.jupiter.api.Assertions.*;

class MappersConfigTest {

    private final MappersConfig mappersConfig = new MappersConfig();

    @Test
    void shouldCreateModelMapper() {
        ModelMapper modelMapper = mappersConfig.modelMapper();
        assertNotNull(modelMapper);
    }

    @Test
    void shouldCreateMergerMapper() {
        ModelMapper mergerMapper = mappersConfig.mergerMapper();
        assertNotNull(mergerMapper);
        assertNotNull(mergerMapper.getConfiguration().getPropertyCondition());
    }

    @Test
    void shouldCreateObjectMapper() {
        assertNotNull(mappersConfig.objectMapper());
    }

    @Test
    void modelMapperAndMergerMapperShouldBeDifferent() {
        ModelMapper modelMapper = mappersConfig.modelMapper();
        ModelMapper mergerMapper = mappersConfig.mergerMapper();
        assertNotSame(modelMapper, mergerMapper);
    }
}
