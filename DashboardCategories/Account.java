package DashboardCategories;

public class Account {

    private String email; 
    private String phoneNumber; 
    private String password; 
    private String confirmationOfPassword; 
    private String preferredContactMethod; 
    public Account(String email, String phoneNumber, String password, String confirmationOfPassword, String preferredContactMethod){
        this.email = email; 
        this.phoneNumber = phoneNumber; 
        this.password = password; 
        this.confirmationOfPassword = confirmationOfPassword; 
        this.preferredContactMethod = preferredContactMethod; 
    }

    public boolean passwordMatch(String password, String confirmationOfPassword) throws NonMatchingPasswordException {
        if (password != confirmationOfPassword){
            throw new NonMatchingPasswordException("Passwords do not match. Please make sure that your passwords match."); 
        } 
        return true; 
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPreferredContactMethod() {
        return preferredContactMethod;
    }

    public void setPreferredContactMethod(String preferredContactMethod) {
        this.preferredContactMethod = preferredContactMethod;
    }

    
}
