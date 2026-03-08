package org.nelson.kidbank.controller;

import org.nelson.kidbank.entity.*;
import org.nelson.kidbank.service.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/child")
@PreAuthorize("hasRole('CHILD')")
public class ChildController {

    private final UserService userService;
    private final AccountService accountService;

    public ChildController(UserService userService, AccountService accountService) {
        this.userService = userService;
        this.accountService = accountService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
        User child = resolveChild(principal);
        SavingsAccount account = accountService.findByChildUserId(child.getId())
                .orElseThrow(() -> new IllegalStateException("No account found for child."));

        model.addAttribute("child", child);
        model.addAttribute("account", account);
        model.addAttribute("transactions", accountService.getLedgerForChild(account.getId(), child.getId()));
        return "child/dashboard";
    }

    @GetMapping("/accounts/{accountId}/ledger")
    public String viewLedger(@PathVariable Long accountId,
                             @AuthenticationPrincipal UserDetails principal,
                             Model model) {
        User child = resolveChild(principal);
        SavingsAccount account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));

        if (!account.getChildUserId().equals(child.getId())) {
            throw new AccessDeniedException("Access denied.");
        }

        model.addAttribute("child", child);
        model.addAttribute("account", account);
        model.addAttribute("transactions", accountService.getLedgerForChild(accountId, child.getId()));
        return "child/ledger";
    }

    private User resolveChild(UserDetails principal) {
        return userService.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB."));
    }
}
