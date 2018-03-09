package uk.gov.hmcts.reform.slc.services.steps.getpdf;

import org.apache.http.util.Asserts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pdf.generator.HTMLToPDFConverter;
import uk.gov.hmcts.reform.slc.logging.AppInsights;
import uk.gov.hmcts.reform.slc.model.Document;
import uk.gov.hmcts.reform.slc.model.Letter;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.duplex.DuplexPreparator;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Service
public class PdfCreator {

    @Autowired
    private AppInsights insights;

    private final DuplexPreparator duplexPreparator;

    public PdfCreator(DuplexPreparator duplexPreparator) {
        this.duplexPreparator = duplexPreparator;
    }

    private static HTMLToPDFConverter converter = new HTMLToPDFConverter();

    public static synchronized byte[] generatePdf(byte[] template, Map<String, Object> content) {
        return converter.convert(template, content);
    }

    private byte[] generatePdf(Document document) {
        Instant start = Instant.now();
        byte[] pdf = generatePdf(document.template.getBytes(), document.values);
        insights.trackPdfGenerator(Duration.between(start, Instant.now()), true);
        return pdf;
    }

    public PdfDoc create(Letter letter) {
        Asserts.notNull(letter, "letter");

        List<byte[]> docs =
            letter.documents
                .stream()
                .map(this::generatePdf)
                .map(duplexPreparator::prepare)
                .collect(toList());

        byte[] finalContent = PdfMerger.mergeDocuments(docs);

        return new PdfDoc(
            FileNameHelper.generateName(letter, "pdf"),
            finalContent
        );
    }
}
