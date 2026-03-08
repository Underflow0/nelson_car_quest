package org.nelson.kidbank.dto;

import jakarta.validation.constraints.*;

public class ResetPasswordForm {

    @NotBlank
    @Size(min = 8, max = 100)
    private String newPassword;

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
