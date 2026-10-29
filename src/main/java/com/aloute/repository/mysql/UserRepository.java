package com.aloute.repository.mysql;

import com.aloute.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.aloute.model.enums.Role;
import com.aloute.model.enums.UserStatus;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);

    @Query("""
            select u from User u left join u.userInfo info
            where (:keyword is null
                or lower(u.email) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(info.fullname, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(info.nickname, '')) like lower(concat('%', :keyword, '%')))
            """)
    Page<User> searchAdminUsers(@Param("keyword") String keyword, Pageable pageable);

    long countByStatus(UserStatus status);

    long countByRoleAndStatus(Role role, UserStatus status);

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}