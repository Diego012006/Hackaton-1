package piatto_pc1.security;

public class JwtAuthenticationResponse {
    private String token;

    public JwtAuthenticationResponse() {}

    public JwtAuthenticationResponse(String token) {
        this.token = token;}

    public String getToken() {
        return token;}

    public void setToken(String token) {
        this.token = token;}}