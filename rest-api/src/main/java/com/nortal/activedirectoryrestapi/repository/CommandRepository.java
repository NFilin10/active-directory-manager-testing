package com.nortal.activedirectoryrestapi.repository;

import com.nortal.activedirectoryrestapi.entities.Commands;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommandRepository extends JpaRepository<Commands, Long> {
// not used but let it remain here for now
}
