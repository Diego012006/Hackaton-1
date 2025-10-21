package piatto_pc1.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Usuario implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nombreDeUsuario;

    private String nombreCompleto;

    private String correo;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    // no tocar aquí abajo

    private Boolean expired = false;

    private Boolean locked = false;

    private Boolean credentialsExpired = false;

    private Boolean enable = true;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));}

    @Override
    public String getUsername() {
        return nombreDeUsuario;}

    @Override
    public boolean isAccountNonExpired() {
        return !expired;}

    @Override
    public boolean isAccountNonLocked() {
        return !locked;}

    @Override
    public boolean isCredentialsNonExpired() {
        return !credentialsExpired;}

    @Override
    public boolean isEnabled() {
        return enable;}}