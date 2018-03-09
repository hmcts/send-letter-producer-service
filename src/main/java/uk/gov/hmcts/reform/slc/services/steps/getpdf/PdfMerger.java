package uk.gov.hmcts.reform.slc.services.steps.getpdf;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import uk.gov.hmcts.reform.slc.services.servicebus.exceptions.PdfMergeException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.apache.pdfbox.io.MemoryUsageSetting.setupMainMemoryOnly;

public final class PdfMerger {

    private PdfMerger() {
        // utility class constructor
    }

    public static byte[] mergeDocuments(List<byte[]> documents) {
        if (documents.size() == 1) {
            return documents.get(0);
        }

        ByteArrayOutputStream docOutputStream = new ByteArrayOutputStream();

        List<InputStream> inputStreams = documents.stream()
            .map(ByteArrayInputStream::new)
            .collect(toList());

        PDFMergerUtility pdfMergerUtility = new PDFMergerUtility();
        pdfMergerUtility.addSources(inputStreams);
        pdfMergerUtility.setDestinationStream(docOutputStream);

        try {
            pdfMergerUtility.mergeDocuments(setupMainMemoryOnly());
            return docOutputStream.toByteArray();
        } catch (IOException e) {
            throw new PdfMergeException("Exception occurred while merging PDF files", e);
        }
    }
}
