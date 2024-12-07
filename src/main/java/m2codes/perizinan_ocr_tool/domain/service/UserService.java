package m2codes.perizinan_ocr_tool.domain.service;

import m2codes.perizinan_ocr_tool.domain.model.User;
import m2codes.perizinan_ocr_tool.interfaces.dto.request.UserDataRequest;

import java.util.List;

public interface UserService {

    User save(UserDataRequest request);

    User findByUsername(String username);

    List<User> getUsers();

    void updateLastLoginById(String id);

    void deleteById(String id);

}