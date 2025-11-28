package com.tradeplatform.tradeservice.repository;

import com.tradeplatform.tradeservice.model.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    
    List<Trade> findByTradeId(String tradeId);
    
    Optional<Trade> findByTradeIdAndVersion(String tradeId, Integer version);
    
    @Query("SELECT MAX(t.version) FROM Trade t WHERE t.tradeId = :tradeId")
    Optional<Integer> findMaxVersionByTradeId(@Param("tradeId") String tradeId);
    
    @Query("SELECT t FROM Trade t WHERE t.tradeId = :tradeId AND t.version = " +
           "(SELECT MAX(t2.version) FROM Trade t2 WHERE t2.tradeId = :tradeId)")
    Optional<Trade> findLatestByTradeId(@Param("tradeId") String tradeId);
    
    List<Trade> findByBookId(String bookId);
    
    List<Trade> findByStatus(String status);
    
    @Query("SELECT t FROM Trade t WHERE t.maturityDate < CURRENT_DATE AND t.expired = 'N'")
    List<Trade> findMaturedTrades();
}

