package org.nelson.kidbank.controller;

import jakarta.validation.Valid;
import org.nelson.kidbank.dto.*;
import org.nelson.kidbank.entity.*;
import org.nelson.kidbank.exception.*;
import org.nelson.kidbank.service.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/parent")
@PreAuthorize("hasRole('PARENT')")
public class ParentController {

    private final UserService userService;
    private final AccountService accountService;

    public ParentController(UserService userService, AccountService accountService) {
        this.userService = userService;
        this.accountService = accountService;
    }

    // ---- Dashboard ----

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
        User parent = resolveParent(principal);
        List<User> children = userService.findChildrenOfParent(parent.getId());

        // Build child -> account map
        Map<Long, SavingsAccount> accountMap = new LinkedHashMap<>();
        for (User child : children) {
            accountService.findByChildUserId(child.getId())
                    .ifPresent(acc -> accountMap.put(child.getId(), acc));
        }

        model.addAttribute("parent", parent);
        model.addAttribute("children", children);
        model.addAttribute("accountMap", accountMap);
        model.addAttribute("createChildForm", new CreateChildForm());
        return "parent/dashboard";
    }

    // ---- Create child + account ----

    @PostMapping("/children")
    public String createChild(@Valid @ModelAttribute CreateChildForm form,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal UserDetails principal,
                              RedirectAttributes redirectAttrs,
                              Model model) {
        User parent = resolveParent(principal);

        if (bindingResult.hasErrors()) {
            // Re-render dashboard with errors
            List<User> children = userService.findChildrenOfParent(parent.getId());
            Map<Long, SavingsAccount> accountMap = buildAccountMap(children);
            model.addAttribute("parent", parent);
            model.addAttribute("children", children);
            model.addAttribute("accountMap", accountMap);
            model.addAttribute("createChildForm", form);
            model.addAttribute("createChildErrors", true);
            return "parent/dashboard";
        }

        try {
            User child = userService.createChildUser(
                    form.getUsername(), form.getDisplayName(), form.getPassword(), parent.getId());
            accountService.createAccount(child.getId(), parent.getId(), java.math.BigDecimal.ZERO);
            redirectAttrs.addFlashAttribute("successMessage",
                    "Child account created for " + child.getEffectiveName() + ".");
        } catch (DuplicateUsernameException | AccountAlreadyExistsException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/parent/dashboard";
    }

    // ---- Deposit ----

    @PostMapping("/accounts/{accountId}/deposit")
    public String deposit(@PathVariable Long accountId,
                          @Valid @ModelAttribute TransactionForm form,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal UserDetails principal,
                          RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute("errorMessage", getFirstError(bindingResult));
            return "redirect:/parent/dashboard";
        }
        User parent = resolveParent(principal);
        try {
            accountService.deposit(accountId, form.getAmount(), form.getNote(), parent.getId());
            redirectAttrs.addFlashAttribute("successMessage", "Deposit successful.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/parent/dashboard";
    }

    // ---- Withdraw ----

    @PostMapping("/accounts/{accountId}/withdraw")
    public String withdraw(@PathVariable Long accountId,
                           @Valid @ModelAttribute TransactionForm form,
                           BindingResult bindingResult,
                           @AuthenticationPrincipal UserDetails principal,
                           RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute("errorMessage", getFirstError(bindingResult));
            return "redirect:/parent/dashboard";
        }
        User parent = resolveParent(principal);
        try {
            accountService.withdraw(accountId, form.getAmount(), form.getNote(), parent.getId());
            redirectAttrs.addFlashAttribute("successMessage", "Withdrawal successful.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/parent/dashboard";
    }

    // ---- Update interest rate ----

    @PostMapping("/accounts/{accountId}/interest-rate")
    public String setInterestRate(@PathVariable Long accountId,
                                  @Valid @ModelAttribute InterestRateForm form,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal UserDetails principal,
                                  RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute("errorMessage", "Interest rate must be between 0% and 100%.");
            return "redirect:/parent/dashboard";
        }
        User parent = resolveParent(principal);
        try {
            accountService.setInterestRate(accountId, form.getRate(), parent.getId());
            redirectAttrs.addFlashAttribute("successMessage", "Interest rate updated.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/parent/dashboard";
    }

    // ---- Reset child password ----

    @PostMapping("/children/{childId}/reset-password")
    public String resetPassword(@PathVariable Long childId,
                                @Valid @ModelAttribute ResetPasswordForm form,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal UserDetails principal,
                                RedirectAttributes redirectAttrs) {
        if (bindingResult.hasErrors()) {
            redirectAttrs.addFlashAttribute("errorMessage", "New password must be at least 8 characters.");
            return "redirect:/parent/dashboard";
        }
        User parent = resolveParent(principal);
        try {
            userService.resetChildPassword(childId, parent.getId(), form.getNewPassword());
            redirectAttrs.addFlashAttribute("successMessage", "Password reset successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/parent/dashboard";
    }

    // ---- Close account ----

    @PostMapping("/accounts/{accountId}/close")
    public String closeAccount(@PathVariable Long accountId,
                               @AuthenticationPrincipal UserDetails principal,
                               RedirectAttributes redirectAttrs) {
        User parent = resolveParent(principal);
        try {
            accountService.closeAccount(accountId, parent.getId());
            redirectAttrs.addFlashAttribute("successMessage", "Account closed.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/parent/dashboard";
    }

    // ---- View ledger ----

    @GetMapping("/accounts/{accountId}/ledger")
    public String viewLedger(@PathVariable Long accountId,
                             @AuthenticationPrincipal UserDetails principal,
                             Model model) {
        User parent = resolveParent(principal);
        SavingsAccount account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));
        User child = userService.findById(account.getChildUserId())
                .orElseThrow(() -> new IllegalArgumentException("Child user not found."));

        model.addAttribute("account", account);
        model.addAttribute("child", child);
        model.addAttribute("transactions", accountService.getLedger(accountId, parent.getId()));
        return "parent/ledger";
    }

    // ---- Helpers ----

    private User resolveParent(UserDetails principal) {
        return userService.findByUsername(principal.getUsername())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB."));
    }

    private Map<Long, SavingsAccount> buildAccountMap(List<User> children) {
        Map<Long, SavingsAccount> map = new LinkedHashMap<>();
        for (User child : children) {
            accountService.findByChildUserId(child.getId()).ifPresent(acc -> map.put(child.getId(), acc));
        }
        return map;
    }

    private String getFirstError(BindingResult br) {
        return br.getAllErrors().isEmpty() ? "Validation error."
                : br.getAllErrors().get(0).getDefaultMessage();
    }
}
