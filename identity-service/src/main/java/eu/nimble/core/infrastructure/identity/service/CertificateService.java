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
    public CertificateType queryCertificate(Long certificateId) {
        CertificateType certificateType = certificateRepository.findOne(certificateId);

        // pre fetch binary (legacy due to dedicated binary database)
        if (certificateType != null && certificateType.getDocumentReference() != null && certificateType.getDocumentReference().isEmpty() == false)
            certificateType.getDocumentReference().get(0).getAttachment().getEmbeddedDocumentBinaryObject().getValue();

        return certificateType;
    }
}
