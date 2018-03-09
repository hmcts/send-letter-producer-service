package uk.gov.hmcts.reform.sendletter.entity;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.UUID;

public interface LetterRepository extends CrudRepository<Letter, UUID> {
    // Find letters in a specific state.
    List<Letter> findByState(LetterState state);

    // Set the state of a specific letter.
    int updateState(UUID letterId, LetterState state);
}
