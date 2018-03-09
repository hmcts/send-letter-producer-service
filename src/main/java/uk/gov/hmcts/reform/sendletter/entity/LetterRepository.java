package uk.gov.hmcts.reform.sendletter.entity;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface LetterRepository extends CrudRepository<Letter, UUID> {
    List<Letter> findByState(LetterState state);
}
