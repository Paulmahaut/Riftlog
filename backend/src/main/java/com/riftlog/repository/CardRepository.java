package com.riftlog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.riftlog.entity.Card;

public interface CardRepository extends JpaRepository<Card, Long> {

    /**
     * Empty strings mean "no filter". Never pass null: PostgreSQL cannot type a null parameter
     * inside lower() and fails with "function lower(bytea) does not exist".
     */
    @Query("""
            select c from Card c
            where (:type = '' or c.cardType = :type)
              and lower(c.name) like concat('%', :query, '%')
              and (:includeVariants = true or c.canonical = true)
            order by c.baseName, c.riftboundId
            """)
    List<Card> search(@Param("type") String type, @Param("query") String lowerCaseQuery,
            @Param("includeVariants") boolean includeVariants);
}
