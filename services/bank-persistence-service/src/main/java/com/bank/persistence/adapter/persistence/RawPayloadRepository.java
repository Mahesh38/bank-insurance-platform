package com.bank.persistence.adapter.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawPayloadRepository extends JpaRepository<RawPayloadEntity, String> {

  List<RawPayloadEntity> findByJobId(String jobId);
}
