package com.pro.Journal_Entry.repository;

import com.pro.Journal_Entry.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role,Long> {
    Optional<Role> findByRole(String name);

    Optional<Role> findByName(String name);
}
