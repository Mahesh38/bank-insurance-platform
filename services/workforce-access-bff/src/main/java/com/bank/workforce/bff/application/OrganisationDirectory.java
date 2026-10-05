package com.bank.workforce.bff.application;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Branch / vertical / SP / RM lookup. Bank HR mapping is the system of record;
 * this is the R0 fixture so NIP-APP can complete assignment screens ({@code D-021}
 * vertical is an optional filter between branch and SP).
 */
@Service
public class OrganisationDirectory {

    private static final List<Branch> BRANCHES = List.of(
        new Branch("BR-MUM-001", "Mumbai Fort", "MUM001"),
        new Branch("BR-PUN-001", "Pune Camp", "PUN001")
    );

    private static final List<Vertical> VERTICALS = List.of(
        new Vertical("VERT-MUM-RET", "Retail", "BR-MUM-001"),
        new Vertical("VERT-MUM-WLT", "Wealth", "BR-MUM-001"),
        new Vertical("VERT-PUN-RET", "Retail", "BR-PUN-001")
    );

    private static final List<SpecifiedPerson> SPS = List.of(
        new SpecifiedPerson("SP-1001", "Anita Sharma", "BR-MUM-001", "VERT-MUM-RET"),
        new SpecifiedPerson("SP-1002", "Rahul Desai", "BR-MUM-001", "VERT-MUM-WLT"),
        new SpecifiedPerson("SP-2001", "Meera Kulkarni", "BR-PUN-001", "VERT-PUN-RET")
    );

    private static final List<RelationshipManager> RMS = List.of(
        new RelationshipManager("RM-5001", "Kiran Joshi", "VERT-MUM-RET", "BR-MUM-001"),
        new RelationshipManager("RM-5002", "Priya Nair", "VERT-MUM-WLT", "BR-MUM-001"),
        new RelationshipManager("RM-6001", "Amit Patil", "VERT-PUN-RET", "BR-PUN-001")
    );

    public List<Branch> branches() {
        return BRANCHES;
    }

    public List<Vertical> verticalsForBranch(String branchId) {
        return VERTICALS.stream().filter(vertical -> vertical.branchId().equals(branchId)).toList();
    }

    public List<SpecifiedPerson> specifiedPersonsForBranch(String branchId) {
        return SPS.stream().filter(sp -> sp.branchId().equals(branchId)).toList();
    }

    public List<RelationshipManager> relationshipManagersForVertical(String verticalId) {
        return RMS.stream().filter(rm -> rm.verticalId().equals(verticalId)).toList();
    }

    public boolean isKnownBranch(String branchId) {
        return BRANCHES.stream().anyMatch(branch -> branch.branchId().equals(branchId));
    }

    public boolean isKnownVertical(String verticalId) {
        return VERTICALS.stream().anyMatch(vertical -> vertical.verticalId().equals(verticalId));
    }

    public record Branch(String branchId, String branchName, String branchCode) {}

    public record Vertical(String verticalId, String verticalName, String branchId) {}

    public record SpecifiedPerson(String empId, String empName, String branchId, String verticalId) {}

    public record RelationshipManager(String empId, String empName, String verticalId, String branchId) {}
}
