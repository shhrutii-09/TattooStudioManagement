package entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
public class LoginHistory implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private AppUser user;

    @Column(name = "LOGIN_TIMESTAMP", nullable = false)
    private LocalDateTime loginTimestamp;

    @Column(name = "IP_ADDRESS", length = 45)
    private String ipAddress;

    public LoginHistory() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public LocalDateTime getLoginTimestamp() { return loginTimestamp; }
    public void setLoginTimestamp(LocalDateTime loginTimestamp) { this.loginTimestamp = loginTimestamp; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}