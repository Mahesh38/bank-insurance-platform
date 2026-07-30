package com.bank.persistence.repo;

import com.bank.persistence.entity.RawPayloadEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RawPayloadRepository extends JpaRepository<RawPayloadEntity, String> {

    List<RawPayloadEntity> findByJobId(String jobId);
}
