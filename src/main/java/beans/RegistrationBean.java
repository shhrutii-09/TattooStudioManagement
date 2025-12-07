package beans;

import dto.UserRegisterDTO;
import entities.AppUser;
import ejb.AuthEJB;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.validator.ValidatorException;
import jakarta.inject.Named;
import java.util.regex.Pattern;
import util.PasswordUtil;

@Named
@RequestScoped
public class RegistrationBean {

    private UserRegisterDTO registerDTO = new UserRegisterDTO();
    private String confirmPassword;

    @EJB
    private AuthEJB authEJB;

    public String register() {
        try {
            if (registerDTO.getRole() == null || registerDTO.getRole().isEmpty()) {
                addError("Please select a role");
                return null;
            }

            if (!registerDTO.getPassword().equals(confirmPassword)) {
                addFieldError("registerForm:pwd2", "Passwords do not match!");
                return null;
            }

            String hashedPassword = PasswordUtil.hashPassword(registerDTO.getPassword());

            AppUser user = new AppUser();
            user.setUsername(registerDTO.getUsername());
            user.setEmail(registerDTO.getEmail());
            user.setFullName(registerDTO.getFullName());
            user.setPhone(registerDTO.getPhone());
            user.setPassword(hashedPassword);

            authEJB.registerUser(user, registerDTO.getRole());

            addInfo("Registration successful! Please log in.");

            // Keep success message after redirect
            FacesContext.getCurrentInstance()
            .getExternalContext()
            .getFlash()
            .setKeepMessages(true);

//            return "/login.xhtml?faces-redirect=true";
            return null;
        } catch (Exception e) {
            addError("Registration failed: " + e.getMessage());
            return null;
        }
    }

    private void addFieldError(String clientId, String msg) {
        FacesContext.getCurrentInstance().addMessage(clientId,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    private void addError(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, null));
    }

    private void addInfo(String msg) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, msg, null));
    }

    // Validators...
    public void validateEmail(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {

        String email = (String) value;
        if (!Pattern.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", email)) {
            throw new ValidatorException(new FacesMessage("Invalid email format"));
        }
    }

    public void validatePhone(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {

        String phone = (String) value;
        String cleaned = phone.replaceAll("[\\s\\-()]+", "");
        if (!cleaned.matches("\\d{10,15}")) {
            throw new ValidatorException(new FacesMessage("Phone must be 10-15 digits"));
        }
    }

    public void validatePassword(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {

        String password = (String) value;

        if (password.length() < 6)
            throw new ValidatorException(new FacesMessage("Password too short (min 6)"));

        if (!password.matches(".*\\d.*"))
            throw new ValidatorException(new FacesMessage("Password needs a number"));

        if (!password.matches(".*[a-zA-Z].*"))
            throw new ValidatorException(new FacesMessage("Password needs a letter"));
    }

    public UserRegisterDTO getRegisterDTO() { return registerDTO; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
