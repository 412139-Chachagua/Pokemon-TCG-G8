package ar.edu.utn.frc.tup.piii.dtos.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorApi {

    private String timestamp;
    private Integer status;
    private String error;
    private String code;
    private String message;
    private String path;
    private Map<String, String> details;
}
