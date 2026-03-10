package swp.be.vn.bs.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import swp.be.vn.bs.entity.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequest {
    private Role role;
}
