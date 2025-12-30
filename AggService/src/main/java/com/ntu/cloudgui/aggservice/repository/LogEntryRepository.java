package com.ntu.cloudgui.aggservice.repository;

import com.ntu.cloudgui.aggservice.model.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {
}
