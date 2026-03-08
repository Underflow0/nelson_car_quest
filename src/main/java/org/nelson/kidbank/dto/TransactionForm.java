package org.nelson.kidbank.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class TransactionForm {

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0.")
    @Digits(integer = 17, fraction = 2, message = "Amount must have at most 2 decimal places.")
    private BigDecimal amount;

    @Size(max = 500)
    private String note;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
