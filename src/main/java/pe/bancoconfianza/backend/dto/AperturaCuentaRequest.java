package pe.bancoconfianza.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class AperturaCuentaRequest {
    
    // Paso 1: Datos básicos y preferencias
    @NotBlank(message = "El tipo de cuenta es obligatorio")
    private String tipoCuenta; // DIGITAL, SUELDO, PREMIO, etc.
    
    private String moneda; // SOLES, DOLARES
    
    // Paso 2: Configuración de tarjeta (si aplica)
    @NotBlank(message = "El número de tarjeta es obligatorio")
    private String numeroTarjeta;
    
    private String tipoMoneda; // Para la tarjeta
    private String region;
    private String provincia;
    
    // Paso 3: Validación de identidad
    @NotBlank(message = "El DNI es obligatorio")
    private String dni;
    
    @NotBlank(message = "La clave es obligatoria")
    private String clave;
    
    private String captcha;
    
    // Paso 4: Autenticación adicional
    private String colegioProfesional;
    private String numerosRegistrados;
    private String nombreRegistrado;
    private String empresaRepresentante;
    
    // Paso 5: Datos personales finales
    private String ocupacion;
    private String profesion;
    private String centroLaboral;
    
    @NotBlank(message = "El email es obligatorio")
    private String email;
    
    private String telefono;
    private String direccion;
    
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate fechaNacimiento;
    
    @NotBlank(message = "El nombre completo es obligatorio")
    private String nombreCompleto;
    
    // Contraseña (se envía desde Paso 3)
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
    
    // Getters y Setters
    public String getTipoCuenta() { return tipoCuenta; }
    public void setTipoCuenta(String tipoCuenta) { this.tipoCuenta = tipoCuenta; }
    
    public String getMoneda() { return moneda; }
    public void setMoneda(String moneda) { this.moneda = moneda; }
    
    public String getNumeroTarjeta() { return numeroTarjeta; }
    public void setNumeroTarjeta(String numeroTarjeta) { this.numeroTarjeta = numeroTarjeta; }
    
    public String getTipoMoneda() { return tipoMoneda; }
    public void setTipoMoneda(String tipoMoneda) { this.tipoMoneda = tipoMoneda; }
    
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    
    public String getProvincia() { return provincia; }
    public void setProvincia(String provincia) { this.provincia = provincia; }
    
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    
    public String getCaptcha() { return captcha; }
    public void setCaptcha(String captcha) { this.captcha = captcha; }
    
    public String getColegioProfesional() { return colegioProfesional; }
    public void setColegioProfesional(String colegioProfesional) { this.colegioProfesional = colegioProfesional; }
    
    public String getNumerosRegistrados() { return numerosRegistrados; }
    public void setNumerosRegistrados(String numerosRegistrados) { this.numerosRegistrados = numerosRegistrados; }
    
    public String getNombreRegistrado() { return nombreRegistrado; }
    public void setNombreRegistrado(String nombreRegistrado) { this.nombreRegistrado = nombreRegistrado; }
    
    public String getEmpresaRepresentante() { return empresaRepresentante; }
    public void setEmpresaRepresentante(String empresaRepresentante) { this.empresaRepresentante = empresaRepresentante; }
    
    public String getOcupacion() { return ocupacion; }
    public void setOcupacion(String ocupacion) { this.ocupacion = ocupacion; }
    
    public String getProfesion() { return profesion; }
    public void setProfesion(String profesion) { this.profesion = profesion; }
    
    public String getCentroLaboral() { return centroLaboral; }
    public void setCentroLaboral(String centroLaboral) { this.centroLaboral = centroLaboral; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
