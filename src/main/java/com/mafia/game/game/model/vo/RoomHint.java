package com.mafia.game.game.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RoomHint {
    private int roomNo;
    private String userName;
    private String hint;
    private String userNick;
}
