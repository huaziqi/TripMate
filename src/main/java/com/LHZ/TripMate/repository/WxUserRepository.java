package com.LHZ.TripMate.repository;

import com.LHZ.TripMate.entity.WxUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WxUserRepository extends JpaRepository<WxUser, Long> {
    Optional<WxUser> findByOpenid(String openid);
}
