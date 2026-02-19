package com.thanaphon.banking_system_with_ai.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequest {

    @NotBlank(message = "Owner name is required")
    @Size(max = 100, message = "Owner name must not exceed 100 characters")
    private String ownerName;

    @NotNull(message = "Initial deposit is required")
    @DecimalMin(value = "100.00", message = "Minimum opening deposit is $100.00")
    @DecimalMax(value = "1000000.00", message = "Maximum transaction amount is $1,000,000.00")
    @Digits(integer = 13, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal initialDeposit;
}
