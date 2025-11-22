package entities;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;

@Entity
@Table(name = "group_master")
public class GroupMaster implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ROLEID")
    private Integer roleId;

    @Column(name = "ROLENAME", nullable = false, unique = true, length = 100)
    private String roleName;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @JsonbTransient 
    @OneToMany(mappedBy = "role", fetch = FetchType.LAZY)
    private List<AppUser> users;

    public GroupMaster() {}

    public Integer getRoleId() { return roleId; }
    public void setRoleId(Integer roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<AppUser> getUsers() { return users; }
    public void setUsers(List<AppUser> users) { this.users = users; }
}