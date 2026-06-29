package pe.bancoconfianza.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "usuarios",
       uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nombre;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @Column(unique = true, length = 8)
    private String dni;

    @Column(name = "numero_tarjeta", unique = true, length = 16)
    private String numeroTarjeta;

    @Column(length = 15)
    private String telefono;

    @Column(name = "fecha_nacimiento")
    private java.time.LocalDate fechaNacimiento;

    @Column(length = 255)
    private String direccion;

    @Column(length = 100)
    private String ocupacion;

    @Column(length = 100)
    private String profesion;

    @Column(name = "centro_laboral", length = 200)
    private String centroLaboral;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol = Rol.CLIENTE;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "cuenta_creada", nullable = false)
    private boolean cuentaCreada = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /* ── Enum ── */
    public enum Rol { CLIENTE, ASESOR, JEFE_REGIONAL, RIESGOS, COMITE, GERENCIA, ADMIN }

    /* ── UserDetails ── */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
    }

    @Override public String getPassword()  { return password; }
    @Override public String getUsername()  { return email; }
    @Override public boolean isAccountNonExpired()   { return true; }
    @Override public boolean isAccountNonLocked()    { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()             { return activo; }

    /* ── Getters / Setters ── */
    public Long getId()                    { return id; }
    public void setId(Long id)             { this.id = id; }

    public String getNombre()              { return nombre; }
    public void setNombre(String nombre)   { this.nombre = nombre; }

    public String getEmail()               { return email; }
    public void setEmail(String email)     { this.email = email; }

    public void setPassword(String pw)     { this.password = pw; }

    public Rol getRol()                    { return rol; }
    public void setRol(Rol rol)            { this.rol = rol; }

    public boolean isActivo()              { return activo; }
    public void setActivo(boolean activo)  { this.activo = activo; }

    public LocalDateTime getCreatedAt()    { return createdAt; }

    public String getDni()                 { return dni; }
    public void setDni(String dni)         { this.dni = dni; }

    public String getNumeroTarjeta()       { return numeroTarjeta; }
    public void setNumeroTarjeta(String numeroTarjeta) { this.numeroTarjeta = numeroTarjeta; }

    public String getTelefono()            { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public java.time.LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(java.time.LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getDireccion()           { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getOcupacion()           { return ocupacion; }
    public void setOcupacion(String ocupacion) { this.ocupacion = ocupacion; }

    public String getProfesion()           { return profesion; }
    public void setProfesion(String profesion) { this.profesion = profesion; }

    public String getCentroLaboral()       { return centroLaboral; }
    public void setCentroLaboral(String centroLaboral) { this.centroLaboral = centroLaboral; }

    public boolean isCuentaCreada()        { return cuentaCreada; }
    public void setCuentaCreada(boolean cuentaCreada) { this.cuentaCreada = cuentaCreada; }
}
