-- FUNC-035 / I1 — persist policy term (PT) and premium paying term (PPT) on job offers.
ALTER TABLE integration_job_offer ADD COLUMN cover_term INTEGER;
ALTER TABLE integration_job_offer ADD COLUMN premium_paying_term INTEGER;
