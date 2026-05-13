package com.currencyconverter.repository;

import com.currencyconverter.entity.ConversionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversionHistoryRepository extends JpaRepository<ConversionHistory, Long> {
    Page<ConversionHistory> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
}
