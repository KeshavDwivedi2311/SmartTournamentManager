package com.sports.SmartSport.player.entity;

import com.sports.SmartSport.team.entity.Team;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

//    @Column(name = "email")
//    private String email;
//
//    @Column(name = "phone")
//    private String phone;
//
//    @Column(name = "position")
//    private String position; // "CAPTAIN", "PLAYER", "SUBSTITUTE"
//
//    @Column(name = "jersey_number")
//    private Integer jerseyNumber;
//
//    @Column(name = "date_of_birth")
//    private LocalDate dateOfBirth;
//
//    @Column(name = "registration_date")
//    private LocalDate registrationDate;

    // Many players belong to one team
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

}