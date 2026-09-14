package ama.modulos.rg90;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface Rg90SecuenciaRepository extends JpaRepository<Rg90Secuencia, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Rg90Secuencia> findByClave(String clave);
}
