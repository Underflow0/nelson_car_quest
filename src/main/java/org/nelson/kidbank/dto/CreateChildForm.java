package org.nelson.kidbank.dto;

import jakarta.validation.constraints.*;

public class CreateChildForm {

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Username may only contain letters, numbers, underscores, and hyphens.")
    private String username;

    @Size(max = 100)
    private String displayName;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
