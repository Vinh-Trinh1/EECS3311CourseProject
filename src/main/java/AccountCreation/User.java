package AccountCreation;

import DashboardCategories.NonMatchingPasswordException;

public class User{

 
    private final String email; 
    private final String phoneNumber; 
    private final String password; 
    private final String confirmationOfPassword; 
    private  String preferredContactMethod; 
    public User(String email, String phoneNumber, String password, String confirmationOfPassword, String preferredContactMethod){
        this.email = email; 
        this.phoneNumber = phoneNumber; 
        this.password = password; 
        this.confirmationOfPassword = confirmationOfPassword; 
        this.preferredContactMethod = preferredContactMethod; 
    }


    /* Precondition: 2 inputs, password and passwordconfirmation
    Postcondition: password match OR exception is thrown */
    public boolean passwordMatch(String password, String confirmationOfPassword) throws NonMatchingPasswordException {
        if (!password.equals(confirmationOfPassword)){
            throw new NonMatchingPasswordException("Passwords do not match. Please make sure that your passwords match."); 
        } else {
            return true; 
        }
        
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }


    public String getPassword() {
        return password;
    }

    public String getConfirmationOfPassword() {
        return confirmationOfPassword;
    }


    public String getPreferredContactMethod() {
        return preferredContactMethod;
    }

    public void setPreferredContactMethod(String preferredContactMethod) {
        this.preferredContactMethod = preferredContactMethod;
    }

}