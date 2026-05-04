package com.library.model;

public class User {
    private int id;
    private String memberId;
    private String email;
    private String password;
    private String name;
    private String createdAt;

    public User() {}

    public User(int id, String memberId, String email, String password, String name, String createdAt) {
        this.id = id;
        this.memberId = memberId;
        this.email = email;
        this.password = password;
        this.name = name;
        this.createdAt = createdAt;
    }

    public int getId()           { return id; }
    public String getMemberId()  { return memberId; }
    public String getEmail()     { return email; }
    public String getPassword()  { return password; }
    public String getName()      { return name; }
    public String getCreatedAt() { return createdAt; }

    public void setId(int id)                 { this.id = id; }
    public void setMemberId(String memberId)  { this.memberId = memberId; }
    public void setEmail(String email)        { this.email = email; }
    public void setPassword(String password)  { this.password = password; }
    public void setName(String name)          { this.name = name; }
    public void setCreatedAt(String createdAt){ this.createdAt = createdAt; }

    // Display name: use part before @ as username if no name set
    public String getDisplayName() {
        if (name != null && !name.isBlank()) return name;
        return email.split("@")[0];
    }
}
