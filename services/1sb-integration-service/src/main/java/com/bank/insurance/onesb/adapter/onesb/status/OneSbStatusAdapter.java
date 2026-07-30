package com.bank.insurance.onesb.adapter.onesb.status;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.model.ApplicationStatus;
import com.bank.insurance.onesb.domain.model.BankStage;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.port.outbound.OneSbStatusPort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 1SB application status adapter — {@code POST /LifeTerm/prostat/} (FUNC-009).
 * TERM only for Phase 3; parses nested manufacturer/product or flat status fields.
 */
@Component
public class OneSbStatusAdapter implements OneSbStatusPort {

    static final String PATH = "/LifeTerm/prostat/";

    private final OneSbHttpClient httpClient;
    private final SecretProvider secretProvider;

    public OneSbStatusAdapter(OneSbHttpClient httpClient, SecretProvider secretProvider) {
        this.httpClient = httpClient;
        this.secretProvider = secretProvider;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ApplicationStatus fetchStatus(String applicationNumber, String lob,
                                         String insuranceCompanyCode, String policyNo) {
        assertTermOnly(lob);

        Map<String, Object> payload = buildPayload(applicationNumber, insuranceCompanyCode, policyNo);
        Map<String, Object> response = httpClient.post(PATH, payload, Map.class);
        return parseStatus(applicationNumber, lob, insuranceCompanyCode, response);
    }

    Map<String, Object> buildPayload(String applicationNumber, String insuranceCompanyCode,
                                     String policyNo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("insuranceCompanyCode", insuranceCompanyCode);
        payload.put("applicationNo", applicationNumber);
        payload.put("distributorID", secretProvider.getDistributorId());
        if (StringUtils.hasText(policyNo)) {
            payload.put("policyNo", policyNo);
        }
        return payload;
    }

    @SuppressWarnings("unchecked")
    static ApplicationStatus parseStatus(String applicationNumber, String lob,
                                         String insuranceCompanyCode,
                                         Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            throw notFound(applicationNumber);
        }

        ParsedFields fields = extractFields(response);
        if (fields == null || !StringUtils.hasText(fields.applicationStatus())) {
            throw notFound(applicationNumber);
        }

        String policyNumber = fields.policyNo();
        String companyCode = StringUtils.hasText(fields.insuranceCompanyCode())
                ? fields.insuranceCompanyCode()
                : insuranceCompanyCode;

        return new ApplicationStatus(
                applicationNumber,
                BankStage.UNKNOWN,
                fields.applicationStatus().trim(),
                fields.manufacturerAppStatus(),
                fields.manufacturerAppStatusDesc(),
                policyNumber,
                companyCode,
                parseLob(lob),
                Instant.now());
    }

    @SuppressWarnings("unchecked")
    private static ParsedFields extractFields(Map<String, Object> response) {
        ParsedFields nested = extractFromManufacturer(response);
        if (nested != null && StringUtils.hasText(nested.applicationStatus())) {
            return nested;
        }

        ParsedFields flat = extractFromStatusNode(response, null);
        if (flat != null && StringUtils.hasText(flat.applicationStatus())) {
            return flat;
        }

        Object statusNode = response.get("applicationStatus");
        if (statusNode instanceof Map<?, ?> statusMap) {
            ParsedFields fromMap = extractFromStatusNode((Map<String, Object>) statusMap, null);
            if (fromMap != null && StringUtils.hasText(fromMap.applicationStatus())) {
                return fromMap;
            }
        }

        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            return extractFields((Map<String, Object>) dataMap);
        }

