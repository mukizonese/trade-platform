package com.tradeplatform.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeDto {
    
    @NotBlank(message = "tradeId is required")
    private String tradeId;
    
    @NotNull(message = "version is required")
    @Positive(message = "version must be positive")
    private Integer version;
    
    @NotBlank(message = "counterPartyId is required")
    private String counterPartyId;
    
    @NotBlank(message = "bookId is required")
    private String bookId;
    
    @NotNull(message = "maturityDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate maturityDate;
}

