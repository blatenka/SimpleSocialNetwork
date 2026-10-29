package com.aloute.repository.mysql;

import com.aloute.model.entity.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Long> {
    
    List<UserInfo> findByFullnameContainingIgnoreCaseOrNicknameContainingIgnoreCase(String fullname, String nickname);
    List<UserInfo> findByFullnameContainingIgnoreCase(String fullname);
}