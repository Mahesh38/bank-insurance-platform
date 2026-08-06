package com.bank.identity.authz.application;

import com.bank.identity.authz.domain.IdentityTypes.UserType;
import com.bank.identity.authz.persistence.BusinessUserEntity;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityResolutionService {

    private final BusinessUserRepository users;

    public IdentityResolutionService(BusinessUserRepository users) {
        this.users = users;
    }

    @Transactional
    public BusinessUserEntity resolve(ResolveIdentityCommand command) {
        return users.findByProviderAndProviderSubject(command.provider(), command.providerSubject())
            .map(existing -> {
                existing.refreshProfile(command.username(), command.email(), command.employeeId());
                return existing;
            })
            .orElseGet(() -> createBankEmployee(command));
    }

    private BusinessUserEntity createBankEmployee(ResolveIdentityCommand command) {
        if (command.userType() != UserType.BANK_EMPLOYEE) {
            throw new IllegalStateException("Partner identity must be approved before first login");
        }
        return users.save(BusinessUserEntity.bankEmployee(command.provider(), command.providerSubject(),
            command.username(), command.email(), command.employeeId()));
    }

    public record ResolveIdentityCommand(
        String provider,
        String providerSubject,
        UserType userType,
        String username,
        String email,
        String employeeId
    ) {}
}