        // Empty manufacturer list / no usable status → treat as not found (prefer 404)
        Object manufacturer = response.get("manufacturer");
        if (manufacturer instanceof List<?> list && list.isEmpty()) {
            return null;
        }
        return nested != null ? nested : flat;
    }

    @SuppressWarnings("unchecked")
    private static ParsedFields extractFromManufacturer(Map<String, Object> response) {
        Object manufacturer = response.get("manufacturer");
        if (!(manufacturer instanceof List<?> manufacturers) || manufacturers.isEmpty()) {
            return null;
        }

        for (Object mfrObj : manufacturers) {
            if (!(mfrObj instanceof Map<?, ?>)) {
                continue;
            }
            Map<String, Object> mfr = (Map<String, Object>) mfrObj;
            String companyCode = firstText(mfr, "insuranceCompanyCode", "manufacturerId", "code");

            Object products = mfr.get("product");
            if (!(products instanceof List<?> productList) || productList.isEmpty()) {
                ParsedFields fromMfr = extractFromStatusNode(mfr, companyCode);
                if (fromMfr != null && StringUtils.hasText(fromMfr.applicationStatus())) {
                    return fromMfr;
                }
                continue;
            }

            for (Object prodObj : productList) {
                if (!(prodObj instanceof Map<?, ?>)) {
                    continue;
                }
                Map<String, Object> product = (Map<String, Object>) prodObj;
                ParsedFields fromProduct = extractFromProduct(product, companyCode);
                if (fromProduct != null && StringUtils.hasText(fromProduct.applicationStatus())) {
                    return fromProduct;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static ParsedFields extractFromProduct(Map<String, Object> product, String companyCode) {
        String policyNo = null;
        Object policyDetails = product.get("policyDetails");
        if (policyDetails instanceof Map<?, ?> pd) {
            policyNo = firstText((Map<String, Object>) pd, "policyNo", "policyNumber", "policy_no");
        }
        if (!StringUtils.hasText(policyNo)) {
            policyNo = firstText(product, "policyNo", "policyNumber");
        }

        Object statusNode = product.get("applicationStatus");
        if (statusNode instanceof Map<?, ?> statusMap) {
            ParsedFields fromNested = extractFromStatusNode((Map<String, Object>) statusMap, companyCode);
            if (fromNested != null) {
                return new ParsedFields(
                        fromNested.applicationStatus(),
                        fromNested.applicationStatusDesc(),
                        fromNested.manufacturerAppStatus(),
                        fromNested.manufacturerAppStatusDesc(),
                        StringUtils.hasText(policyNo) ? policyNo : fromNested.policyNo(),
                        companyCode);
            }
        }

        ParsedFields flat = extractFromStatusNode(product, companyCode);
        if (flat != null) {
            return new ParsedFields(
                    flat.applicationStatus(),
                    flat.applicationStatusDesc(),
                    flat.manufacturerAppStatus(),
                    flat.manufacturerAppStatusDesc(),
                    StringUtils.hasText(policyNo) ? policyNo : flat.policyNo(),
                    companyCode);
        }
        return null;
    }

    private static ParsedFields extractFromStatusNode(Map<String, Object> node, String companyCode) {
        if (node == null) {
            return null;
        }
        String applicationStatus = firstText(node, "applicationStatus");
        // When applicationStatus is itself a nested map value already handled; string only here
        if (applicationStatus != null && applicationStatus.startsWith("{")) {
            return null;
        }
        // If key holds a non-string (Map), firstText returns toString — skip if not a status-like token
        Object raw = node.get("applicationStatus");
        if (raw instanceof Map<?, ?>) {
            return null;
        }

        String desc = firstText(node, "applicationStatusDesc", "desc");
        String mfrStatus = firstText(node, "manufacturerAppStatus");
        String mfrDesc = firstText(node, "manufacturerAppStatusDesc");
        String policyNo = firstText(node, "policyNo", "policyNumber");

        if (!StringUtils.hasText(applicationStatus)
                && !StringUtils.hasText(mfrStatus)
                && !StringUtils.hasText(policyNo)) {
            return null;
        }

        return new ParsedFields(applicationStatus, desc, mfrStatus, mfrDesc, policyNo, companyCode);
    }

    private static void assertTermOnly(String lob) {
        if (!StringUtils.hasText(lob)) {
            throw unsupportedLob(null);
        }
        String normalised = lob.trim().toUpperCase(Locale.ROOT);
        if (!"TERM".equals(normalised)) {
            throw unsupportedLob(lob);
        }
    }

    private static Lob parseLob(String lob) {
        if (!StringUtils.hasText(lob)) {
            return Lob.TERM;
        }
        try {
            return Lob.valueOf(lob.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Lob.TERM;
        }
    }

    private static String firstText(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object v = map.get(key);
            if (v != null && !(v instanceof Map) && !(v instanceof List)) {
                String s = v.toString();
                if (StringUtils.hasText(s)) {
                    return s;
                }
            }
        }
        return null;
    }

    private static ServiceException notFound(String applicationNumber) {
        return new ServiceException(ServiceErrorResponse.builder()
                .title("Not Found")
                .status(404)
                .detail("Application not found: " + applicationNumber)
                .code(ErrorCodes.RESOURCE_NOT_FOUND)
                .retryable(false)
                .build());
    }

    private static ServiceException unsupportedLob(String lob) {
        String detail = lob == null ? "lob is required" : "Unsupported lob: " + lob + " (TERM only)";
        return new ServiceException(ServiceErrorResponse.builder()
                .title("Unsupported LOB")
                .status(422)
                .detail(detail)
                .code(ErrorCodes.UNSUPPORTED_LOB)
                .retryable(false)
                .addError(ServiceError.ofField(ErrorCodes.UNSUPPORTED_LOB, detail, "lob"))
                .build());
    }

    private record ParsedFields(
            String applicationStatus,
            String applicationStatusDesc,
            String manufacturerAppStatus,
            String manufacturerAppStatusDesc,
            String policyNo,
            String insuranceCompanyCode
    ) {}
}
