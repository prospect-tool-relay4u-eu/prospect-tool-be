package eu.relay4u.prospecting.mapper;

import eu.relay4u.prospecting.configuration.MapperConfig;
import eu.relay4u.prospecting.dto.UserDto;
import eu.relay4u.prospecting.dto.login.LoginRequest;
import eu.relay4u.prospecting.dto.register.RegisterRequest;
import eu.relay4u.prospecting.model.User;
import org.mapstruct.Mapper;

@Mapper(config = MapperConfig.class)
public interface UserMapper {
    User toEntity(UserDto dto);

    User toEntity(RegisterRequest request);

    User toEntity(LoginRequest request);

    UserDto toDto(User entity);
}
