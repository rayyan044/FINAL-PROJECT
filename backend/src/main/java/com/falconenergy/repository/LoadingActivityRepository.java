package com.falconenergy.repository;

import com.falconenergy.entity.LoadingActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoadingActivityRepository extends JpaRepository<LoadingActivity, Long> {
}
