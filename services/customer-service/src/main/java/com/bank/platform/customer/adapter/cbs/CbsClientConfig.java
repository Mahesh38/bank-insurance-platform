package com.bank.platform.customer.adapter.cbs;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CbsInquiryProperties.class)
public class CbsClientConfig {}
