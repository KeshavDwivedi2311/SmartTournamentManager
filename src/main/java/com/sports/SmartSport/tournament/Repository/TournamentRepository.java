package com.sports.SmartSport.tournament.Repository;

import com.sports.SmartSport.tournament.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    @Query("SELECT t from Tournament t WHERE t.title LIKE %:name%")
    List<Tournament> findByName(@Param("Title") String title);

    //ToDo: check if later how this is supposed to work
    List<Tournament> findByStatus(com.sports.SmartSport.tournament.entity.TournamentStatus status);
}
