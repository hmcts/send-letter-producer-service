package uk.gov.hmcts.reform.sendletter.entity;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface LetterRepository extends CrudRepository<Letter, UUID> {
}
