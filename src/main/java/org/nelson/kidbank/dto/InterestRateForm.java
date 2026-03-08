package org.nelson.kidbank.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class InterestRateForm {

    @NotNull
    @DecimalMin(value = "0.0000", message = "Interest rate must be between 0% and 100%.")
    @DecimalMax(value = "1.0000", message = "Interest rate must be between 0% and 100%.")
    @Digits(integer = 1, fraction = 4, message = "Interest rate must have at most 4 decimal places.")
    private BigDecimal rate;

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
}
