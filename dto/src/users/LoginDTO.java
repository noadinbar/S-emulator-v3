package users;

public class LoginDTO {
    public boolean ok;
    public String username;
    public String error;

    public LoginDTO() {}

    public boolean isOk() {
        return ok;
    }
    public String getUsername() {
        return username;
    }
    public String getError() {
        return error;
    }
}