package org.nelson.kidbank.controller;

import org.nelson.kidbank.entity.*;
import org.nelson.kidbank.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/child")
@PreAuthorize("hasRole('CHILD')")
public class ChildController {

    private final UserService userService;
    private final AccountService accountService;
    private final StatementPdfService statementPdfService;

    public ChildController(UserService userService, AccountService accountService,
                           StatementPdfService statementPdfService) {
        this.userService = userService;
        this.accountService = accountService;
        this.statementPdfService = statementPdfService;
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

    @GetMapping("/accounts/{accountId}/statement")
    @ResponseBody
    public ResponseEntity<byte[]> downloadStatement(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "3m") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @AuthenticationPrincipal UserDetails principal) {

        User child = resolveChild(principal);
        SavingsAccount account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));

        if (!account.getChildUserId().equals(child.getId())) {
            throw new AccessDeniedException("Access denied.");
        }

        LocalDate today = LocalDate.now();
        LocalDate fromDate = switch (period) {
            case "1m"    -> today.minusMonths(1);
            case "6m"    -> today.minusMonths(6);
            case "1y"    -> today.minusYears(1);
            case "custom" -> LocalDate.parse(from);
            default      -> today.minusMonths(3);
        };
        LocalDate toDate = "custom".equals(period) && to != null ? LocalDate.parse(to) : today;

        var transactions = accountService.getTransactionsForChildStatement(
                accountId, child.getId(),
                fromDate.atStartOfDay(),
                toDate.plusDays(1).atStartOfDay());

        byte[] pdf = statementPdfService.generate(account, child, transactions, fromDate, toDate);

        String safeName = child.getEffectiveName().replaceAll("[^a-zA-Z0-9]", "_");
        String filename = "statement_" + safeName + "_" + fromDate + "_to_" + toDate + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }

    private User resolveChild(UserDetails principal) {
        return userService.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB."));
    }
}
