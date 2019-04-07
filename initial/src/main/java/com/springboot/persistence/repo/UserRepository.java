package com.springboot.persistence.repo;

import com.springboot.persistence.model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepository extends CrudRepository<User, Long> {

    List<User> findByUsername(String uName);
}
