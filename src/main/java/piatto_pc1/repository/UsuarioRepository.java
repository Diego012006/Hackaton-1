package piatto_pc1.repository;

import com.example.piatto_pc1.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Usuario findByNombreDeUsuario(String nombreDeUsuario);}