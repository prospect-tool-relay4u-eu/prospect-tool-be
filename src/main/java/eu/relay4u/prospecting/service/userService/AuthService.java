package eu.relay4u.prospecting.service.userService;

import eu.relay4u.prospecting.dto.UserDto;
import eu.relay4u.prospecting.dto.login.AuthenticationResponse;
import eu.relay4u.prospecting.dto.login.LoginRequest;
import eu.relay4u.prospecting.dto.register.RegisterRequest;
import jakarta.validation.Valid;

public interface AuthService {
    UserDto register(@Valid RegisterRequest request);

    AuthenticationResponse login(@Valid LoginRequest request);
}
