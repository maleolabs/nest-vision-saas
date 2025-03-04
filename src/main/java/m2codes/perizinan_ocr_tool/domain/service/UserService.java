package m2codes.perizinan_ocr_tool.domain.service;

import m2codes.perizinan_ocr_tool.domain.model.Role;
import m2codes.perizinan_ocr_tool.domain.model.User;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.UserDataRequest;

import java.util.List;
import java.util.Optional;

public interface UserService {

    User save(UserDataRequest request, Role role);

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    List<User> getUsers();

    void updateLastLoginById(String id);

    void deleteById(String id);

    void activateUser(String username);

    void disableUser(String username);

    void lockUser(String username);

}