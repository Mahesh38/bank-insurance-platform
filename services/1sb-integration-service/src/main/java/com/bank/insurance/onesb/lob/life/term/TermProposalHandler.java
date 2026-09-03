package com.bank.insurance.onesb.lob.life.term;

import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.SubmitProposalCommand;
import com.bank.common.domain.Lob;
import com.bank.insurance.onesb.lob.LobProposalHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Term LOB proposal handler — schema path, submit path, and payload build.
 * <p>
 * Schema: {@code GET /insurance/lifeterm/v1/proposal?productId=&manufacturerId=&version=}
 * (portal retail Term proposal form — not {@code /proposal/form})
 * Submit: {@code POST /insurance/lifeterm/v1/proposal}
 * Poll: {@code GET /insurance/lifeterm/v1/proposal/poll/{reqId}}
 */
@Component
public class TermProposalHandler implements LobProposalHandler {

    static final String SCHEMA_PATH = "/insurance/lifeterm/v1/proposal";
    static final String SUBMIT_PATH = "/insurance/lifeterm/v1/proposal";
    static final String POLL_PATH_PREFIX = "/insurance/lifeterm/v1/proposal/poll/";

    private final SecretProvider secretProvider;

    public TermProposalHandler(SecretProvider secretProvider) {
        this.secretProvider = secretProvider;
    }

    @Override
    public Lob supportedLob() {
        return Lob.TERM;
    }

    @Override
    public String schemaPath(String productCode, String manufacturerId, String version) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(SCHEMA_PATH);
        if (productCode != null && !productCode.isBlank()) {
            // Portal query param is productId (slug: …-product-id-product-id-…).
            builder.queryParam("productId", productCode);
        }
        if (manufacturerId != null && !manufacturerId.isBlank()) {
            builder.queryParam("manufacturerId", manufacturerId);
        }
        if (version != null && !version.isBlank()) {
            builder.queryParam("version", version);
        }
        return builder.build().encode().toUriString();
    }

    @Override
    public String submitPath() {
        return SUBMIT_PATH;
    }

    @Override
    public String pollPath(String externalReqId) {
        return POLL_PATH_PREFIX + externalReqId;
    }

    /**
     * Builds 1SB submit body from the flat schema {@code values} map and injects
     * distributor attribution from {@link SecretProvider} (never from the client).
     */
    @Override
    public Object buildSubmitPayload(SubmitProposalCommand command) {
        Map<String, Object> root = new LinkedHashMap<>();
        if (command.values() != null) {
            root.putAll(command.values());
        }

        // Strip any client-supplied distributor identity (COMP-004 / D7)
        root.remove("distributorId");
        root.remove("distributorID");

        Map<String, Object> distributor = new LinkedHashMap<>();
        Object existing = root.get("distributor");
        if (existing instanceof Map<?, ?> map) {
            map.forEach((k, v) -> distributor.put(String.valueOf(k), v));
        }
        distributor.remove("distributorId");
        distributor.remove("distributorID");
        distributor.put("distributorID", secretProvider.getDistributorId());
        distributor.put("agentID", resolveAgentId(command));
        String channel = resolveChannelType(command);
        if (StringUtils.hasText(channel)) {
            distributor.put("channelType", channel);
        }
        root.put("distributor", distributor);

        if (StringUtils.hasText(command.productCode())) {
            root.putIfAbsent("productCode", command.productCode());
        }
        if (StringUtils.hasText(command.manufacturerId())) {
            root.putIfAbsent("manufacturerId", command.manufacturerId());
        }
        if (StringUtils.hasText(command.version())) {
            root.putIfAbsent("version", command.version());
        }
        if (StringUtils.hasText(command.offerId())) {
            root.putIfAbsent("offerId", command.offerId());
        }
        if (StringUtils.hasText(command.schemaId())) {
            root.putIfAbsent("schemaId", command.schemaId());
        }

        return root;
    }

    public static String resolveAgentId(SubmitProposalCommand command) {
        if (StringUtils.hasText(command.agentId())) {
            return command.agentId().trim();
        }
        if (command.distribution() != null && StringUtils.hasText(command.distribution().agentId())) {
            return command.distribution().agentId().trim();
        }
        return "";
    }

    private static String resolveChannelType(SubmitProposalCommand command) {
        if (command.distribution() != null
                && StringUtils.hasText(command.distribution().channelType())) {
            return command.distribution().channelType().trim();
        }
        return "B2B";
    }
}
