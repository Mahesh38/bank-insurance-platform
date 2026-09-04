package com.bank.insurance.onesb.api.v1;

import com.bank.insurance.onesb.api.v1.dto.MasterLookupRequest;
import com.bank.insurance.onesb.api.v1.dto.MasterLookupResponse;
import com.bank.insurance.onesb.application.MasterDataService;
import com.bank.insurance.onesb.application.MasterLookupOutcome;
import com.bank.common.domain.LookupValue;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bank-facing master / enum lookup API.
 */
@RestController
@RequestMapping("/v1/master-data")
public class MasterDataController {

    public static final String CACHE_HEADER = "X-Master-Cache";

    private final MasterDataService masterDataService;

    public MasterDataController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @PostMapping("/lookup")
    public ResponseEntity<MasterLookupResponse> lookup(@Valid @RequestBody MasterLookupRequest request) {
        MasterLookupOutcome outcome = masterDataService.lookup(
                request.lob(),
                request.lookUpCategory(),
                request.entityIds(),
                request.manufacturerId());

        MasterLookupResponse body = toResponse(outcome);
        return ResponseEntity.ok()
                .header(CACHE_HEADER, outcome.cacheHeader())
                .body(body);
    }

    private static MasterLookupResponse toResponse(MasterLookupOutcome outcome) {
        Map<String, List<MasterLookupResponse.CodeLabel>> lookups = new LinkedHashMap<>();
        for (Map.Entry<String, List<LookupValue>> e : outcome.lookups().entrySet()) {
            List<MasterLookupResponse.CodeLabel> values = e.getValue().stream()
                    .map(v -> new MasterLookupResponse.CodeLabel(v.code(), v.label()))
                    .collect(Collectors.toList());
            lookups.put(e.getKey(), values);
        }
        return new MasterLookupResponse(
                outcome.lob(),
                lookups,
                new MasterLookupResponse.CacheInfo(outcome.cacheHit(), outcome.stale()));
    }
}
