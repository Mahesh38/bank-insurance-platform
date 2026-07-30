package com.bank.insurance.persistence.persistence.repo;

import com.bank.insurance.persistence.persistence.entity.JobPollAttemptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobPollAttemptRepository extends JpaRepository<JobPollAttemptEntity, Long> {

    List<JobPollAttemptEntity> findByJobIdOrderByAttemptNumberAsc(String jobId);
}
