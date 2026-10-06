package com.tata.carelink.application.internal.queryservices;

import com.tata.carelink.application.internal.CareLinkApplicationException;
import com.tata.carelink.application.internal.CareLinkMapper;
import com.tata.carelink.application.models.CareLinkResult;
import com.tata.carelink.application.models.OlderAdultProfileResult;
import com.tata.carelink.application.queryservices.CareLinkQueryService;
import com.tata.carelink.domain.repositories.CareLinkRepository;
import com.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CareLinkQueryServiceImpl implements CareLinkQueryService {
    private final CareLinkRepository careLinkRepository;
    private final OlderAdultProfileRepository olderAdultRepository;

    public CareLinkQueryServiceImpl(
            CareLinkRepository careLinkRepository,
            OlderAdultProfileRepository olderAdultRepository
    ) {
        this.careLinkRepository = careLinkRepository;
        this.olderAdultRepository = olderAdultRepository;
    }

    @Override
    public CareLinkResult getById(String careLinkId) {
        return careLinkRepository.findById(careLinkId)
                .map(CareLinkMapper::toResult)
                .orElseThrow(() -> new CareLinkApplicationException(
                        CareLinkApplicationException.Code.CARE_LINK_NOT_FOUND,
                        "care link not found"
                ));
    }

    @Override
    public OlderAdultProfileResult getOlderAdult(String olderAdultId) {
        return olderAdultRepository.findById(olderAdultId)
                .map(CareLinkMapper::toResult)
                .orElseThrow(() -> new CareLinkApplicationException(
                        CareLinkApplicationException.Code.OLDER_ADULT_NOT_FOUND,
                        "older adult profile not found"
                ));
    }

    @Override
    public boolean isAuthorized(String caregiverId, String olderAdultId) {
        return careLinkRepository.findConfirmed(caregiverId, olderAdultId)
                .map(link -> link.isActive())
                .orElse(false);
    }
}
