package eu.nimble.core.infrastructure.identity.service;

import eu.nimble.core.infrastructure.identity.repository.CertificateRepository;
import eu.nimble.service.model.ubl.commonaggregatecomponents.CertificateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by Johannes Innerbichler on 08.08.18.
 */
@Service
public class CertificateService {

    @Autowired
    private CertificateRepository certificateRepository;

    @Transactional
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public CertificateType queryCertificate(Long certificateId) {
        CertificateType certificateType = certificateRepository.findOne(certificateId);

        // pre fetch binary
        if (certificateType != null)
            certificateType.getDocumentReference().get(0).getAttachment().getEmbeddedDocumentBinaryObject().getValue();

        return certificateType;
    }
}
