public class User {
    private int userId;
    private String username;
    private String password;
    private String fullName;
    private String address;
    private String userType;  // "CUSTOMER", "EMPLOYEE", or "ADMIN"

    // Constructor
    public User(String username, String password, String fullName, String address, String userType) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.address = address;
        this.userType = userType;
    }

    // Getters and Setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
}