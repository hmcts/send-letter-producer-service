package uk.gov.hmcts.reform.sendletter.services;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.Letter;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class LetterChecksumGeneratorTest {

    @Test
    public void should_return_same_md5_checksum_hex_for_same_letter_objects() {

        Letter letter1 = new Letter(
            singletonList(new Document(
                "cmc-template",
                ImmutableMap.of(
                    "key11", "value11",
                    "key21", "value21"
                )
            )),
            "print-job-1234",
            ImmutableMap.of(
                "doc_type", "my doc type",
                "caseId", "123"
            )
        );

        Letter letter2 = new Letter(
            singletonList(new Document(
                "cmc-template",
                ImmutableMap.of(
                    "key11", "value11",
                    "key21", "value21"
                )
            )),
            "print-job-1234",
            ImmutableMap.of(
                "doc_type", "my doc type",
                "caseId", "123"
            )
        );

        assertThat(LetterChecksumGenerator.generateChecksum(letter1))
            .isEqualTo(LetterChecksumGenerator.generateChecksum(letter2));
    }

    @Test
    public void should_return_different_md5_checksum_hex_for_different_letter_objects() {

        Letter letter1 = new Letter(
            singletonList(new Document(
                "cmc-template",
                ImmutableMap.of(
                    "key11", "value11",
                    "key12", "value12")
            )),
            "print-job-1234",
            ImmutableMap.of(
                "doc_type", "my doc type",
                "caseId", "123"
            )
        );

        Letter letter2 = new Letter(
            singletonList(new Document(
                "cmc-template",
                ImmutableMap.of(
                    "key21", "key21",
                    "key22", "value22")
            )),
            "print-job-1234",
            ImmutableMap.of(
                "doc_type", "my doc type",
                "caseId", "123"
            )
        );

        assertThat(LetterChecksumGenerator.generateChecksum(letter1))
            .isNotEqualTo(LetterChecksumGenerator.generateChecksum(letter2));
    }
}
