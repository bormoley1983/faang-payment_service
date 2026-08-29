package faang.school.paymentservice.service.id;

import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidCreatorV7Generator implements UuidV7Generator {

    @Override
    public UUID generate() {
        return UuidCreator.getTimeOrderedEpoch();
    }
}
