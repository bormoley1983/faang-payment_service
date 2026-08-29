package faang.school.paymentservice;

import faang.school.paymentservice.service.id.UuidCreatorV7Generator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UuidCreatorV7GeneratorTest {

    @Test
    void generatesVersionSevenUuid() {
        assertThat(new UuidCreatorV7Generator().generate().version()).isEqualTo(7);
    }
}
