package com.bank.persistence.adapter.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobPollAttemptRepository extends JpaRepository<JobPollAttemptEntity, Long> {

  List<JobPollAttemptEntity> findByJobIdOrderByAttemptNumberAsc(String jobId);
}
