package piatto_pc1.security;

import com.example.piatto_pc1.domain.Role;
import com.example.piatto_pc1.exception.BadRequest;

public class SigninRequest {
    private String email;
    private String password;
    private String role;

    public SigninRequest() {}

    public SigninRequest(String email, String password, String role) {
        this.email = email;
        this.password = password;
        if (!Role.contiene(role)) {
            throw new BadRequest("ingresa como CLIENTE o COCINERO");}
        this.role = role;}

    public String getEmail() {
        return email;}

    public void setEmail(String email) {
        this.email = email;}

    public String getPassword() {
        return password;}

    public void setPassword(String password) {
        this.password = password;}

    public String getRole() {
        return role;}

    public void setRole(String role) {
        if (!Role.contiene(role)) {
            throw new BadRequest("ingresa como CLIENTE o COCINERO");}
        this.role = role;}}