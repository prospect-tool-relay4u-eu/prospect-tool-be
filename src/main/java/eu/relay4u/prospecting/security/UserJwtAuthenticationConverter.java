package eu.relay4u.prospecting.security;

import eu.relay4u.prospecting.model.User;
import eu.relay4u.prospecting.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Long userId = Long.valueOf(jwt.getSubject());
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        User user = userRepository.findById(userId).orElseGet(User::new);
        user.setId(userId);
        user.setEmail(email);
        user.setName(name);
        userRepository.save(user);

        return new UsernamePasswordAuthenticationToken(user, jwt, List.of());
    }
}
